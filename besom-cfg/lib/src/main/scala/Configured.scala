package besom.cfg

import scala.quoted.*
import besom.json.*
import besom.cfg.internal.*

// trait Constraint[A]:
//   def validate(a: A): Boolean
//   def &(other: Constraint[A]): Constraint[A] =
//     (a: A) => validate(a) && other.validate(a)
//   def |(other: Constraint[A]): Constraint[A] =
//     (a: A) => validate(a) || other.validate(a)

// object Positive extends Constraint[Int]:
//   def validate(a: Int): Boolean = a > 0

// object Negative extends Constraint[Int]:
//   def validate(a: Int): Boolean = a < 0

// object NonZero extends Constraint[Int]:
//   def validate(a: Int): Boolean = a != 0

// object NonEmpty extends Constraint[String]:
//   def validate(a: String): Boolean = a.nonEmpty

// object NonBlank extends Constraint[String]:
//   def validate(a: String): Boolean = a.trim.nonEmpty

// trait FromEnv[A]:
//   def fromEnv(parentKey: String, selected: Map[String, String]): A

// object StringFromEnv extends FromEnv[String]:
//   def fromEnv(parentKey: String, selected: Map[String, String]): String =
//     selected.getOrElse(parentKey, throw new Exception(s"Key $parentKey not found in env"))

// given ListFromEnv[A](using FromEnv[A]): FromEnv[List[A]] with
//   def fromEnv(parentKey: String, selected: Map[String, String]): List[A] =
//     val prefix = s"$parentKey."
//     val subselected = selected.filter(_._1.startsWith(prefix))
//     subselected.keys
//       .map { k =>
//         val index = k.stripPrefix(prefix)
//         val value = summon[FromEnv[A]].fromEnv(k, selected)
//         index.toInt -> value
//       }
//       .toList
//       .sortBy(_._1)
//       .map(_._2)

trait Configured[A]:
  def schema: Schema
  def newInstanceFromEnv(env: Map[String, String] = sys.env): A

object Configured:
  val Version = "0.1.0"

  inline def derived[A <: Product]: Configured[A] = ${ derivedImpl[A] }

  def derivedImpl[A <: Product: Type](using ctx: Quotes): Expr[Configured[A]] =
    import ctx.reflect.*

    val tpe = TypeRepr.of[A]
    val fields = tpe.typeSymbol.caseFields.map { case sym =>
      val name = Expr(sym.name)
      val ftpe: Expr[ConfiguredType[_]] =
        tpe.memberType(sym).dealias.asType match
          case '[t] =>
            Expr.summon[ConfiguredType[t]].getOrElse {
              report.error(
                s"Cannot find ConfiguredType for type ${tpe.memberType(sym).dealias.show}"
              )
              throw new Exception("Cannot find ConfiguredType")
            }
          case _ =>
            report.error("Unsupported type")
            throw new Exception("Unsupported type")

      '{ Field(${ name }, ${ ftpe }.toFieldType) }
    }

    val fromEnvExpr = Expr.summon[FromEnv[A]].getOrElse {
      report.error(s"Cannot find FromEnv for type ${tpe.show}")
      throw new Exception("Cannot find FromEnv")
    }

    val schemaExpr = '{ Schema(${ Expr.ofList(fields) }.toList, ${ Expr(Version) }) }

    '{
      new Configured[A] {
        def schema = $schemaExpr
        def newInstanceFromEnv(env: Map[String, String] = sys.env): A =
          $fromEnvExpr.decode(env, "").getOrElse {
            throw new Exception("Failed to decode")
          }
      }
    }
end Configured

def resolveConfiguration[A](using c: Configured[A]): A =
  c.newInstanceFromEnv()
