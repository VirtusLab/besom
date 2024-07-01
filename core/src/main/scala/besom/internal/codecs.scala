package besom.internal

import besom.internal.ProtobufUtil.{*, given}
import besom.types.*
import besom.types.Archive.*
import besom.types.Asset.*
import besom.util.Validated.*
import besom.util.{NonEmptyString, *}
import com.google.protobuf.struct.*
import com.google.protobuf.struct.Value.Kind

import scala.annotation.implicitNotFound
import scala.deriving.Mirror
import scala.util.*

//noinspection ScalaFileName
object Constants:
  /** Well-known signatures used in gRPC protocol, see sdk/go/common/resource/properties.go. */
  enum SpecialSig(value: String):
    /** Signature used to identify assets in maps in gRPC protocol */
    case AssetSig extends SpecialSig("c44067f5952c0a294b673a41bacd8c17")

    /** Signature used to identify archives in maps in gRPC protocol */
    case ArchiveSig extends SpecialSig("0def7320c3a5731c473e5ecbe6d01bc7")

    /** Signature used to identify secrets maps in gRPC protocol */
    case SecretSig extends SpecialSig("1b47061264138c4ac30d75fd1eb44270")

    /** Signature used to identify resources in maps in gRPC protocol */
    case ResourceSig extends SpecialSig("5cf8f73096256a8f31e491e813e4eb8e")

    /** Signature used to identify outputs in maps in gRPC protocol */
    case OutputSig extends SpecialSig("d0e6a833031e9bbcd3f4e8bde6ca49a4")

    /** @return the signature raw value */
    def asString: String = value

  object SpecialSig:
    /** Signature used to encode type identity inside of a map in gRPC protocol.
      *
      * This is required when flattening into ordinary maps, like we do when performing serialization, to ensure recoverability of type
      * identities later on.
      */
    final val Key = "4dabf18193072939515e22adb298388d"

    def fromString(s: String): Option[SpecialSig] = SpecialSig.values.find(_.asString == s)

  end SpecialSig

  /** Well-known sentinels used in gRPC protocol, see sdk/go/common/resource/plugin/rpc.go */

  /** Sentinel indicating that a string property's value is not known, because it depends on a computation with values whose values
    * themselves are not yet known (e.g., dependent upon an output property).
    */
  final val UnknownStringValue = "04da6b54-80e4-46f7-96ec-b56ff0331ba9"

  /** Well-known property names used in gRPC protocol */

  final val ValueName              = "value"
  final val SecretName             = "secret"
  final val DependenciesName       = "dependencies"
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
end Constants

@SerialVersionUID(1L)
case class DecodingError(message: String, label: Label, cause: Option[Throwable])
    extends Exception(s"""[$label] ${message}""", cause.orNull)
    with Product
    with Serializable
object DecodingError:
  def apply(message: String, cause: Throwable, label: Label): DecodingError = new DecodingError(message, label, Some(cause))
  def apply(message: String, label: Label): DecodingError                   = new DecodingError(message, label, None)
end DecodingError
@SerialVersionUID(1L)
case class AggregatedDecodingError private (message: String, cause: Throwable, errors: NonEmptyVector[DecodingError])
    extends Exception(message, cause)
    with Product
    with Serializable:
end AggregatedDecodingError
object AggregatedDecodingError:
  def apply(errors: NonEmptyVector[DecodingError]): AggregatedDecodingError =
    val msg =
      if errors.size > 1
      then s"""Decoding Errors [${errors.size}]:
           |${errors.map(_.message).mkString("  ", "\n  ", "")}
           |(with aggregate stack trace)""".stripMargin
      else errors.head.message

    errors.tail.foreach(errors.head.addSuppressed)
    new AggregatedDecodingError(msg, errors.head, errors)
  end apply

/** Decoders handle the details of the deserialization.
  *
  * @see
  *   also [[Encoder]]
  */
/*
 * Would be awesome to make error reporting better, ie:
 *  - render yamled or jsoned version of top Value on error (so at the bottom of the stack!)
 *    this, along with the jq-like path to errors, would allow for very easy debugging
 *
 * [ ] TODO IMPORTANT: Decoder[A] derivation for case classes and ADTs MUST respect secretness of constituent fields
 *                     Whole record/adt must be considered secret if one of the fields held in plain value is secret!
 *                     Write a test, OutputData.combine should have taken care of it in sequence/traverse chains.
 * [ ] TODO: a better way than current Try/throw combo to traverse OutputData's lack of error channel
 */
trait Decoder[A]:
  self =>

  /** Deserialize the given gRPC value into `OutputData[A]` or return an error
    */
  def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[A]] =
    Decoder
      .decodeAsPossibleSecretOrOutput(value, label)
      .flatMap((odv: OutputData[Value]) =>
        ValidatedResult(
          odv
            .traverseValidated(mapping(_, label))
            .lmap(exception =>
              DecodingError(
                s"$label: Encountered an error - possible secret - ${odv}",
                label = label,
                cause = exception
              )
            )
        )
      )

  // TODO this might not be the best idea for simplification in the end, just look at the impls for nested datatypes
  def mapping(value: Value, label: Label): Validated[DecodingError, A]

  def emap[B](f: (A, Label) => ValidatedResult[DecodingError, B]): Decoder[B] = new Decoder[B]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[B]] =
      self.decode(value, label).flatMap { odA =>
        odA
          .traverseValidatedResult { a =>
            f(a, label).map(OutputData(_))
          }
          .map(_.flatten)
          .lmap(exception => DecodingError(s"$label: Encountered an error", label = label, cause = exception))
      }
    override def mapping(value: Value, label: Label): Validated[DecodingError, B] = ???
end Decoder

object NameUnmangler:
  private val anyRefMethodNames = Set(
    "eq",
    "ne",
    "notify",
    "notifyAll",
    "synchronized",
    "wait",
    "asInstanceOf",
    "clone",
    "equals",
    "getClass",
    "hashCode",
    "isInstanceOf",
    "toString",
    "finalize"
  )

  private val reservedMethods = Set(
    "pulumiResourceName",
    "asString"
  )

  private val reservedPackages = Set(
    "java",
    "javax",
    "scala",
    "besom"
  )

  private val reserved = (anyRefMethodNames ++ reservedMethods ++ reservedPackages).map(_ + "_")

  // This logic must be undone the same way in codegen
  // Keep in sync with `manglePropertyName` in CodeGen.scala
  def unmanglePropertyName(name: String): String =
    if reserved.contains(name) then name.dropRight(1) // drop the underscore
    else name

