package besom

import scala.compiletime.*
import scala.compiletime.ops.string.*
import scala.language.implicitConversions
import scala.util.Try
import com.google.protobuf.struct.*
import besom.internal.ProtobufUtil.*
import besom.util.*
import besom.internal.*

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

  /** Pulumi type token, used to identify the type of a resource, provider, or function.
    */
  opaque type PulumiToken <: String = String

  extension (pt: PulumiToken)
    /** @return
      *   the Pulumi package name of the [[ResourceType]], for example: `aws`, `kubernetes`, `random`
      */
    def getPackage: String = if isProviderType then pt.split(":").last else pt.split(":").head

    /** @return
      *   true if the [[ResourceType]] is a provider, false otherwise
      */
    def isProviderType: Boolean = pt.startsWith("pulumi:providers:")

  /** Each resource is an instance of a specific Pulumi resource type. This type is specified by a type token in the format
    * `<package>:<module>:<typename>`, where:
    *   - The `<package>` component of the type (e.g. aws, azure-native, kubernetes, random) specifies which Pulumi Package defines the
    *     resource. This is mapped to the package in the Pulumi Registry and to the per-language Pulumi SDK package.
    *   - The `<module>` component of the type (e.g. s3/bucket, compute, apps/v1, index) is the module path where the resource lives within
    *     the package. It is / delimited by component of the path. The name index indicates that the resource is not nested, and is instead
    *     available at the top level of the package. Per-language Pulumi SDKs use the module path to emit nested namespaces/modules in a
    *     language-specific way to organize all the types defined in a package.
    *   - The `<typename>` component of the type (e.g. Bucket, VirtualMachine, Deployment, RandomPassword) is the identifier used to refer
    *     to the resource itself. It is mapped to the class or constructor name in the per-language Pulumi SDK.
    */
  opaque type ResourceType <: PulumiToken = String

  /** See [[ResourceType]] type for more information.
    */
  object ResourceType:
    /** Parse a resource type string into a [[ResourceType]].
      * @param s
      *   a resource type string to parse
      * @return
      *   a [[ResourceType]] if the string is valid, otherwise an compile time error occurs
      */
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

  /** A special [[ResourceType]] that identifies a Pulumi provider.
    */
  opaque type ProviderType <: ResourceType = String

  /** See [[ProviderType]] type for more information.
    */
  object ProviderType:

    /** Create a [[ProviderType]] with a provided `provider`` identifier, in format `pulumi:providers:${provider}`.
      *
      * @param provider
      *   a provider identifier
      * @return
      *   a [[ProviderType]] with the provided identifier
      */
    def apply(provider: NonEmptyString): ProviderType = s"pulumi:providers:${provider}"

    /** Parse a provider type [[String]] into a [[ProviderType]].
      * @param s
      *   a provider type string to parse
      * @return
      *   a [[ProviderType]] if the string is valid, otherwise an compile time error occurs
      */
    // validate that provider type contains a prefix of `pulumi:providers:` and the provider identifier
    inline def from(s: String): ProviderType =
      requireConst(s)
      inline if !constValue[Matches[s.type, "pulumi:providers:.+"]] then
        error("this string doesn't have a prefix of `pulumi:providers:` or the provider identifier is missing")
      else s

    // TODO should we use Conversion?
    implicit inline def str2ProviderType(inline s: String): ProviderType = ProviderType.from(s)

  end ProviderType

  /** A special [[ResourceType]] that identifies a Pulumi provider function.
    */
  opaque type FunctionToken <: PulumiToken = String

  /** See [[FunctionToken]] type for more information.
    */
  object FunctionToken:
    /** Parse a function token [[String]] into a [[FunctionToken]].
      * @param s
      *   a function token string to parse
      * @return
      *   a [[FunctionToken]] if the string is valid, otherwise an compile time error occurs
      */
    // validate that function token contains two colons between three identifiers, see @ResourceType
    inline def from(s: String): FunctionToken =
      requireConst(s)
      inline if !constValue[Matches[s.type, ".+:.+:.+"]] then error("Invalid function token")
      else s

    implicit inline def str2FunctionToken(inline s: String): FunctionToken = FunctionToken.from(s)

  end FunctionToken

  /** A logger label.
    */
  opaque type Label <: String = String

  extension (label: Label)
    def withKey(key: String): Label = s"$label.$key" // ie.: myBucket[aws:s3:Bucket].url
    def atIndex(index: Int): Label  = s"$label($index)" // ie.: myBucket[aws:s3:Bucket].files(0)

  object Label:
    def fromNameAndType(name: NonEmptyString, rt: ResourceType): Label = s"$name[$rt]"
    def fromFunctionToken(ft: FunctionToken): Label                    = s"$ft()"

  /** URN is an automatically constructed globally unique identifier for the resource
    */
  opaque type URN = String

  /** URN is and automatically constructed globally unique identifier for the resource
    */
  object URN:
    /** The instance of [[URN]] that represents an empty URN
      */
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
      /** @return
        *   the [[URN]] as a [[String]]
        */
      def asString: String = urn

      /** @return
        *   the Pulumi stack name
        */
      def stack: String = URN.UrnRegex.findFirstMatchIn(urn).get.group("stack")

      /** @return
        *   the Pulumi project name
        */
      def project: String = URN.UrnRegex.findFirstMatchIn(urn).get.group("project")

      /** @return
        *   the type of the parent [[besom.internal.Resource]]
        */
      def parentType: String = URN.UrnRegex.findFirstMatchIn(urn).get.group("parentType")

      /** @return
        *   the type of this [[besom.internal.Resource]]
        */
      def resourceType: Option[String] =
        URN.UrnRegex.findFirstMatchIn(urn).map(_.group("resourceType")).flatMap(Option(_))

      /** @return
        *   the logical name of this [[besom.internal.Resource]]
        */
      def resourceName: String = URN.UrnRegex.findFirstMatchIn(urn).get.group("resourceName")
  end URN

  // TODO This should not be a subtype of string, user's access to underlying string has no meaning
  // TODO User should never modify Resource Ids, they should be opaque but they should also be
  // TODO parameterized with their resource type, ie.: ResourceId[aws.s3.Bucket]
  /** A resource’s physical name, known to the outside world.
    */
  opaque type ResourceId <: String = String

  /** A resource’s physical name, known to the outside world.
    */
  object ResourceId:
    /** The instance of [[ResourceId]] that represents an empty resource ID
      */
    val empty: ResourceId = ""

    def apply(nes: NonEmptyString): ResourceId = nes

    implicit inline def str2ResourceId(inline s: String): ResourceId = apply(NonEmptyString.from(s))

    // this should be used ONLY in Decoder and RawResourceResult smart constructors
    private[besom] def unsafeOf(s: String): ResourceId = s

    extension (id: ResourceId)
      /** @return
        *   the [[ResourceId]] as a [[String]]
        */
      private[besom] def asString: String = id

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

  sealed trait PulumiEnum[V]:
    def name: String
    def value: V

  trait BooleanEnum extends PulumiEnum[Boolean]

  trait IntegerEnum extends PulumiEnum[Int]

  trait NumberEnum extends PulumiEnum[Double]

  trait StringEnum extends PulumiEnum[String]

  trait EnumCompanion[V, E <: PulumiEnum[V]](enumName: String):
    def allInstances: Seq[E]
    private lazy val valuesToInstances: Map[Any, E] = allInstances.map(instance => instance.value -> instance).toMap

    extension [A](a: A)
      def asValueAny: Value = a match
        case a: Int     => a.asValue
        case a: Double  => a.asValue
        case a: Boolean => a.asValue
        case a: String  => a.asValue

    given Encoder[E] = (a: E) => Result.pure(Set.empty -> a.value.asValueAny)
    given (using decV: Decoder[V]): Decoder[E] =
      decV.emap { (value, label) =>
        valuesToInstances
          .get(value)
          .toValidatedResultOrError(DecodingError(s"$label: `${value}` is not a valid value of `${enumName}`", label = label))
      }

  export besom.aliases.{*, given}
end types
