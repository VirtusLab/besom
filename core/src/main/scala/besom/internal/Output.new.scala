package besom.internal

import scala.collection.BuildFrom

// trait Output[+A]:
//   def map[B](f: A => B): Output[B]
//   def flatMap[B](f: A => Output[B]): Output[B]
//   def flatMap[F[_]: Result.ToFuture, B](f: A => F[B]): Output[B]
//   inline def flatMap[B](f: A => B): Nothing = scala.compiletime.error(
//     """Output#flatMap can only be used with functions that return an Output or a structure like scala.concurrent.Future, cats.effect.IO or zio.Task.
//   If you want to map over the value of an Output, use the map method instead."""
//   )
//   def recover[B >: A](f: Throwable => B): Output[B]
//   def recoverWith[B >: A](f: Throwable => Output[B]): Output[B]
//   def recoverWith[B >: A, F[_]: Result.ToFuture](f: Throwable => F[B]): Output[B]
//   def tap(f: A => Output[Unit]): Output[A]
//   def tapError(f: Throwable => Output[Unit]): Output[A]
//   def tapBoth(f: A => Output[Unit], onError: Throwable => Output[Unit]): Output[A]
//   def zip[B](that: => Output[B])(using z: Zippable[A, B]): Output[z.Out]
//   def flatten[B](using ev: A <:< Output[B]): Output[B]
//   def asPlaintext: Output[A]
//   def asSecret: Output[A]
//   def void: Output[Unit]

//   private[besom] def flatMapOption[B, C](using ev: A <:< Option[B])(f: B => Output[C] | Output[Option[C]]): Output[Option[C]]

//   private[internal] def getData: Result[OutputData[A]]
//   private[internal] def getValue: Result[Option[A]]
//   private[internal] def getValueOrElse[B >: A](default: => B): Result[B]
//   private[internal] def getValueOrFail(msg: String): Result[A]
//   private[internal] def withIsSecret(isSecretEff: Result[Boolean]): Output[A]

class Output[+A](private val inner: Context => Result[OutputData[A]]):

  def map[B](f: A => B): Output[B] = new Output[B](ctx => inner(ctx).map(_.map(f)))

  def flatMap[B](f: A => Output[B]): Output[B] = new Output(ctx =>
    for
      outputData: OutputData[A]         <- inner(ctx)
      nested: OutputData[OutputData[B]] <- outputData.traverseResult(a => f(a).inner(ctx))
    yield nested.flatten
  )

  def flatMap[F[_]: Result.ToFuture, B](f: A => F[B]): Output[B] = new Output(ctx =>
    for
      outputData: OutputData[A]         <- inner(ctx)
      nested: OutputData[OutputData[B]] <- outputData.traverseResult(a => Result.eval(f(a)).map(OutputData(_)))
    yield nested.flatten
  )

  inline def flatMap[B](f: A => B): Nothing = scala.compiletime.error(
    """Output#flatMap can only be used with functions that return an Output or a structure like scala.concurrent.Future, cats.effect.IO or zio.Task.
  If you want to map over the value of an Output, use the map method instead."""
  )

  def flatten[B](using ev: A <:< Output[B]): Output[B] = flatMap(a => ev(a))

  def recover[B >: A](f: Throwable => B): Output[B] = new Output[B](ctx => inner(ctx).recover { t => Result.pure(OutputData(f(t))) })

  def recoverWith[B >: A](f: Throwable => Output[B]): Output[B] = new Output(ctx =>
    inner(ctx).recover { t =>
      f(t).inner(ctx)
    }
  )

  def recoverWith[B >: A, F[_]: Result.ToFuture](f: Throwable => F[B]): Output[B] = new Output(ctx =>
    inner(ctx).recover { t =>
      Result.eval(f(t)).map(OutputData(_))
    }
  )

  def tap(f: A => Output[Unit]): Output[A] =
    flatMap { a =>
      f(a).map(_ => a)
    }

  def tapError(f: Throwable => Output[Unit]): Output[A] = new Output(ctx =>
    inner(ctx).tapBoth {
      case Left(t) => f(t).inner(ctx).void
      case _       => Result.unit
    }
  )

  def tapBoth(f: A => Output[Unit], onError: Throwable => Output[Unit]): Output[A] = new Output(ctx =>
    inner(ctx).tapBoth {
      case Left(t)                                => onError(t).inner(ctx).void
      case Right(OutputData.Known(_, _, Some(a))) => f(a).inner(ctx).void
      case Right(_)                               => Result.unit
    }
  )

  def zip[B](that: => Output[B])(using z: Zippable[A, B]): Output[z.Out] = new Output(ctx =>
    inner(ctx).zip(that.inner(ctx)).map((a, b) => a.zip(b))
  )

  def asPlaintext: Output[A] = withIsSecret(Result.pure(false))
  def asSecret: Output[A]    = withIsSecret(Result.pure(true))

  def void: Output[Unit] = map(_ => ())

  private[besom] def flatMapOption[B, C](using ev: A <:< Option[B])(f: B => Output[C] | Output[Option[C]]): Output[Option[C]] =
    flatMap { a =>
      ev(a) match
        case Some(b) =>
          f(b).map {
            case Some(c) => Some(c.asInstanceOf[C])
            case None    => None
            case c       => Some(c.asInstanceOf[C])
          }
        case None => Output(None)
    }

  private[internal] def getData: Result[OutputData[A]]                   = ???
  private[internal] def getValue: Result[Option[A]]                      = ???
  private[internal] def getValueOrElse[B >: A](default: => B): Result[B] = ???
  private[internal] def getValueOrFail(msg: String): Result[A]           = ???

  private[internal] def withIsSecret(isSecretEff: Result[Boolean]): Output[A] = new Output(ctx =>
    for
      secret <- isSecretEff
      o      <- inner(ctx)
    yield o.withIsSecret(secret)
  )

