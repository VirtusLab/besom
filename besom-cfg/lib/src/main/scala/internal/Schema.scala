package besom.cfg.internal

import scala.quoted.*
import besom.json.*

enum FieldType:
  case Int, Long, Float, Double, String, Boolean
  case Array(inner: FieldType)
  case Struct(fields: (String, FieldType)*)
  case Optional(inner: FieldType)

object FieldType:
  given ToExpr[FieldType] with
    def apply(fieldType: FieldType)(using Quotes): Expr[FieldType] =
      import quotes.reflect.*
      fieldType match
        case FieldType.Int                => '{ FieldType.Int }
        case FieldType.Long               => '{ FieldType.Long }
        case FieldType.Float              => '{ FieldType.Float }
        case FieldType.Double             => '{ FieldType.Double }
        case FieldType.String             => '{ FieldType.String }
        case FieldType.Boolean            => '{ FieldType.Boolean }
        case FieldType.Array(inner)       => '{ FieldType.Array(${ Expr(inner) }) }
        case FieldType.Struct(fields: _*) => '{ FieldType.Struct(${ Expr(fields) }: _*) }
        case FieldType.Optional(inner)    => '{ FieldType.Optional(${ Expr(inner) }) }

  given FromExpr[FieldType] with
    def unapply(expr: Expr[FieldType])(using Quotes): Option[FieldType] =
      import quotes.reflect.*
      expr match
        case '{ FieldType.Int }                 => Some(FieldType.Int)
        case '{ FieldType.Long }                => Some(FieldType.Long)
        case '{ FieldType.Float }               => Some(FieldType.Float)
        case '{ FieldType.Double }              => Some(FieldType.Double)
        case '{ FieldType.String }              => Some(FieldType.String)
        case '{ FieldType.Boolean }             => Some(FieldType.Boolean)
        case '{ FieldType.Array($inner) }       => Some(FieldType.Array(inner.valueOrAbort))
        case '{ FieldType.Struct($fields: _*) } => Some(FieldType.Struct(fields.valueOrAbort: _*))
        case '{ FieldType.Optional($inner) }    => Some(FieldType.Optional(inner.valueOrAbort))
        case _                                  => println("didn't match in FieldType"); None

  given JsonFormat[FieldType] with
    def write(fieldType: FieldType): JsValue = fieldType match
      case FieldType.Int          => JsObject("type" -> JsString("int"))
      case FieldType.Long         => JsObject("type" -> JsString("long"))
      case FieldType.Float        => JsObject("type" -> JsString("float"))
      case FieldType.Double       => JsObject("type" -> JsString("double"))
      case FieldType.String       => JsObject("type" -> JsString("string"))
      case FieldType.Boolean      => JsObject("type" -> JsString("boolean"))
      case FieldType.Array(inner) => JsObject("type" -> JsString("array"), "inner" -> write(inner))
      case FieldType.Struct(fields: _*) =>
        JsObject(
          "type" -> JsString("struct"),
          "fields" -> JsObject(fields.map { case (k, v) => k -> write(v) }.toMap)
        )
      case FieldType.Optional(inner) => JsObject("type" -> JsString("optional"), "inner" -> write(inner))

    def read(json: JsValue): FieldType = json match
      case JsObject(fields) =>
        fields.get("type") match
          case Some(JsString("int"))     => FieldType.Int
          case Some(JsString("long"))    => FieldType.Long
          case Some(JsString("float"))   => FieldType.Float
          case Some(JsString("double"))  => FieldType.Double
          case Some(JsString("string"))  => FieldType.String
          case Some(JsString("boolean")) => FieldType.Boolean
          case Some(JsString("array")) =>
            fields.get("inner") match
              case Some(inner) => FieldType.Array(read(inner))
              case _           => throw new Exception("Invalid JSON: array.inner must be present")
          case Some(JsString("struct")) =>
            fields.get("fields") match
              case Some(JsObject(innerFields)) =>
                val structFields = innerFields.map { case (k, v) => k -> read(v) }
                FieldType.Struct(structFields.toVector: _*)
              case None => throw new Exception("Invalid JSON: struct.fields must be present")
              case _    => throw new Exception("Invalid JSON: struct.fields must be an object")
          case Some(JsString("optional")) =>
            fields.get("inner") match
              case Some(inner) => FieldType.Optional(read(inner))
              case _           => throw new Exception("Invalid JSON: optional.inner must be present")
          case Some(what) =>
            throw new Exception(s"Invalid JSON: unknown type $what")
          case None => throw new Exception("Invalid JSON: type must present")
      case _ => throw new Exception("Invalid JSON: expected object")
  end given
end FieldType

case class Field(name: String, `type`: FieldType)
object Field:
  given ToExpr[Field] with
    def apply(field: Field)(using Quotes): Expr[Field] =
      import quotes.reflect.*
      '{ Field(${ Expr(field.name) }, ${ Expr(field.`type`) }) }

  given FromExpr[Field] with
    def unapply(expr: Expr[Field])(using Quotes): Option[Field] =
      import quotes.reflect.*
      expr match
        case '{ Field($name, $fieldType) } => Some(Field(name.valueOrAbort, fieldType.valueOrAbort))
        case _                             => println("didn't match in Field"); None

  given fieldGiven(using fieldTypeWriter: JsonFormat[FieldType]): JsonFormat[Field] with
    def write(field: Field): JsValue =
      JsObject("name" -> JsString(field.name), "details" -> fieldTypeWriter.write(field.`type`))
    def read(json: JsValue): Field =
      json match
        case JsObject(fields) =>
          val name = fields.get("name") match
            case Some(JsString(name)) => name
            case _                    => throw new Exception("Invalid JSON: field.name must be present")
          val details = fields.get("details") match
            case Some(details) => fieldTypeWriter.read(details)
            case _             => throw new Exception("Invalid JSON: field.details must be present")
          Field(name, details)
        case _ => throw new Exception("Invalid JSON: expected object")

case class Schema(fields: List[Field], version: String)
object Schema:
  given ToExpr[Schema] with
    def apply(schema: Schema)(using Quotes): Expr[Schema] =
      import quotes.reflect.*
      '{ Schema(${ Expr(schema.fields) }, ${ Expr(schema.version) }) }

  given FromExpr[Schema] with
    def unapply(expr: Expr[Schema])(using Quotes): Option[Schema] =
      import quotes.reflect.*
      expr match
        case '{ Schema($fields, $version) } => Some(Schema(fields.valueOrAbort, version.valueOrAbort))
        case _                              => println("didn't match in Schema"); None

  given schemaGiven(using fieldWriter: JsonFormat[Field]): JsonFormat[Schema] with
    def write(schema: Schema): JsValue =
      JsObject(
        "version" -> JsString(schema.version),
        "schema" -> JsArray(schema.fields.map(field => fieldWriter.write(field)): _*)
      )

    def read(json: JsValue): Schema =
      json match
        case JsObject(fields) =>
          val version = fields.get("version") match
            case Some(JsString(version)) => version
            case _                       => throw new Exception("Invalid JSON: schema.version must be present")
          val schema = fields.get("schema") match
            case Some(JsArray(fields)) => fields.map(fieldWriter.read).toList
            case _                     => throw new Exception("Invalid JSON: schema.schema must be present")
          Schema(schema, version)
        case _ => throw new Exception("Invalid JSON: expected object")
