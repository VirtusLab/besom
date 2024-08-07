package besom.internal

import scala.collection.BuildFrom
import scala.reflect.Typeable

/** Output is a wrapper for a monadic effect used to model async execution that allows Pulumi to track information about dependencies
  * between resources and properties of data (whether it's known or a secret for instance).
  *
  * Invariant: `dataResult` has to be registered in [[TaskTracker]] by the time it reaches the constructor here.
  * @param dataResult
  *   Effect of type [[Result]][A]
  * @param ctx
  *   the Besom [[Context]]
  */
class Output[+A] private[internal] (using private[besom] val ctx: Context)(
  private val dataResult: Result[OutputData[A]]
):
  /** Maps the value of the Output using the given function.
    * @param f
    *   the function to apply to the value
    * @return
    *   an Output with the mapped value
    */
  def map[B](f: A => B): Output[B] = Output.ofData(dataResult.map(_.map(f)))

  /** Flat-maps the value of the Output using the given function.
    * @tparam B
    *   the type of the value
    * @param f
    *   the function to apply to the value
    * @return
    *   an Output with the flat-mapped value
    * @see
    *   `flatMap(A => F[B])` for flat-mapping with an effectful function
    */
  def flatMap[B](f: A => Output[B]): Output[B] =
    Output.ofData(
      for
        outputData: OutputData[A]         <- dataResult
        nested: OutputData[OutputData[B]] <- outputData.traverseResult(a => f(a).getData)
      yield nested.flatten
    )

  /** Flat-maps the value of the Output using the given effectful function.
    * @tparam F
    *   the effect type
    * @tparam B
    *   the type of the value
    * @param f
    *   the effectful function to apply to the value
    * @return
    *   an Output with the flat-mapped value
    * @see
    *   `flatMap(A => Output[B])` for flat-mapping with Output-returning function
    */
  def flatMap[F[_]: Result.ToFuture, B](f: A => F[B]): Output[B] =
    Output.ofData(
      for
        outputData: OutputData[A]         <- dataResult
        nested: OutputData[OutputData[B]] <- outputData.traverseResult(a => Result.eval(f(a)).map(OutputData(_)))
      yield nested.flatten
    )

  /** Mock variant of flatMap that will fail at compile time if used with a function that returns a value instead of an Output.
    *
    * @param f
    *   function to apply to the value of the Output
    */
  inline def flatMap[B](f: A => B): Nothing = scala.compiletime.error(
    """Output#flatMap can only be used with functions that return an Output or a structure like scala.concurrent.Future, cats.effect.IO or zio.Task.
If you want to map over the value of an Output, use the map method instead."""
  )

  /** Recovers from a failed Output by applying the given function to the [[Throwable]].
    * @param f
    *   the function to apply to the [[Throwable]]
    * @return
    *   an Output with the recovered value
    */
  def recover[B >: A](f: Throwable => B): Output[B] =
    Output.ofData(dataResult.recover { t => Result.pure(OutputData(f(t))) })

  /** Recovers from a failed Output by applying the given effectful function to the [[Throwable]]. Can be used to recover with another
    * property of the same type.
    * @tparam F
    *   the effect type
    * @param f
    *   the effectful function to apply to the [[Throwable]]
    * @return
    *   an Output with the recovered value
    */
  def recoverWith[B >: A](f: Throwable => Output[B]): Output[B] =
    Output.ofData(
      dataResult.recover { t =>
        f(t).getData
      }
    )

  /** Recovers from a failed Output by applying the given effectful function to the [[Throwable]]. Can be used to recover with an effect of
    * a different type.
    * @tparam B
    *   the type of the recovered value
    * @tparam F
    *   the effect type
    * @param f
    *   the effectful function to apply to the [[Throwable]]
    * @return
    *   an Output with the recovered value
    */
  def recoverWith[B >: A, F[_]: Result.ToFuture](f: Throwable => F[B]): Output[B] =
    Output.ofData(
      dataResult.recover { t =>
        Result.eval(f(t)).map(OutputData(_))
      }
    )

  /** Applies the given function to the value of the Output and discards the result. Useful for logging or other side effects.
    * @param f
    *   the function to apply to the value
    * @return
    *   an Output with the original value
    */
  def tap(f: A => Output[Unit]): Output[A] =
    flatMap { a =>
      f(a).map(_ => a)
    }

  /** Applies the given function to the error of the Output and discards the result. Useful for logging or other side effects.
    * @param f
    *   the function to apply to the error
    * @return
    *   an Output with the original value
    */
  def tapError(f: Throwable => Output[Unit]): Output[A] =
    Output.ofData(
      dataResult.tapBoth {
        case Left(t) => f(t).getData.void
        case _       => Result.unit
      }
    )

  /** Applies the given functions to the value and error of the Output and discards the results. Useful for logging or other side effects.
    * Only one of the functions will be called, depending on whether the Output is a success or a failure.
    * @param f
    *   the function to apply to the value
    * @param onError
    *   the function to apply to the error
    * @return
    *   an Output with the original value
    */
  def tapBoth(f: A => Output[Unit], onError: Throwable => Output[Unit]): Output[A] =
    Output.ofData(
      dataResult.tapBoth {
        case Left(t)                                => onError(t).getData.void
        case Right(OutputData.Known(_, _, Some(a))) => f(a).getData.void
        case Right(_)                               => Result.unit
      }
    )

  /** Combines [[Output]] with the given [[Output]] using the given [[Zippable]], the default implementation results in a [[Tuple]].
    *
    * @tparam B
    *   the type of the other [[Output]]
    * @param that
    *   the other [[Output]] to combine with this one
    * @param z
    *   the [[Zippable]] instance that determines the behavior and the result type of the zip operation
    * @return
    *   an [[Output]] with the zipped value, by default a [[Tuple]]
    */
  def zip[B](that: => Output[B])(using z: Zippable[A, B]): Output[z.Out] =
    Output.ofData(dataResult.zip(that.getData).map((a, b) => a.zip(b)))

  /** Creates an un-nested [[Output]] from an [[Output]] of an [[Output]].
    * @tparam B
    *   the type of the inner [[Output]]
    * @param ev
    *   evidence that the type of the inner [[Output]] is the same as the type of the outer [[Output]]
    * @return
    *   an [[Output]] with the value of the inner [[Output]]
    */
  def flatten[B](using ev: A <:< Output[B]): Output[B] = flatMap(a => ev(a))

  /** Turns a secret into a plaintext! Only use if you know what you're doing.
    *
    * THIS IS UNSAFE AND SHOULD BE USED WITH EXTREME CAUTION.
    *
    * @return
    *   a plaintext [[Output]], the value is no longer a secret
    */
  def asPlaintext: Output[A] = withIsSecret(Result.pure(false))

  /** Turns a plaintext into a secret.
    *
    * This is useful when you have a value that is sensitive in nature, such as a password or cryptographic key, that you don't want to be
    * exposed.
    *
    * @return
    *   a secret [[Output]], the value is now a secret
    */
  def asSecret: Output[A] = withIsSecret(Result.pure(true))

  /** Discards the value of the Output and replaces it with Unit. Useful for ignoring the value of an Output but preserving the metadata
    * about dependencies, secrecy.
    * @return
    *   an Output with the value of Unit
    */
  def void: Output[Unit] = map(_ => ())

  private[besom] def flatMapOption[B, C: Typeable](using ev: A <:< Option[B])(f: B => Output[C] | Output[Option[C]]): Output[Option[C]] =
    flatMap { a =>
      ev(a) match
        case Some(b) =>
          f(b).map {
            case Some(c: C) => Some(c)
            case None       => None
            case x: C       => Some(x)
          }
        case None => Output(None)
    }

  private[internal] def getData: Result[OutputData[A]] = dataResult

  private[internal] def getValue: Result[Option[A]] = dataResult.map(_.getValue)

  private[internal] def getValueOrElse[B >: A](default: => B): Result[B] =
    dataResult.map(_.getValueOrElse(default))

  private[internal] def getValueOrFail(msg: String): Result[A] =
    dataResult.flatMap {
      case OutputData.Known(_, _, Some(value)) => Result.pure(value)
      case _                                   => Result.fail(Exception(msg))
    }

  private[internal] def withIsSecret(isSecretEff: Result[Boolean]): Output[A] =
    Output.ofData(
      for
        secret <- isSecretEff
        o      <- dataResult
      yield o.withIsSecret(secret)
    )
