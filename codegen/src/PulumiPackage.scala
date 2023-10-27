package besom.codegen.metaschema

import upickle.implicits.{key => fieldKey}
import besom.codegen.{GeneralCodegenException, UpickleApi}
import besom.codegen.UpickleApi._

/** PulumiPackage describes a Pulumi package.
  *
  * Pulumi package metaschema:
  *   - ../resources/pulumi.json
  *   - https://github.com/pulumi/pulumi/blob/master/pkg/codegen/schema/schema.go
 *
  * @param name
  *   Name is the unqualified name of the package
  * @param version
  *   Version is the version of the package
  * @param meta
  *   Format metadata about this package
  * @param pluginDownloadURL
  *   The URL to use when downloading the provider plugin binary
  * @param types
  *   A map from type token to complexTypeSpec that describes the set of complex types (i.e. object, enum) defined by
  *   this package.
  * @param config
  *   The package's configuration variables
  * @param provider
  *   The provider type for this package, if any
  * @param resources
  *   A map from type token to resourceSpec that describes the set of resources and components defined by this package.
  * @param functions
  *   A map from token to functionSpec that describes the set of functions defined by this package.
  * @param language
  *   Additional language-specific data about the package
  */
case class PulumiPackage(
  name: String,
  version: Option[String] = None,
  meta: Meta = Meta(),
  pluginDownloadURL: Option[String] = None,
  types: Map[String, TypeDefinition] = Map.empty,
  config: ConfigDefinition = ConfigDefinition(),
  provider: ResourceDefinition = ResourceDefinition(),
  resources: Map[String, ResourceDefinition] = Map.empty,
  functions: Map[String, FunctionDefinition] = Map.empty,
  language: Language = Language()
)
object PulumiPackage {
  implicit val reader: Reader[PulumiPackage] = macroR

  /** Reads a Pulumi package from a Pulumi schema JSON file
    * @param filePath
    *   the path to the Pulumi schema JSON file
    * @return
    *   the Pulumi package
    */
  def fromFile(filePath: os.Path): PulumiPackage = {
    // noinspection SimplifyBooleanMatch
    val input = os.exists(filePath) match {
      case false => throw GeneralCodegenException(s"File $filePath does not exist")
      case true  => os.read(filePath)
    }
    val json = ujson.read(input)
    read[PulumiPackage](json)
  }
}

/** Describes a Pulumi resource or component.
  *
  * @param properties
  *   A map from property name to propertySpec that describes the object's properties.
  * @param required
  *   A list of the names of an object type's required properties. These properties must be set for inputs and will
  *   always be set for outputs.
  * @param deprecationMessage
  *   Indicates whether the resource is deprecated
  * @param description
  *   The description of the resource, if any. Interpreted as Markdown.
  * @param inputProperties
  *   A map from property name to propertySpec that describes the resource's input properties.
  * @param isComponent
  *   Indicates whether the resource is a component.
  * @param isOverlay
  *   Indicates that the implementation of the resource should not be generated from the schema, and is instead provided
  *   out-of-band by the package author
  * @param methods
  *   A map from method name to function token that describes the resource's method set.
  * @param requiredInputs
  *   A list of the names of the resource's required input properties.
  */
case class ResourceDefinition(
  properties: Map[String, PropertyDefinition] = Map.empty,
  required: List[String] = Nil,

  // aliases: List[AliasDefinition] = Nil,
  deprecationMessage: Option[String] = None,
  description: Option[String] = None,
  inputProperties: Map[String, PropertyDefinition] = Map.empty,
  isComponent: Boolean = false,
  isOverlay: Boolean = false,
  methods: Map[String, String] = Map.empty,
  requiredInputs: List[String] = Nil
  // stateInputs: ,
) extends ObjectTypeDetails
object ResourceDefinition {
  implicit val reader: Reader[ResourceDefinition] = macroR
}

/** Function describes a Pulumi function a.k.a. functionSpec.
  * @param description
  *   The description of the function, if any. Interpreted as Markdown.
  * @param deprecationMessage
  *   Indicates whether the function is deprecated.
  * @param inputs
  *   The bag of input values for the function, if any.
  * @param multiArgumentInputs
  *   A list of parameter names that determines whether the input bag should be treated as a single argument or as
  *   multiple arguments. The list corresponds to the order in which the parameters should be passed to the function.
  * @param isOverlay
  *   Indicates that the implementation of the function should not be generated from the schema, and is instead provided
  *   out-of-band by the package author.
  */
case class FunctionDefinition(
  description: Option[String] = None,
  deprecationMessage: Option[String] = None,
  isOverlay: Boolean = false,
//  inputs: ObjectTypeDetails = ObjectTypeDetails(),
//  outputs: TypeReference | ObjectTypeDetails = TypeReference | ObjectTypeDetails(),
  multiArgumentInputs: List[String] = Nil
)
object FunctionDefinition {
  implicit val reader: Reader[FunctionDefinition] = macroR
}