object Decoder extends DecoderInstancesLowPrio1:
  import besom.json.*

  given unitDecoder: Decoder[Unit] with
    def mapping(value: Value, label: Label): Validated[DecodingError, Unit] = ().valid

  // for recursive stuff like Map[String, Value]
  given valueDecoder: Decoder[Value] with
    def mapping(value: Value, label: Label): Validated[DecodingError, Value] = value.valid

  given doubleDecoder: Decoder[Double] with
    def mapping(v: Value, label: Label): Validated[DecodingError, Double] =
      if v.kind.isNumberValue then v.getNumberValue.valid
      else error(s"$label: Expected a number, got: '${v.kind}'", label).invalid

  given intDecoder(using doubleDecoder: Decoder[Double]): Decoder[Int] =
    doubleDecoder.emap { (double, label) =>
      if (double % 1 == 0) ValidatedResult.valid(double.toInt)
      else error(s"$label: Numeric value was expected to be integer, but had a decimal value", label).invalidResult
    }

  given stringDecoder: Decoder[String] with
    def mapping(v: Value, label: Label): Validated[DecodingError, String] =
      if v.kind.isStringValue then v.getStringValue.valid
      else error(s"$label: Expected a string, got: '${v.kind}'", label).invalid

  given urnDecoder: Decoder[URN] with
    def mapping(value: Value, label: Label): Validated[DecodingError, URN] =
      if value.kind.isStringValue then
        URN.from(value.getStringValue) match
          case Success(value)     => value.valid
          case Failure(exception) => error(s"$label: Expected a valid URN string", label, exception).invalid
      else error(s"$label: Expected a string, got: '${value.kind}'", label).invalid

  given boolDecoder: Decoder[Boolean] with
    def mapping(v: Value, label: Label): Validated[DecodingError, Boolean] =
      if v.kind.isBoolValue then v.getBoolValue.valid
      else error(s"$label: Expected a boolean, got: '${v.kind}'", label).invalid

  given outputDecoder[A](using inner: Decoder[A]): Decoder[Output[A]] = new Decoder[Output[A]]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[Output[A]]] =
      inner
        .decode(value, label)
        .redeem(
          errs => OutputData(Output.fail(AggregatedDecodingError(errs))),
          _.map(Output(_))
        )

    override def mapping(value: Value, label: Label): Validated[DecodingError, Output[A]] = ???

  given jsonDecoder: Decoder[JsValue] with
    private def convertToJsValue(value: Value): JsValue =
      value.kind match
        case Kind.Empty                => JsNull
        case Kind.NullValue(_)         => JsNull
        case Kind.NumberValue(num)     => JsNumber(num)
        case Kind.StringValue(str)     => JsString(str)
        case Kind.BoolValue(bool)      => JsBoolean(bool)
        case Kind.StructValue(struct)  => convertStructToJsObject(struct)
        case Kind.ListValue(listValue) => convertListValueToJsArray(listValue)

    private def convertStructToJsObject(struct: Struct): JsObject =
      val fields = struct.fields.view.mapValues(convertToJsValue).toMap
      JsObject(fields)

    private def convertListValueToJsArray(listValue: ListValue): JsArray =
      val values = listValue.values.map(convertToJsValue)
      JsArray(values: _*)

    def mapping(value: Value, label: Label): Validated[DecodingError, JsValue] = convertToJsValue(value).valid

  given optDecoder[A](using innerDecoder: Decoder[A]): Decoder[Option[A]] = new Decoder[Option[A]]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[Option[A]]] =
      decodeAsPossibleSecretOrOutput(value, label).flatMap { odv =>
        odv
          .traverseValidatedResult {
            case Value(Kind.NullValue(_), _) => ValidatedResult.valid(OutputData(None))
            case v                           => innerDecoder.decode(v, label).map(_.map(Some(_)))
          }
          .map(_.flatten)
          .lmap(exception =>
            DecodingError(
              s"$label: Encountered an error when deserializing an option",
              label = label,
              cause = exception
            )
          )
      }

    override def mapping(value: Value, label: Label): Validated[DecodingError, Option[A]] = ???

  given unionIntStringDecoder: Decoder[Int | String]         = unionDecoder2[Int, String]
  given unionBooleanStringDecoder: Decoder[Boolean | String] = unionDecoder2[Boolean, String]

  // this is kinda different from what other pulumi sdks are doing because we disallow nulls in the list
  given listDecoder[A](using innerDecoder: Decoder[A]): Decoder[List[A]] = new Decoder[List[A]]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[List[A]]] =
      decodeAsPossibleSecretOrOutput(value, label).flatMap { (odv: OutputData[Value]) =>
        odv
          .traverseValidatedResult { (v: Value) =>
            if !v.kind.isListValue then error(s"$label: Expected a list, got ${v.kind}", label).invalidResult
            else
              v.getListValue.values.zipWithIndex
                .map { case (v, i) =>
                  innerDecoder.decode(v, label.atIndex(i))
                }
                .foldLeft[ValidatedResult[DecodingError, Vector[OutputData[A]]]](ValidatedResult.valid(Vector.empty))(
                  accumulatedOutputDataOrErrors(_, _, "list", label)
                )
                .map(_.toList)
                .map(OutputData.sequence)
            end if
          }
          .map(_.flatten)
          .lmap(exception => DecodingError(s"$label: Encountered an error when deserializing a list", label = label, cause = exception))
      }

    def mapping(value: Value, label: Label): Validated[DecodingError, List[A]] = ???

  given setDecoder[A](using innerDecoder: Decoder[A]): Decoder[Set[A]] = new Decoder[Set[A]]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[Set[A]]] =
      decodeAsPossibleSecretOrOutput(value, label).flatMap { (odv: OutputData[Value]) =>
        odv
          .traverseValidatedResult { (v: Value) =>
            if !v.kind.isListValue then error(s"$label: Expected a list kind, got: '${v.kind}'", label).invalidResult
            else
              v.getListValue.values.zipWithIndex
                .map { case (v, i) =>
                  innerDecoder.decode(v, label.atIndex(i))
                }
                .foldLeft[ValidatedResult[DecodingError, Vector[OutputData[A]]]](ValidatedResult.valid(Vector.empty))(
                  accumulatedOutputDataOrErrors(_, _, "list", label)
                )
                .map(_.toSet)
                .map(OutputData.sequence)
            end if
          }
          .map(_.flatten)
          .lmap(exception => DecodingError(s"$label: Encountered an error when deserializing a set", label = label, cause = exception))
      }

    def mapping(value: Value, label: Label): Validated[DecodingError, Set[A]] = ???

  given mapDecoder[A](using innerDecoder: Decoder[A]): Decoder[Map[String, A]] = new Decoder[Map[String, A]]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[Map[String, A]]] =
      decodeAsPossibleSecretOrOutput(value, label).flatMap { odv =>
        odv
          .traverseValidatedResult { v =>
            if !v.kind.isStructValue then error(s"$label: Expected a struct kind, got: '${v.kind}'", label).invalidResult
            else
              v.getStructValue.fields.iterator
                .filterNot { (key, _) => key.startsWith("__") } // we filter out internal pulumi fields
                .map { (k, v) =>
                  innerDecoder.decode(v, label.withKey(k)).map(_.map(nv => (k, nv)))
                }
                .foldLeft[ValidatedResult[DecodingError, Vector[OutputData[(String, A)]]]](
                  ValidatedResult.valid(Vector.empty)
                )(
                  accumulatedOutputDataOrErrors(_, _, "struct", label)
                )
                .map(OutputData.sequence)
                .map(_.map(_.toMap))
            end if
          }
          .map(_.flatten)
          .lmap(exception => DecodingError(s"$label: Encountered an error when deserializing a map", label = label, cause = exception))
      }
    def mapping(value: Value, label: Label): Validated[DecodingError, Map[String, A]] = ???

  private def getResourceBasedResourceDecoder[R <: Resource: ResourceDecoder]: Decoder[R] =
    new Decoder[R]:
      override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[R]] =
        decodeAsPossibleSecretOrOutput(value, label).flatMap { odv =>
          odv
            .traverseValidatedResult { innerValue =>
              innerValue.kind.structValue.flatMap(_.specialSignature) match
                case None => error(s"$label: Expected a special struct signature", label).invalidResult
                case Some(Constants.SpecialSig.ResourceSig) =>
                  val structValue = innerValue.getStructValue
                  structValue.fields
                    .get(Constants.ResourceUrnName)
                    .map(_.getStringValue)
                    .toValidatedResultOrError(
                      error(s"$label: Expected a resource urn in resource struct, not found", label)
                    )
                    .flatMap(urnString => URN.from(urnString).toEither.toValidatedResult)
                    .flatMap { urn =>
                      NonEmptyString(urn.resourceName) match
                        case None =>
                          error(s"$label: Expected a non-empty resource name in resource urn", label).invalidResult
                        case Some(resourceName) =>
                          val opts =
                            CustomResourceOptions(urn = urn) // triggers GetResource instead of RegisterResource
                          Context()
                            .readOrRegisterResource[R, EmptyArgs](urn.resourceType, resourceName, EmptyArgs(), opts)
                            .getData
                            .either
                            .map {
                              case Right(outpudDataOfR) => outpudDataOfR.valid
                              case Left(err)            => err.invalid
                            }
                            .asValidatedResult
                      end match
                    }
                case Some(sig) =>
                  error(s"$label: Expected a special resource signature, got: '$sig'", label).invalidResult
            }
            .map(_.flatten)
            .lmap(exception =>
              DecodingError(
                s"$label: Encountered an error when deserializing a resource: ${exception.getMessage}",
                label = label,
                cause = exception
              )
            )
        }

      override def mapping(value: Value, label: Label): Validated[DecodingError, R] = ???

  // handles ProviderResources too as they extend CustomResource
  given customResourceDecoder[R <: CustomResource: ResourceDecoder]: Decoder[R] =
    getResourceBasedResourceDecoder[R]

  given remoteComponentResourceDecoder[R <: RemoteComponentResource: ResourceDecoder]: Decoder[R] =
    getResourceBasedResourceDecoder[R]

  // wondering if this works, it's a bit of a hack
  given dependencyResourceDecoder: Decoder[DependencyResource] = new Decoder[DependencyResource]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[DependencyResource]] =
      decodeAsPossibleSecretOrOutput(value, label).flatMap { odv =>
        odv
          .traverseValidatedResult { innerValue =>
            innerValue.kind.structValue.flatMap(_.specialSignature) match
              case None => error(s"$label: Expected a special struct signature", label).invalidResult
              case Some(Constants.SpecialSig.ResourceSig) =>
                val structValue = innerValue.getStructValue
                structValue.fields
                  .get(Constants.ResourceUrnName)
                  .map(_.getStringValue)
                  .toValidatedResultOrError(
                    error(s"$label: Expected a resource urn in resource struct, not found", label)
                  )
                  .flatMap(urnString => URN.from(urnString).toEither.toValidatedResult)
                  .map(urn => OutputData(DependencyResource(Output(urn))))
              case Some(sig) =>
                error(s"$label: Expected a special resource signature, got: '$sig'", label).invalidResult
          }
          .map(_.flatten)
          .lmap(exception =>
            DecodingError(
              s"$label: Encountered an error when deserializing a resource",
              label = label,
              cause = exception
            )
          )
      }

    override def mapping(value: Value, label: Label): Validated[DecodingError, DependencyResource] = ???

  def assetArchiveDecoder[A](
    specialSig: Constants.SpecialSig,
    handle: Context ?=> (Label, Struct) => ValidatedResult[DecodingError, OutputData[A]]
  ): Decoder[A] = new Decoder[A]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[A]] =
      decodeAsPossibleSecretOrOutput(value, label).flatMap { odv =>
        odv
          .traverseValidatedResult { innerValue =>
            innerValue.kind.structValue.flatMap(_.specialSignature) match
              case None => error(s"$label: Expected a special struct signature", label).invalidResult
              case Some(extractedSpecialSig) =>
                if extractedSpecialSig != specialSig then
                  error(
                    s"$label: Expected a special asset signature, got: '${extractedSpecialSig}'",
                    label
                  ).invalidResult
                else
                  val structValue = innerValue.getStructValue
                  handle(label, structValue)
          }
          .map(_.flatten)
          .lmap(exception => DecodingError(s"$label: Encountered an error when deserializing an asset", label = label, cause = exception))
      }

    override def mapping(value: Value, label: Label): Validated[DecodingError, A] = ???

  given fileAssetDecoder: Decoder[FileAsset] = assetArchiveDecoder[FileAsset](
    Constants.SpecialSig.AssetSig,
    (label, structValue) =>
      structValue.fields
        .get(Constants.AssetOrArchivePathName)
        .map(_.getStringValue)
        .toValidatedResultOrError(DecodingError(s"$label: Expected a path in asset struct", label = label))
        .map(path => OutputData(FileAsset(path)))
  )

  given remoteAssetDecoder: Decoder[RemoteAsset] = assetArchiveDecoder[RemoteAsset](
    Constants.SpecialSig.AssetSig,
    (label, structValue) =>
      structValue.fields
        .get(Constants.AssetOrArchiveUriName)
        .map(_.getStringValue)
        .toValidatedResultOrError(DecodingError(s"$label: Expected a uri in asset struct", label = label))
        .map(uri => OutputData(RemoteAsset(uri)))
  )

  given stringAssetDecoder: Decoder[StringAsset] = assetArchiveDecoder(
    Constants.SpecialSig.AssetSig,
    (label, structValue) =>
      structValue.fields
        .get(Constants.AssetTextName)
        .map(_.getStringValue)
        .toValidatedResultOrError(DecodingError(s"$label: Expected a text in asset struct", label = label))
        .map(text => OutputData(StringAsset(text)))
  )

  given fileArchiveDecoder: Decoder[FileArchive] = assetArchiveDecoder[FileArchive](
    Constants.SpecialSig.ArchiveSig,
    (label, structValue) =>
      structValue.fields
        .get(Constants.AssetOrArchivePathName)
        .map(_.getStringValue)
        .toValidatedResultOrError(DecodingError(s"$label: Expected a path in archive struct", label = label))
        .map(path => OutputData(FileArchive(path)))
  )

  given remoteArchiveDecoder: Decoder[RemoteArchive] = assetArchiveDecoder[RemoteArchive](
    Constants.SpecialSig.ArchiveSig,
    (label, structValue) =>
      structValue.fields
        .get(Constants.AssetOrArchiveUriName)
        .map(_.getStringValue)
        .toValidatedResultOrError(DecodingError(s"$label: Expected a uri in archive struct", label = label))
        .map(uri => OutputData(RemoteArchive(uri)))
  )

  // noinspection NoTailRecursionAnnotation
  given assetArchiveDecoder: Decoder[AssetArchive] = assetArchiveDecoder[AssetArchive](
    Constants.SpecialSig.ArchiveSig,
    (label, structValue) =>
      val nested = structValue.fields
        .get(Constants.ArchiveAssetsName)
        .map(_.getStructValue)
        .toValidatedResultOrError(
          DecodingError(s"$label: Expected an assets field in archive struct, not found", label = label)
        )

      val outputDataVecV = nested.flatMap { nestedVal =>
        nestedVal.fields.toVector.traverseVR { (name, value) =>
          assetOrArchiveDecoder
            .decode(value, label.withKey(name))
            .lmap((exception: DecodingError) =>
              DecodingError(
                s"$label: Encountered an error when deserializing an archive",
                label = label,
                cause = exception
              )
            )
            .map(_.map((name, _)))
        }
      }

      outputDataVecV.map { outputDataVec =>
        OutputData.sequence(outputDataVec).map(vec => AssetArchive(vec.toMap))
      }
  )

  given assetDecoder: Decoder[Asset] = new Decoder[Asset]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[Asset]] =
      fileAssetDecoder
        .decode(value, label)
        .orElse(stringAssetDecoder.decode(value, label))
        .orElse(remoteAssetDecoder.decode(value, label))
        .orElse(error(s"$label: Found value is neither a FileAsset, StringAsset nor RemoteAsset", label).invalidResult)

    override def mapping(value: Value, label: Label): Validated[DecodingError, Asset] = ???

  given archiveDecoder: Decoder[Archive] = new Decoder[Archive]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[Archive]] =
      fileArchiveDecoder
        .decode(value, label)
        .orElse(remoteArchiveDecoder.decode(value, label))
        .orElse(assetArchiveDecoder.decode(value, label))
        .orElse(
          error(s"$label: Found value is neither a FileArchive, AssetArchive nor RemoteArchive", label).invalidResult
        )
    override def mapping(value: Value, label: Label): Validated[DecodingError, Archive] = ???

  given assetOrArchiveDecoder: Decoder[AssetOrArchive] = new Decoder[AssetOrArchive]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[AssetOrArchive]] =
      assetDecoder
        .decode(value, label)
        .orElse(archiveDecoder.decode(value, label))
        .orElse(error(s"$label: Found value is neither an Asset nor an Archive", label).invalidResult)
    override def mapping(value: Value, label: Label): Validated[DecodingError, AssetOrArchive] = ???
