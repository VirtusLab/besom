package besom.internal

import com.google.protobuf.struct.{Struct, Value}
import scala.quoted.*
import scala.deriving.Mirror

trait ResourceResolver[A]:
  def resolve(errorOrResourceResult: Either[Throwable, RawResourceResult])(using Context): Result[Unit]

trait ResourceDecoder[A <: Resource]: // TODO rename to something more sensible
  def makeResolver(using Context): Result[(A, ResourceResolver[A])]

object ResourceDecoder:
  class CustomPropertyExtractor[A](propertyName: String, decoder: Decoder[A]):
    def extract(fields: Map[String, Value], dependencies: Map[String, Set[Resource]], resource: Resource)(using
      ctx: Context
    ) =
      val fieldDependencies = dependencies.get(propertyName).getOrElse(Set.empty)

      val outputData = fields
        .get(propertyName)
        .map { value =>
          decoder.decode(value).map(_.withDependency(resource)) match
            case Left(err)    => throw err
            case Right(value) => value
        }
        .getOrElse {
          if ctx.isDryRun then OutputData.unknown().withDependency(resource)
          else OutputData.empty(Set(resource))
        }
        .withDependencies(fieldDependencies)

      outputData

  def makeResolver[A <: Resource](
    fromProduct: Product => A,
    customPropertyExtractors: Vector[CustomPropertyExtractor[?]]
  )(using Context): Result[(A, ResourceResolver[A])] =
    val customPropertiesCount = customPropertyExtractors.length
    val customPropertiesResults = Result.sequence(
      Vector.fill(customPropertiesCount)(Promise[OutputData[Option[Any]]])
    )

    Promise[OutputData[String]].zip(Promise[OutputData[String]]).zip(customPropertiesResults).map {
      case (urnPromise, idPromise, customPopertiesPromises) =>
        val allPromises = Vector(urnPromise, idPromise) ++ customPopertiesPromises.toList

        val propertiesOutputs = allPromises.map(promise => Output(promise.get)).toArray
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
              case Left(err) => failAllPromises(err)

              case Right(rawResourceResult) =>
                val urnOutputData = OutputData(rawResourceResult.urn).withDependency(resource)

                // TODO what if id is a blank string? does this happen? wtf?
                val idOutputData = rawResourceResult.id.map(OutputData(_).withDependency(resource)).getOrElse {
                  OutputData.unknown().withDependency(resource)
                }

                val fields       = rawResourceResult.data.fields
                val dependencies = rawResourceResult.dependencies

                try
                  val propertiesFulfilmentResults =
                    customPopertiesPromises.zip(customPropertyExtractors).map { (promise, extractor) =>
                      promise.fulfillAny(extractor.extract(fields, dependencies, resource))
                    }

                  val fulfilmentResults = Vector(
                    urnPromise.fulfill(urnOutputData),
                    idPromise.fulfill(idOutputData)
                  ) ++ propertiesFulfilmentResults

                  Result.sequence(fulfilmentResults).void
                catch case err: DecodingError => failAllPromises(err)

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
              val label = Type.valueOfConstant[label].get.asInstanceOf[String]
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
            def makeResolver(using Context): Result[(A, ResourceResolver[A])] =
              ResourceDecoder.makeResolver(
                fromProduct = ${ m }.fromProduct,
                customPropertyExtractors = ${ customPropertyExtractorsExpr }.toVector
              )
        }