/** Config is the set of configuration properties defined by the package.
  * @param variables
  *   A map from variable name to propertySpec that describes a package's configuration variables.
  * @param defaults
  *   A list of the names of the package's non-required configuration variables.
  */
case class ConfigDefinition(
  variables: Map[String, PropertyDefinition] = Map.empty,
  defaults: List[String] = Nil
)
object ConfigDefinition {
  implicit val reader: Reader[ConfigDefinition] = macroR
}

// Language provides hooks for importing language-specific metadata in a package.
case class Language(
  java: Java = Java(),
  nodejs: NodeJs = NodeJs()
)
object Language {
  implicit val reader: Reader[Language] = macroR
}

case class Java(
  packages: Map[String, String] = Map.empty
)
object Java {
  implicit val reader: Reader[Java] = macroR
}

case class NodeJs(
  moduleToPackage: Map[String, String] = Map.empty
)
object NodeJs {
  implicit val reader: Reader[NodeJs] = macroR
}

/** Format metadata about this package.
  *
  * @param moduleFormat
  *   A regex that is used by the importer to extract a module name from the module portion of a type token. Packages
  *   that use the module format "namespace1/namespace2/.../namespaceN" do not need to specify a format. The regex must
  *   define one capturing group that contains the module name, which must be formatted as
  *   "namespace1/namespace2/...namespaceN".
  */
case class Meta(moduleFormat: String = Meta.defaultModuleFormat)
//noinspection ScalaWeakerAccess
object Meta {
  val defaultModuleFormat           = "(.*)"
  implicit val reader: Reader[Meta] = macroR
}

/** @see
  *   [[TypeDefinition]]
  */
case class TypeDefinitionProto(
  `type`: String,
  properties: Map[String, PropertyDefinition] = Map.empty,
  required: List[String] = Nil,
  `enum`: List[EnumValueDefinition] = Nil,
  isOverlay: Boolean = false,
  description: Option[String] = None
)
//noinspection ScalaUnusedSymbol
object TypeDefinitionProto {
  implicit val reader: Reader[TypeDefinitionProto] = macroR
}

sealed trait ConstValue

//noinspection TypeAnnotation
object ConstValue {
  // TODO: Handle other possible data types?
  implicit val reader: Reader[ConstValue] = new SimpleReader[ConstValue] {
    override def expectedMsg                              = "expected string, boolean or integer"
    override def visitString(s: CharSequence, index: Int) = StringConstValue(s.toString)
    override def visitTrue(index: Int)                    = BooleanConstValue(true)
    override def visitFalse(index: Int)                   = BooleanConstValue(false)
    override def visitFloat64(d: Double, index: Int) =
      if (d.isWhole)
        IntConstValue(d.toInt)
      else
        DoubleConstValue(d)
  }
}

case class StringConstValue(value: String) extends ConstValue
case class BooleanConstValue(value: Boolean) extends ConstValue
case class DoubleConstValue(value: Double) extends ConstValue
case class IntConstValue(value: Int) extends ConstValue

/** Describes a Pulumi metaschema object type details
  */
trait ObjectTypeDetails {

  /** @return
    *   A map from property name to propertySpec that describes the object's properties
    */
  def properties: Map[String, PropertyDefinition]

  /** @return
    *   A list of the names of an object type's required properties. These properties must be set for inputs and will
    *   always be set for outputs
    */
  def required: List[String]
}

/** Informs the consumer of an alternative schema based on the value associated with it
  * @param propertyName
  *   PropertyName is the name of the property in the payload that will hold the discriminator value
  * @param mapping
  *   an optional object to hold mappings between payload values and schema names or references
  */
case class Discriminator(propertyName: String, mapping: Map[String, String] = Map.empty)
//noinspection ScalaUnusedSymbol
object Discriminator {
  implicit val reader: Reader[Discriminator] = macroR
}

/** @see
  *   [[TypeReference]] and [[TypeReferenceProto]]
  */
trait TypeReferenceProtoLike {
  def `type`: Option[String]
  def additionalProperties: Option[TypeReference]
  def items: Option[TypeReference]
  def oneOf: List[TypeReference]
  def discriminator: Option[Discriminator]
  @fieldKey("$ref")
  def ref: Option[String]

  def toTypeReference: TypeReference = {
    if (oneOf.nonEmpty) {
      val primitiveType = `type`.map(PrimitiveType.fromString)
      UnionType(oneOf = oneOf, `type` = primitiveType)
    } else {
      ref match {
        case Some(typeUri) =>
          val primitiveType = `type`.map(PrimitiveType.fromString)
          NamedType(typeUri = typeUri, `type` = primitiveType)
        case None =>
          `type`
            .map {
              case "string"  => StringType
              case "integer" => IntegerType
              case "number"  => NumberType
              case "boolean" => BooleanType
              case "array" =>
                ArrayType(items = items.getOrElse(throw new Exception(s"TypeReference $this lacks items")))
              case "object" =>
                MapType(additionalProperties = additionalProperties.getOrElse(StringType))
            }
            .getOrElse(throw new Exception(s"TypeReference '$this' lacks type"))
      }
    }
  }
}

