package besom.util

import scala.compiletime.*
import scala.compiletime.ops.string.*
import scala.language.implicitConversions

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
          "this string isn't a correct pulumi type identifier, it should contain two colons between three identifiers"
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
    def withKey(key: String): Label = s"$label.$key" // ie.: resource:myBucket[aws:s3:Bucket].url
    def atIndex(index: Int): Label  = s"$label($index)" // ie.: resource:myBucket[aws:s3:Bucket].files(0)

  object Label:
    def fromNameAndType(name: NonEmptyString, rt: ResourceType): Label = s"resource:$name[$rt]"
