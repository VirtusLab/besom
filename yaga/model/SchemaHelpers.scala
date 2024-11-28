package yaga.model

import scala.quoted.*

class SchemaHelpers:
  protected def getFieldType(using Quotes)(tpe: quotes.reflect.TypeRepr): FieldType =
    import quotes.reflect.*

    tpe.asType match
      case '[Boolean] => FieldType.Boolean
      case '[Byte] => FieldType.Byte
      case '[Short] => FieldType.Short
      case '[Int] => FieldType.Int
      case '[Long] => FieldType.String
      case '[Float] => FieldType.Float
      case '[Double] => FieldType.Double
      case '[String] => FieldType.String
      case '[Unit] => FieldType.Struct()
      // TODO handle optionals, arrays and other(?) built-in types
      case _ =>
        val caseClassSymbol = tpe.dealias.typeSymbol // TODO should be `.dealiasKeepOpaques` but it's not available in LTS

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
