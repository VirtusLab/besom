package besom.internal

import com.google.protobuf.struct.*
import scala.quoted.*

trait RegistersOutputs[A <: ComponentResource & Product]:
  def toMapOfOutputs(a: A): Map[String, Result[(Set[Resource], Value)]]

object RegistersOutputs:
  def apply[A <: ComponentResource & Product](using ro: RegistersOutputs[A]): RegistersOutputs[A] = ro

  inline given derived[A <: ComponentResource & Product]: RegistersOutputs[A] = new RegistersOutputs[A] {
    def toMapOfOutputs(a: A): Map[String, Result[(Set[Resource], Value)]] = derivedImpl[A](a)
  }

  private inline def derivedImpl[A](a: A): Map[String, Result[(Set[Resource], Value)]] = ${ toMapOfOutputsImpl[A]('a) }

  def toMapOfOutputsImpl[A: Type](using Quotes)(a: Expr[Any]): Expr[Map[String, Result[(Set[Resource], Value)]]] = {
    import quotes.reflect.*
    val tpe = TypeRepr.of[A]
    val summonTerm = Ref(Symbol.requiredMethod("scala.compiletime.summonInline"))
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
      val result = Select.unique(encoder.asTerm, "encode").appliedTo(fieldExpr.asTerm).asExprOf[Result[(Set[Resource], Value)]]
      '{ (${ Expr(fieldName) }, $result) }
    }
    '{ Map(${Varargs(extractedFields.toList)}: _*) }.asExprOf[Map[String, Result[(Set[Resource], Value)]]]
  }