end Output

/** These factory methods should be the only way to create [[Output]] instances in user code.
  */
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
  def eval[F[_]: Result.ToFuture, A](value: F[A])(using Context): Output[A] = Output.eval(value)

  /** Creates an `Output` with the given `value`
    *
    * @see
    *   [[secret]] for creating an `Output` with a secret value
    */
  def apply[A](value: A)(using Context): Output[A] = Output(value)

  /** Creates an `Output` that is known to be a secret
    */
  def secret[A](value: A)(using Context): Output[A] = Output.secret(value)

  /** Creates an `Output` of a collection from a collection of Outputs.
    *
    * @see
    *   [[parSequence]] for parallel execution
    */
  def sequence[A, CC[X] <: Iterable[X], To](
    coll: CC[Output[A]]
  )(using BuildFrom[CC[Output[A]], A, To], Context): Output[To] = Output.sequence(coll)

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
  )(using BuildFrom[CC[Output[B]], B, To], Context): Output[To] = sequence(coll.map(f).asInstanceOf[CC[Output[B]]])

  /** Creates a failed [[Output]] containing given [[Throwable]]
    */
  def fail(t: Throwable)(using Context): Output[Nothing] = Output.fail(t)

  /** Creates an `Output` with the given `a` if the given `condition` is `true` or returns `None` if the condition is `false`
    */
  def when[A](condition: => Input[Boolean])(a: => Input.Optional[A])(using ctx: Context): Output[Option[A]] =
    Output.when(condition)(a)

  /** Creates an `Output` that contains Unit
    */
  def unit(using Context): Output[Unit] = Output(())

