package besom.util

import scala.quoted.*
import scala.language.implicitConversions

/** A [[String]] that is not empty or blank.
  */
opaque type NonEmptyString <: String = String

/** A [[String]] that is not empty or blank.
  */
object NonEmptyString:
  /** @param s
    *   a [[String]] to be converted to a [[NonEmptyString]].
    * @return
    *   an optional [[NonEmptyString]] if the given [[String]] is not empty or blank, otherwise [[None]].
    */
  def apply(s: String): Option[NonEmptyString] =
    if s.isBlank then None else Some(s)

  given nonEmptyStringOps: {} with
    extension (nes: NonEmptyString)
      /** Concatenates this [[NonEmptyString]] with the given [[String]].
        */
      inline def +++(other: String): NonEmptyString = nes + other

      /** @return
        *   this [[NonEmptyString]] as a [[String]].
        */
      inline def asString: String = nes

  /** @param s
    *   a [[String]] to be converted to a [[NonEmptyString]].
    * @return
    *   a [[NonEmptyString]] if the given [[String]] is not empty or blank.
    */
  inline def from(inline s: String): NonEmptyString = ${ fromImpl('s) }

  private def fromImpl(expr: Expr[String])(using quotes: Quotes): Expr[NonEmptyString] =
    import quotes.reflect.*

    expr match
      // TODO add `pulumi` and `p` interpolators here but remember they return `Output[NonEmptyString]` in this case.
      case '{ scala.StringContext.apply(${ Varargs(parts) }: _*).s(${ Varargs(_) }: _*) } =>
        val atLeastOneSegmentIsNonEmptyAndNonBlank =
          parts
            .collect { case Expr(str) => str }
            .exists(!_.isBlank)

        if atLeastOneSegmentIsNonEmptyAndNonBlank then expr.asExprOf[NonEmptyString]
        else report.errorAndAbort("This interpolated string is possibly empty, empty strings are not allowed here!")
      case '{ scala.StringContext.apply(${ Varargs(parts) }: _*).f(${ Varargs(_) }: _*) } =>
        val atLeastOneSegmentIsNonEmptyAndNonBlank =
          parts
            .collect { case Expr(str) => str }
            .exists(!_.isBlank)

        if atLeastOneSegmentIsNonEmptyAndNonBlank then expr.asExprOf[NonEmptyString]
        else report.errorAndAbort("This interpolated string is possibly empty, empty strings are not allowed here!")
      case '{ scala.StringContext.apply(${ Varargs(parts) }: _*).raw(${ Varargs(_) }: _*) } =>
        val atLeastOneSegmentIsNonEmptyAndNonBlank =
          parts
            .collect { case Expr(str) => str }
            .exists(!_.isBlank)

        if atLeastOneSegmentIsNonEmptyAndNonBlank then expr.asExprOf[NonEmptyString]
        else report.errorAndAbort("This interpolated string is possibly empty, empty strings are not allowed here!")
      case '{ $str: String } =>
        str.value match
          case Some(s) =>
            if s.isEmpty then report.errorAndAbort("Empty string is not allowed here!")
            else if s.isBlank then report.errorAndAbort("Blank string is not allowed here!")
            else str.asExprOf[NonEmptyString]
          case None =>
            Some(expr.asTerm)
              .collect { case Inlined(_, _, id: Ident) =>
                id.tpe.termSymbol.tree
              }
              .collect { case ValDef(_, _, Some(rhs)) =>
                rhs match
                  case Apply(Select(Apply(Ident("augmentString"), List(augmented)), "*"), List(multiplier)) =>
                    for
                      multiplierValue <- multiplier.asExprOf[Int].value if multiplierValue > 0
                      augmentedValue  <- augmented.asExprOf[String].value
                    yield augmentedValue

                  case Apply(Select(lhs, "+"), args) =>
                    val baseAndArgs           = (lhs.asExprOf[String].value +: args.toVector.map(_.asExprOf[String].value)).flatten
                    val hasNonEmptyBaseOrArgs = baseAndArgs.exists(_.nonEmpty)
                    if hasNonEmptyBaseOrArgs then baseAndArgs.headOption
                    else None

                  case _ => rhs.asExprOf[String].value
              }
              .flatten
              .match
                case Some(value) =>
                  if value.isEmpty then report.errorAndAbort("Empty string is not allowed here!")
                  else if value.isBlank then report.errorAndAbort("Blank string is not allowed here!")
                  else Expr(value)
                case None =>
                  Some(expr.asTerm)
                    .tapEach { case Inlined(_, _, x) =>
                      println(x)
                    }
                    .collect { case Inlined(_, _, id: Ident) =>
                      id.tpe.termSymbol.tree
                    }
                    .collect { case ValDef(_, _, Some(rhs)) =>
                      // rhs match
                      //   case _ =>
                      //     println(s"rhs = ${rhs.show} - $rhs - ${rhs.asExprOf[String].value}")
                      rhs.asExprOf[String].value
                    }
                  report.errorAndAbort(
                    "Only constant strings or string interpolations are allowed here, use NonEmptyString.apply instead!"
                  )
    end match
  end fromImpl

  implicit inline def str2NonEmptyString(inline s: String): NonEmptyString = NonEmptyString.from(s)
end NonEmptyString

trait NonEmptyStringFactory:
  /** @param s
    *   a [[String]] to be converted to a [[NonEmptyString]].
    * @return
    *   an optional [[NonEmptyString]] if the given [[String]] is not empty or blank, otherwise [[None]].
    */
  def apply(s: String): Option[NonEmptyString] = NonEmptyString(s)

  /** @param s
    *   a [[String]] to be converted to a [[NonEmptyString]].
    * @return
    *   a [[NonEmptyString]] if the given [[String]] is not empty or blank.
    */
  inline def from(s: String): NonEmptyString = NonEmptyString.from(s)
