package besom

import scala.compiletime.*
import scala.compiletime.ops.string.*
import scala.language.implicitConversions
import scala.util.Try
import besom.util.NonEmptyString
import besom.internal.{Decoder, Encoder, DecodingError}
import besom.internal.ResourceDecoder
import besom.internal.CustomResourceOptions
import besom.internal.ArgsEncoder

object types:
  // TODO: replace these stubs with proper implementations
  private object Opaques:
    opaque type PulumiAny = spray.json.JsValue
    object PulumiAny:
      given Encoder[PulumiAny] = Encoder.jsonEncoder
      given Decoder[PulumiAny] = Decoder.jsonDecoder

    opaque type PulumiJson = spray.json.JsValue
    object PulumiJson:
      given Encoder[PulumiJson] = Encoder.jsonEncoder
      given Decoder[PulumiJson] = Decoder.jsonDecoder

  export Opaques.*

  opaque type ResourceType <: String = String

  extension (rt: ResourceType)
    def getPackage: String      = if isProviderType then rt.split(":").last else rt.split(":").head
    def isProviderType: Boolean = rt.startsWith("pulumi:providers:")

  object ResourceType:
    // validate that resource type contains two colons between three identifiers, special characters are allowed, for instance:
    // pulumi:providers:kubernetes is a valid type
    // kubernetes:core/v1:ConfigMap is a valid type
    inline def from(s: String): ResourceType =
      requireConst(s)
      inline if !constValue[Matches[s.type, ".+:.+:.+"]] then
        error(
          """A component resource must register a unique type name with the base constructor, for example: `pkg:index:MyComponent`. 
  To reduce the potential of other type name conflicts, this name contains the package and module name, in addition to the type: <package>:<module>:<type>. 
  These names are namespaced alongside non-component resources, such as `aws:lambda:Function`."""
        )
      else s

    implicit inline def str2ResourceType(inline s: String): ResourceType = ResourceType.from(s)

  end ResourceType

  opaque type ProviderType <: ResourceType = String

  object ProviderType:

    def apply(provider: NonEmptyString): ProviderType = s"pulumi:providers:${provider}"

    // validate that provider type contains a prefix of `pulumi:providers:` and the provider identifier
    inline def from(s: String): ProviderType =
      requireConst(s)
      inline if !constValue[Matches[s.type, "pulumi:providers:.+"]] then
        error("this string doesn't have a prefix of `pulumi:providers:` or the provider identifier is missing")
      else s

    // TODO should we use Conversion?
    implicit inline def str2ProviderType(inline s: String): ProviderType = ProviderType.from(s)

  end ProviderType

  opaque type Label <: String = String

  extension (label: Label)
    def withKey(key: String): Label = s"$label.$key" // ie.: myBucket[aws:s3:Bucket].url
    def atIndex(index: Int): Label  = s"$label($index)" // ie.: myBucket[aws:s3:Bucket].files(0)

  object Label:
    def fromNameAndType(name: NonEmptyString, rt: ResourceType): Label = s"$name[$rt]"

  opaque type URN = String
  object URN:
    val empty: URN = ""

    // This is implemented according to https://www.pulumi.com/docs/concepts/resources/names/#urns
    private[types] val UrnRegex =
      """urn:pulumi:(?<stack>[^:]+)::(?<project>[^:]+)::(?<parentType>.+?)((\$(?<resourceType>.+))?)::(?<resourceName>.+)""".r

    private[besom] inline def apply(s: String): URN =
      requireConst(s)
      inline if !constValue[Matches[
          s.type,
          """urn:pulumi:(?<stack>[^:]+)::(?<project>[^:]+)::(?<parentType>.+?)((\$(?<resourceType>.+))?)::(?<resourceName>.+)"""
        ]]
      then
        error(
          "this string doesn't match the URN format, see https://www.pulumi.com/docs/concepts/resources/names/#urns"
        )
      else s

    // TODO this should be only usable in Decoder and RawResourceResult.fromResponse
    private[besom] def from(s: String): Try[URN] = Try {
      if UrnRegex.matches(s) then s
      else throw IllegalArgumentException(s"URN $s is not valid")
    }

    // trait CanDeserializeURN:
    //   protected def parseURN(s: String): Try[URN] = Try {
    //     if UrnRegex.matches(s) then s
    //     else throw IllegalArgumentException(s"URN $s is not valid")
    //   }

    extension (urn: URN)
      def asString: String   = urn
      def stack: String      = URN.UrnRegex.findFirstMatchIn(urn).get.group("stack")
      def project: String    = URN.UrnRegex.findFirstMatchIn(urn).get.group("project")
      def parentType: String = URN.UrnRegex.findFirstMatchIn(urn).get.group("parentType")
      def resourceType: Option[String] =
        URN.UrnRegex.findFirstMatchIn(urn).map(_.group("resourceType")).flatMap(Option(_))
      def resourceName: String = URN.UrnRegex.findFirstMatchIn(urn).get.group("resourceName")

  // TODO This should not be a subtype of string, user's access to underlying string has no meaning
  // TODO User should never modify Resource Ids, they should be opaque but they should also be
  // TODO parameterized with their resource type, ie.: ResourceId[aws.s3.Bucket]
  opaque type ResourceId <: String = String
  object ResourceId:
    val empty: ResourceId = ""

    // this should be only usable in Decoder and RawResourceResult.fromResponse
    private[besom] def apply(s: String): ResourceId = s

    extension (id: ResourceId) def asString: String = id

  sealed trait AssetOrArchive

  enum Asset extends AssetOrArchive:
    case FileAsset(path: String) // TODO: java.nio.file.Path? validate it's a correct extension or MIME at compile time?
    case StringAsset(text: String)
    // TODO: a proper URI type? validate it's a proper URI? allows file://, http(s)://, custom schemes
    case RemoteAsset(uri: String)
    // case InvalidAsset // TODO - should we use this?

  enum Archive extends AssetOrArchive:
    case FileArchive(
      path: String
    ) // TODO: java.nio.file.Path? validate it's a correct extension or MIME at compile time?
    // TODO: a proper URI type? validate it's a proper URI? allows file://, http(s)://, custom schemes
    case RemoteArchive(uri: String)
    case AssetArchive(assets: Map[String, AssetOrArchive])
    // case InvalidArchive // TODO - should we use this?

  sealed trait PulumiEnum:
    def name: String

  trait BooleanEnum extends PulumiEnum:
    def value: Boolean

  trait IntegerEnum extends PulumiEnum:
    def value: Int

  trait NumberEnum extends PulumiEnum:
    def value: Double

  trait StringEnum extends PulumiEnum:
    def value: String

  trait EnumCompanion[E <: PulumiEnum](enumName: String):
    def allInstances: Seq[E]
    private lazy val namesToInstances: Map[String, E] = allInstances.map(instance => instance.name -> instance).toMap

    given Encoder[E] = Encoder.stringEncoder.contramap(instance => instance.name)
    given Decoder[E] = Decoder.stringDecoder.emap { (name, label) =>
      namesToInstances.get(name).toRight(DecodingError(s"$label: `${name}` is not a valid name of `${enumName}`"))
    }

  export besom.aliases.{*, given}
