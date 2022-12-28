package besom.internal

import scala.util.{NotGiven => Not}

// trait Output[F[+_], A](using M: Monad[F]):
//   def map[B](f: A => B): Output[F, B]
//   def flatMap[B](f: A => Output[F, B]): Output[F, B]
//   def asPlaintext: Output[F, A]
//   def asSecret: Output[F, A]
//   private[internal] def getData: F[OutputData[A]]

// extends Output[F, A]:

/** Output is a wrapper for a monadic effect used to model async execution that allows Pulumi to track information about
  * dependencies between resources and properties of data (whether it's known or a secret for instance).
  *
  * Invariant: dataMonad has to be registered in Context by the time when it reaches the constructor here!
  * @param dataMonad
  *   Effect of type F[A]
  * @param F
  *   Monad instance for F[+_]
  * @param ctx
  *   Context
  */
class Output[F[+_], +A] private[internal] (using val ctx: Context.Of[F])(private val dataMonad: F[OutputData[A]]):
  import IsOutputData.given

  def map[B](f: A => B): Output[F, B] = Output(dataMonad.map(_.map(f)))

  def flatMap[B](f: A => Output[F, B]): Output[F, B] =
    Output(
      for
        data: OutputData[A]               <- dataMonad
        nested: OutputData[OutputData[B]] <- data.traverseM(a => f(a).getData)
      yield nested.flatten
    )

  def zip[B](that: => Output[F, B])(using z: Zippable[A, B]): Output[F, z.Out] =
    Output(dataMonad.zip(that.getData).map((a, b) => a.zip(b)))

  def flatten[B](using ev: A <:< Output[F, B]): Output[F, B] = flatMap(identity)

  def asPlaintext: Output[F, A] = withIsSecret(ctx.monad.eval(false))

  def asSecret: Output[F, A] = withIsSecret(ctx.monad.eval(true))

  private[internal] def getData: F[OutputData[A]] = dataMonad

  private[internal] def withIsSecret(isSecretEff: F[Boolean]): Output[F, A] =
    Output(
      for
        secret <- isSecretEff
        o      <- dataMonad
      yield o.withIsSecret(secret)
    )

sealed trait IsOutputData[A]
object IsOutputData:
  given [A](using A =:= OutputData[_]): IsOutputData[OutputData[_]] = null

sealed trait IsFData[F[+_], A]
object IsFData:
  given [F[+_], A]: IsFData[F, F[A]] = null

/** These factory methods should be the only way to create Output instances!
  */

trait OutputFactory:
  def apply[A](value: A)(using ctx: Context, ev: Not[IsOutputData[A]]): Output[ctx.F, A] = Output(value)
  // def apply[A](using ctx: Context, ev: Not[IsOutputData[A]])(value: => ctx.F[A]): Output[ctx.F, A] = Output(value)
  def apply[A](data: OutputData[A])(using ctx: Context): Output[ctx.F, A]            = Output(data)
  def apply[A](using ctx: Context)(value: => ctx.F[OutputData[A]]): Output[ctx.F, A] = Output(value)

  def secret[A](value: A)(using ctx: Context): Output[ctx.F, A] = Output.secret(value)

object Output:
  // should be NonEmptyString
  def traverseMap[A](using ctx: Context)(map: Map[String, Output[ctx.F, A]]): Output[ctx.F, Map[String, A]] =
    sequence(map.map((key, value) => value.map(result => (key, result))).toVector).map(_.toMap)

  def sequence[A](using ctx: Context)(v: Vector[Output[ctx.F, A]]): Output[ctx.F, Vector[A]] =
    v.foldLeft[Output[ctx.F, Vector[A]]](Output(Vector.empty[A])) { case (out, curr) =>
      curr.flatMap(a => out.map(_ appended a))
    }

  def empty(using ctx: Context): Output[ctx.F, Nothing] =
    new Output(ctx.registerTask(ctx.monad.eval(OutputData.empty[Nothing]())))

  def apply[A](value: A)(using ctx: Context, ev: Not[IsOutputData[A]], ev2: Not[IsFData[ctx.F, A]]): Output[ctx.F, A] =
    new Output[ctx.F, A](ctx.registerTask(ctx.monad.eval(OutputData(value))))

  def apply[A](data: OutputData[A])(using ctx: Context): Output[ctx.F, A] =
    new Output[ctx.F, A](ctx.registerTask(ctx.monad.eval(data)))

  def apply[F[+_], A](value: => F[OutputData[A]])(using ctx: Context.Of[F]): Output[F, A] =
    new Output[F, A](ctx.registerTask((value)))

  def secret[A](using ctx: Context)(value: A): Output[ctx.F, A] =
    new Output[ctx.F, A](ctx.registerTask(ctx.monad.eval(OutputData(value))))
