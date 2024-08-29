package besom.cfg.internal

import scala.quoted.*
import besom.json.*

case class Schema(fields: List[Field], version: String, medium: String)
object Schema:
  given ToExpr[Schema] with
    def apply(schema: Schema)(using Quotes): Expr[Schema] =
      import quotes.reflect.*
      '{ Schema(${ Expr(schema.fields) }, ${ Expr(schema.version) }, ${ Expr(schema.medium) }) }

  given FromExpr[Schema] with
    def unapply(expr: Expr[Schema])(using Quotes): Option[Schema] =
      import quotes.reflect.*
      expr match
        case '{ Schema($fields, $version, $medium) } => Some(Schema(fields.valueOrAbort, version.valueOrAbort, medium.valueOrAbort))
        case _                                       => println("didn't match in Schema"); None

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
          val medium = fields.get("medium") match
            case Some(JsString(medium)) => medium
            case _                      => throw new Exception("Invalid JSON: schema.medium must be present")
          Schema(schema, version, medium)
        case _ => throw new Exception("Invalid JSON: expected object")
