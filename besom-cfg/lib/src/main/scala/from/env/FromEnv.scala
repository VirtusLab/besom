package besom.cfg.from.env

import besom.util.*
import scala.util.Try
import besom.cfg.Struct
import besom.types.{Output, Context}

extension (s: Struct)
  def foldToEnv: Output[List[(String, String)]] = s.fold[List[(String, String)]](
    onStruct = { mapB =>
      mapB.foldLeft(Output(List.empty[(String, String)])) { case (acc, (k, v)) =>
        acc.flatMap { accList =>
          v.map { vList =>
            accList ++ vList.map { case (k2, v2) =>
              if k2.isBlank then k -> v2 else s"$k.$k2" -> v2
            }
          }
        }
      }
    },
    onList = { list =>
      Output(list.zipWithIndex.flatMap { (lst, idx) =>
        lst.map { case (k, v) =>
          if k.isBlank() then s"$k$idx" -> v else s"$idx.$k" -> v
        }
      })
    },
    onValue = a => Output(List("" -> a.toString))
  )

enum EnvPath:
  case Root
  case Sub(path: String)

  def subpath(path: String): EnvPath =
    this match
      case Root   => Sub(path)
      case Sub(p) => Sub(s"$p.$path")

  def asKey: String = this match
    case Root      => ReadFromEnvVars.Prefix
    case Sub(path) => s"${ReadFromEnvVars.Prefix}_$path"

trait ReadFromEnvVars[A]:
  def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, A]

object ReadFromEnvVars:

  private[cfg] val Prefix = "BESOM_CFG"

  case class MissingKey(path: EnvPath)
      extends Exception(s"Missing value for key: ${path.asKey.stripPrefix(Prefix + "_")} (env var: `${path.asKey}`)")

  extension (env: Map[String, String])
    private[ReadFromEnvVars] def lookup(path: EnvPath): Validated[Throwable, String] =
      env.get(path.asKey).filter(_.nonEmpty).toValidatedOrError(MissingKey(path))

  import scala.deriving.*
  import scala.compiletime.{erasedValue, summonInline}

  inline def summonAllInstances[T <: Tuple]: List[ReadFromEnvVars[?]] =
    inline erasedValue[T] match
      case _: (t *: ts)  => summonInline[ReadFromEnvVars[t]] :: summonAllInstances[ts]
      case _: EmptyTuple => Nil

  inline def summonLabels[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) =>
        summonInline[ValueOf[t]].value.asInstanceOf[String] :: summonLabels[ts]

  given [A: ReadFromEnvVars]: ReadFromEnvVars[Option[A]] with
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, Option[A]] =
      summon[ReadFromEnvVars[A]].decode(env, path).redeem(_ => None, Some(_))

  given ReadFromEnvVars[Int] with
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, Int] =
      env.lookup(path).flatMap(s => Try(s.toInt).toValidated)

  given ReadFromEnvVars[Long] with
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, Long] =
      env.lookup(path).flatMap(s => Try(s.toLong).toValidated)

  given ReadFromEnvVars[String] with
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, String] =
      env.lookup(path)

  given ReadFromEnvVars[Double] with
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, Double] =
      env.lookup(path).flatMap(s => Try(s.toDouble).toValidated)

  given ReadFromEnvVars[Float] with
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, Float] =
      env.lookup(path).flatMap(s => Try(s.toFloat).toValidated)

  given ReadFromEnvVars[Boolean] with
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, Boolean] =
      env.lookup(path).flatMap(s => Try(s.toBoolean).toValidated)

  given [A: ReadFromEnvVars]: ReadFromEnvVars[List[A]] with
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, List[A]] =
      Iterator.from(0).map(i => summon[ReadFromEnvVars[A]].decode(env, path.subpath(i.toString()))).takeWhile(_.isValid).toList.sequenceL

  given [A: ReadFromEnvVars]: ReadFromEnvVars[Vector[A]] with
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, Vector[A]] =
      Iterator.from(0).map(i => summon[ReadFromEnvVars[A]].decode(env, path.subpath(i.toString()))).takeWhile(_.isValid).toVector.sequenceV

  inline given derived[A](using m: Mirror.ProductOf[A]): ReadFromEnvVars[A] = new ReadFromEnvVars[A]:
    def decode(env: Map[String, String], path: EnvPath): Validated[Throwable, A] =
      val elemDecoders = summonAllInstances[m.MirroredElemTypes]
      val labels       = summonLabels[m.MirroredElemLabels]

      val elemValues = elemDecoders.zip(labels).map { case (decoder, label) =>
        // handle top-level gracefully (empty path)
        val computedPath = path match
          case EnvPath.Root   => EnvPath.Sub(label)
          case s: EnvPath.Sub => s.subpath(label)
        decoder.asInstanceOf[ReadFromEnvVars[Any]].decode(env, computedPath)
      }

      elemValues.sequenceL.map { values =>
        val product = m.fromProduct(Tuple.fromArray(values.toArray))
        product.asInstanceOf[A]
      }

end ReadFromEnvVars
