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
class Output[F[+_], +A] private[internal] (val dataMonad: F[OutputData[A]])(using F: Monad[F], ctx: Context[F]):
  import IsOutputData.given

  def map[B](f: A => B): Output[F, B] = Output(dataMonad.map(_.map(f)))

  def flatMap[B](f: A => Output[F, B]): Output[F, B] =
    Output(
      for
        data   <- dataMonad
        nested <- data.traverseM(f.andThen(o => o.getData))
      yield nested.flatten
    )

  def zip[B](that: => Output[F, B])(using z: Zippable[A, B]): Output[F, z.Out] =
    Output(dataMonad.zip(that.getData).map((a, b) => a.zip(b)))

  def flatten[B](using ev: A <:< Output[F, B]): Output[F, B] = flatMap(identity)

  def asPlaintext: Output[F, A] = withIsSecret(F.eval(false))

  def asSecret: Output[F, A] = withIsSecret(F.eval(true))

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

/** These factory methods should be the only way to create Output instances!
  */

trait OutputFactory[F[+_]](using F: Monad[F]):
  def apply[A](value: A)(using ctx: Context[F], ev: Not[IsOutputData[A]]): Output[F, A] = Output(value)
  def apply[A](value: => F[A])(using ctx: Context[F], ev: Not[IsOutputData[A]]): Output[F, A] =
    Output(value)
  def apply[A](data: OutputData[A])(using ctx: Context[F]): Output[F, A]        = Output(data)
  def apply[A](value: => F[OutputData[A]])(using ctx: Context[F]): Output[F, A] = Output(value)

  def secret[A](value: A)(using ctx: Context[F]): Output[F, A] = Output[F].secret(value)

object Output:
  def empty[F[+_]](using ctx: Context[F], F: Monad[F]): Output[F, Nothing] =
    new Output(ctx.registerTask(F.eval(OutputData.empty[Nothing]())))

  def apply[F[+_]](using ctx: Context[F], F: Monad[F]): OutputPartiallyApplied[F] =
    new OutputPartiallyApplied[F](using ctx, F)

class OutputPartiallyApplied[F[+_]](using ctx: Context[F], F: Monad[F]):
  def apply[A](value: A)(using ev: Not[IsOutputData[A]]): Output[F, A] =
    new Output[F, A](ctx.registerTask(F.eval(OutputData(value))))

  def apply[A](value: => F[A])(using ev: Not[IsOutputData[A]]): Output[F, A] =
    new Output[F, A](ctx.registerTask(value.map(OutputData(_))))

  def apply[A](data: OutputData[A]): Output[F, A] =
    new Output[F, A](ctx.registerTask(F.eval(data)))

  def apply[A](value: => F[OutputData[A]]): Output[F, A] =
    new Output[F, A](ctx.registerTask((value)))

  def secret[A](value: A): Output[F, A] =
    new Output[F, A](ctx.registerTask(F.eval(OutputData(value))))
