package yaga.shapes

import scala.quoted.*
import yaga.extensions.aws.lambda.ShapedFunctionHandle

object Schema:
  inline def deriveSchemaStr[A] = ${ deriveSchemaStrImpl[A] }

  private def deriveSchemaStrImpl[A : Type](using Quotes): Expr[String] =
    import quotes.reflect.*
    Expr(getFieldType(TypeRepr.of[A]).toString)

  def getSchemaStr[A : Type](using Quotes): String =
    import quotes.reflect.*
    getFieldType(TypeRepr.of[A]).toString

  /* private */ def getFieldType(using Quotes)(tpe: quotes.reflect.TypeRepr): FieldType =
    import quotes.reflect.*

    tpe.asType match
      case '[Int] => FieldType.Int
      case '[String] => FieldType.String
      case '[Unit] => FieldType.Struct()
      case '[ShapedFunctionHandle[?, ?]] => // TODO Don't special-case this here?
        FieldType.Struct(
          "functionName" -> FieldType.String,
          "inputSchema" -> FieldType.String,
          "outputSchema" -> FieldType.String
        )
      // TODO other primitive types
      case _ =>
        val caseClassSymbol = tpe.typeSymbol

        // Ensure it's a case class
        val caseClassFields = if (!caseClassSymbol.flags.is(Flags.Case)) {
          report.error(s"${tpe.show} is not a case class")
          Nil
        } else {
          caseClassSymbol.caseFields
        }
        val structFields = caseClassFields.map { field =>
          field.name -> getFieldType(tpe.memberType(field) )
        }
        FieldType.Struct(structFields*)

class SchemaProvider[A](
  val schemaStr: String
)

object SchemaProvider:
  inline given[A]: SchemaProvider[A] =
    SchemaProvider[A](Schema.deriveSchemaStr[A])