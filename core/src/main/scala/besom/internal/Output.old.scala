package besom.internal.old

import besom.internal.*

import scala.collection.BuildFrom

/** OutputX is a wrapper for a monadic effect used to model async execution that allows Pulumi to track information about dependencies
  * between resources and properties of data (whether it's known or a secret for instance).
  *
  * Invariant: `dataResult` has to be registered in [[TaskTracker]] by the time it reaches the constructor here.
  * @param dataResult
  *   Effect of type [[Result]][A]
  * @param ctx
  *   the Besom [[Context]]
  */
class OutputX[+A] private[internal] (using private[besom] val ctx: Context)(
  private val dataResult: Result[OutputData[A]]
):
  /** Maps the value of the OutputX using the given function.
    * @param f
    *   the function to apply to the value
    * @return
    *   an OutputX with the mapped value
    */
  def map[B](f: A => B): OutputX[B] = OutputX.ofData(dataResult.map(_.map(f)))

  /** Flat-maps the value of the OutputX using the given function.
    * @tparam B
    *   the type of the value
    * @param f
    *   the function to apply to the value
    * @return
    *   an OutputX with the flat-mapped value
    * @see
    *   `flatMap(A => F[B])` for flat-mapping with an effectful function
    */
  def flatMap[B](f: A => OutputX[B]): OutputX[B] =
    OutputX.ofData(
      for
        outputData: OutputData[A]         <- dataResult
        nested: OutputData[OutputData[B]] <- outputData.traverseResult(a => f(a).getData)
      yield nested.flatten
    )

  /** Flat-maps the value of the OutputX using the given effectful function.
    * @tparam F
    *   the effect type
    * @tparam B
    *   the type of the value
    * @param f
    *   the effectful function to apply to the value
    * @return
    *   an OutputX with the flat-mapped value
    * @see
    *   `flatMap(A => OutputX[B])` for flat-mapping with OutputX-returning function
    */
  def flatMap[F[_]: Result.ToFuture, B](f: A => F[B]): OutputX[B] =
    OutputX.ofData(
      for
        outputData: OutputData[A]         <- dataResult
        nested: OutputData[OutputData[B]] <- outputData.traverseResult(a => Result.eval(f(a)).map(OutputData(_)))
      yield nested.flatten
    )

  /** Mock variant of flatMap that will fail at compile time if used with a function that returns a value instead of an OutputX.
    *
    * @param f
    *   function to apply to the value of the OutputX
    */
  inline def flatMap[B](f: A => B): Nothing = scala.compiletime.error(
    """OutputX#flatMap can only be used with functions that return an OutputX or a structure like scala.concurrent.Future, cats.effect.IO or zio.Task.
If you want to map over the value of an OutputX, use the map method instead."""
  )

  /** Recovers from a failed OutputX by applying the given function to the [[Throwable]].
    * @param f
    *   the function to apply to the [[Throwable]]
    * @return
    *   an OutputX with the recovered value
    */
  def recover[B >: A](f: Throwable => B): OutputX[B] =
    OutputX.ofData(dataResult.recover { t => Result.pure(OutputData(f(t))) })

  /** Recovers from a failed OutputX by applying the given effectful function to the [[Throwable]]. Can be used to recover with another
    * property of the same type.
    * @tparam F
    *   the effect type
    * @param f
    *   the effectful function to apply to the [[Throwable]]
    * @return
    *   an OutputX with the recovered value
    */
  def recoverWith[B >: A](f: Throwable => OutputX[B]): OutputX[B] =
    OutputX.ofData(
      dataResult.recover { t =>
        f(t).getData
      }
    )

  /** Recovers from a failed OutputX by applying the given effectful function to the [[Throwable]]. Can be used to recover with an effect of
    * a different type.
    * @tparam B
    *   the type of the recovered value
    * @tparam F
    *   the effect type
    * @param f
    *   the effectful function to apply to the [[Throwable]]
    * @return
    *   an OutputX with the recovered value
    */
  def recoverWith[B >: A, F[_]: Result.ToFuture](f: Throwable => F[B]): OutputX[B] =
    OutputX.ofData(
      dataResult.recover { t =>
        Result.eval(f(t)).map(OutputData(_))
      }
    )

  /** Applies the given function to the value of the OutputX and discards the result. Useful for logging or other side effects.
    * @param f
    *   the function to apply to the value
    * @return
    *   an OutputX with the original value
    */
  def tap(f: A => OutputX[Unit]): OutputX[A] =
    flatMap { a =>
      f(a).map(_ => a)
    }

  /** Applies the given function to the error of the OutputX and discards the result. Useful for logging or other side effects.
    * @param f
    *   the function to apply to the error
    * @return
    *   an OutputX with the original value
    */
  def tapError(f: Throwable => OutputX[Unit]): OutputX[A] =
    OutputX.ofData(
      dataResult.tapBoth {
        case Left(t) => f(t).getData.void
        case _       => Result.unit
      }
    )

  /** Applies the given functions to the value and error of the OutputX and discards the results. Useful for logging or other side effects.
    * Only one of the functions will be called, depending on whether the OutputX is a success or a failure.
    * @param f
    *   the function to apply to the value
    * @param onError
    *   the function to apply to the error
    * @return
    *   an OutputX with the original value
    */
  def tapBoth(f: A => OutputX[Unit], onError: Throwable => OutputX[Unit]): OutputX[A] =
    OutputX.ofData(
      dataResult.tapBoth {
        case Left(t)                                => onError(t).getData.void
        case Right(OutputData.Known(_, _, Some(a))) => f(a).getData.void
        case Right(_)                               => Result.unit
      }
    )

  /** Combines [[OutputX]] with the given [[OutputX]] using the given [[Zippable]], the default implementation results in a [[Tuple]].
    *
    * @tparam B
    *   the type of the other [[OutputX]]
    * @param that
    *   the other [[OutputX]] to combine with this one
    * @param z
    *   the [[Zippable]] instance that determines the behavior and the result type of the zip operation
    * @return
    *   an [[OutputX]] with the zipped value, by default a [[Tuple]]
    */
  def zip[B](that: => OutputX[B])(using z: Zippable[A, B]): OutputX[z.Out] =
    OutputX.ofData(dataResult.zip(that.getData).map((a, b) => a.zip(b)))

  /** Creates an un-nested [[OutputX]] from an [[OutputX]] of an [[OutputX]].
    * @tparam B
    *   the type of the inner [[OutputX]]
    * @param ev
    *   evidence that the type of the inner [[OutputX]] is the same as the type of the outer [[OutputX]]
    * @return
    *   an [[OutputX]] with the value of the inner [[OutputX]]
    */
  def flatten[B](using ev: A <:< OutputX[B]): OutputX[B] = flatMap(a => ev(a))

  /** Turns a secret into a plaintext! Only use if you know what you're doing.
    *
    * THIS IS UNSAFE AND SHOULD BE USED WITH EXTREME CAUTION.
    *
    * @return
    *   a plaintext [[OutputX]], the value is no longer a secret
    */
  def asPlaintext: OutputX[A] = withIsSecret(Result.pure(false))

  /** Turns a plaintext into a secret.
    *
    * This is useful when you have a value that is sensitive in nature, such as a password or cryptographic key, that you don't want to be
    * exposed.
    *
    * @return
    *   a secret [[OutputX]], the value is now a secret
    */
  def asSecret: OutputX[A] = withIsSecret(Result.pure(true))

  /** Discards the value of the OutputX and replaces it with Unit. Useful for ignoring the value of an OutputX but preserving the metadata
    * about dependencies, secrecy.
    * @return
    *   an OutputX with the value of Unit
    */
  def void: OutputX[Unit] = map(_ => ())

  private[besom] def flatMapOption[B, C](using ev: A <:< Option[B])(f: B => OutputX[C] | OutputX[Option[C]]): OutputX[Option[C]] =
    flatMap { a =>
      ev(a) match
        case Some(b) =>
          f(b).map {
            case Some(c) => Some(c.asInstanceOf[C])
            case None    => None
            case c       => Some(c.asInstanceOf[C])
          }
        case None => OutputX(None)
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

  private[internal] def withIsSecret(isSecretEff: Result[Boolean]): OutputX[A] =
    OutputX.ofData(
      for
        secret <- isSecretEff
        o      <- dataResult
      yield o.withIsSecret(secret)
    )
end OutputX

/** These factory methods should be the only way to create [[OutputX]] instances in user code.
  */
trait OutputXFactory:
  /** Creates an `OutputX` that evaluates given effectful computation.
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
    *   the value to wrap in an `OutputX`
    */
  def eval[F[_]: Result.ToFuture, A](value: F[A])(using Context): OutputX[A] = OutputX.eval(value)

  /** Creates an `OutputX` with the given `value`
    *
    * @see
    *   [[secret]] for creating an `OutputX` with a secret value
    */
  def apply[A](value: A)(using Context): OutputX[A] = OutputX(value)

  /** Creates an `OutputX` that is known to be a secret
    */
  def secret[A](value: A)(using Context): OutputX[A] = OutputX.secret(value)

  /** Creates an `OutputX` of a collection from a collection of OutputXs.
    *
    * @see
    *   [[parSequence]] for parallel execution
    */
  def sequence[A, CC[X] <: Iterable[X], To](
    coll: CC[OutputX[A]]
  )(using BuildFrom[CC[OutputX[A]], A, To], Context): OutputX[To] = OutputX.sequence(coll)

  /** Creates an `OutputX` of a collection from a collection of values mapped with the function `f`
    *
    * @param coll
    *   the collection to map with `f`
    * @param f
    *   the OutputX-returning function to apply to each element in the collection
    */
  def traverse[A, CC[X] <: Iterable[X], B, To](
    coll: CC[A]
  )(
    f: A => OutputX[B]
  )(using BuildFrom[CC[OutputX[B]], B, To], Context): OutputX[To] = sequence(coll.map(f).asInstanceOf[CC[OutputX[B]]])

  /** Creates a failed [[OutputX]] containing given [[Throwable]]
    */
  def fail(t: Throwable)(using Context): OutputX[Nothing] = OutputX.fail(t)

  /** Creates an `OutputX` with the given `a` if the given `condition` is `true` or returns `None` if the condition is `false`
    */
  // def when[A](condition: => Input[Boolean])(a: => Input.Optional[A])(using ctx: Context): OutputX[Option[A]] =
  //   OutputX.when(condition)(a)

  /** Creates an `OutputX` that contains Unit
    */
  def unit(using Context): OutputX[Unit] = OutputX(())

end OutputXFactory

/** These factory methods provide additional methods on [[OutputX]] instances for convenience.
  */
trait OutputXExtensionsFactory:
  implicit object OutputXSequenceOps:
    extension [A, CC[X] <: Iterable[X]](coll: CC[OutputX[A]])
      /** Creates an `OutputX` of a collection from a collection of OutputXs.
        *
        * @see
        *   [[parSequence]] for parallel execution
        */
      def sequence[To](using BuildFrom[CC[OutputX[A]], A, To], Context): OutputX[To] =
        OutputX.sequence(coll)

      /** Creates an `OutputX` of a collection from a collection of OutputXs in parallel.
        * @see
        *   [[sequence]] for sequential execution
        */
      def parSequence[To](using BuildFrom[CC[OutputX[A]], A, To], Context): OutputX[To] =
        OutputX.parSequence(coll)

  implicit object OutputXTraverseOps:
    extension [A, CC[X] <: Iterable[X]](coll: CC[A])
      /** Applies an OutputX-returning function to each element in the collection, and then combines the results into an OutputX.
        *
        * @param f
        *   the OutputX-returning function to apply to each element in the collection
        * @see
        *   [[parTraverse]] for parallel execution
        */
      def traverse[B, To](f: A => OutputX[B])(using BuildFrom[CC[OutputX[B]], B, To], Context): OutputX[To] =
        OutputX.sequence(coll.map(f).asInstanceOf[CC[OutputX[B]]])

      /** Applies an OutputX-returning function to each element in the collection, in parallel, and then combines the results into an
        * OutputX.
        *
        * @param f
        *   the OutputX-returning function to apply to each element in the collection
        * @see
        *   [[traverse]] for sequential execution
        */
      def parTraverse[B, To](f: A => OutputX[B])(using BuildFrom[CC[OutputX[B]], B, To], Context): OutputX[To] =
        coll.map(f).asInstanceOf[CC[OutputX[B]]].parSequence

  implicit final class OutputXOptionOps[A](output: OutputX[Option[A]]):
    /** Behaves like [[Option.getOrElse]] on the underlying [[Option]]
      * @param default
      *   the default value to return if the underlying [[Option]] is [[None]]
      * @return
      *   an [[OutputX]] with the value of the underlying [[Some]] or the `default` value if [[None]]
      */
    def getOrElse[B >: A](default: => B | OutputX[B])(using ctx: Context): OutputX[B] =
      output.flatMap { opt =>
        opt match
          case Some(a) => OutputX(a)
          case None =>
            default match
              case b: OutputX[B @unchecked] => b
              case b: B @unchecked          => OutputX(b)
      }

    /** Get the value of the underlying [[Option]] or fail the outer [[OutputX]] with the given [[Throwable]]
      *
      * @param throwable
      *   the throwable to fail with if the underlying [[Option]] is [[None]]
      * @return
      *   an [[OutputX]] with the value of the underlying [[Some]] or a failed [[OutputX]] with the given `throwable` if [[None]]
      * @see
      *   [[OutputXFactory.fail]] for creating a failed [[OutputX]] with a [[Throwable]]
      */
    def getOrFail(throwable: => Throwable)(using ctx: Context): OutputX[A] =
      output.flatMap {
        case Some(a) => OutputX(a)
        case None    => OutputX.fail(throwable)
      }

    /** Behaves like [[Option.orElse]] on the underlying [[Option]]
      * @param alternative
      *   the alternative [[Option]] to return if the underlying [[Option]] is [[None]]
      * @return
      *   an [[OutputX]] with the underlying [[Some]] or the `alternative` value if [[None]]
      */
    def orElse[B >: A](alternative: => Option[B] | OutputX[Option[B]])(using ctx: Context): OutputX[Option[B]] =
      output.flatMap {
        case some @ Some(_) => OutputX(some)
        case None =>
          alternative match
            case b: OutputX[Option[B]] => b
            case b: Option[B]          => OutputX(b)
      }

    /** Calls [[Option.map]] on the underlying [[Option]] with the given function
      * @return
      *   an [[OutputX]] of the mapped [[Option]]
      */
    def mapInner[B](f: A => B | OutputX[B])(using ctx: Context): OutputX[Option[B]] =
      output.flatMap {
        case Some(a) =>
          f(a) match
            case b: OutputX[B @unchecked] => b.map(Some(_))
            case b: B @unchecked          => OutputX(Some(b))
        case None => OutputX(None)
      }

    /** Calls [[Option.flatMap]] on the underlying [[Option]] with the given function
      * @return
      *   an [[OutputX]] of the flat-mapped [[Option]]
      */
    def flatMapInner[B](f: A => Option[B] | OutputX[Option[B]])(using ctx: Context): OutputX[Option[B]] =
      output.flatMap {
        case Some(a) =>
          f(a) match
            case b: OutputX[Option[B]] => b
            case b: Option[B]          => OutputX(b)
        case None => OutputX(None)
      }
  end OutputXOptionOps

  implicit final class OutputXListOps[A](output: OutputX[List[A]]):
    /** Calls [[List.headOption]] on the underlying [[List]]
      * @return
      *   an [[OutputX]] of [[Option]] of the head of the list
      */
    def headOption: OutputX[Option[A]] = output.map(_.headOption)

    /** Calls [[List.lastOption]] on the underlying [[List]]
      * @return
      *   an [[OutputX]] of [[Option]] of the last element of the list
      */
    def lastOption: OutputX[Option[A]] = output.map(_.lastOption)

    /** Calls [[List.tail]] on the underlying [[List]], but does not fail on `Nil`
      * @return
      *   an [[OutputX]] of the `tail` of the [[List]] or an empty list if the list is empty
      */
    def tailOrEmpty: OutputX[List[A]] = output.map {
      case Nil  => Nil
      case list => list.tail
    }

    /** Calls [[List.init]] on the underlying [[List]], but does not fail on `Nil`
      * @return
      *   an [[OutputX]] of the `init` of the [[List]] or an empty list if the list is empty
      */
    def initOrEmpty: OutputX[List[A]] = output.map {
      case Nil  => Nil
      case list => list.init
    }

    /** Calls [[List.map]] on the underlying [[List]] with the given function
      * @return
      *   an [[OutputX]] of the mapped [[List]]
      */
    def mapInner[B](f: A => B | OutputX[B])(using Context): OutputX[List[B]] = output.flatMap {
      case Nil => OutputX(List.empty[B])
      case h :: t =>
        f(h) match
          case b: OutputX[B @unchecked] =>
            OutputX.sequence(b :: t.map(f.asInstanceOf[A => OutputX[B]](_)))
          case b: B @unchecked =>
            OutputX(b :: t.map(f.asInstanceOf[A => B](_)))
    }

    /** Calls [[List.flatMap]] on the underlying [[List]] with the given function
      * @return
      *   an [[OutputX]] of the flat-mapped [[List]]
      */
    def flatMapInner[B](f: A => List[B] | OutputX[List[B]])(using Context): OutputX[List[B]] = output.flatMap {
      case Nil => OutputX(List.empty[B])
      case h :: t =>
        f(h) match
          case bs: OutputX[List[B]] =>
            bs.flatMap { (bb: List[B]) =>
              val tailOfOutputXs = t.map(f.asInstanceOf[A => OutputX[List[B]]](_))
              val tailOutputX    = OutputX.sequence(tailOfOutputXs).map(_.flatten)
              tailOutputX.map(bb ::: _)
            }
          case bs: List[B] =>
            OutputX(bs ::: t.flatMap(f.asInstanceOf[A => List[B]](_)))
    }
  end OutputXListOps

  implicit final class OutputXOptionListOps[A](output: OutputX[Option[List[A]]]):

    /** Calls [[List.headOption]] on the underlying optional [[List]]
      * @return
      *   an [[OutputX]] of [[Option]] of the head of the [[List]]
      */
    def headOption: OutputX[Option[A]] = output.map(_.flatMap(_.headOption))

    /** Calls [[List.lastOption]] on the underlying optional [[List]]
      * @return
      *   an [[OutputX]] of [[Option]] of the last element of the [[List]]
      */
    def lastOption: OutputX[Option[A]] = output.map(_.flatMap(_.lastOption))

    /** Calls [[List.tail]] on the underlying optional [[List]], but does not fail on `Nil`
      * @return
      *   an [[OutputX]] of the `tail` of the [[List]] or an empty list if the optional [[List]] is [[None]]
      */
    def tailOrEmpty: OutputX[List[A]] = output.map {
      case Some(list) => if list.isEmpty then List.empty else list.tail
      case None       => List.empty
    }

    /** Calls [[List.init]] on the underlying optional [[List]], but does not fail on `Nil`
      * @return
      *   an [[OutputX]] of the `init` of the [[List]] or an empty list if the optional [[List]] is [[None]]
      */
    def initOrEmpty: OutputX[List[A]] = output.map {
      case Some(list) => if list.isEmpty then List.empty else list.init
      case None       => List.empty
    }

    /** Calls [[List.map]] on the underlying optional [[List]] with the given function
      * @param f
      *   the function to apply to the value
      * @return
      *   an [[OutputX]] of the mapped [[List]] or an empty list if the optional [[List]] is [[None]]
      */
    def mapInner[B](f: A => B | OutputX[B])(using Context): OutputX[List[B]] = output
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
      *   an [[OutputX]] of the flat-mapped [[List]] or an empty list if the optional [[List]] is [[None]]
      */
    def flatMapInner[B](f: A => List[B] | OutputX[List[B]])(using Context): OutputX[List[B]] = output
      .map {
        case Some(list) => list
        case None       => List.empty
      }
      .flatMapInner(f)

  end OutputXOptionListOps

  // implicit class OutputXOfTupleOps[A <: NonEmptyTuple](private val output: OutputX[A]):

  /** Unzips the [[OutputX]] of a non-empty tuple into a tuple of [[OutputX]]s of the same arity. This operation is equivalent to:
    *
    * {{{o: OutputX[(A, B, C)] => (o.map(_._1), o.map(_._2), o.map(_._3))}}}
    *
    * and therefore will yield three descendants of the original [[OutputX]]. Evaluation of the descendants will cause the original
    * [[OutputX]] to be evaluated as well and may therefore lead to unexpected side effects. This is usually not a problem with properties
    * of resources but can be surprising if other effects are subsumed into the original [[OutputX]]. If this behavior is not desired,
    * consider using [[unzipOutputX]] instead.
    *
    * @tparam OutputX
    *   the type of the [[OutputX]]s
    * @return
    *   a tuple of [[OutputX]]s
    */
  // inline def unzip: Tuple.Map[A, OutputX] = OutputXUnzip.unzip(output)
  // end OutputXOfTupleOps

end OutputXExtensionsFactory

object OutputX:
  // should be NonEmptyString
  def traverseMap[A](using ctx: Context)(map: Map[String, OutputX[A]]): OutputX[Map[String, A]] =
    sequence(map.map((key, value) => value.map(result => (key, result))).toVector).map(_.toMap)

  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[OutputX[A]]
  )(using bf: BuildFrom[CC[OutputX[A]], A, To], ctx: Context): OutputX[To] =
    OutputX {
      Result.defer {
        coll.iterator
          .foldLeft(OutputX(bf.newBuilder(coll))) { (acc, curr) =>
            acc.zip(curr).map { case (b, r) =>
              b += r
            }
          }
          .map(_.result())
      }
    }.flatten

  def parSequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[OutputX[A]]
  )(using bf: BuildFrom[CC[OutputX[A]], A, To], ctx: Context): OutputX[To] =
    OutputX {
      Result
        .defer {
          Result.parSequence(coll.iterator.map(_.dataResult).toVector)
        }
        .flatten
        .map { vecOfOutputData =>
          vecOfOutputData.map(OutputX.ofData(_))
        }
    }.flatMap { vecOfOutputX =>
      OutputX.sequence(vecOfOutputX).map { vecOfA =>
        bf.fromSpecific(coll)(vecOfA)
      }
    }

  def empty(isSecret: Boolean = false)(using ctx: Context): OutputX[Nothing] =
    new OutputX(ctx.registerTask(Result.pure(OutputData.empty[Nothing](isSecret = isSecret))))

  def eval[F[_]: Result.ToFuture, A](value: F[A])(using
    ctx: Context
  ): OutputX[A] =
    new OutputX[A](ctx.registerTask(Result.eval(value)).map(OutputData(_)))

  def fail(t: Throwable)(using ctx: Context): OutputX[Nothing] =
    new OutputX[Nothing](ctx.registerTask(Result.fail(t)))

  def apply[A](value: => Result[A])(using
    ctx: Context
  ): OutputX[A] =
    new OutputX[A](ctx.registerTask(OutputData.traverseResult(value)))

  // TODO could this be pure without implicit Context? it's not async in any way so? only test when all tests are written
  def apply[A](value: A)(using ctx: Context): OutputX[A] =
    new OutputX[A](ctx.registerTask(Result.pure(OutputData(value))))

  def ofData[A](value: => Result[OutputData[A]])(using ctx: Context): OutputX[A] =
    new OutputX[A](ctx.registerTask(value))

  // TODO could this be pure without implicit Context? it's not async in any way so? only test when all tests are written
  def ofData[A](data: OutputData[A])(using ctx: Context): OutputX[A] =
    new OutputX[A](ctx.registerTask(Result.pure(data)))

  def secret[A](value: A)(using ctx: Context): OutputX[A] =
    new OutputX[A](ctx.registerTask(Result.pure(OutputData(value, Set.empty, isSecret = true))))

  // def when[A](cond: => Input[Boolean])(
  //   a: => Input.Optional[A]
  // )(using ctx: Context): OutputX[Option[A]] =
  //   cond.asOutputX().flatMap { c =>
  //     if c then a.asOptionOutputX(isSecret = false) else OutputX(None)
  //   }
  // end when
end OutputX