end OutputFactory

/** These factory methods provide additional methods on [[Output]] instances for convenience.
  */
trait OutputExtensionsFactory:
  implicit object OutputSequenceOps:
    extension [A, CC[X] <: Iterable[X]](coll: CC[Output[A]])
      /** Creates an `Output` of a collection from a collection of Outputs.
        *
        * @see
        *   [[parSequence]] for parallel execution
        */
      def sequence[To](using BuildFrom[CC[Output[A]], A, To], Context): Output[To] =
        Output.sequence(coll)

      /** Creates an `Output` of a collection from a collection of Outputs in parallel.
        * @see
        *   [[sequence]] for sequential execution
        */
      def parSequence[To](using BuildFrom[CC[Output[A]], A, To], Context): Output[To] =
        Output.parSequence(coll)

  implicit object OutputTraverseOps:
    extension [A, CC[X] <: Iterable[X]](coll: CC[A])
      /** Applies an Output-returning function to each element in the collection, and then combines the results into an Output.
        *
        * @param f
        *   the Output-returning function to apply to each element in the collection
        * @see
        *   [[parTraverse]] for parallel execution
        */
      def traverse[B, To](f: A => Output[B])(using BuildFrom[CC[Output[B]], B, To], Context): Output[To] =
        Output.sequence(coll.map(f).asInstanceOf[CC[Output[B]]])

      /** Applies an Output-returning function to each element in the collection, in parallel, and then combines the results into an Output.
        *
        * @param f
        *   the Output-returning function to apply to each element in the collection
        * @see
        *   [[traverse]] for sequential execution
        */
      def parTraverse[B, To](f: A => Output[B])(using BuildFrom[CC[Output[B]], B, To], Context): Output[To] =
        coll.map(f).asInstanceOf[CC[Output[B]]].parSequence

  implicit final class OutputOptionOps[A](output: Output[Option[A]]):
    /** Behaves like [[Option.getOrElse]] on the underlying [[Option]]
      * @param default
      *   the default value to return if the underlying [[Option]] is [[None]]
      * @return
      *   an [[Output]] with the value of the underlying [[Some]] or the `default` value if [[None]]
      */
    def getOrElse[B >: A](default: => B | Output[B])(using ctx: Context): Output[B] =
      output.flatMap { opt =>
        opt match
          case Some(a) => Output(a)
          case None =>
            default match
              case b: Output[B @unchecked] => b
              case b: B @unchecked         => Output(b)
      }

    /** Get the value of the underlying [[Option]] or fail the outer [[Output]] with the given [[Throwable]]
      *
      * @param throwable
      *   the throwable to fail with if the underlying [[Option]] is [[None]]
      * @return
      *   an [[Output]] with the value of the underlying [[Some]] or a failed [[Output]] with the given `throwable` if [[None]]
      * @see
      *   [[OutputFactory.fail]] for creating a failed [[Output]] with a [[Throwable]]
      */
    def getOrFail(throwable: => Throwable)(using ctx: Context): Output[A] =
      output.flatMap {
        case Some(a) => Output(a)
        case None    => Output.fail(throwable)
      }

    /** Behaves like [[Option.orElse]] on the underlying [[Option]]
      * @param alternative
      *   the alternative [[Option]] to return if the underlying [[Option]] is [[None]]
      * @return
      *   an [[Output]] with the underlying [[Some]] or the `alternative` value if [[None]]
      */
    def orElse[B >: A](alternative: => Option[B] | Output[Option[B]])(using ctx: Context): Output[Option[B]] =
      output.flatMap {
        case some @ Some(_) => Output(some)
        case None =>
          alternative match
            case b: Output[Option[B]] => b
            case b: Option[B]         => Output(b)
      }

    /** Calls [[Option.map]] on the underlying [[Option]] with the given function
      * @return
      *   an [[Output]] of the mapped [[Option]]
      */
    def mapInner[B](f: A => B | Output[B])(using ctx: Context): Output[Option[B]] =
      output.flatMap {
        case Some(a) =>
          f(a) match
            case b: Output[B @unchecked] => b.map(Some(_))
            case b: B @unchecked         => Output(Some(b))
        case None => Output(None)
      }

    /** Calls [[Option.flatMap]] on the underlying [[Option]] with the given function
      * @return
      *   an [[Output]] of the flat-mapped [[Option]]
      */
    def flatMapInner[B](f: A => Option[B] | Output[Option[B]])(using ctx: Context): Output[Option[B]] =
      output.flatMap {
        case Some(a) =>
          f(a) match
            case b: Output[Option[B]] => b
            case b: Option[B]         => Output(b)
        case None => Output(None)
      }
  end OutputOptionOps

  implicit final class OutputListOps[A](output: Output[List[A]]):
    /** Calls [[List.headOption]] on the underlying [[List]]
      * @return
      *   an [[Output]] of [[Option]] of the head of the list
      */
    def headOption: Output[Option[A]] = output.map(_.headOption)

    /** Calls [[List.lastOption]] on the underlying [[List]]
      * @return
      *   an [[Output]] of [[Option]] of the last element of the list
      */
    def lastOption: Output[Option[A]] = output.map(_.lastOption)

    /** Calls [[List.tail]] on the underlying [[List]], but does not fail on `Nil`
      * @return
      *   an [[Output]] of the `tail` of the [[List]] or an empty list if the list is empty
      */
    def tailOrEmpty: Output[List[A]] = output.map {
      case Nil  => Nil
      case list => list.tail
    }

    /** Calls [[List.init]] on the underlying [[List]], but does not fail on `Nil`
      * @return
      *   an [[Output]] of the `init` of the [[List]] or an empty list if the list is empty
      */
    def initOrEmpty: Output[List[A]] = output.map {
      case Nil  => Nil
      case list => list.init
    }

    /** Calls [[List.map]] on the underlying [[List]] with the given function
      * @return
      *   an [[Output]] of the mapped [[List]]
      */
    def mapInner[B](f: A => B | Output[B])(using Context): Output[List[B]] = output.flatMap {
      case Nil => Output(List.empty[B])
      case h :: t =>
        f(h) match
          case b: Output[B @unchecked] =>
            Output.sequence(b :: t.map(f.asInstanceOf[A => Output[B]](_)))
          case b: B @unchecked =>
            Output(b :: t.map(f.asInstanceOf[A => B](_)))
    }

    /** Calls [[List.flatMap]] on the underlying [[List]] with the given function
      * @return
      *   an [[Output]] of the flat-mapped [[List]]
      */
    def flatMapInner[B](f: A => List[B] | Output[List[B]])(using Context): Output[List[B]] = output.flatMap {
      case Nil => Output(List.empty[B])
      case h :: t =>
        f(h) match
          case bs: Output[List[B]] =>
            bs.flatMap { (bb: List[B]) =>
              val tailOfOutputs = t.map(f.asInstanceOf[A => Output[List[B]]](_))
              val tailOutput    = Output.sequence(tailOfOutputs).map(_.flatten)
              tailOutput.map(bb ::: _)
            }
          case bs: List[B] =>
            Output(bs ::: t.flatMap(f.asInstanceOf[A => List[B]](_)))
    }
  end OutputListOps

  implicit final class OutputOptionListOps[A](output: Output[Option[List[A]]]):

    /** Calls [[List.headOption]] on the underlying optional [[List]]
      * @return
      *   an [[Output]] of [[Option]] of the head of the [[List]]
      */
    def headOption: Output[Option[A]] = output.map(_.flatMap(_.headOption))

    /** Calls [[List.lastOption]] on the underlying optional [[List]]
      * @return
      *   an [[Output]] of [[Option]] of the last element of the [[List]]
      */
    def lastOption: Output[Option[A]] = output.map(_.flatMap(_.lastOption))

    /** Calls [[List.tail]] on the underlying optional [[List]], but does not fail on `Nil`
      * @return
      *   an [[Output]] of the `tail` of the [[List]] or an empty list if the optional [[List]] is [[None]]
      */
    def tailOrEmpty: Output[List[A]] = output.map {
      case Some(list) => if list.isEmpty then List.empty else list.tail
      case None       => List.empty
    }

    /** Calls [[List.init]] on the underlying optional [[List]], but does not fail on `Nil`
      * @return
      *   an [[Output]] of the `init` of the [[List]] or an empty list if the optional [[List]] is [[None]]
      */
    def initOrEmpty: Output[List[A]] = output.map {
      case Some(list) => if list.isEmpty then List.empty else list.init
      case None       => List.empty
    }

    /** Calls [[List.map]] on the underlying optional [[List]] with the given function
      * @param f
      *   the function to apply to the value
      * @return
      *   an [[Output]] of the mapped [[List]] or an empty list if the optional [[List]] is [[None]]
      */
    def mapInner[B](f: A => B | Output[B])(using Context): Output[List[B]] = output
      .map {
        case Some(list) => list
        case None       => List.empty
      }
      .mapInner(f)

    /** Calls [[List.flatMap]] on the underlying optional [[List]] with the given function
      *
      * @param f
      *   the function to apply to the value
      * @return
      *   an [[Output]] of the flat-mapped [[List]] or an empty list if the optional [[List]] is [[None]]
      */
    def flatMapInner[B](f: A => List[B] | Output[List[B]])(using Context): Output[List[B]] = output
      .map {
        case Some(list) => list
        case None       => List.empty
      }
      .flatMapInner(f)

  end OutputOptionListOps

  implicit class OutputOfTupleOps[A <: NonEmptyTuple](private val output: Output[A]):
    /** Unzips the [[Output]] of a non-empty tuple into a tuple of [[Output]]s of the same arity. This operation is equivalent to:
      *
      * {{{o: Output[(A, B, C)] => (o.map(_._1), o.map(_._2), o.map(_._3))}}}
      *
      * and therefore will yield three descendants of the original [[Output]]. Evaluation of the descendants will cause the original
      * [[Output]] to be evaluated as well and may therefore lead to unexpected side effects. This is usually not a problem with properties
      * of resources but can be surprising if other effects are subsumed into the original [[Output]]. If this behavior is not desired,
      * consider using [[unzipOutput]] instead.
      *
      * @tparam Output
      *   the type of the [[Output]]s
      * @return
      *   a tuple of [[Output]]s
      */
    inline def unzip: Tuple.Map[A, Output] = OutputUnzip.unzip(output)
  end OutputOfTupleOps

