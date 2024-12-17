package yaga.extensions.aws.model

import scala.quoted.*

// TODO Should we have separate classes for covariant/contravariant (input-like/output-like) types of compatibility?
class TypeSchemasCompatibility[T1, T2]

object TypeSchemasCompatibility:
  inline given schemasCompatibility[T1, T2]: TypeSchemasCompatibility[T1, T2] = ${ schemasCompatibilityImpl[T1, T2] }

  private def schemasCompatibilityImpl[T1 : Type, T2 : Type](using Quotes): Expr[TypeSchemasCompatibility[T1, T2]] =
    import quotes.reflect.*

    val fieldType1 = Schema.getFieldType(TypeRepr.of[T1])
    val fieldType2 = Schema.getFieldType(TypeRepr.of[T2])

    val schemasAreCompatible = fieldType1 == fieldType2 // TODO deeply compare structures for semantic compatibility

    if schemasAreCompatible then
        '{ TypeSchemasCompatibility[T1, T2] }
    else
      val type1Name = Type.show[T1]
      val type2Name = Type.show[T2]
      report.errorAndAbort(s"Schemas of type ${type1Name} and ${type2Name} are not compatible.\nSchema of $type1Name:\n${fieldType1}\n\nSchema of $type2Name:\n${fieldType2}")
