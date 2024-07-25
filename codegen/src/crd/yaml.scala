package besom.codegen.crd

import besom.codegen.*
import org.virtuslab.yaml.*
import org.virtuslab.yaml.Node.ScalarNode

import scala.util.Try

case class CRD(spec: CRDSpec) derives YamlDecoder
case class CRDSpec(names: CRDNames, versions: Seq[CRDVersion]) derives YamlDecoder
case class CRDNames(singular: String, kind: String) derives YamlDecoder
case class CRDVersion(name: String, schema: CustomResourceValidation) derives YamlDecoder
case class CustomResourceValidation(openAPIV3Schema: JsonSchemaProps) derives YamlDecoder
case class JsonSchemaProps(
  `enum`: Option[List[String]],
  `type`: Option[DataTypeEnum],
  format: Option[String],
  required: Option[Set[String]],
  items: Option[JsonSchemaProps],
  properties: Option[Map[String, JsonSchemaProps]],
  additionalProperties: Option[Boolean | JsonSchemaProps]
) derives YamlDecoder

object JsonSchemaProps:
  given YamlDecoder[DataTypeEnum] = YamlDecoder { case s @ ScalarNode(value, _) =>
    Try(DataTypeEnum.valueOf(value)).toEither.left
      .map(ConstructError.from(_, "enum DataTypeEnum", s))
  }
  given YamlDecoder[Option[JsonSchemaProps]] = YamlDecoder { case node =>
    YamlDecoder.forOption[JsonSchemaProps].construct(node)
  }
  given YamlDecoder[Map[String, JsonSchemaProps]] = YamlDecoder { case node =>
    YamlDecoder.forMap[String, JsonSchemaProps].construct(node)
  }
  given booleanOrJsonSchemaProps: YamlDecoder[Boolean | JsonSchemaProps] = YamlDecoder { case node =>
    YamlDecoder.forBoolean
      .construct(node)
      .left
      .flatMap(_ => summon[YamlDecoder[JsonSchemaProps]].construct(node))
  }

enum DataTypeEnum:
  case string extends DataTypeEnum
  case integer extends DataTypeEnum
  case number extends DataTypeEnum
  case `object` extends DataTypeEnum
  case boolean extends DataTypeEnum
  case array extends DataTypeEnum

enum StringFormat:
  case date extends StringFormat
  case `date-time` extends StringFormat
  case password extends StringFormat
  case byte extends StringFormat
  case binary extends StringFormat

enum NumberFormat:
  case float extends NumberFormat
  case double extends NumberFormat

enum IntegerFormat:
  case int32 extends IntegerFormat
  case int64 extends IntegerFormat