/** @see
  *   [[TypeReference]] and [[TypeReferenceProtoLike]]
  */
case class TypeReferenceProto(
  `type`: Option[String] = None,
  additionalProperties: Option[TypeReference] = None,
  items: Option[TypeReference] = None,
  oneOf: List[TypeReference] = Nil,
  discriminator: Option[Discriminator] = None,
  @fieldKey("$ref") ref: Option[String] = None
) extends TypeReferenceProtoLike
object TypeReferenceProto {
  implicit val reader: Reader[TypeReferenceProto] = macroR
}

/** A reference to a type. The particular kind of type referenced is determined based on the contents of the "type"
  * property and the presence or absence of the "additionalProperties", "items", "oneOf", and "ref" properties.
  * @see
  *   [[TypeReferenceProto]], [[TypeReferenceProtoLike]] and [[besom.codegen.Utils.TypeReferenceOps]]
  */
sealed trait TypeReference {

  /** @return
    *   Indicates that when used as an input, this type does not accept eventual values.
    */
  def plain: Boolean = false // TODO: Handle this
}

object TypeReference {
  implicit val reader: Reader[TypeReference] = TypeReferenceProto.reader.map { proto =>
    proto.toTypeReference
  }
}

/** A reference to a primitive type. A primitive type must have only the "type" property set.
  *
  * Can be one of: [[StringType]], [[IntegerType]], [[NumberType]], [[BooleanType]]
  *
  * @see
  *   [[TypeReference]]
  */
sealed trait PrimitiveType extends TypeReference
object PrimitiveType {
  val fromString: String => PrimitiveType = {
    case "string"  => StringType
    case "integer" => IntegerType
    case "number"  => NumberType
    case "boolean" => BooleanType
  }
  implicit val reader: Reader[PrimitiveType] = UpickleApi.reader[String].map(fromString)
}

object StringType extends PrimitiveType
object IntegerType extends PrimitiveType
object NumberType extends PrimitiveType
object BooleanType extends PrimitiveType

object UrnType extends TypeReference
object ResourceIdType extends TypeReference

/** A reference to an array type. The "type" property must be set to "array" and the "items" property must be present.
  * No other properties may be present.
  * @param items
  *   "The element type of the array"
  */
case class ArrayType(items: TypeReference) extends TypeReference

/** A reference to a map type. The "type" property must be set to "object" and the "additionalProperties" property may
  * be present. No other properties may be present.
  * @param additionalProperties
  *   The element type of the map. Defaults to "string" when omitted.
  */
case class MapType(additionalProperties: TypeReference) extends TypeReference

/** A reference to a type in this or another document. The "ref" property must be present. The "type" property is
  * ignored if it is present. No other properties may be present.
  * @param typeUri
  *   The URI of the referenced type. For example, the built-in Archive, Asset, and Any types are referenced as
  *   "pulumi.json#/Archive", "pulumi.json#/Asset", and "pulumi.json#/Any", respectively. A type from this document is
  *   referenced as "#/types/pulumi:type:token". A type from another document is referenced as
  *   "path#/types/pulumi:type:token", where path is of the form: "/provider/vX.Y.Z/schema.json" or "pulumi.json" or
  *   "http[s]://example.com/provider/vX.Y.Z/schema.json".
  * @param `type`
  *   ignored; present for compatibility with existing schemas
  */
case class NamedType(typeUri: String, `type`: Option[PrimitiveType]) extends TypeReference

/** A reference to a union type. The "oneOf" property must be present. The union may additional specify an underlying
  * primitive type via the "type" property and a discriminator via the "discriminator" property. No other properties may
  * be present.
  * @param oneOf
  *   If present, indicates that values of the type may be one of any of the listed types
  * @param `type`
  *   The underlying primitive type of the union, if any
  * @param discriminator
  *   Informs the consumer of an alternative schema based on the value associated with it
  */
case class UnionType(
  oneOf: List[TypeReference],
  `type`: Option[PrimitiveType],
  discriminator: Option[Discriminator] = None
) extends TypeReference

/** @see
  *   [[PropertyDefinition]]
  */
