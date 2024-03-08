package besom.internal

import scala.language.dynamics

import scala.quoted.*
import com.google.protobuf.struct.Struct

//noinspection ScalaUnusedSymbol
object EmptyExport extends Dynamic:
  inline def applyDynamic(name: "apply")(inline args: Any*)(using ctx: Context): Stack = ${
    Export.applyDynamicImpl('args, 'None)
  }
  inline def applyDynamicNamed(name: "apply")(inline args: (String, Any)*)(using ctx: Context): Stack = ${
    Export.applyDynamicNamedImpl('args, 'ctx, 'None)
  }

//noinspection ScalaUnusedSymbol
class Export(private val stack: Stack) extends Dynamic:
  private val maybeStack: Option[Stack] = Some(stack)
  inline def applyDynamic(name: "apply")(inline args: Any*)(using ctx: Context): Stack = ${
    Export.applyDynamicImpl('args, 'maybeStack)
  }
  inline def applyDynamicNamed(name: "apply")(inline args: (String, Any)*)(using ctx: Context): Stack = ${
    Export.applyDynamicNamedImpl('args, 'ctx, 'maybeStack)
  }

object Export:
  def applyDynamicImpl(args: Expr[Seq[Any]], stack: Expr[Option[Stack]])(using Quotes): Expr[Stack] =
    import quotes.reflect.*

    args match
      case Varargs(arguments) =>
        if arguments.isEmpty then '{ ${ stack }.getOrElse(Stack.empty) }
        else report.errorAndAbort("All arguments of `exports(...)` must be explicitly named.")
      case _ =>
        report.errorAndAbort("Expanding arguments of `exports(...)` with `*` is not allowed.")

  def applyDynamicNamedImpl(args: Expr[Seq[(String, Any)]], ctx: Expr[Context], stack: Expr[Option[Stack]])(using Quotes): Expr[Stack] =
    import quotes.reflect.*

    // TODO: check if parameter names are unique

    val (errorReports, results) = args match
      case Varargs(arguments) =>
        arguments.partitionMap { case '{ ($name: String, $value: v) } =>
          if name.valueOrAbort.isEmpty then Left(() => report.error(s"All arguments of `exports(...)` must be explicitly named.", value))
          else
            Expr.summon[Encoder[v]] match
              case Some(encoder) =>
                // TODO make sure we don't need deps here (replaced with _)
                Right('{ ${ encoder }.encode(${ value })(using ${ ctx }).map { (_, value1) => (${ name }, value1) } })
              case None =>
                Left(() => report.error(s"Encoder[${Type.show[v]}] is missing", value))
        }
      case _ =>
        report.errorAndAbort("Expanding arguments of `exports(...)` with `*` is not allowed.")

    errorReports.foreach(_.apply)

    if errorReports.nonEmpty then report.errorAndAbort("Some of arguments of `exports` cannot be encoded.")

    val resultsExpr = Expr.ofSeq(results)
    '{
      val previousStack = ${ stack }.getOrElse(Stack.empty)

      val exports = Exports(
        Result.sequence(${ resultsExpr }).map { seq =>
          Struct(fields = seq.toMap)
        }
      )

      val mergedExports = previousStack.getExports.merge(exports)

      Stack(mergedExports, previousStack.getDependsOn)
    }
  end applyDynamicNamedImpl
end Export

case class Exports(result: Result[Struct]):
  private[besom] def merge(other: Exports): Exports =
    Exports {
      for
        struct      <- result
        otherStruct <- other.result
      yield Struct(fields = struct.fields ++ otherStruct.fields)
    }
