package besom.experimental.genericEncoder

trait Encoder[A]:
  def encode(a: A): String

// object Encoder extends EncoderLowPrio:
//   given Encoder[String] with
//     def encode(str: String): String = s"""string("$str")"""

//   given Encoder[Int] with
//     def encode(int: Int): String = s"""int($int)"""

//   given Encoder[Boolean] with
//     def encode(bool: Boolean): String = s"""bool($bool)"""

// trait EncoderLowPrio:
//   transparent inline given genericEncoder[U]: Encoder[U] = ${ EncoderLowPrio.genericEncoderImpl[U] }

// object EncoderLowPrio:
//   import scala.quoted.*

//   private case class Alternatives[TpeRepr](nongenerics: List[TpeRepr] = Nil, listElements: List[TpeRepr] = Nil, mapElements: List[TpeRepr] = Nil):
//     def ++(that: Alternatives[TpeRepr]) = Alternatives(
//       this.nongenerics ++ that.nongenerics,
//       this.listElements ++ that.listElements,
//       this.mapElements ++ that.mapElements
//     )

//   private def getAlternatives(using q: Quotes)(tpe: q.reflect.TypeRepr): Alternatives[q.reflect.TypeRepr] =
//     import quotes.reflect.{Alternatives => _, *}
//     tpe.dealias.simplified match
//       case OrType(t1, t2) =>
//         getAlternatives(t1) ++ getAlternatives(t2)
//       case _ =>
//         tpe.asType match
//           case '[List[e]] => Alternatives(listElements = List(TypeRepr.of[e]))
//           case '[Map[String, e]] => Alternatives(mapElements = List(TypeRepr.of[e]))
//           case _ => Alternatives(nongenerics = List(tpe))

//   private def unionOf(using q: Quotes)(tpes: List[q.reflect.TypeRepr]): q.reflect.TypeRepr =
//     import quotes.reflect.*
//     (tpes: @unchecked) match
//       case tpe :: Nil => tpe
//       case tpe :: tail => OrType(tpe, unionOf(tail))

//   private def makeMatchCase[A : Type](f: Expr[A] => Expr[String])(using Quotes) =
//     import quotes.reflect.*
//     val bind = Symbol.newBind(Symbol.spliceOwner, "x", Flags.EmptyFlags, TypeRepr.of[A])
//     CaseDef(
//       Bind(bind, Typed(Wildcard(), TypeTree.of[A])),
//       guard = None,
//       rhs = f(Ref(bind).asExprOf[A]).asTerm
//     )

//   private def encodeListBody[Elem : Type](expr: Expr[List[Elem]])(using Quotes) =
//     val encoderExpr = Expr.summon[Encoder[Elem]].get
//     '{
//       val elemEncoder = ${encoderExpr}
//       val encodedElems = ${ expr }.map(elemEncoder.encode).mkString(", ")
//       s"""list(${encodedElems})"""
//     }

//   private def encodeMapBody[Elem : Type](expr: Expr[Map[String, Elem]])(using Quotes) =
//     val encoderExpr = Expr.summon[Encoder[Elem]].get
//     '{
//       val elemEncoder = ${encoderExpr}
//       val encodedElems = ${ expr }.map( (key, value) => s"\"${key}\" -> ${elemEncoder.encode(value)}").mkString(", ")
//       s"""map(${encodedElems})"""
//     }

//   private def encodeNongenericBody[A : Type](expr: Expr[A])(using Quotes) =
//     import quotes.reflect.*
//     val encoderExpr = Expr.summon[Encoder[A]].getOrElse(report.errorAndAbort(s"Missing Encoder for ${Type.show[A]}"))
//     '{
//       val elemEncoder = ${ encoderExpr }
//       elemEncoder.encode(${ expr })
//     }

//   private def encodeBody[T : Type](expr: Expr[T])(using Quotes): Expr[String] =
//     import quotes.reflect.*
//     val alternatives = getAlternatives(TypeRepr.of[T])
//     val listElemTpe = Option.when(alternatives.listElements.nonEmpty)(unionOf(alternatives.listElements).asType)
//     val mapElemTpe = Option.when(alternatives.mapElements.nonEmpty)(unionOf(alternatives.mapElements).asType)
//     val nongenericTpes = alternatives.nongenerics

//     (listElemTpe, mapElemTpe, nongenericTpes) match
//       case (Some('[x]), None, Nil) =>
//         encodeListBody(expr.asExprOf[List[x]])
//       case (None, Some('[y]), Nil) =>
//         encodeMapBody(expr.asExprOf[Map[String, y]])
//       case (None, None, _) if nongenericTpes.length < 2 => report.errorAndAbort("At least 2 types are needed to make a union")
//       case (_, _, _) =>
//         val listCase = listElemTpe.map { case '[tpe] =>
//           makeMatchCase[List[tpe]](boundExpr =>
//             encodeListBody[tpe](boundExpr.asExprOf[List[tpe]])
//           )
//         }
//         val mapCase = mapElemTpe.map { case '[tpe] => makeMatchCase[tpe](boundExpr => encodeMapBody(boundExpr.asExprOf[Map[String, tpe]])) }
//         val nongenericCases = nongenericTpes.map(_.asType).map { case '[tpe] =>

//           makeMatchCase[tpe](boundExpr =>
//             println(boundExpr.asExprOf[tpe])
//             encodeNongenericBody(boundExpr.asExprOf[tpe])
//           )
//         }
//         val matchCases = listCase ++ mapCase ++ nongenericCases
//         Match(expr.asTerm, matchCases.toList).asExprOf[String]

//   // Cannot be private -> bug in scala compiler
//   /* private */ def genericEncoderImpl[T : Type](using Quotes): Expr[Encoder[T]] =
//     '{
//       new Encoder[T]:
//         def encode(t: T): String = ${ encodeBody('t) }
//     }