case class PropertyDefinitionProto(
  `type`: Option[String] = None,
  additionalProperties: Option[TypeReference] = None,
  items: Option[TypeReference] = None,
  oneOf: List[TypeReference] = Nil,
  discriminator: Option[Discriminator] = None,
  @fieldKey("$ref") ref: Option[String] = None,
  const: Option[ConstValue] = None,
  default: Option[ConstValue] = None,
  /* defaultInfo */
  deprecationMessage: Option[String] = None,
  description: Option[String] = None,
  // language: ,
  replaceOnChanges: Boolean = false,
  willReplaceOnChanges: Boolean = false,
  secret: Boolean = false
) extends TypeReferenceProtoLike
object PropertyDefinitionProto {
  implicit val reader: Reader[PropertyDefinitionProto] = macroR
}

/** Describes an object or resource property
  *
  * @see
  *   [[PropertyDefinitionProto]]
  * @param typeReference
  *   A reference to a type that describes the property's type
  * @param const
  *   The constant value for the property, if any. The type of the value must be assignable to the type of the property.
  * @param default
  *   The default value for the property, if any. The type of the value must be assignable to the type of the property.
  * @param deprecationMessage
  *   Indicates whether the property is deprecated
  * @param description
  *   The description of the property, if any. Interpreted as Markdown.
  * @param replaceOnChanges
  *   Specifies whether a change to the property causes its containing resource to be replaced instead of updated
  *   (default false).
  * @param willReplaceOnChanges
  *   Indicates that the provider will replace the resource when this property is changed.
  * @param secret
  *   Specifies whether the property is secret (default false).
  */
case class PropertyDefinition(
  typeReference: TypeReference,
  const: Option[ConstValue] = None,
  default: Option[ConstValue] = None,
  deprecationMessage: Option[String] = None,
  description: Option[String] = None,
  replaceOnChanges: Boolean = false,
  willReplaceOnChanges: Boolean = false,
  secret: Boolean = false
)
object PropertyDefinition {
  implicit val reader: Reader[PropertyDefinition] = PropertyDefinitionProto.reader.map { proto =>
    PropertyDefinition(
      typeReference = proto.toTypeReference,
      const = proto.const,
      default = proto.default,
      deprecationMessage = proto.deprecationMessage,
      description = proto.description,
      replaceOnChanges = proto.replaceOnChanges,
      willReplaceOnChanges = proto.willReplaceOnChanges,
      secret = proto.secret
    )
  }
}

// TODO Handle `value`s of other primitive types

/** Describes a Pulumi metaschema enum value
  * @param value
  *   The enum value itself
  * @param name
  *   If present, overrides the name of the enum value that would usually be derived from the value
  * @param description
  *   The description of the enum value, if any. Interpreted as Markdown
  * @param deprecationMessage
  *   Indicates whether the value is deprecated
  */
case class EnumValueDefinition(
  value: ConstValue,
  name: Option[String] = None,
  description: Option[String] = None,
  deprecationMessage: Option[String] = None
)
//noinspection ScalaUnusedSymbol
object EnumValueDefinition {
  implicit val reader: Reader[EnumValueDefinition] = macroR
}

/** Describes an object or enum type, a.k.a. a complex type (complexTypeSpec).
  *
  * Can be one of: [[EnumTypeDefinition]] or [[ObjectTypeDefinition]]
  *
  * @param description
  *   The description of the type, if any. Interpreted as Markdown.
  * @param isOverlay
  *   Indicates that the implementation of the type should not be generated from the schema, and is instead provided
  *   out-of-band by the package author
  */
sealed abstract class TypeDefinition(
  description: Option[String] = None,
  isOverlay: Boolean = false
)
object TypeDefinition {
  implicit val reader: Reader[TypeDefinition] = UpickleApi.reader[TypeDefinitionProto].map { proto =>
    if (proto.`enum`.nonEmpty) {
      EnumTypeDefinition(
        `enum` = proto.`enum`,
        `type` = PrimitiveType.fromString(proto.`type`),
        isOverlay = proto.isOverlay
      )
    } else {
      ObjectTypeDefinition(
        properties = proto.properties,
        required = proto.required,
        isOverlay = proto.isOverlay
      )
    }
  }
}

/** Describes a Pulumi metaschema enum type
  * @param enum
  *   The list of possible values for the enum
  * @param type
  *   The underlying primitive type of the enum, one of: string, integer, number, boolean
  */
case class EnumTypeDefinition(
  `enum`: List[EnumValueDefinition],
  `type`: PrimitiveType,
  description: Option[String] = None,
  isOverlay: Boolean = false
) extends TypeDefinition

/** Describes a Pulumi metaschema object type
  * @param properties
  *   A map from property name to propertySpec that describes the object's properties
  * @param required
  *   A list of the names of an object type's required properties. These properties must be set for inputs and will
  *   always be set for outputs
  */
case class ObjectTypeDefinition(
  properties: Map[String, PropertyDefinition],
  required: List[String] = Nil,
  description: Option[String] = None,
  isOverlay: Boolean = false
) extends TypeDefinition
    with ObjectTypeDetails
