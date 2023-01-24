package besom.internal

import com.google.protobuf.struct.Value, Value.Kind
import scala.util.*

object Constants:
  final val UnknownValue           = "04da6b54-80e4-46f7-96ec-b56ff0331ba9"
  final val SpecialSigKey          = "4dabf18193072939515e22adb298388d"
  final val SpecialAssetSig        = "c44067f5952c0a294b673a41bacd8c17"
  final val SpecialArchiveSig      = "0def7320c3a5731c473e5ecbe6d01bc7"
  final val SpecialSecretSig       = "1b47061264138c4ac30d75fd1eb44270"
  final val SpecialResourceSig     = "5cf8f73096256a8f31e491e813e4eb8e"
  final val SecretValueName        = "value"
  final val AssetTextName          = "text"
  final val ArchiveAssetsName      = "assets"
  final val AssetOrArchivePathName = "path"
  final val AssetOrArchiveUriName  = "uri"
  final val ResourceUrnName        = "urn"
  final val ResourceIdName         = "id"
  final val ResourceVersionName    = "packageVersion"
  final val IdPropertyName         = "id"
  final val UrnPropertyName        = "urn"
  final val StatePropertyName      = "state"

case class DecodingError(message: String, cause: Throwable = null) extends Exception(message, cause)

/*
 * Would be awesome to make error reporting better, ie:
 *  - make the Decoder typeclass decoding stack-aware
 *  - pass information about field name, key (in map) or index (in list)
 *  - make DecodingError work with any arity of errors and return aggregates
 *  - render yamled or jsoned version of top Value on error (so at the bottom of the stack!)
 *    this, along with the jq-like path to errors, would allow for very easy debugging
 */
trait Decoder[A]:
  self =>

  def decode(value: Value): Either[DecodingError, OutputData[A]] =
    Decoder.decodeAsPossibleSecret(value).flatMap { odv =>
      Try(odv.map(mapping)) match
        case Failure(exception) => Decoder.errorLeft("Encountered an error", exception)
        case Success(oda)       => Right(oda)
    }

  // TODO this might not be the best idea for simplification in the end, just look at the impls for nested datatypes
  def mapping(value: Value): A

  def map[B](f: A => B): Decoder[B] = new Decoder[B]:
    override def decode(value: Value): Either[DecodingError, OutputData[B]] = self.decode(value).map(_.map(f))
    override def mapping(value: Value): B                                   = f(self.mapping(value)) // unnecessary