end Decoder

trait DecoderInstancesLowPrio1 extends DecoderInstancesLowPrio2:
  import besom.json.JsValue

  given unionBooleanDecoder[A: Decoder](using NotGiven[A <:< Boolean]): Decoder[Boolean | A] = unionDecoder2[A, Boolean]
  given unionStringDecoder[A: Decoder](using NotGiven[A <:< String]): Decoder[String | A]    = unionDecoder2[A, String]
  given unionJsonDecoder[A: Decoder](using NotGiven[A <:< JsValue]): Decoder[JsValue | A]    = unionDecoder2[A, JsValue]

trait DecoderInstancesLowPrio2 extends DecoderHelpers:
  given singleOrListDecoder[A: Decoder, L <: List[?]: Decoder]: Decoder[A | L] = unionDecoder2[A, L]

trait DecoderHelpers:
  import Constants.*

  def unionDecoder2[A, B](using aDecoder: Decoder[A], bDecoder: Decoder[B]): Decoder[A | B] = new Decoder[A | B]:
    override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[A | B]] =
      decodeAsPossibleSecretOrOutput(value, label).flatMap { odv =>
        odv
          .traverseValidatedResult { v =>
            aDecoder
              .decode(v, label)
              .orElse(bDecoder.decode(v, label))
              .asInstanceOf[ValidatedResult[DecodingError, OutputData[A | B]]]
          }
          .map(_.flatten)
      }

    override def mapping(value: Value, label: Label): Validated[DecodingError, A | B] = ???

  inline def error(msg: String, label: Label, cause: Throwable = null): DecodingError = DecodingError(msg, cause, label)

  inline def derived[A](using m: Mirror.Of[A]): Decoder[A] =
    lazy val labels           = CodecMacros.summonLabels[m.MirroredElemLabels]
    lazy val instances        = CodecMacros.summonDecoders[m.MirroredElemTypes]
    lazy val nameDecoderPairs = labels.map(NameUnmangler.unmanglePropertyName).zip(instances)

    inline m match
      case s: Mirror.SumOf[A]     => decoderSum(s, nameDecoderPairs)
      case p: Mirror.ProductOf[A] => decoderProduct(p, nameDecoderPairs)
  end derived

  def discriminated[A](
    discriminatingPropertyName: String,
    typeMap: Map[String, Decoder[?]] // in practice, we assume at least one decoder from codegen
  ): Decoder[A] =
    require(typeMap.nonEmpty, s"Decoder.discriminated requires non-empty 'typeMap' parameter")
    new Decoder[A]:
      private def getDecoder(key: String): Decoder[A] = typeMap(key).asInstanceOf[Decoder[A]]
      private def decodeValue(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[A]] =
        if !value.kind.isStructValue then error(s"$label: Expected a struct", label).invalidResult
        else
          val fields = value.getStructValue.fields
          val discriminatingValue: Option[String] = fields
            .get(discriminatingPropertyName)
            .map(_.getStringValue)

          discriminatingValue match
            case Some(key) => getDecoder(key).decode(value, label.withKey(key))
            case None =>
              error(
                s"$label: Expected a discriminator string in field '$discriminatingPropertyName'",
                label
              ).invalidResult
      end decodeValue

      override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[A]] =
        decodeAsPossibleSecretOrOutput(value, label).flatMap { (odv: OutputData[Value]) =>
          odv.traverseValidatedResult(decodeValue(_, label)).map(_.flatten)
        }

      override def mapping(value: Value, label: Label): Validated[DecodingError, A] = ???
  end discriminated

  def nonDiscriminated[A](
    typeMap: Map[Int, Decoder[?]] // in practice, we assume at least one decoder from codegen
  ): Decoder[A] =
    require(typeMap.nonEmpty, s"Decoder.nonDiscriminated requires non-empty 'typeMap' parameter")
    new Decoder[A]:
      private def getDecoder(key: Int): Decoder[A] = typeMap(key).asInstanceOf[Decoder[A]]
      private def decodeValue(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[A]] =
        val dd = for key <- 0 until typeMap.size yield getDecoder(key).decode(value, label)
        dd.reduce(_ orElse _) // we assume that only one of the decoders will succeed
      end decodeValue

      override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[A]] =
        decodeAsPossibleSecretOrOutput(value, label).flatMap { (odv: OutputData[Value]) =>
          odv.traverseValidatedResult(decodeValue(_, label)).map(_.flatten)
        }

      override def mapping(value: Value, label: Label): Validated[DecodingError, A] = ???
  end nonDiscriminated

  // this is, effectively, Decoder[Enum]
  def decoderSum[A](s: Mirror.SumOf[A], elems: => List[(String, Decoder[?])]): Decoder[A] =
    new Decoder[A]:
      private val enumNameToDecoder                   = elems.toMap
      private def getDecoder(key: String): Decoder[A] = enumNameToDecoder(key).asInstanceOf[Decoder[A]]
      override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[A]] =
        if value.kind.isStringValue then
          val key = value.getStringValue
          getDecoder(key).decode(Map.empty[String, Value].asValue, label.withKey(key))
        else
          error(
            s"$label: Value was not a string, Enums should be serialized as strings", // TODO: This is not necessarily true
            label
          ).invalidResult

      override def mapping(value: Value, label: Label): Validated[DecodingError, A] = ???

  def decoderProduct[A](p: Mirror.ProductOf[A], elems: => List[(String, Decoder[?])]): Decoder[A] =
    new Decoder[A]:
      override def decode(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[A]] =
        decodeAsPossibleSecretOrOutput(value, label).flatMap { odv =>
          odv
            .traverseValidatedResult { innerValue =>
              if innerValue.kind.isStructValue then
                val fields = innerValue.getStructValue.fields
                elems
                  .foldLeft[ValidatedResult[DecodingError, OutputData[Tuple]]](
                    ValidatedResult.valid(OutputData(EmptyTuple))
                  ) { case (validatedAcc, (name -> decoder)) =>
                    val fieldValue: Value = fields.getOrElse(name, Null)
                    validatedAcc.zipWith(decoder.decode(fieldValue, label.withKey(name))) { (acc, odField) =>
                      acc.zip(odField)
                    }
                  }
                  .map(_.map(p.fromProduct(_)))
              else
                error(
                  s"$label: Expected a struct to deserialize Product[$p], got: '${innerValue.kind}'",
                  label = label
                ).invalidResult
            }
            .map(_.flatten)
        }

      override def mapping(value: Value, label: Label): Validated[DecodingError, A] = ???

  def decodeAsPossibleSecretOrOutput(value: Value, label: Label)(using Context): ValidatedResult[DecodingError, OutputData[Value]] =
    value
      .withSpecialSignature {
        case (struct, SpecialSig.SecretSig) =>
          val innerValue = struct.fields
            .get(ValueName)
            .map(ValidatedResult.valid)
            .getOrElse(error(s"$label: Secrets must have a field called $ValueName", label).invalidResult)

          innerValue.map { (v: Value) =>
            // handle secret unknown values
            if v.kind.stringValue.map(_ == Constants.UnknownStringValue).getOrElse(false)
            then OutputData.unknown(isSecret = true)
            else OutputData(v, isSecret = true)
          }
        case (struct, SpecialSig.OutputSig) =>
          val innerValue: ValidatedResult[DecodingError, Option[Value]] =
            struct.fields
              .get(ValueName)
              .validResult
          val isSecret: ValidatedResult[DecodingError, Boolean] =
            struct.fields
              .get(SecretName)
              .collectFirst({ case Value(Kind.BoolValue(b), _) => b })
              .getOrElse(false)
              .validResult

          val deps: ValidatedResult[DecodingError, Vector[Resource]] =
            struct.fields
              .get(DependenciesName)
              .map { v =>
                if !v.kind.isListValue then error(s"$label: Expected a dependencies list in output, got ${v.kind}", label).invalidResult
                else
                  val depsLabel = label.withKey(DependenciesName)
                  v.getListValue.values.zipWithIndex
                    .map { case (v, i) =>
                      val l = depsLabel.atIndex(i)
                      v.getStringValue.validResult
                        .flatMap(URN.from(_).toEither.left.map(e => error(s"$l: Expected a valid URN string", l, e)).toValidatedResult)
                        .map(urn => DependencyResource(Output(urn)))
                    }
                    .foldLeft[ValidatedResult[DecodingError, Vector[Resource]]](ValidatedResult.valid(Vector.empty)) { (acc, vr) =>
                      acc.zipWith(vr) { (acc, v) =>
                        acc :+ v
                      }
                    }
              }
              .getOrElse(ValidatedResult.valid(Vector.empty))

          for
            innerValue: Option[Value] <- innerValue
            isSecret: Boolean         <- isSecret
            deps: Vector[Resource]    <- deps
          yield
            if innerValue.isEmpty
            then OutputData.unknown(isSecret, deps.toSet)
            else OutputData(deps.toSet, innerValue, isSecret)
      }
      .getOrElse {
        // handle plain unknown values
        if value.kind.isStringValue && Constants.UnknownStringValue == value.getStringValue
        then ValidatedResult.valid(OutputData.unknown(isSecret = false))
        else ValidatedResult.valid(OutputData(value))
      }
  end decodeAsPossibleSecretOrOutput

  private[internal] def accumulatedOutputDataOrErrors[A](
    acc: ValidatedResult[DecodingError, Vector[OutputData[A]]],
    elementValidatedResult: ValidatedResult[DecodingError, OutputData[A]],
    typ: String,
    label: Label
  ): ValidatedResult[DecodingError, Vector[OutputData[A]]] =
    acc.zipWith(
      elementValidatedResult
        // TODO this should have an issue number from GH and should suggest reporting this to us
        .filterOrError(_.nonEmpty)(
          DecodingError(s"Encountered a 'null' in '$typ', this is illegal in Besom, please file an issue on GitHub", label = label)
        )
    ) { (acc, elementOutputData) =>
      acc :+ elementOutputData
    }

