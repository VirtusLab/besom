package besom.util

import scala.compiletime.*
import scala.compiletime.ops.string.*
import scala.language.implicitConversions
import scala.util.Try

// TODO this should be besom.Types, not besom.util.Types nor besom.internal.Types
object Types:

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
    private[Types] val UrnRegex =
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

  opaque type ResourceId = String
  object ResourceId:
    val empty: ResourceId = ""

    // this should be only usable in Decoder and RawResourceResult.fromResponse
    private[besom] def apply(s: String): ResourceId = s

    extension (id: ResourceId) def asString: String = id
