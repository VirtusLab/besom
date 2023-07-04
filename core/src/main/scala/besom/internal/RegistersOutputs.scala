package besom.internal

import scala.quoted.*

trait RegistersOutputs[A <: ComponentResource & Product] {
  def toMapOfOutputs(a: A): Map[String, (Encoder[?], Output[Any])]
}

object RegistersOutputs {
  inline given derived[A <: ComponentResource & Product]: RegistersOutputs[A] = new RegistersOutputs[A] {
    def toMapOfOutputs(a: A): Map[String, (Encoder[?], Output[Any])] = derivedImpl[A](a)
  }

  private inline def derivedImpl[A](a: A): Map[String, (Encoder[?], Output[Any])] = ${ toMapOfOutputsImpl[A]('a) }

  def toMapOfOutputsImpl[A: Type](using Quotes)(a: Expr[Any]): Expr[Map[String, (Encoder[?], Output[Any])]] = {
    import quotes.reflect.*
    val tpe = TypeRepr.of[A]
    val fields = tpe.typeSymbol.caseFields
    val outputSymbol = Symbol.requiredClass("besom.internal.Output")
    val fieldInfoSyms = fields.map { field =>
      val tpeRepr = field.tree match
        case v: ValDef => v.tpt.tpe
        case d: DefDef => d.returnTpt.tpe
      tpeRepr.typeSymbol
    }
    if fieldInfoSyms.exists(_ != outputSymbol) then
      report.errorAndAbort("Cannot derive RegistersOutputs for a component with non-Output fields")
    val extractedFields = fields.map { field =>
      val fieldName = field.name
      val fieldExpr = Select.unique(a.asTerm, fieldName).asExprOf[Output[Any]]
      val fieldTpe = field.tree match
        case v: ValDef => v.tpt.tpe
        case d: DefDef => d.returnTpt.tpe
      val encoder = fieldTpe.asType match
        case '[p] =>
          Expr.summon[Encoder[p]]
          .getOrElse(report.errorAndAbort(s"Couldn't find encoder of type ${fieldTpe.show} for field ${field.name}"))
          .asExprOf[Encoder[?]]
      '{ (${ Expr(fieldName) }, ($encoder, $fieldExpr)) }
    }
    '{ Map(${Varargs(extractedFields.toList)}: _*) }.asExprOf[Map[String, (Encoder[?], Output[Any])]]
  }
}