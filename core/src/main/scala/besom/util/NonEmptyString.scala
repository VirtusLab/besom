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

  object OutputOps:
    import besom.internal.{Output, Context}
    import besom.util.interpolator.*
    inline def fromStringOutput(inline s: Output[String]): Output[NonEmptyString] = ${ fromStringOutputImpl('s) }

    private def fromStringOutputImpl(expr: Expr[Output[String]])(using quotes: Quotes): Expr[Output[NonEmptyString]] =
      import quotes.reflect.*

      def handleParts(parts: Seq[Expr[String]]): Expr[Output[NonEmptyString]] =
        val atLeastOneSegmentIsNonEmptyAndNonBlank =
          parts
            .collect { case Expr(str) => str }
            .exists(!_.isBlank)

        if atLeastOneSegmentIsNonEmptyAndNonBlank then expr.asExprOf[Output[NonEmptyString]]
        else report.errorAndAbort("This interpolated string is possibly empty, empty strings are not allowed here!")

      expr match
        case '{ scala.StringContext.apply(${ Varargs(parts) }: _*).p(${ Varargs(_) }: _*)(using ${ xd }: Context) } =>
          handleParts(parts)
        case '{ scala.StringContext.apply(${ Varargs(parts) }: _*).pulumi(${ Varargs(_) }: _*)(using ${ xd }: Context) } =>
          handleParts(parts)

    end fromStringOutputImpl

  /** @param s
    *   a [[String]] to be inferred to be a [[NonEmptyString]].
    * @return
    *   a [[NonEmptyString]] if the given [[String]] is not empty or blank and can be looked up as a static constant, otherwise a
    *   compile-time error.
    */
  inline def from(inline s: String): NonEmptyString = ${ fromImpl('s) }

  private def fromImpl(expr: Expr[String])(using quotes: Quotes): Expr[NonEmptyString] =
    import quotes.reflect.*

    // utility to validate the string and convert it to a NonEmptyString
    def toValidNesOrFail(s: String, strExpr: Expr[String]): Expr[NonEmptyString] =
      if s.isEmpty then report.errorAndAbort("Empty string is not allowed here!")
      else if s.isBlank then report.errorAndAbort("Blank string is not allowed here!")
      else strExpr.asExprOf[NonEmptyString]

    // kudos to @Kordyjan for most of this implementation
    // this function traverses the tree of the given expression and tries to extract the constant string value from it in Right
    // if it fails, it returns a Left with the final tree that couldn't be extracted from
    def downTheRabbitHole(tree: Term): Either[Term, String] =
      tree match
        // it's a val x = "y" situation, we take the rhs
        case Inlined(_, _, inner) => downTheRabbitHole(inner)

        // it's a constant string, yay, we're done
        case Literal(StringConstant(value)) =>
          Right(value.toString)

        // it's a `"x" * 5` situation, we verify that multiplier is > 0 (otherwise this operation returns an empty string)
        // and extract the string constant that is being multiplied, if the string can't be extracted we return a Left
        case augment @ Apply(Select(Apply(Ident("augmentString"), List(augmented)), "*"), List(multiplier)) =>
          val opt = for
            multiplierValue <- multiplier.asExprOf[Int].value if multiplierValue > 0
            augmentedValue  <- augmented.asExprOf[String].value
          yield augmentedValue

          opt.toRight(augment)

        // it's a `"x" + "y"` situation, we try to extract both sides, if they can't be extracted we replace their value
        // with an empty string and concatenate them, final logic checks if the string is empty or blank anyway
        case Apply(Select(left, "+"), right :: Nil) =>
          for
            l <- downTheRabbitHole(left).orElse(Right(""))
            r <- downTheRabbitHole(right).orElse(Right(""))
          yield l + r

        // it's a `val x = someObject.x` situation, we take the rhs and go deeper
        case t if t.tpe.termSymbol != Symbol.noSymbol && t.tpe <:< TypeRepr.of[String] =>
          t.tpe.termSymbol.tree match
            case ValDef(_, _, Some(rhs)) => downTheRabbitHole(rhs)
            case _ =>
              Left(t)

        // uh, can't match, fail the whole thing
        case other =>
          Left(other)

    expr match
      // if it's a string interpolation, we check if at least one segment is non-empty and non-blank
      case '{ scala.StringContext.apply(${ Varargs(parts) }: _*).s(${ Varargs(_) }: _*) } =>
        val atLeastOneSegmentIsNonEmptyAndNonBlank =
          parts
            .collect { case Expr(str) => str }
            .exists(!_.isBlank)

        if atLeastOneSegmentIsNonEmptyAndNonBlank then expr.asExprOf[NonEmptyString]
        else report.errorAndAbort("This interpolated string is possibly empty, empty strings are not allowed here!")

      // if it's a formatted string interpolation, we check if at least one segment is non-empty and non-blank
      case '{ scala.StringContext.apply(${ Varargs(parts) }: _*).f(${ Varargs(_) }: _*) } =>
        val atLeastOneSegmentIsNonEmptyAndNonBlank =
          parts
            .collect { case Expr(str) => str }
            .exists(!_.isBlank)

        if atLeastOneSegmentIsNonEmptyAndNonBlank then expr.asExprOf[NonEmptyString]
        else report.errorAndAbort("This interpolated string is possibly empty, empty strings are not allowed here!")

      // if it's a raw string interpolation, we check if at least one segment is non-empty and non-blank
      case '{ scala.StringContext.apply(${ Varargs(parts) }: _*).raw(${ Varargs(_) }: _*) } =>
        val atLeastOneSegmentIsNonEmptyAndNonBlank =
          parts
            .collect { case Expr(str) => str }
            .exists(!_.isBlank)

        if atLeastOneSegmentIsNonEmptyAndNonBlank then expr.asExprOf[NonEmptyString]
        else report.errorAndAbort("This interpolated string is possibly empty, empty strings are not allowed here!")

      // oh, it's just an expression of type String, let's try to extract the constant value from it directly
      // if it's not a constant value let's see if we can infer it using the tree traversal function `downTheRabbitHole`
      case '{ $str: String } =>
        str.value match
          case Some(s) => toValidNesOrFail(s, str)
          case None =>
            downTheRabbitHole(expr.asTerm) match
              case Right(s) => toValidNesOrFail(s, str)
              case Left(tree) =>
                report.errorAndAbort(
                  "Only constant strings or string interpolations are allowed here, use NonEmptyString.apply instead! " + tree.show
                )
    end match
  end fromImpl

  // these implicit conversions allow inference of NonEmptyString from String literals

  implicit inline def str2NonEmptyString(inline s: String): NonEmptyString = NonEmptyString.from(s)

  import besom.internal.{Output, Context}
  implicit inline def outputStr2OutputNonEmptyString(inline s: Output[String])(using Context): Output[NonEmptyString] =
    OutputOps.fromStringOutput(s)

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
