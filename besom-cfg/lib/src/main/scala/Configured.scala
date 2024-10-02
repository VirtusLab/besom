package besom.cfg

import scala.quoted.*
import besom.json.*
import besom.cfg.internal.*
import besom.util.Validated
import scala.util.control.NoStackTrace

final val Version = "0.4.0-SNAPSHOT"

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

case class ConfigurationError(errors: Iterable[Throwable]) extends Exception(ConfigurationError.render(errors)) with NoStackTrace:
  override def toString(): String = getMessage()

object ConfigurationError:
  def render(errors: Iterable[Throwable]): String =
    s"""Start of the application was impossible due to the following configuration errors:
         |${errors.map(_.getMessage).mkString("  * ", "\n  * ", "")}
         |""".stripMargin

trait Default[A]:
  def default: A

trait Configured[A, INPUT]:
  def schema: Schema
  def newInstance(input: INPUT): A

object Configured:

  trait FromEnv[A] extends Configured[A, Map[String, String]]:
    def schema: Schema
    def newInstance(input: Map[String, String]): A

  object FromEnv:
    val MediumIdentifier = "env"

    inline def derived[A <: Product]: Configured.FromEnv[A] = ${ derivedImpl[A] }

    def derivedImpl[A <: Product: Type](using ctx: Quotes): Expr[Configured.FromEnv[A]] =
      import ctx.reflect.*

      val tpe = TypeRepr.of[A]
      val fields = tpe.typeSymbol.caseFields.map { case sym =>
        val name = Expr(sym.name)
        val ftpe: Expr[ConfiguredType[_]] =
          tpe.memberType(sym).dealias.asType match
            case '[t] =>
              Expr.summon[ConfiguredType[t]].getOrElse {
                report.errorAndAbort(
                  s"Cannot find ConfiguredType for type ${tpe.memberType(sym).dealias.show}"
                )
              }

            case _ => report.errorAndAbort("Unsupported type")

        '{ Field(${ name }, ${ ftpe }.toFieldType) }
      }

      val fromEnvExpr = Expr.summon[from.env.ReadFromEnvVars[A]].getOrElse {
        report.error(s"Cannot find FromEnv for type ${tpe.show}")
        throw new Exception("Cannot find FromEnv")
      }

      val schemaExpr = '{ Schema(${ Expr.ofList(fields) }.toList, ${ Expr(Version) }, ${ Expr(FromEnv.MediumIdentifier) }) }

      '{
        new Configured.FromEnv[A] {
          def schema = $schemaExpr
          def newInstance(input: Map[String, String]): A =
            $fromEnvExpr.decode(input, from.env.EnvPath.Root) match
              case Validated.Valid(a)        => a
              case Validated.Invalid(errors) => throw ConfigurationError(errors.toVector)

        }
      }
    end derivedImpl
  end FromEnv
end Configured

def resolveConfiguration[A, INPUT: Default](using c: Configured[A, INPUT]): A =
  c.newInstance(summon[Default[INPUT]].default)

def resolveConfiguration[A, INPUT](input: INPUT)(using c: Configured[A, INPUT]): A =
  c.newInstance(input)

def resolveConfigurationEither[A, INPUT: Default](using c: Configured[A, INPUT]): Either[ConfigurationError, A] =
  try Right(resolveConfiguration[A, INPUT])
  catch case e: ConfigurationError => Left(e)

def resolveConfigurationEither[A, INPUT](input: INPUT)(using c: Configured[A, INPUT]): Either[ConfigurationError, A] =
  try Right(resolveConfiguration[A, INPUT](input))
  catch case e: ConfigurationError => Left(e)