end DecoderHelpers

/** Encoders handle the details of serialization.
  *
  * High-level encoders:
  *   - [[ArgsEncoder]] - this is a separate typeclass required for serialization of top-level *Args classes
  *   - [[ProviderArgsEncoder]] - this is a separate typeclass required for serialization of top-level ProviderArgs classes that have most
  *     fields serialized as JSON strings, with the exception of `null` and `String`
  *
  * @see
  *   also [[Decoder]]
  */
@implicitNotFound("""Instance of Encoder[${A}] is missing! Most likely you are trying to use structured
    |stack exports and you have defined a case class that holds your exported values.
    |
    |Add `derives Encoder` (or `derives besom.Encoder` if you don't have the global import in
    |that scope) to your case class definition like this:
    |
    |case class MyExportedValues(...) derives Encoder""")
trait Encoder[A]:
  self =>

  /** Serializes the given object to gRPC value and Pulumi [[Metadata]]
    */
  def encode(a: A)(using Context): Result[(Metadata, Value)]
  def contramap[B](f: B => A): Encoder[B] = new Encoder[B]:
    def encode(b: B)(using Context): Result[(Metadata, Value)] = self.encode(f(b))

object Encoder:
  import Constants.*
  import besom.json.*

  // noinspection ScalaWeakerAccess
  def encoderSum[A](mirror: Mirror.SumOf[A], nameEncoderPairs: => List[(String, Encoder[?])]): Encoder[A] =
    new Encoder[A]:
      // TODO We only serialize dumb enums!!
      // private val encoderMap                                    = nameEncoderPairs.toMap
      override def encode(a: A)(using Context): Result[(Metadata, Value)] = Result.pure(Metadata.empty -> a.toString.asValue)

  // noinspection ScalaWeakerAccess
  def encoderProduct[A](nameEncoderPairs: => List[(String, Encoder[?])]): Encoder[A] =
    new Encoder[A]:
      override def encode(a: A)(using Context): Result[(Metadata, Value)] =
        Result
          .sequence {
            a.asInstanceOf[Product]
              .productIterator
              .zip(nameEncoderPairs)
              .map { case (v, (label, encoder)) =>
                encoder.asInstanceOf[Encoder[Any]].encode(v).map(res => label -> res)
              }
              .toList
          }
          .map { lst =>
            val (resources, labelsToValuesMap) =
              lst.foldLeft[(Metadata, Map[String, Value])](Metadata.empty -> Map.empty) {
                case ((allMetadata, props), (label, (fieldMetadata, fieldValue))) =>
                  val concatMetadata = allMetadata.combine(fieldMetadata)
                  if fieldMetadata.empty then (concatMetadata, props) // we treat properties with NullValue values as if they do not exist
                  else concatMetadata -> (props + (label -> fieldValue))
              }

            resources -> labelsToValuesMap.asValue
          }

  inline def derived[A](using m: Mirror.Of[A]): Encoder[A] =
    lazy val labels           = CodecMacros.summonLabels[m.MirroredElemLabels]
    lazy val instances        = CodecMacros.summonEncoders[m.MirroredElemTypes]
    lazy val nameEncoderPairs = labels.map(NameUnmangler.unmanglePropertyName).zip(instances)
    inline m match
      case s: Mirror.SumOf[A]     => encoderSum(s, nameEncoderPairs)
      case _: Mirror.ProductOf[A] => encoderProduct(nameEncoderPairs)

  given customResourceEncoder[A <: CustomResource](using
    outputIdEnc: Encoder[Output[ResourceId]],
    outputURNEnc: Encoder[Output[URN]]
  ): Encoder[A] =
    new Encoder[A]:
      def encode(a: A)(using ctx: Context): Result[(Metadata, Value)] =
        outputIdEnc.encode(a.id).flatMap { (idMetadata, idValue) =>
          if ctx.featureSupport.keepResources then
            outputURNEnc.encode(a.urn).flatMap { (urnMetadata, urnValue) =>
              ResourceValue(urnValue, idValue).fold(
                e => Result.fail(e),
                r => Result.pure(idMetadata.combine(urnMetadata) -> r.asValue)
              )
            }
          else Result.pure(idMetadata -> idValue) // return plain urn for backward compatibility
        }

  given componentResourceEncoder[A <: ComponentResource](using
    outputURNEnc: Encoder[Output[URN]]
  ): Encoder[A] =
    new Encoder[A]:
      def encode(a: A)(using ctx: Context): Result[(Metadata, Value)] =
        outputURNEnc.encode(a.urn).flatMap { (urnMetadata, urnValue) =>
          if ctx.featureSupport.keepResources then
            ResourceValue(urnValue).fold(
              e => Result.fail(e),
              r => Result.pure(urnMetadata -> r.asValue)
            )
          else Result.pure(urnMetadata -> urnValue) // return plain urn for backward compatibility
        }

  given remoteComponentResourceEncoder[A <: RemoteComponentResource](using
    outputURNEnc: Encoder[Output[URN]]
  ): Encoder[A] =
    new Encoder[A]:
      def encode(a: A)(using ctx: Context): Result[(Metadata, Value)] =
        outputURNEnc.encode(a.urn).flatMap { (urnMetadata, urnValue) =>
          if ctx.featureSupport.keepResources then
            ResourceValue(urnValue).fold(
              e => Result.fail(e),
              r => Result.pure(urnMetadata -> r.asValue)
            )
          else Result.pure(urnMetadata -> urnValue) // return plain urn for backward compatibility
        }

  given stringEncoder: Encoder[String] with
    def encode(str: String)(using Context): Result[(Metadata, Value)] = Result.pure(Metadata.empty -> str.asValue)

  given urnEncoder: Encoder[URN] with
    def encode(urn: URN)(using Context): Result[(Metadata, Value)] = Result.pure(Metadata.empty -> urn.asString.asValue)

  given idEncoder: Encoder[ResourceId] with
    def encode(id: ResourceId)(using Context): Result[(Metadata, Value)] = Result.pure(Metadata.empty -> id.asString.asValue)

  given nesEncoder: Encoder[NonEmptyString] with
    def encode(nes: NonEmptyString)(using Context): Result[(Metadata, Value)] =
      Result.pure(Metadata.empty -> Value(Kind.StringValue(nes.asString)))

  given intEncoder: Encoder[Int] with
    def encode(int: Int)(using Context): Result[(Metadata, Value)] = Result.pure(Metadata.empty -> int.asValue)

  given doubleEncoder: Encoder[Double] with
    def encode(dbl: Double)(using Context): Result[(Metadata, Value)] = Result.pure(Metadata.empty -> dbl.asValue)

  given valueEncoder: Encoder[Value] with
    def encode(vl: Value)(using Context): Result[(Metadata, Value)] = Result.pure(Metadata.empty -> vl)

  given boolEncoder: Encoder[Boolean] with
    def encode(bool: Boolean)(using Context): Result[(Metadata, Value)] = Result.pure(Metadata.empty -> bool.asValue)

  given jsonEncoder: Encoder[JsValue] with
    // TODO not stack-safe
    private def encodeInternal(json: JsValue): Value =
      json match
        case JsObject(fields)  => fields.view.mapValues(encodeInternal(_)).toMap.asValue
        case JsArray(elements) => elements.map(encodeInternal(_)).asValue
        case JsString(str)     => str.asValue
        case JsNumber(num)     => num.toDouble.asValue // TODO unsafe but oh well
        case JsTrue            => true.asValue
        case JsFalse           => false.asValue
        case JsNull            => Null

    def encode(json: JsValue)(using Context): Result[(Metadata, Value)] =
      Result.pure(Metadata.empty -> encodeInternal(json))

  given optEncoder[A](using inner: Encoder[A]): Encoder[Option[A]] with
    def encode(opt: Option[A])(using Context): Result[(Metadata, Value)] =
      opt match
        case Some(value) => inner.encode(value)
        case None        =>
          // It is important to understand that we are sending a "known empty value" here
          // as opposed to an unknown value, see `outputEncoder` for more details.
          Result.pure(Metadata(known = true, secret = false, empty = true, Nil) -> Null)

  given outputEncoder[A](using inner: Encoder[Option[A]]): Encoder[Output[A]] with
    def encode(out: Output[A])(using ctx: Context): Result[(Metadata, Value)] =
      out.getData.flatMap {
        case OutputData.Unknown(resources, secret) =>
          for urns <- urns(resources)
          yield
            // It is important to understand that unknown value is neither empty nor non-empty
            // but we have no ability to express this directly, what we do is:
            // - we make sure the `empty == false` to not short-circuit the Metadata#combine method
            // - we use Metadata#render to determine the appropriate wire representation for gRPC
            val metadata = Metadata(known = false, secret = secret, empty = false, urns)
            metadata -> metadata.render(Null) // we pass null, but in this case it can be anything, because Metadata#render will decide
        case OutputData.Known(resources, secret, maybeValue) =>
          inner.encode(maybeValue).flatMap { (innerMetadata, serializedValue) =>
            for urns <- urns(resources)
            yield
              val combinedMetadata = Metadata(known = true, secret, serializedValue.kind.isNullValue, urns).combine(innerMetadata)
              combinedMetadata -> combinedMetadata.render(serializedValue)
          }
      }

    private def urns(r: Set[Resource]): Result[List[URN]] = Result
      .sequence {
        r.toList.map(_.urn.getValue)
      }
      .map(_.flatten)
  end outputEncoder

  private def assetWrapper(key: String, value: Value): Value = Map(
    SpecialSig.Key -> SpecialSig.AssetSig.asValue,
    key -> value
  ).asValue

  private def archiveWrapper(key: String, value: Value): Value = Map(
    SpecialSig.Key -> SpecialSig.ArchiveSig.asValue,
    key -> value
  ).asValue

  given fileAssetEncoder: Encoder[FileAsset] = new Encoder[FileAsset]:
    def encode(fileAsset: FileAsset)(using Context): Result[(Metadata, Value)] = Result {
      Metadata.empty -> assetWrapper(Constants.AssetOrArchivePathName, fileAsset.path.asValue)
    }

  given remoteAssetEncoder: Encoder[RemoteAsset] = new Encoder[RemoteAsset]:
    def encode(remoteAsset: RemoteAsset)(using Context): Result[(Metadata, Value)] = Result {
      Metadata.empty -> assetWrapper(Constants.AssetOrArchiveUriName, remoteAsset.uri.asValue)
    }

  given stringAssetEncoder: Encoder[StringAsset] = new Encoder[StringAsset]:
    def encode(stringAsset: StringAsset)(using Context): Result[(Metadata, Value)] = Result {
      Metadata.empty -> assetWrapper(Constants.AssetTextName, stringAsset.text.asValue)
    }

  given fileArchiveEncoder: Encoder[FileArchive] = new Encoder[FileArchive]:
    def encode(fileArchive: FileArchive)(using Context): Result[(Metadata, Value)] = Result {
      Metadata.empty -> archiveWrapper(Constants.AssetOrArchivePathName, fileArchive.path.asValue)
    }

  given remoteArchiveEncoder: Encoder[RemoteArchive] = new Encoder[RemoteArchive]:
    def encode(remoteArchive: RemoteArchive)(using Context): Result[(Metadata, Value)] = Result {
      Metadata.empty -> archiveWrapper(Constants.AssetOrArchiveUriName, remoteArchive.uri.asValue)
    }

  given assetArchiveEncoder: Encoder[AssetArchive] = new Encoder[AssetArchive]:
    def encode(assetArchive: AssetArchive)(using Context): Result[(Metadata, Value)] =
      val serializedAssets =
        assetArchive.assets.toVector.map { case (key, assetOrArchive) =>
          assetOrArchiveEncoder.encode(assetOrArchive).map((key, _))
        }

      Result.sequence(serializedAssets).map { vec =>
        val (keys, depsAndValues) = vec.unzip
        val (deps, values)        = depsAndValues.unzip
        val allMetadata           = deps.foldLeft(Metadata.empty)(_.combine(_))

        allMetadata -> archiveWrapper(Constants.ArchiveAssetsName, keys.zip(values).toMap.asValue)
      }

  given assetEncoder: Encoder[Asset] = new Encoder[Asset]:
    def encode(asset: Asset)(using Context): Result[(Metadata, Value)] =
      asset match
        case fa: FileAsset   => fileAssetEncoder.encode(fa)
        case ra: RemoteAsset => remoteAssetEncoder.encode(ra)
        case sa: StringAsset => stringAssetEncoder.encode(sa)
        // case ia: InvalidAsset.type =>
        //   Result.fail(Exception("Cannot serialize invalid asset")) // TODO is this necessary?

  given archiveEncoder: Encoder[Archive] = new Encoder[Archive]:
    def encode(archive: Archive)(using Context): Result[(Metadata, Value)] =
      archive match
        case fa: FileArchive   => fileArchiveEncoder.encode(fa)
        case ra: RemoteArchive => remoteArchiveEncoder.encode(ra)
        case aa: AssetArchive  => assetArchiveEncoder.encode(aa)
        // case ia: InvalidArchive.type =>
        //   Result.fail(Exception("Cannot serialize invalid archive")) // TODO is this necessary?

  given assetOrArchiveEncoder: Encoder[AssetOrArchive] = new Encoder[AssetOrArchive]:
    def encode(assetOrArchive: AssetOrArchive)(using Context): Result[(Metadata, Value)] =
      assetOrArchive match
        case a: Asset   => assetEncoder.encode(a)
        case a: Archive => archiveEncoder.encode(a)

  given listEncoder[A](using innerEncoder: Encoder[A]): Encoder[List[A]] = new Encoder[List[A]]:
    def encode(list: List[A])(using Context): Result[(Metadata, Value)] =
      Result
        .sequence(list.map(innerEncoder.encode(_)))
        .map { lst =>
          val (resources, values) = lst.unzip
          val joinedMetadata      = resources.foldLeft(Metadata.empty)(_.combine(_))

          joinedMetadata -> values.asValue
        }

  // TODO is this ever necessary?
  given vectorEncoder[A](using lstEncoder: Encoder[List[A]]): Encoder[Vector[A]] =
    new Encoder[Vector[A]]:
      def encode(vector: Vector[A])(using Context): Result[(Metadata, Value)] = lstEncoder.encode(vector.toList)

  given mapEncoder[A](using inner: Encoder[A]): Encoder[Map[String, A]] = new Encoder[Map[String, A]]:
    def encode(map: Map[String, A])(using Context): Result[(Metadata, Value)] =
      Result
        .sequence {
          map.toList.map { case (key, value) =>
            inner.encode(value).map(key -> _)
          }
        }
        .map { (entries: List[(String, (Metadata, Value))]) =>
          val (metadata, map) = entries.foldLeft(Metadata.empty -> Map.empty[String, Value]) {
            case ((allMetadata, map), (key, (metadata, value))) =>
              val concatMetadata = allMetadata.combine(metadata)
              if metadata.empty then (concatMetadata, map) // we treat properties with null values as if they do not exist.
              else concatMetadata -> (map + (key -> value))
          }

          metadata -> map.asValue
        }

  // Support for Scala 3 Union Types or Tagged Unions vel Either below:

  transparent inline given unionEncoder[U]: Encoder[U] = ${ unionEncoderImpl[U] }

  import scala.quoted.*

  private def unionEncoderImpl[U: Type](using Quotes) =
    import scala.quoted.quotes.reflect.*
    TypeRepr.of[U] match
      case OrType(t1, t2) =>
        (t1.asType, t2.asType) match
          case ('[t1], '[t2]) =>
            (Expr.summon[Encoder[t1]], Expr.summon[Encoder[t2]]) match
              case (Some(enc1), Some(enc2)) =>
                '{
                  new Encoder[U]:
                    def encode(aOrB: U)(using Context): Result[(Metadata, Value)] = aOrB match
                      case a: t1 => ${ enc1 }.encode(a)
                      case b: t2 => ${ enc2 }.encode(b)
                }
              case (None, None) =>
                report.errorAndAbort(s"Encoders for ${Type.show[t1]} and ${Type.show[t2]} are missing")
              case (None, _) =>
                report.errorAndAbort(s"Encoder for ${Type.show[t1]} is missing")
              case (_, None) =>
                report.errorAndAbort(s"Encoder for ${Type.show[t2]} is missing")

  given eitherEncoder[A, B](using innerA: Encoder[A], innerB: Encoder[B]): Encoder[Either[A, B]] =
    new Encoder[Either[A, B]]:
      def encode(optA: Either[A, B])(using Context): Result[(Metadata, Value)] =
        optA match
          case Left(a)  => innerA.encode(a)
          case Right(b) => innerB.encode(b)

