package besom.codegen.metaschema

import java.io.File
import upickle.implicits.{key => fieldKey}

import besom.codegen.UpickleApi
import besom.codegen.UpickleApi._

case class PulumiPackage(name: String, version: String, language: Language, meta: Meta, types: Map[String, TypeDefinition] = Map.empty, provider: ResourceDefinition, resources: Map[String, ResourceDefinition])
object PulumiPackage {
  implicit val reader: Reader[PulumiPackage] = macroR

  def fromFile(filePath: String) = {
    val input = ujson.Readable.fromFile(new File(filePath))
    val json = ujson.read(input)
    read[PulumiPackage](json)
  }
}

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

case class Language(java: Java = Java(packages = Map.empty))
object Language {
  implicit val reader: Reader[Language] = macroR 
}

case class Java(packages: Map[String, String])
object Java {
  implicit val reader: Reader[Java] = macroR 
}

case class Meta(moduleFormat: String = "(.*)")
object Meta {
  implicit val reader: Reader[Meta] = macroR 
}

case class TypeDefinitionProto(`type`: String, properties: Map[String, PropertyDefinition] = Map.empty, required: List[String] = Nil, `enum`: List[EnumValueDefinition] = Nil, isOverlay: Boolean = false)
object TypeDefinitionProto {
  implicit val reader: Reader[TypeDefinitionProto] = macroR
}

trait ObjectTypeDetails {
  def properties: Map[String, PropertyDefinition]
  def required: List[String]
}

case class Discriminator(propertyName: String, mapping: Map[String, String] = Map.empty)

object Discriminator {
  implicit val reader: Reader[Discriminator] = macroR
}

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
          `type`.map {
            case "string" => StringType
            case "integer" => IntegerType
            case "number" => NumberType
            case "boolean" => BooleanType
            case "array" =>
              ArrayType(items = items.getOrElse(throw new Exception(s"TypeReference ${this} lacks items")))
            case "object" => 
              MapType(additionalProperties = additionalProperties.getOrElse(StringType))
          }.getOrElse(throw new Exception(s"TypeReference ${this} lacks type"))
      }
    } 
  }
}

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

sealed trait TypeReference

object TypeReference {
  implicit val reader: Reader[TypeReference] = TypeReferenceProto.reader.map { proto =>
    proto.toTypeReference
  }
}

sealed trait PrimitiveType extends TypeReference
object PrimitiveType {
  val fromString: String => PrimitiveType = {
    case "string" => StringType
    case "integer" => IntegerType
    case "number" => NumberType
    case "boolean" => BooleanType
  }
  implicit val reader: Reader[PrimitiveType] = UpickleApi.reader[String].map(fromString)
}

object StringType extends PrimitiveType
object IntegerType extends PrimitiveType
object NumberType extends PrimitiveType
object BooleanType extends PrimitiveType

case class ArrayType(items: TypeReference) extends TypeReference
case class MapType(additionalProperties: TypeReference) extends TypeReference
case class UnionType(oneOf: List[TypeReference], `type`: Option[PrimitiveType]) extends TypeReference

case class NamedType(typeUri: String, `type`: Option[PrimitiveType]) extends TypeReference

case class PropertyDefinitionProto(
  `type`: Option[String] = None,
  additionalProperties: Option[TypeReference] = None,
  items: Option[TypeReference] = None,
  oneOf: List[TypeReference] = Nil,
  discriminator: Option[Discriminator] = None,
  @fieldKey("$ref") ref: Option[String] = None,

  /* const, default, defaultInfo */
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

case class PropertyDefinition(
  typeReference: TypeReference,
  /* const, default, defaultInfo */
  deprecationMessage: Option[String] = None,
  description: Option[String] = None,
  // language: ,
  replaceOnChanges: Boolean = false,
  willReplaceOnChanges: Boolean = false,
  secret: Boolean = false,

  
) //extends TypeReference
object PropertyDefinition {
  implicit val reader: Reader[PropertyDefinition] = PropertyDefinitionProto.reader.map { proto =>
    PropertyDefinition(
      typeReference = proto.toTypeReference,
      deprecationMessage = proto.deprecationMessage,
      description = proto.description,
      replaceOnChanges = proto.replaceOnChanges,
      willReplaceOnChanges = proto.willReplaceOnChanges,
      secret = proto.secret
    )
  }
}


// TODO Handle `value`s of other primitive types
case class EnumValueDefinition(value: String, name: Option[String] = None, description: Option[String] = None, deprecationMessage: Option[String] = None)
object EnumValueDefinition {
  implicit val reader: Reader[EnumValueDefinition] = macroR
}

sealed trait TypeDefinition {
  def isOverlay: Boolean
}

object TypeDefinition {
  implicit val reader: Reader[TypeDefinition] = UpickleApi.reader[TypeDefinitionProto].map { proto =>
    if (proto.`enum`.nonEmpty) {
      EnumTypeDefinition(`enum` = proto.`enum`, `type` = PrimitiveType.fromString(proto.`type`), isOverlay = proto.isOverlay)
    } else {
      ObjectTypeDefinition(properties = proto.properties, required = proto.required, isOverlay = proto.isOverlay)
    }
  }
}

case class EnumTypeDefinition(`enum`: List[EnumValueDefinition], `type`: PrimitiveType, isOverlay: Boolean) extends TypeDefinition
case class ObjectTypeDefinition(properties: Map[String, PropertyDefinition], required: List[String] = Nil, isOverlay: Boolean) extends TypeDefinition with ObjectTypeDetails