object Decoder:
  import Constants.*

  inline def error(msg: String): Nothing = throw DecodingError(msg)
  inline def errorLeft(msg: String, cause: Throwable = null): Either[DecodingError, Nothing] = Left(
    DecodingError(msg, cause)
  )

  def decodeAsPossibleSecret(value: Value): Either[DecodingError, OutputData[Value]] =
    extractSpecialStructSignature(value) match
      case Some(sig) if sig == SpecialSecretSig =>
        val innerValue = value.getStructValue.fields
          .get(SecretValueName)
          .map(Right.apply)
          .getOrElse(errorLeft(s"Secrets must have a field called $SecretValueName!"))

        innerValue.map(OutputData(_, isSecret = true))
      case _ =>
        if value.kind.isStringValue && Constants.UnknownValue == value.getStringValue then
          Right(OutputData.unknown(false))
        else Right(OutputData(value))

  def extractSpecialStructSignature(value: Value): Option[String] =
    Iterator(value)
      .filter(_.kind.isStructValue)
      .flatMap(_.getStructValue.fields)
      .filter((k, v) => k == SpecialSigKey)
      .flatMap((_, v) => v.kind.stringValue)
      .nextOption

  def accumulatedOutputDatasOrFirstError[A](
    acc: Either[DecodingError, Vector[OutputData[A]]],
    elementEither: Either[DecodingError, OutputData[A]],
    typ: String
  ): Either[DecodingError, Vector[OutputData[A]]] =
    acc match
      case Left(error) => Left(error)
      case Right(vec) =>
        elementEither match
          case Left(error)              => Left(error)
          case Right(elementOutputData) =>
            // TODO this should have an issue number from GH and should suggest reporting this to us
            if elementOutputData.isEmpty then errorLeft(s"Encountered a null in $typ, this is illegal in besom!")
            else Right(vec :+ elementOutputData)

  given Decoder[Double] with
    def mapping(value: Value): Double =
      if value.kind.isNumberValue then value.getNumberValue else error("Expected a number!")

  given Decoder[String] with
    def mapping(value: Value): String =
      if value.kind.isStringValue then value.getStringValue else error("Expected a string!")

  given Decoder[Boolean] with
    def mapping(value: Value): Boolean =
      if value.kind.isBoolValue then value.getBoolValue else error("Expected a boolean!")

  given optDecoder[A](using innerDecoder: Decoder[A]): Decoder[Option[A]] = new Decoder[Option[A]]:
    override def decode(value: Value): Either[DecodingError, OutputData[Option[A]]] =
      decodeAsPossibleSecret(value).flatMap { odv =>
        Try {
          odv.flatMap { v =>
            innerDecoder.decode(v) match
              case Left(error) => throw error
              case Right(oa)   => oa.optional
          }
        } match
          case Failure(exception) => errorLeft("Encountered an error", exception)
          case Success(value)     => Right(value)

      }

    override def mapping(value: Value): Option[A] = ???

  // this is kinda different from what other pulumi sdks are doing because we disallow nulls in the list
  given listDecoder[A](using innerDecoder: Decoder[A]): Decoder[List[A]] = new Decoder[List[A]]:
    override def decode(value: Value): Either[DecodingError, OutputData[List[A]]] =
      decodeAsPossibleSecret(value).flatMap { odv =>
        Try {
          odv.flatMap { v =>
            val listValue = if v.kind.isListValue then v.getListValue else error("Expected a list!")

            val eitherFirstErrorOrOutputDataOfList =
              listValue.values
                .map(innerDecoder.decode)
                .foldLeft[Either[DecodingError, Vector[OutputData[A]]]](Right(Vector.empty))(
                  accumulatedOutputDatasOrFirstError(_, _, "list")
                )
                .map(_.toList)
                .map(OutputData.sequence)

            eitherFirstErrorOrOutputDataOfList match
              case Left(de)    => throw de
              case Right(odla) => odla
          }
        } match
          case Failure(exception) => errorLeft("Encountered an error", exception)
          case Success(value)     => Right(value)
      }

    def mapping(value: Value): List[A] = List.empty

  given mapDecoder[A](using innerDecoder: Decoder[A]): Decoder[Map[String, A]] = new Decoder[Map[String, A]]:
    override def decode(value: Value): Either[DecodingError, OutputData[Map[String, A]]] =
      decodeAsPossibleSecret(value).flatMap { odv =>
        Try {
          odv.flatMap { innerValue =>
            val structValue =
              if innerValue.kind.isStructValue then innerValue.getStructValue else error("Expected a struct!")

            val eitherFirstErrorOrOutputDataOfMap =
              structValue.fields.iterator
                .map { (k, v) =>
                  innerDecoder.decode(v).map(_.map(nv => (k, nv)))
                }
                .foldLeft[Either[DecodingError, Vector[OutputData[(String, A)]]]](Right(Vector.empty))(
                  accumulatedOutputDatasOrFirstError(_, _, "map")
                )
                .map(OutputData.sequence)

            eitherFirstErrorOrOutputDataOfMap match
              case Left(error)  => throw error
              case Right(value) => value.map(_.toMap)
          }
        } match
          case Failure(exception) => errorLeft("Encountered an error", exception)
          case Success(value)     => Right(value)

      }
    def mapping(value: Value): Map[String, A] = Map.empty