end Encoder

// ArgsEncoder and ProviderArgsEncoder are nearly the same with the small difference, that in ProviderArgsEncoder
// some fields in being serialized to JSON strings, with the exception of `null` and `String`
trait ArgsEncoder[A]:
  def encode(a: A, filterOut: String => Boolean)(using Context): Result[(Map[String, Metadata], Struct)]

object ArgsEncoder:
  inline def derived[A <: Product](using m: Mirror.ProductOf[A]): ArgsEncoder[A] =
    lazy val labels: List[String]        = CodecMacros.summonLabels[m.MirroredElemLabels]
    lazy val instances: List[Encoder[?]] = CodecMacros.summonEncoders[m.MirroredElemTypes]
    lazy val nameEncoderPairs            = labels.zip(instances)

    argsEncoderProduct(nameEncoderPairs)

  private def argsEncoderProduct[A](
    elems: List[(String, Encoder[?])]
  ): ArgsEncoder[A] =
    new ArgsEncoder[A]:
      import Constants.*
      override def encode(a: A, filterOut: String => Boolean)(using Context): Result[(Map[String, Metadata], Struct)] =
        Result
          .sequence {
            a.asInstanceOf[Product]
              .productIterator
              .zip(elems)
              .map { case (v, (label, encoder)) =>
                encoder.asInstanceOf[Encoder[Any]].encode(v).map(res => label -> res)
              }
              .toList
          }
          .map(_.toMap)
          .flatMap { serializedMap =>
            val (mapOfMetadata: Map[String, Metadata], mapOfValues: Map[String, Value]) =
              serializedMap.foldLeft[(Map[String, Metadata], Map[String, Value])](Map.empty -> Map.empty) {
                case ((mapOfMetadata, mapOfValues), (label, (metadata, value))) =>
                  if filterOut(label) then (mapOfMetadata, mapOfValues) // skip filtered
                  else if metadata.empty then
                    (mapOfMetadata, mapOfValues) // we treat properties with NullValue values as if they do not exist
                  else (mapOfMetadata + (label -> metadata), mapOfValues + (label -> value))
              }

            if mapOfMetadata.keySet != mapOfValues.keySet then
              Result.fail(
                Exception(
                  s"Post-condition: expected ArgsEncoder to return metadata and values for the same keys, " +
                    s"got: ${mapOfMetadata.keys.mkString(",")}, ${mapOfValues.keys.mkString(",")}"
                )
              )
            else Result.pure(mapOfMetadata -> mapOfValues.asStruct)
          }
