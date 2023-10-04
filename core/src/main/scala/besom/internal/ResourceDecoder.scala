package besom.internal

import com.google.protobuf.struct.{Struct, Value}
import scala.quoted.*
import scala.deriving.Mirror
import scala.util.chaining.*
import besom.util.*
import besom.internal.logging.*
import besom.types.{Label, URN, ResourceId}

trait ResourceDecoder[A <: Resource]: // TODO rename to something more sensible
  def makeResolver(using Context, MDC[Label]): Result[(A, ResourceResolver[A])]

object ResourceDecoder:
  class CustomPropertyExtractor[A](propertyName: String, decoder: Decoder[A]):

    // TODO think if this could be pure (ie.: return Result)
    def extract(
      fields: Map[String, Value],
      dependencies: Map[String, Set[Resource]],
      resource: Resource
    )(using
      ctx: Context,
      mdc: MDC[Label]
    ): Validated[DecodingError, OutputData[A]] =
      val resourceLabel = mdc.get(Key.LabelKey)

      val fieldDependencies = dependencies.get(propertyName).getOrElse(Set.empty)
      val propertyLabel     = resourceLabel.withKey(propertyName)

      val outputData =
        fields
          .get(NameUnmangler.unmanglePropertyName(propertyName))
          .map { value =>
            log.trace(s"extracting custom property $propertyName from $value using decoder $decoder")
            decoder.decode(value, propertyLabel).map(_.withDependency(resource)) match
              case Validated.Invalid(err) =>
                log.trace(s"failed to extract custom property $propertyName from $value: $err")
                Validated.Invalid(err)
              case Validated.Valid(value) =>
                log.trace(s"extracted custom property $propertyName from $value")
                Validated.Valid(value)
          }
          .getOrElse {
            if ctx.isDryRun then Validated.valid(OutputData.unknown().withDependency(resource))
            // TODO: formatted DecodingError
            else
              Validated.invalid(DecodingError(s"Missing property $propertyName in resource $resourceLabel", label = propertyLabel))
          }
          .map(_.withDependencies(fieldDependencies))

      outputData

  end CustomPropertyExtractor

  def makeResolver[A <: Resource](
    fromProduct: Product => A,
    customPropertyExtractors: Vector[CustomPropertyExtractor[?]]
  )(using Context, MDC[Label]): Result[(A, ResourceResolver[A])] =
    val customPropertiesCount = customPropertyExtractors.length
    val customPropertiesResults = Result.sequence(
      Vector.fill(customPropertiesCount)(Promise[OutputData[Option[Any]]]())
    )

    Promise[OutputData[URN]]().zip(Promise[OutputData[ResourceId]]()).zip(customPropertiesResults).map {
      case (urnPromise, idPromise, customPopertiesPromises) =>
        val allPromises = Vector(urnPromise, idPromise) ++ customPopertiesPromises.toList

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
                  s"Resolve resource: received error from gRPC call: ${err.getMessage()}, failing resolution"

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
                  customPopertiesPromises.zip(customPropertyExtractors).map { (promise, extractor) =>
                    extractor.extract(fields, dependencies, resource) match
                      case Validated.Valid(outputData) => promise.fulfillAny(outputData)
                      case Validated.Invalid(errs)     => promise.fail(AggregatedDecodingError(errs))
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

  inline def derived[A <: Resource]: ResourceDecoder[A] = ${ derivedImpl[A] }

  def derivedImpl[A <: Resource: Type](using q: Quotes): Expr[ResourceDecoder[A]] =
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
            def makeResolver(using Context, MDC[Label]): Result[(A, ResourceResolver[A])] =
              ResourceDecoder.makeResolver(
                fromProduct = ${ m }.fromProduct,
                customPropertyExtractors = ${ customPropertyExtractorsExpr }.toVector
              )
        }