end OutputExtensionsFactory

object Output:
  // should be NonEmptyString
  def traverseMap[A](using ctx: Context)(map: Map[String, Output[A]]): Output[Map[String, A]] =
    sequence(map.map((key, value) => value.map(result => (key, result))).toVector).map(_.toMap)

  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[Output[A]]
  )(using bf: BuildFrom[CC[Output[A]], A, To], ctx: Context): Output[To] =
    Output {
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
  )(using bf: BuildFrom[CC[Output[A]], A, To], ctx: Context): Output[To] =
    Output {
      Result
        .defer {
          Result.parSequence(coll.iterator.map(_.dataResult).toVector)
        }
        .flatten
        .map { vecOfOutputData =>
          vecOfOutputData.map(Output.ofData(_))
        }
    }.flatMap { vecOfOutput =>
      Output.sequence(vecOfOutput).map { vecOfA =>
        bf.fromSpecific(coll)(vecOfA)
      }
    }

  def empty(isSecret: Boolean = false)(using ctx: Context): Output[Nothing] =
    new Output(ctx.registerTask(Result.pure(OutputData.empty[Nothing](isSecret = isSecret))))

  def eval[F[_]: Result.ToFuture, A](value: F[A])(using
    ctx: Context
  ): Output[A] =
    new Output[A](ctx.registerTask(Result.eval(value)).map(OutputData(_)))

  def fail(t: Throwable)(using ctx: Context): Output[Nothing] =
    new Output[Nothing](ctx.registerTask(Result.fail(t)))

  def apply[A](value: => Result[A])(using
    ctx: Context
  ): Output[A] =
    new Output[A](ctx.registerTask(OutputData.traverseResult(value)))

  // TODO could this be pure without implicit Context? it's not async in any way so? only test when all tests are written
  def apply[A](value: A)(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(OutputData(value))))

  def ofData[A](value: => Result[OutputData[A]])(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask(value))

  // TODO could this be pure without implicit Context? it's not async in any way so? only test when all tests are written
  def ofData[A](data: OutputData[A])(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(data)))

  def secret[A](value: A)(using ctx: Context): Output[A] =
    new Output[A](ctx.registerTask(Result.pure(OutputData(value, Set.empty, isSecret = true))))

  def when[A](cond: => Input[Boolean])(
    a: => Input.Optional[A]
  )(using ctx: Context): Output[Option[A]] =
    cond.asOutput().flatMap { c =>
      if c then a.asOptionOutput(isSecret = false) else Output(None)
    }
  end when
end Output
