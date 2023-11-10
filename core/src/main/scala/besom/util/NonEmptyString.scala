package besom.util

import scala.quoted.*
import scala.language.implicitConversions

opaque type NonEmptyString <: String = String

object NonEmptyString:
  def apply(s: String): Option[NonEmptyString] =
    if s.isBlank then None else Some(s)

  given nonEmptyStringOps: {} with
    extension (nes: NonEmptyString)
      inline def +++(other: String): NonEmptyString = nes + other
      inline def asString: String                   = nes

  inline def from(inline s: String): NonEmptyString = ${ fromImpl('s) }

  def fromImpl(expr: Expr[String])(using quotes: Quotes): Expr[NonEmptyString] =
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
            report.errorAndAbort(
              "Only constant strings or string interpolations are allowed here, use NonEmptyString.apply instead!"
            )

  implicit inline def str2NonEmptyString(inline s: String): NonEmptyString = NonEmptyString.from(s)

  extension (s: String)
    private def isBlank = s.trim.isEmpty

trait NonEmptyStringFactory:
  def apply(s: String): Option[NonEmptyString] = NonEmptyString(s)
  inline def from(s: String): NonEmptyString   = NonEmptyString.from(s)
