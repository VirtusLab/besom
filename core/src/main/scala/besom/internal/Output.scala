package besom.internal

import scala.util.{NotGiven => Not}
import besom.util.NotProvided

/** Output is a wrapper for a monadic effect used to model async execution that allows Pulumi to track information about
  * dependencies between resources and properties of data (whether it's known or a secret for instance).
  *
  * Invariant: dataResult has to be registered in Context by the time when it reaches the constructor here!
  * @param dataResult
  *   Effect of type F[A]
  * @param F
  *   Monad instance for F[+_]
  * @param ctx
  *   Context
  */
class Output[+A] private[internal] (using private[besom] val ctx: Context)(
  private val dataResult: Result[OutputData[A]]
):
  import IsOutputData.given

  def map[B](f: A => B): Output[B] = Output(dataResult.map(_.map(f)))

  def flatMap[B](f: A => Output[B]): Output[B] =
    Output(
      for
        outputData: OutputData[A]         <- dataResult
        nested: OutputData[OutputData[B]] <- outputData.traverseResult(a => f(a).getData)
      yield nested.flatten
    )

  def zip[B](that: => Output[B])(using z: Zippable[A, B]): Output[z.Out] =
    Output(dataResult.zip(that.getData).map((a, b) => a.zip(b)))

  def flatten[B](using ev: A <:< Output[B]): Output[B] = flatMap(identity)

  def asPlaintext: Output[A] = withIsSecret(Result.pure(false))

  def asSecret: Output[A] = withIsSecret(Result.pure(true))

  private[internal] def getData: Result[OutputData[A]] = dataResult

  private[internal] def withIsSecret(isSecretEff: Result[Boolean]): Output[A] =
    Output(
      for
        secret <- isSecretEff
        o      <- dataResult
      yield o.withIsSecret(secret)
    )

sealed trait IsOutputData[A]
object IsOutputData:
  given [A](using A =:= OutputData[_]): IsOutputData[OutputData[_]] = null

/** These factory methods should be the only way to create Output instances!
  */

trait OutputFactory:
  def apply[A](value: A)(using ctx: Context, ev: Not[IsOutputData[A]]): Output[A] = Output(value)
  // def apply[A](using ctx: Context, ev: Not[IsOutputData[A]])(value: => ctx.F[A]): Output[A] = Output(value)
  def apply[A](data: OutputData[A])(using ctx: Context): Output[A]             = Output(data)
  def apply[A](using ctx: Context)(value: => Result[OutputData[A]]): Output[A] = Output(value)

  def secret[A](value: A)(using ctx: Context): Output[A] = Output.secret(value)

object Output:
  // should be NonEmptyString
  def traverseMap[A](using ctx: Context)(map: Map[String, Output[A]]): Output[Map[String, A]] =
    sequence(map.map((key, value) => value.map(result => (key, result))).toVector).map(_.toMap)

  def sequence[A](using ctx: Context)(v: Vector[Output[A]]): Output[Vector[A]] =
    v.foldLeft[Output[Vector[A]]](Output(Vector.empty[A])) { case (out, curr) =>
      curr.flatMap(a => out.map(_ appended a))
    }

  def empty(isSecret: Boolean = false)(using ctx: Context): Output[Nothing] =
    new Output(ctx.registerTask(Result.pure(OutputData.empty[Nothing](isSecret = isSecret))))

  def apply[A](using ev: Not[IsOutputData[A]])(value: => Result[A])(using
    ctx: Context
  ): Output[A] =
    new Output[A](ctx.registerTask(OutputData.traverseResult(value)))

  // could this be pure without implicit Context? it's not async in any way so?
  def apply[A](value: A)(using ev: Not[IsOutputData[A]])(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(OutputData(value))))

  def apply[A](value: => Result[OutputData[A]])(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask((value)))

  // could this be pure without implicit Context? it's not async in any way so?
  def apply[A](data: OutputData[A])(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(data)))

  def secret[A](using ctx: Context)(value: A): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(OutputData(value))))

  extension [A](v: A | Output[A] | NotProvided)
    def asOutput(isSecret: Boolean = false)(using ctx: Context): Output[A] =
      v match
        case NotProvided     => Output.empty(isSecret)
        case out: Output[_]  => out.asInstanceOf[Output[A]] // TODO TypeTest?
        case a: A @unchecked => if isSecret then Output.secret(a) else Output(a)

  extension [A](v: Map[String, A] | Map[String, Output[A]] | Output[Map[String, A]] | NotProvided)
    def asOutputMap(isSecret: Boolean = false)(using ctx: Context): Output[Map[String, A]] =
      v match
        case NotProvided    => Output.empty(isSecret)
        case out: Output[_] => out.asInstanceOf[Output[Map[String, A]]] // TODO TypeTest?
        case m: Map[_, _] @unchecked =>
          if m.exists((_, v) => v.isInstanceOf[Output[_]]) then
            Output.traverseMap(m.asInstanceOf[Map[String, Output[A]]])
          else if isSecret then Output.secret(m.asInstanceOf[Map[String, A]])
          else Output(m.asInstanceOf[Map[String, A]])

// prototype, not really useful, sadly
// object OutputLift extends OutputGiven0:

//   def lift[F[+_], A, In <: Output[A] | F[A] | A](using ctx: Context)(
//     value: In
//   )(using ol: OutputLifter[In, A]): Output[A] = ol.lift(value)

//   trait OutputLifter[F[+_], In, A]:
//     def lift(in: => In): Output[F, A]

// trait OutputGiven0 extends OutputGiven1:
//   self: OutputLift.type =>

//   given [F[+_], A](using Context.Of[F]): OutputLifter[F, Output[F, A], A] =
//     new OutputLifter[F, Output[F, A], A]:
//       def lift(in: => Output[F, A]): Output[F, A] = in

// trait OutputGiven1 extends OutputGiven2:
//   self: OutputLift.type =>

//   import scala.util.{NotGiven => Not}

//   given [F[+_], A0, A >: A0](using ctx: Context.Of[F], ev: Not[IsOutputData[A0]]): OutputLifter[F, F[A0], A] =
//     new OutputLifter[F, F[A0], A]:
//       def lift(in: => F[A0]): Output[F, A0] = Output.apply[ctx.F].apply(using ev)(in)

// trait OutputGiven2:
//   self: OutputLift.type =>
//   given [F[+_], A](using Context.Of[F]): OutputLifter[F, A, A] =
//     new OutputLifter[F, A, A]:
//       def lift(in: => A): Output[F, A] = Output(in)
