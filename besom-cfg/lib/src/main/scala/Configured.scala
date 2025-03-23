package besom.cfg

import scala.quoted.*
import besom.json.*
import besom.cfg.internal.*
import besom.util.Validated
import scala.util.control.NoStackTrace

final val Version = "0.4.0"

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

trait Configured[A]:
  type INPUT
  def schema: Schema
  def newInstance(input: INPUT): A

object Configured:

  trait FromEnv[A] extends Configured[A]:
    type INPUT = FromEnv.EnvData

  object FromEnv:
    val MediumIdentifier = "env"

    opaque type EnvData >: Map[String, String] = Map[String, String]
    extension (env: EnvData) def unwrap: Map[String, String] = env
    object EnvData:
      given Default[EnvData] = new Default[EnvData]:
        def default: EnvData = sys.env

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
        report.errorAndAbort(s"Cannot find FromEnv for type ${tpe.show}")
      }

      val schemaExpr = '{ Schema(${ Expr.ofList(fields) }.toList, ${ Expr(Version) }, ${ Expr(FromEnv.MediumIdentifier) }) }

      '{
        new Configured.FromEnv[A] {

          def schema = $schemaExpr
          def newInstance(input: INPUT): A =
            $fromEnvExpr.decode(input.unwrap, from.env.EnvPath.Root) match
              case Validated.Valid(a)        => a
              case Validated.Invalid(errors) => throw ConfigurationError(errors.toVector)
        }
      }
    end derivedImpl
  end FromEnv
end Configured

def resolveConfiguration[A](using c: Configured[A], d: Default[c.INPUT]): A =
  c.newInstance(d.default)

def resolveConfiguration[A](using c: Configured[A])(input: c.INPUT): A =
  c.newInstance(input)

def resolveConfigurationEither[A](using c: Configured[A], d: Default[c.INPUT]): Either[ConfigurationError, A] =
  try Right(resolveConfiguration[A])
  catch case e: ConfigurationError => Left(e)

def resolveConfigurationEither[A](using c: Configured[A])(input: c.INPUT): Either[ConfigurationError, A] =
  try Right(resolveConfiguration[A](input))
  catch case e: ConfigurationError => Left(e)
