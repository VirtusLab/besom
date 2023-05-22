package besom.internal

import scala.quoted.*

object CodecMacros:
  inline def summonLabels[A] = ${ summonLabelsImpl[A] }

  private def summonLabelsImpl[A: Type](using Quotes): Expr[List[String]] =
    import quotes.reflect.*
    Expr(recSummonLabelsImpl(Type.of[A]))

  private def recSummonLabelsImpl(t: Type[?])(using Quotes): List[String] =
    import quotes.reflect.*
    t match
      case '[EmptyTuple] => Nil
      case '[head *: tail] =>
        val headValue = Type.valueOfConstant[head].getOrElse(report.errorAndAbort("This was not a literal!")).toString()
        headValue :: recSummonLabelsImpl(Type.of[tail])
      case _ => report.errorAndAbort("This can be ONLY called on tuples of string singleton types!")

  // TODO all of these could summon a generic TC[head] typeclass but it's harder so... later?
  inline def summonDecoders[A]: List[Decoder[?]] = ${ summonDecodersImpl[A] }

  private def summonDecodersImpl[A: Type](using Quotes): Expr[List[Decoder[?]]] =
    import quotes.reflect.*
    // report.info(s"Deriving for ${Type.show[A]}")
    Expr.ofList(recSummonDecodersImpl(Type.of[A]))

  private def recSummonDecodersImpl(t: Type[?])(using Quotes): List[Expr[Decoder[?]]] =
    import quotes.reflect.*
    t match
      case '[EmptyTuple] => Nil
      case '[head *: tail] =>
        val exprOfDecoder = Expr.summon[Decoder[head]].getOrElse {
          report.errorAndAbort(s"Decoder for ${Type.show[head]} was not found!")
        }
        exprOfDecoder :: recSummonDecodersImpl(Type.of[tail])
      case _ => report.errorAndAbort("This can be ONLY called on tuples of  types!")

  inline def summonEncoders[A]: List[Encoder[?]] = ${ summonEncodersImpl[A] }

  private def summonEncodersImpl[A: Type](using Quotes): Expr[List[Encoder[?]]] =
    import quotes.reflect.*
    Expr.ofList(recSummonEncodersImpl(Type.of[A]))

  private def recSummonEncodersImpl(t: Type[?])(using Quotes): List[Expr[Encoder[?]]] =
    import quotes.reflect.*
    t match
      case '[EmptyTuple] => Nil
      case '[head *: tail] =>
        val exprOfEncoder = Expr.summon[Encoder[head]].getOrElse {
          report.errorAndAbort(s"Encoder for ${Type.show[head]} was not found!")
        }
        exprOfEncoder :: recSummonEncodersImpl(Type.of[tail])
      case _ => report.errorAndAbort("This can be ONLY called on tuples!")

  inline def summonJsonEncoders[A]: List[JsonEncoder[?]] = ${ summonJsonEncodersImpl[A] }

  private def summonJsonEncodersImpl[A: Type](using Quotes): Expr[List[JsonEncoder[?]]] =
    import quotes.reflect.*
    Expr.ofList(recSummonJsonEncodersImpl(Type.of[A]))

  private def recSummonJsonEncodersImpl(t: Type[?])(using Quotes): List[Expr[JsonEncoder[?]]] =
    import quotes.reflect.*
    t match
      case '[EmptyTuple] => Nil
      case '[head *: tail] =>
        val exprOfJsonEncoder = Expr.summon[JsonEncoder[head]].getOrElse {
          report.errorAndAbort(s"JsonEncoder for ${Type.show[head]} was not found!")
        }
        exprOfJsonEncoder :: recSummonJsonEncodersImpl(Type.of[tail])
      case _ => report.errorAndAbort("This can be ONLY called on tuples!")

  // inline def summonTypeclasses[A, TC[_]]: List[TC[Any]] = ${ summonTypeclassesImpl[A, TC] }

  // private def summonTypeclassesImpl[A: Type, TC[_]: Type](using Quotes): Expr[List[TC[Any]]] =
  //   import quotes.reflect.*

  //   '{ List.empty[TC[Any]] }