end ArgsEncoder

// ProviderArgsEncoder and ArgsEncoder are nearly the same with the small difference, that in ProviderArgsEncoder
// some fields in being serialized to JSON strings, with the exception of `null` and `String`
trait ProviderArgsEncoder[A] extends ArgsEncoder[A]:
  def encode(a: A, filterOut: String => Boolean)(using Context): Result[(Map[String, Metadata], Struct)]

object ProviderArgsEncoder:
  inline def derived[A <: Product](using m: Mirror.ProductOf[A]): ProviderArgsEncoder[A] =
    lazy val labels: List[String]        = CodecMacros.summonLabels[m.MirroredElemLabels]
    lazy val instances: List[Encoder[?]] = CodecMacros.summonEncoders[m.MirroredElemTypes]
    lazy val nameEncoderPairs            = labels.zip(instances)

    providerArgsEncoderProduct(nameEncoderPairs)

  private def providerArgsEncoderProduct[A](
    elems: List[(String, Encoder[?])]
  ): ProviderArgsEncoder[A] =
    new ProviderArgsEncoder[A]:
      import Constants.*
      override def encode(a: A, filterOut: String => Boolean)(using Context): Result[(Map[String, Metadata], Struct)] =
        Result
          .sequence {
            a.asInstanceOf[Product]
              .productIterator
              .zip(elems)
              .map { case (v, (label, encoder)) =>
                encoder.asInstanceOf[Encoder[Any]].encode(v).flatMap {
                  case (metadata: Metadata, value: Value) => {
                    // We need to unwrap pulumi special structures for outputs and secrets before we serialize the payload to json,
                    // and before we serialize the payload to Json we need to:
                    // - remove all of the special structures to reintroduce them at high level
                    // - bypass null and string protobuf values
                    // - retain the special output structures metadata (i.e. secretness, dependencies)
                    for jsonValue <- maybeWrapAsJson(removeSpecialStructures(value))
                    yield label -> (metadata, metadata.render(jsonValue))
                  }
                }
              }
              .toList
          }
          .map(_.toMap)
          .flatMap { (serializedMap: Map[String, (Metadata, Value)]) =>
            val (mapOfMetadata, mapOfValues) =
              serializedMap.foldLeft[(Map[String, Metadata], Map[String, Value])](Map.empty -> Map.empty) {
                case ( /* acc */ (mapOfMetadata, mapOfValues), /* fields */ (label, (metadata, value))) =>
                  if filterOut(label) then (mapOfMetadata, mapOfValues)
                  else if metadata.empty then
                    (mapOfMetadata, mapOfValues) // we treat properties with NullValue values as if they do not exist
                  else (mapOfMetadata + (label -> metadata), mapOfValues + (label -> value))
              }

            if (mapOfMetadata.keySet != mapOfValues.keySet)
              Result.fail(
                Exception(
                  s"Post-condition: expected ProviderArgsEncoder to return metadata and values for the same keys, " +
                    s"got: ${mapOfMetadata.keys.mkString(",")}, ${mapOfValues.keys.mkString(",")}"
                )
              )
            else
              Result.pure(mapOfMetadata -> mapOfValues.asStruct)
          }

  private def removeSpecialStructures(value: Value): Value =
    value match
      case OutputValue(o) => removeSpecialStructures(o.asRawValueOrUnknown)
      case SecretValue(s) => removeSpecialStructures(s.value)
      case Value(Kind.StructValue(s), _) =>
        Struct(fields = s.fields.map {
          case (k, v) => {
            k -> removeSpecialStructures(v)
          }
        }).asValue
      case Value(Kind.ListValue(l), _) =>
        ListValue(values = l.values.map(removeSpecialStructures)).asValue
      case v => v

  private def maybeWrapAsJson(value: Value): Result[Value] = value match
    // if the value is an empty secret, we don't want to serialize it as a JSON string, ProviderArgsEncoder will filter it out
    case v if v.kind.isNullValue   => Result.pure(v)
    case v if v.kind.isStringValue => Result.pure(v)
    case v                         => wrapAsJson(v)

  private def wrapAsJson(value: Value): Result[Value] =
    Result.evalEither(value.asJsonString).transform {
      case Left(e)           => Left(Exception("Encountered a malformed protobuf Value that could not be serialized to JSON", e))
      case Right(jsonString) => Right(jsonString.asValue)
    }
end ProviderArgsEncoder
