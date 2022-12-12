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
class Output[F[+_], +A] private[internal] (using val ctx: Context)(private val dataMonad: ctx.F[OutputData[A]]):
  import IsOutputData.given
  inline given Monad[ctx.F] = ctx.monad

  def map[B](f: A => B): Output[ctx.F, B] = Output(dataMonad.map(_.map(f)))

  def flatMap[B](f: A => Output[ctx.F, B]): Output[ctx.F, B] =
    Output(
      for
        data   <- dataMonad
        nested <- data.traverseM(a => f(a).getData)
      yield nested.flatten
    )

  def zip[B](that: => Output[ctx.F, B])(using z: Zippable[A, B]): Output[ctx.F, z.Out] =
    Output(dataMonad.zip(that.getData).map((a, b) => a.zip(b)))

  def flatten[B](using ev: A <:< Output[ctx.F, B]): Output[ctx.F, B] = flatMap(identity)

  def asPlaintext: Output[ctx.F, A] = withIsSecret(ctx.monad.eval(false))

  def asSecret: Output[ctx.F, A] = withIsSecret(ctx.monad.eval(true))

  private[internal] def getData: ctx.F[OutputData[A]] = dataMonad

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

trait OutputFactory:
  def apply[A](value: A)(using ctx: Context, ev: Not[IsOutputData[A]]): Output[ctx.F, A]           = Output(value)
  def apply[A](using ctx: Context, ev: Not[IsOutputData[A]])(value: => ctx.F[A]): Output[ctx.F, A] = Output(value)
  def apply[A](data: OutputData[A])(using ctx: Context): Output[ctx.F, A]                          = Output(data)
  def apply[A](using ctx: Context)(value: => ctx.F[OutputData[A]]): Output[ctx.F, A]               = Output(value)

  def secret[A](value: A)(using ctx: Context): Output[ctx.F, A] = Output.secret(value)

object Output:
  def empty(using ctx: Context): Output[ctx.F, Nothing] =
    new Output(ctx.registerTask(ctx.monad.eval(OutputData.empty[Nothing]())))

  def apply[A](using ctx: Context)(value: A)(using ev: Not[IsOutputData[A]]): Output[ctx.F, A] =
    new Output[ctx.F, A](ctx.registerTask(ctx.monad.eval(OutputData(value))))

  def apply[A](using ctx: Context)(value: => ctx.F[A])(using ev: Not[IsOutputData[A]]): Output[ctx.F, A] =
    given Monad[ctx.F] = ctx.monad
    new Output[ctx.F, A](ctx.registerTask(value.map(OutputData(_))))

  def apply[A](using ctx: Context)(data: OutputData[A]): Output[ctx.F, A] =
    new Output[ctx.F, A](ctx.registerTask(ctx.monad.eval(data)))

  def apply[A](using ctx: Context)(value: => ctx.F[OutputData[A]]): Output[ctx.F, A] =
    new Output[ctx.F, A](ctx.registerTask((value)))

  def secret[A](using ctx: Context)(value: A): Output[ctx.F, A] =
    new Output[ctx.F, A](ctx.registerTask(ctx.monad.eval(OutputData(value))))

  // def apply(using ctx: Context): OutputPartiallyApplied =
  // new OutputPartiallyApplied

// class OutputPartiallyApplied(using val ctx: Context):
//   def apply[A](value: A)(using ev: Not[IsOutputData[A]]): Output[ctx.F, A] =
//     new Output[ctx.F, A](ctx.registerTask(ctx.monad.eval(OutputData(value))))(using ctx.monad)

//   def apply[A](value: => ctx.F[A])(using ev: Not[IsOutputData[A]]): Output[ctx.F, A] =
//     given Monad[ctx.F] = ctx.monad
//     new Output[ctx.F, A](ctx.registerTask(value.map(OutputData(_))))(using ctx.monad)

//   def apply[A](data: OutputData[A]): Output[ctx.F, A] =
//     new Output[ctx.F, A](ctx.registerTask(ctx.monad.eval(data)))(using ctx.monad)

//   def apply[A](value: => ctx.F[OutputData[A]]): Output[ctx.F, A] =
//     new Output[ctx.F, A](ctx.registerTask((value)))(using ctx.monad)

//   def secret[A](value: A): Output[ctx.F, A] =
//     new Output[ctx.F, A](ctx.registerTask(ctx.monad.eval(OutputData(value))))(using ctx.monad)
