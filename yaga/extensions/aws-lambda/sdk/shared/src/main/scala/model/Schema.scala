package yaga.extensions.aws.model

import scala.quoted.*
import yaga.extensions.aws.lambda.LambdaHandle
import yaga.model.FieldType
import yaga.model.SchemaHelpers

object Schema extends SchemaHelpers:
  inline def deriveSchemaStr[A] = ${ deriveSchemaStrImpl[A] }

  private def deriveSchemaStrImpl[A : Type](using Quotes): Expr[String] =
    import quotes.reflect.*
    Expr(getFieldType(TypeRepr.of[A]).toString)

  def getSchemaStr[A : Type](using Quotes): String =
    import quotes.reflect.*
    getFieldType(TypeRepr.of[A]).toString

  override def getFieldType(using Quotes)(tpe: quotes.reflect.TypeRepr): FieldType =
    import quotes.reflect.*

    tpe.asType match
      case '[LambdaHandle[?, ?]] =>
        FieldType.Struct(
          "functionName" -> FieldType.String,
          "inputSchema" -> FieldType.String,
          "outputSchema" -> FieldType.String
        )
      case _ =>
        super.getFieldType(tpe)

class SchemaProvider[A](
  val schemaStr: String
)

object SchemaProvider:
  inline given[A]: SchemaProvider[A] =
    SchemaProvider[A](Schema.deriveSchemaStr[A])
