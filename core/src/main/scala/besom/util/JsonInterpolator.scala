package besom.util

import besom.json.*
import besom.internal.{Context, Output}
import scala.util.{Failure, Success}
import besom.json.JsValue
import interpolator.interleave
import java.util.Objects

object JsonInterpolator:
  import scala.quoted.*

  private val NL = System.lineSeparator()

  given {} with
    extension (sc: StringContext)
      inline def json(inline args: Any*)(using ctx: besom.internal.Context): Output[JsValue] = ${ jsonImpl('sc, 'args, 'ctx) }

  private def jsonImpl(sc: Expr[StringContext], args: Expr[Seq[Any]], ctx: Expr[Context])(using Quotes): Expr[Output[JsValue]] =
    import quotes.reflect.*

    // this function traverses the tree of the given expression and tries to extract the constant string context tree from it in Right
    // if it fails, it returns a Left with the final tree that couldn't be extracted from
    def resolveStringContext(tree: Term, i: Int = 0): Either[Term, Seq[Expr[String]]] =
      tree match
        // resolve reference if possible
        case t if t.tpe.termSymbol != Symbol.noSymbol && t.tpe <:< TypeRepr.of[StringContext] =>
          t.tpe.termSymbol.tree match
            case ValDef(_, _, Some(rhs)) => resolveStringContext(rhs, i + 1)
            case _                       => Left(t)

        // maybe resolved reference?
        case other =>
          tree.asExpr match
            case '{ scala.StringContext.apply(${ Varargs(parts) }: _*) } =>
              Right(parts)
            case _ =>
              Left(other)

    resolveStringContext(sc.asTerm) match
      case Left(badTerm) =>
        report.errorAndAbort(s"$sc -> $badTerm is not a string context :O") // this should never happen

      case Right(parts) =>
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
              val defaults = argExprs.map {
                case '{ $part: String }          => ""
                case '{ $part: Int }             => 0
                case '{ $part: Long }            => 0L
                case '{ $part: Float }           => 0f
                case '{ $part: Double }          => 0d
                case '{ $part: Boolean }         => true
                case '{ $part: JsValue }         => JsNull
                case '{ $part: Output[String] }  => ""
                case '{ $part: Output[Int] }     => 0
                case '{ $part: Output[Long] }    => 0L
                case '{ $part: Output[Float] }   => 0f
                case '{ $part: Output[Double] }  => 0d
                case '{ $part: Output[Boolean] } => true
                case '{ $part: Output[JsValue] } => JsNull
                case '{ $other: t } =>
                  report.errorAndAbort(
                    s"`Value ${other.show}: ${Type.show[t]}` is not a valid JSON interpolation type.$NL$NL" +
                      s"Types Available for interpolation are: " +
                      s"String, Int, Long, Float, Double, Boolean, JsValue and Outputs of those types.$NL" +
                      s"If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.$NL$NL"
                  )
              }

              val str = interleave(parts.map(_.valueOrAbort).toList, defaults.map(_.toString()).toList).reduce(_ + _)

              scala.util.Try(JsonParser(str)) match
                case Failure(exception) =>
                  report.errorAndAbort(s"Failed to parse JSON (default values inserted at compile time):$NL  ${exception.getMessage}")
                case Success(value) =>
                  val liftedSeqOfExpr: Seq[Expr[Output[?]]] = argExprs.map {
                    case '{ $part: String } =>
                      '{
                        Output($part)(using $ctx).map { str =>
                          val sb = java.lang.StringBuilder()
                          besom.json.CompactPrinter.print(JsString(str), sb) // escape strings
                          sb.toString().drop(1).dropRight(1) // json strings start and end with "
                        }
                      }
                    case '{ $part: Int }     => '{ Output($part)(using $ctx) }
                    case '{ $part: Long }    => '{ Output($part)(using $ctx) }
                    case '{ $part: Float }   => '{ Output($part)(using $ctx) }
                    case '{ $part: Double }  => '{ Output($part)(using $ctx) }
                    case '{ $part: Boolean } => '{ Output($part)(using $ctx) }
                    case '{ $part: JsValue } => '{ Output($part)(using $ctx) }
                    case '{ $part: Output[String] } =>
                      '{
                        $part.map { str =>
                          val sb = java.lang.StringBuilder()
                          besom.json.CompactPrinter.print(JsString(str), sb) // escape strings
                          sb.toString().drop(1).dropRight(1) // json strings start and end with "
                        }
                      }
                    case '{ $part: Output[Int] }     => part
                    case '{ $part: Output[Long] }    => part
                    case '{ $part: Output[Float] }   => part
                    case '{ $part: Output[Double] }  => part
                    case '{ $part: Output[Boolean] } => part
                    case '{ $part: Output[JsValue] } => part
                    case '{ $other: t } =>
                      report.errorAndAbort(
                        s"`Value ${other.show}: ${Type.show[t]}` is not a valid JSON interpolation type.$NL$NL" +
                          s"Types Available for interpolation are: " +
                          s"String, Int, Long, Float, Double, Boolean, JsValue and Outputs of those types.$NL" +
                          s"If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.$NL$NL"
                      )
                  }

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
