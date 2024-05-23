package besom.cfg

import besom.json.*
import besom.cfg.internal.*
import scala.quoted.*

object ErrorsSupport:
  inline def apply[C <: Struct](schemaStr: String, configuration: C): Either[fansi.Str, Unit] = ${
    callPerformDiff('schemaStr, 'configuration)
  }

  def callPerformDiff[C <: Struct: Type](schemaStr: Expr[String], configuration: Expr[C])(using Quotes): Expr[Either[fansi.Str, Unit]] =
    import quotes.reflect.*

    given ToExpr[fansi.Str] with
      def apply(str: fansi.Str)(using Quotes): Expr[fansi.Str] = '{ fansi.Str(${ Expr(str.toString) }) }

    val schema = schemaStr.value match
      case None             => report.errorAndAbort("Schema has to be a literal!", schemaStr)
      case Some(schemaJson) => summon[JsonFormat[Schema]].read(schemaJson.parseJson)

    val eitherDiffOrUnit = Diff.performDiff(schema, configuration)

    eitherDiffOrUnit match
      case Left(diff) => '{ Left(${ Expr(diff) }) }
      case Right(())  => '{ Right(()) }
