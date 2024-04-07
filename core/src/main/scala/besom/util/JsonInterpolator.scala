package besom.util

import besom.json.*
import besom.internal.{Context, Output}
import scala.util.{Failure, Success}
import interpolator.interleave
import java.util.Objects
import besom.json.JsValue

object JsonInterpolator:
  import scala.quoted.*

  private val NL = System.lineSeparator()

  extension (inline sc: StringContext)
    inline def json(inline args: Any*)(using ctx: besom.internal.Context): Output[JsValue] = ${ jsonImpl('sc, 'args, 'ctx) }

  private def jsonImpl(sc: Expr[StringContext], args: Expr[Seq[Any]], ctx: Expr[Context])(using Quotes): Expr[Output[JsValue]] =
    import quotes.reflect.*

    def defaultFor(field: Expr[Any], tpe: Type[_], wrappers: List[Type[_]] = Nil): Any = tpe match
      case '[String]    => JsString("")
      case '[Short]     => JsNumber(0)
      case '[Int]       => JsNumber(0)
      case '[Long]      => JsNumber(0L)
      case '[Float]     => JsNumber(0f)
      case '[Double]    => JsNumber(0d)
      case '[Boolean]   => JsBoolean(true)
      case '[JsValue]   => JsNull
      case '[Output[t]] => defaultFor(field, TypeRepr.of[t].asType, TypeRepr.of[Output[_]].asType :: wrappers)
      case '[Option[t]] => defaultFor(field, TypeRepr.of[t].asType, TypeRepr.of[Option[_]].asType :: wrappers)
      case '[t] => // this is a non-supported type! let's reduce wrappers (if any) and produce a nice error message
        val tpeRepr = TypeRepr.of[t]
        if wrappers.nonEmpty then
          // we apply types from the most inner to the most outer
          val fullyAppliedType = wrappers.foldLeft(tpeRepr) { case (inner, outer) =>
            outer match
              case '[o] =>
                val outerSym = TypeRepr.of[o].typeSymbol
                val applied  = AppliedType(outerSym.typeRef, List(inner))

                applied
          }

          // and now we have a full type available for error!
          report.errorAndAbort(
            s"Value of type `${fullyAppliedType.show}` is not a valid JSON interpolation type because of type `${tpeRepr.show}`.$NL$NL" +
              s"Types available for interpolation are: " +
              s"String, Int, Short, Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.$NL" +
              s"If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.$NL"
          )
        else
          // t is a simple type
          report.errorAndAbort(
            s"Value of type `${tpeRepr.show}: ${tpeRepr.typeSymbol.fullName}` is not a valid JSON interpolation type.$NL$NL" +
              s"Types available for interpolation are: " +
              s"String, Int, Short, Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.$NL" +
              s"If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.$NL"
          )
    end defaultFor

    // recursively convert all Exprs of arguments from user to Exprs of Output[JsValue]
    def convert(arg: Expr[Any]): Expr[Output[JsValue]] =
      arg match
        case '{ $arg: String } =>
          '{
            Output($arg)(using $ctx).map { str =>
              val sb = java.lang.StringBuilder()
              if str == null then JsNull
              else
                besom.json.CompactPrinter.print(JsString(str), sb) // escape strings
                sb.toString()
                JsString(str)
            }
          }
        case '{ $arg: Int }     => '{ Output(JsNumber($arg))(using $ctx) }
        case '{ $arg: Short }   => '{ Output(JsNumber($arg))(using $ctx) }
        case '{ $arg: Long }    => '{ Output(JsNumber($arg))(using $ctx) }
        case '{ $arg: Float }   => '{ Output(JsNumber($arg))(using $ctx) }
        case '{ $arg: Double }  => '{ Output(JsNumber($arg))(using $ctx) }
        case '{ $arg: Boolean } => '{ Output(JsBoolean($arg))(using $ctx) }
        case '{ $arg: JsValue } => '{ Output($arg)(using $ctx) }
        case _ =>
          arg.asTerm.tpe.asType match
            case '[Output[Option[t]]] =>
              '{
                $arg.asInstanceOf[Output[Option[t]]].flatMap {
                  case Some(value) => ${ convert('value) }
                  case None        => Output(JsNull)(using $ctx)
                }
              }
            case '[Output[t]] =>
              '{
                $arg.asInstanceOf[Output[t]].flatMap { value =>
                  ${ convert('value) }
                }
              }

            case '[Option[t]] =>
              '{
                $arg.asInstanceOf[Option[t]] match
                  case Some(value) => ${ convert('value) }
                  case None        => Output(JsNull)(using $ctx)
              }

            case '[t] =>
              val tpeRepr = TypeRepr.of[t]
              report.errorAndAbort(
                s"Value of type `${tpeRepr.show}` is not a valid JSON interpolation type.$NL$NL" +
                  s"Types available for interpolation are: " +
                  s"String, Int, Short Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.$NL" +
                  s"If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.$NL$NL"
              )
    end convert

    sc match
      case '{ scala.StringContext.apply(${ Varargs(parts) }: _*) } =>
        args match
          case Varargs(argExprs) =>
            if argExprs.isEmpty then
              parts.map(_.valueOrAbort).mkString match
                case "" => '{ Output(JsObject.empty)(using $ctx) }
                case str =>
                  scala.util.Try(JsonParser(str)) match
                    case Failure(exception) =>
                      report.errorAndAbort(s"Failed to parse JSON:$NL  ${exception.getMessage}")
                    case Success(value) =>
                      '{ Output(JsonParser(ParserInput.apply(${ Expr(str) })))(using $ctx) }
            else
              val defaults = argExprs.map { arg =>
                defaultFor(arg, arg.asTerm.tpe.asType)
              }

              val str = interleave(parts.map(_.valueOrAbort).toList, defaults.map(_.toString()).toList).reduce(_ + _)

              scala.util.Try(JsonParser(str)) match
                case Failure(exception) =>
                  report.errorAndAbort(s"Failed to parse JSON (default values inserted at compile time):$NL  ${exception.getMessage}")
                case Success(value) =>
                  val liftedSeqOfExpr: Seq[Expr[Output[?]]] = argExprs.map(convert)

                  val liftedExprOfSeq = Expr.ofSeq(liftedSeqOfExpr)
                  val liftedParts     = Expr.ofSeq(parts)

                  '{
                    interleave(${ liftedParts }.toList, ${ liftedExprOfSeq }.toList)
                      .foldLeft(Output("")(using $ctx)) { case (acc, e) =>
                        e match
                          case o: Output[?] => acc.flatMap(s => o.map(v => s + Objects.toString(v))) // handle nulls too
                          case s: String    => acc.map(_ + s)
                      }
                      .map { str =>
                        scala.util.Try(JsonParser(str)) match
                          case Failure(exception) =>
                            throw Exception(s"Failed to parse JSON:\n$str", exception)
                          case Success(value) =>
                            value
                      }
                  }
              end match
    end match
  end jsonImpl
end JsonInterpolator
