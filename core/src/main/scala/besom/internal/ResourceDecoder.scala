package besom.internal

import com.google.protobuf.struct.Value
import scala.quoted.*
import scala.deriving.Mirror
import besom.util.*, Validated.ValidatedResult
import besom.internal.logging.*
import besom.types.{Label, URN, ResourceId}

trait ResourceDecoder[A <: Resource]: // TODO rename to something more sensible
  def makeResolver(using Context, BesomMDC[Label]): Result[(A, ResourceResolver[A])]

object ResourceDecoder:
  inline def derived[A <: Resource]: ResourceDecoder[A] = ${ derivedImpl[A] }

  class CustomPropertyExtractor[A](propertyName: String, decoder: Decoder[A]):

    // noinspection ScalaUnusedSymbol
    def extract(
      fields: Map[String, Value],
      dependencies: Map[String, Set[Resource]],
      resource: Resource
    )(using
      ctx: Context,
      mdc: BesomMDC[Label]
    ): ValidatedResult[DecodingError, OutputData[A]] =
      val resourceLabel = mdc.get(Key.LabelKey)

      val fieldDependencies = dependencies.getOrElse(propertyName, Set.empty)
      val propertyLabel     = resourceLabel.withKey(propertyName)

      val outputData =
        fields
          .get(NameUnmangler.unmanglePropertyName(propertyName))
          .map { value =>
            val decoded = decoder
              .decode(value, propertyLabel)
              .map(_.withDependency(resource))
              .tapBoth(
                err => log.debug(s"failed to extract custom property '$propertyName' from '$value': '$err'"),
                value => log.debug(s"extracted custom property '$propertyName' from '$value'")
              )

            ValidatedResult.transparent(decoded).in { result =>
              log.debug(s"extracting custom property '$propertyName' from '$value' using decoder '$decoder'") *> result
            }
          }
          .getOrElse {
            if ctx.isDryRun then ValidatedResult.valid(OutputData.unknown().withDependency(resource))
            // TODO: formatted DecodingError
            else
              ValidatedResult.invalid(
                DecodingError(s"Missing property '$propertyName' in resource '$resourceLabel'", label = propertyLabel)
              )
          }
          .map(_.withDependencies(fieldDependencies))

      outputData
    end extract

  end CustomPropertyExtractor

  def makeResolver[A <: Resource](
    fromProduct: Product => A,
    customPropertyExtractors: Vector[CustomPropertyExtractor[?]]
  )(using Context, BesomMDC[Label]): Result[(A, ResourceResolver[A])] =
    val customPropertiesCount = customPropertyExtractors.length
    val customPropertiesResults = Result.sequence(
      Vector.fill(customPropertiesCount)(Promise[OutputData[Option[Any]]]())
    )

    Promise[OutputData[URN]]().zip(Promise[OutputData[ResourceId]]()).zip(customPropertiesResults).map {
      case (urnPromise, idPromise, customPropertiesPromises) =>
        val allPromises = Vector(urnPromise, idPromise) ++ customPropertiesPromises.toList

        val propertiesOutputs = allPromises.map(promise => Output.ofData(promise.get)).toArray
        val resource          = fromProduct(Tuple.fromArray(propertiesOutputs))

        def failAllPromises(err: Throwable): Result[Unit] =
          Result
            .sequence(
              allPromises.map(_.fail(err))
            )
            .void
            .flatMap(_ => Result.fail(err))

        val resolver = new ResourceResolver[A]:
          def resolve(errorOrResourceResult: Either[Throwable, RawResourceResult])(using ctx: Context): Result[Unit] =
            errorOrResourceResult match
              case Left(err) =>
                val message =
                  s"Resolve resource: received error from gRPC call: '${err.getMessage}', failing resolution"

                ctx.logger.error(message) *> failAllPromises(err)

              case Right(rawResourceResult) =>
                val urnOutputData = OutputData(rawResourceResult.urn).withDependency(resource)

                // TODO what if id is a blank string? does this happen? wtf?
                val idOutputData = rawResourceResult.id.map(OutputData(_).withDependency(resource)).getOrElse {
                  OutputData.unknown().withDependency(resource)
                }

                val fields       = rawResourceResult.data.fields
                val dependencies = rawResourceResult.dependencies

                val propertiesFulfilmentResults =
                  customPropertiesPromises.zip(customPropertyExtractors).map { (promise, extractor) =>
                    extractor.extract(fields, dependencies, resource).asResult.flatMap {
                      case Validated.Valid(outputData) => promise.fulfillAny(outputData)
                      case Validated.Invalid(errs)     => promise.fail(AggregatedDecodingError(errs))
                    }
                  }

                val fulfilmentResults = Vector(
                  urnPromise.fulfill(urnOutputData),
                  idPromise.fulfill(idOutputData)
                ) ++ propertiesFulfilmentResults

                for
                  _ <- ctx.logger
                    .trace(s"Resolve resource: fulfilling ${fulfilmentResults.size} promises")
                  _ <- Result.sequence(fulfilmentResults).void
                  _ <- ctx.logger.debug(s"Resolve resource: resolved successfully")
                yield ()

        (resource, resolver)
    }
  end makeResolver

  private def derivedImpl[A <: Resource: Type](using q: Quotes): Expr[ResourceDecoder[A]] =
    Expr.summon[Mirror.Of[A]].get match
      case '{
            $m: Mirror.ProductOf[A] { type MirroredElemLabels = elementLabels; type MirroredElemTypes = elementTypes }
          } =>
        def prepareExtractors(elemLabels: Type[?], elemTypes: Type[?]): List[Expr[CustomPropertyExtractor[?]]] =
          (elemLabels, elemTypes) match
            case ('[EmptyTuple], '[EmptyTuple]) => Nil
            case ('[label *: labelsTail], '[Output[tpe] *: tpesTail]) =>
              val label          = Type.valueOfConstant[label].get.asInstanceOf[String]
              def tailExtractors = prepareExtractors(Type.of[labelsTail], Type.of[tpesTail])

              if label == "id" | label == "urn" then
                // Skipping `id` and `urn` properties as they're not custom properties
                tailExtractors
              else
                val propertyName = Expr(label)
                val propertyDecoder = Expr.summon[Decoder[tpe]].getOrElse {
                  quotes.reflect.report.errorAndAbort("Missing given instance of Decoder[" ++ Type.show[tpe] ++ "]")
                } // TODO: Handle missing decoder
                val extractor = '{ CustomPropertyExtractor(${ propertyName }, ${ propertyDecoder }) }
                extractor :: tailExtractors

        val customPropertyExtractorsExpr =
          Expr.ofList(prepareExtractors(Type.of[elementLabels], Type.of[elementTypes]))

        '{
          new ResourceDecoder[A]:
            def makeResolver(using Context, BesomMDC[Label]): Result[(A, ResourceResolver[A])] =
              ResourceDecoder.makeResolver(
                fromProduct = ${ m }.fromProduct,
                customPropertyExtractors = ${ customPropertyExtractorsExpr }.toVector
              )
        }
end ResourceDecoder
