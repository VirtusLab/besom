package besom.internal

import scala.language.dynamics

import scala.quoted.*
import com.google.protobuf.struct.Struct

object Export extends Dynamic:
  inline def applyDynamic(name: "apply")(inline args: Any*)(using ctx: Context): Exports = ${ applyDynamicImpl('args) }
  inline def applyDynamicNamed(name: "apply")(inline args: (String, Any)*)(using ctx: Context): Exports = ${ applyDynamicNamedImpl('args, 'ctx) }

  def applyDynamicImpl(args: Expr[Seq[Any]])(using Quotes): Expr[Exports] =
    import quotes.reflect.*

    args match
      case Varargs(arguments) =>
        if arguments.isEmpty then
          '{ Exports.fromStructResult(Result(Struct(Map.empty))) }
        else
          report.errorAndAbort("All arguments of `exports(...)` must be explicitly named.")
      case _ =>
        report.errorAndAbort("Expanding arguments of `exports(...)` with `*` is not allowed.")

  def applyDynamicNamedImpl(args: Expr[Seq[(String, Any)]], ctx: Expr[Context])(using Quotes): Expr[Exports] =
    import quotes.reflect.*

    // TODO: check if parameter names are unique

    val (errorReports, results) = args match
      case Varargs(arguments) => arguments.partitionMap { 
        case '{ ($name: String, $value: v) } =>
          if name.valueOrAbort.isEmpty then
            Left(() => report.error(s"All arguments of `exports(...)` must be explicitly named.", value))
          else    
            Expr.summon[Encoder[v]] match
              case Some(encoder) =>
                // TODO make sure we don't need deps here (replaced with _)
                Right('{ ${encoder}.encode(${value}).map { (_, value1) => (${name}, value1) }})
              case None =>
                Left(() => report.error(s"Encoder[${Type.show[v]}] is missing", value))
      }
      case _ =>
        report.errorAndAbort("Expanding arguments of `exports(...)` with `*` is not allowed.")

    errorReports.foreach(_.apply)

    if errorReports.nonEmpty then
      report.errorAndAbort("Some of arguments of `exports` cannot be encoded.")

    val resultsExpr = Expr.ofSeq(results)
    '{
      Exports.fromStructResult(
        Result.sequence(${resultsExpr}).map { seq =>
          Struct(fields = seq.toMap)
        }
      )
    }
    
object ExportsOpaque:
  opaque type Exports = Result[Struct]

  object Exports:
    def fromStructResult(result: Result[Struct]): Exports = result
    extension (exports: Exports)
      def toResult: Result[Struct] = exports

export ExportsOpaque.Exports