end Output

trait OutputFactory:
  /** Creates an `Output` that evaluates given effectful computation.
    *
    * The type `F[_]` is constrained to types for which an instance of `Result.ToFuture` is defined.
    *
    * Besom offers the following instances:
    *   - `besom-core` provides a `ToFuture` instance for `scala.concurrent.Future`
    *   - `besom-zio` provides a `ToFuture` instance for `zio.Task`
    *   - `besom-cats` provides a `ToFuture` instance for `cats.effect.IO`
    *
    * @tparam F
    *   the effect type
    * @tparam A
    *   the type of the value
    * @param value
    *   the value to wrap in an `Output`
    */
  def eval[F[_]: Result.ToFuture, A](value: F[A]): Output[A] = Output.eval(value)

  /** Creates an `Output` with the given `value`
    *
    * @see
    *   [[secret]] for creating an `Output` with a secret value
    */
  def apply[A](value: A): Output[A] = Output.pure(value)

  /** Creates an `Output` that is known to be a secret
    */
  def secret[A](value: A): Output[A] = Output.secret(value)

  /** Creates an `Output` of a collection from a collection of Outputs.
    *
    * @see
    *   [[parSequence]] for parallel execution
    */
  def sequence[A, CC[X] <: Iterable[X], To](
    coll: CC[Output[A]]
  )(using BuildFrom[CC[Output[A]], A, To]): Output[To] = Output.sequence(coll)

  /** Creates an `Output` of a collection from a collection of values mapped with the function `f`
    *
    * @param coll
    *   the collection to map with `f`
    * @param f
    *   the Output-returning function to apply to each element in the collection
    */
  def traverse[A, CC[X] <: Iterable[X], B, To](
    coll: CC[A]
  )(
    f: A => Output[B]
  )(using BuildFrom[CC[Output[B]], B, To]): Output[To] = sequence(coll.map(f).asInstanceOf[CC[Output[B]]])

  /** Creates a failed [[Output]] containing given [[Throwable]]
    */
  def fail(t: Throwable): Output[Nothing] = Output.fail(t)

  /** Creates an `Output` with the given `a` if the given `condition` is `true` or returns `None` if the condition is `false`
    */
  // def when[A](condition: => Input[Boolean])(a: => Input.Optional[A]): Output[Option[A]] =
  //   Output.when(condition)(a)

  /** Creates an `Output` that contains Unit
    */
  def unit: Output[Unit] = Output(())

end OutputFactory

object Output:
  def getContext: Output[Context] = new Output(ctx => Result.pure(OutputData(ctx)))

  // should be NonEmptyString
  def traverseMap[A](map: Map[String, Output[A]]): Output[Map[String, A]] =
    sequence(map.map((key, value) => value.map(result => (key, result))).toVector).map(_.toMap)

  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[Output[A]]
  )(using bf: BuildFrom[CC[Output[A]], A, To]): Output[To] =
    Output.ofResult {
      Result.defer {
        coll.iterator
          .foldLeft(Output(bf.newBuilder(coll))) { (acc, curr) =>
            acc.zip(curr).map { case (b, r) =>
              b += r
            }
          }
          .map(_.result())
      }
    }.flatten

  def parSequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[Output[A]]
  )(using bf: BuildFrom[CC[Output[A]], A, To]): Output[To] = ???
  // Output
  //   .ofResult {
  //     Result
  //       .defer {
  //         Result.parSequence(coll.iterator.map(_.dataResult).toVector)
  //       }
  //       .flatten
  //       .map { vecOfOutputData =>
  //         vecOfOutputData.map(OutputX.ofData(_))
  //       }
  //   }
  //   .flatMap { vecOfOutputX =>
  //     OutputX.sequence(vecOfOutputX).map { vecOfA =>
  //       bf.fromSpecific(coll)(vecOfA)
  //     }
  //   }

  def empty(isSecret: Boolean = false): Output[Nothing] =
    new Output(ctx => Result.pure(OutputData.empty[Nothing](isSecret = isSecret)))

  def eval[F[_]: Result.ToFuture, A](value: F[A]): Output[A] =
    new Output[A](ctx => Result.eval[F, A](value).map(OutputData(_)))

  def fail(t: Throwable): Output[Nothing] = new Output(ctx => Result.fail(t))

  def ofResult[A](value: => Result[A]): Output[A] = new Output(ctx => value.map(OutputData(_)))

  def apply[A](value: => A): Output[A] = new Output(ctx => Result.defer(OutputData(value)))

  def pure[A](value: A): Output[A] = new Output(ctx => Result.pure(OutputData(value)))

  def ofData[A](value: => Result[OutputData[A]]): Output[A] = new Output(ctx => value)

  def ofData[A](data: OutputData[A]): Output[A] = new Output(ctx => Result.pure(data))

  def secret[A](value: A): Output[A] = new Output(ctx => Result.pure(OutputData(value, Set.empty, isSecret = true)))

  def when[A](cond: => Input[Boolean])(
    a: => Input.Optional[A]
  ): Output[Option[A]] =
    ???
  // cond.asOutput().flatMap { c =>
  //   if c then a.asOptionOutput(isSecret = false) else Output.pure(None)
  // }
  end when
end Output
