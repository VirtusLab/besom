package besom.internal

import com.google.protobuf.struct.*
import scala.quoted.*

trait RegistersOutputs[A <: ComponentResource & Product]:
  def serializeOutputs(a: Output[A])(using Context): Result[Struct]

object RegistersOutputs:
  def apply[A <: ComponentResource & Product](using ro: RegistersOutputs[A]): RegistersOutputs[A] = ro

  inline given derived[A <: ComponentResource & Product]: RegistersOutputs[A] = new RegistersOutputs[A] {
    def serializeOutputs(a: Output[A])(using Context): Result[Struct] = derivedImpl[A](a)
  }

  // noinspection ScalaUnusedSymbol
  private inline def derivedImpl[A](a: Output[A])(using ctx: Context): Result[Struct] = ${ serializeOutputsImpl[A]('ctx, 'a) }

  // noinspection ScalaUnusedSymbol
  private def serializeOutputsImpl[A: Type](using Quotes)(ctx: Expr[Context], a: Expr[Output[Any]]): Expr[Result[Struct]] = {
    import quotes.reflect.*

    val tpe          = TypeRepr.of[A]
    val fields       = tpe.typeSymbol.caseFields
    val outputSymbol = Symbol.requiredClass("besom.internal.Output")

    val fieldInfoSymbols = fields.map { field =>
      val tpeRepr = field.tree match
        case v: ValDef => v.tpt.tpe
        case d: DefDef => d.returnTpt.tpe
      tpeRepr.typeSymbol
    }

    if fieldInfoSymbols.exists(_ != outputSymbol) then
      report.errorAndAbort("Cannot derive RegistersOutputs for a component with non-Output fields")

    val extractedFields = fields.map { field =>
      val fieldName = field.name
      val fieldExpr = '{ ($a.asInstanceOf[Output[A]]).flatMap(r => ${ Select.unique('r.asTerm, fieldName).asExprOf[Output[A]] }) }
      val fieldTpe = field.tree match
        case v: ValDef => v.tpt.tpe
        case d: DefDef => d.returnTpt.tpe

      val encoder = fieldTpe.asType match
        case '[p] =>
          Expr
            .summon[Encoder[p]]
            .getOrElse(report.errorAndAbort(s"Couldn't find encoder of type ${fieldTpe.show} for field ${field.name}"))
            .asExprOf[Encoder[?]]

      val encoded =
        Select
          .unique(encoder.asTerm, "encode")
          .appliedTo(fieldExpr.asTerm)
          .appliedTo(ctx.asTerm)
          .asExprOf[Result[(Metadata, Value)]]

      '{ $encoded.map(metaAndValue => (${ Expr(fieldName) }, metaAndValue._2)) } // discard dependencies
    }

    val listOfResults = Expr.ofSeq(extractedFields)

    '{
      Result.sequence($listOfResults).map { seq =>
        Struct(fields = seq.toMap)
      }
    }
  }
end RegistersOutputs
