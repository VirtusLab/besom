package besom.internal

import scala.collection.BuildFrom

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
        case None => Output.pure(None)
    }

  private[internal] def getData(using ctx: Context): Result[OutputData[A]] =
    Context().registerTask(inner(ctx))

  private[internal] def getValue(using ctx: Context): Result[Option[A]] =
    Context().registerTask(inner(ctx)).map(_.getValue)

  private[internal] def getValueOrElse[B >: A](default: => B)(using ctx: Context): Result[B] =
    Context().registerTask(inner(ctx)).map(_.getValueOrElse(default))

  private[internal] def getValueOrFail(msg: String)(using ctx: Context): Result[A] =
    Context().registerTask(inner(ctx)).flatMap {
      case OutputData.Known(_, _, Some(value)) => Result.pure(value)
      case _                                   => Result.fail(Exception(msg))
    }

  private[internal] def withContext(ctx: Context): Output[A] = Output.ofData(inner(ctx))

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
  def when[A](condition: => Input[Boolean])(a: => Input.Optional[A]): Output[Option[A]] =
    Output.when(condition)(a)

  /** Creates an `Output` that contains Unit
    */
  def unit: Output[Unit] = Output.pure(())

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
          .foldLeft(Output.pure(bf.newBuilder(coll))) { (acc, curr) =>
            acc.zip(curr).map { case (b, r) =>
              b += r
            }
          }
          .map(_.result())
      }
    }.flatten

  def parSequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[Output[A]]
  )(using bf: BuildFrom[CC[Output[A]], A, To]): Output[To] =
    new Output(ctx => {
      Result
        .defer {
          Result.parSequence(coll.iterator.map(_.inner(ctx)).toVector)
        }
        .flatten
        .map { vecOfOutputData =>
          OutputData.sequence(vecOfOutputData).map(v => bf.fromSpecific(coll)(v))
        }
    })

  def empty(isSecret: Boolean = false): Output[Nothing] =
    new Output(ctx => Result.pure(OutputData.empty[Nothing](isSecret = isSecret)))

  def eval[F[_]: Result.ToFuture, A](value: F[A]): Output[A] =
    new Output[A](ctx => Result.eval[F, A](value).map(OutputData(_)))

  def fail(t: Throwable): Output[Nothing] = new Output(ctx => Result.fail(t))

  def ofResult[A](value: => Result[A]): Output[A] = new Output(ctx => value.map(OutputData(_)))

  def defer[A](value: => A): Output[A] = new Output(ctx => Result.defer(OutputData(value)))

  def pure[A](value: A): Output[A] = new Output(ctx => Result.pure(OutputData(value)))

  def ofData[A](value: => Result[OutputData[A]]): Output[A] = new Output(ctx => value)

  def ofData[A](data: OutputData[A]): Output[A] = new Output(ctx => Result.pure(data))

  def secret[A](value: A): Output[A] = new Output(ctx => Result.pure(OutputData(value, Set.empty, isSecret = true)))

  def when[A](cond: => Input[Boolean])(
    a: => Input.Optional[A]
  ): Output[Option[A]] =
    cond.asOutput().flatMap { c =>
      if c then a.asOptionOutput(isSecret = false) else Output.pure(None)
    }
  end when
end Output

trait OutputExtensionsFactory:
  implicit object OutputSequenceOps:
    extension [A, CC[X] <: Iterable[X]](coll: CC[Output[A]])
      /** Creates an `Output` of a collection from a collection of Outputs.
        *
        * @see
        *   [[parSequence]] for parallel execution
        */
      def sequence[To](using BuildFrom[CC[Output[A]], A, To]): Output[To] =
        Output.sequence(coll)

      /** Creates an `Output` of a collection from a collection of Outputs in parallel.
        * @see
        *   [[sequence]] for sequential execution
        */
      def parSequence[To](using BuildFrom[CC[Output[A]], A, To]): Output[To] =
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
      def traverse[B, To](f: A => Output[B])(using BuildFrom[CC[Output[B]], B, To]): Output[To] =
        Output.sequence(coll.map(f).asInstanceOf[CC[Output[B]]])

      /** Applies an Output-returning function to each element in the collection, in parallel, and then combines the results into an Output.
        *
        * @param f
        *   the Output-returning function to apply to each element in the collection
        * @see
        *   [[traverse]] for sequential execution
        */
      def parTraverse[B, To](f: A => Output[B])(using BuildFrom[CC[Output[B]], B, To]): Output[To] =
        coll.map(f).asInstanceOf[CC[Output[B]]].parSequence

  implicit final class OutputOptionOps[A](output: Output[Option[A]]):
    /** Behaves like [[Option.getOrElse]] on the underlying [[Option]]
      * @param default
      *   the default value to return if the underlying [[Option]] is [[None]]
      * @return
      *   an [[Output]] with the value of the underlying [[Some]] or the `default` value if [[None]]
      */
    def getOrElse[B >: A](default: => B | Output[B]): Output[B] =
      output.flatMap { opt =>
        opt match
          case Some(a) => Output.pure(a)
          case None =>
            default match
              case b: Output[B @unchecked] => b
              case b: B @unchecked         => Output.pure(b)
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
    def getOrFail(throwable: => Throwable): Output[A] =
      output.flatMap {
        case Some(a) => Output.pure(a)
        case None    => Output.fail(throwable)
      }

    /** Behaves like [[Option.orElse]] on the underlying [[Option]]
      * @param alternative
      *   the alternative [[Option]] to return if the underlying [[Option]] is [[None]]
      * @return
      *   an [[Output]] with the underlying [[Some]] or the `alternative` value if [[None]]
      */
    def orElse[B >: A](alternative: => Option[B] | Output[Option[B]]): Output[Option[B]] =
      output.flatMap {
        case some @ Some(_) => Output.pure(some)
        case None =>
          alternative match
            case b: Output[Option[B]] => b
            case b: Option[B]         => Output.pure(b)
      }

    /** Calls [[Option.map]] on the underlying [[Option]] with the given function
      * @return
      *   an [[Output]] of the mapped [[Option]]
      */
    def mapInner[B](f: A => B | Output[B]): Output[Option[B]] =
      output.flatMap {
        case Some(a) =>
          f(a) match
            case b: Output[B @unchecked] => b.map(Some(_))
            case b: B @unchecked         => Output.pure(Some(b))
        case None => Output.pure(None)
      }

    /** Calls [[Option.flatMap]] on the underlying [[Option]] with the given function
      * @return
      *   an [[Output]] of the flat-mapped [[Option]]
      */
    def flatMapInner[B](f: A => Option[B] | Output[Option[B]]): Output[Option[B]] =
      output.flatMap {
        case Some(a) =>
          f(a) match
            case b: Output[Option[B]] => b
            case b: Option[B]         => Output.pure(b)
        case None => Output.pure(None)
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
    def mapInner[B](f: A => B | Output[B]): Output[List[B]] = output.flatMap {
      case Nil => Output.pure(List.empty[B])
      case h :: t =>
        f(h) match
          case b: Output[B @unchecked] =>
            Output.sequence(b :: t.map(f.asInstanceOf[A => Output[B]](_)))
          case b: B @unchecked =>
            Output.pure(b :: t.map(f.asInstanceOf[A => B](_)))
    }

    /** Calls [[List.flatMap]] on the underlying [[List]] with the given function
      * @return
      *   an [[Output]] of the flat-mapped [[List]]
      */
    def flatMapInner[B](f: A => List[B] | Output[List[B]]): Output[List[B]] = output.flatMap {
      case Nil => Output.pure(List.empty[B])
      case h :: t =>
        f(h) match
          case bs: Output[List[B]] =>
            bs.flatMap { (bb: List[B]) =>
              val tailOfOutputs = t.map(f.asInstanceOf[A => Output[List[B]]](_))
              val tailOutput    = Output.sequence(tailOfOutputs).map(_.flatten)
              tailOutput.map(bb ::: _)
            }
          case bs: List[B] =>
            Output.pure(bs ::: t.flatMap(f.asInstanceOf[A => List[B]](_)))
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
    def mapInner[B](f: A => B | Output[B]): Output[List[B]] = output
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
    def flatMapInner[B](f: A => List[B] | Output[List[B]]): Output[List[B]] = output
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
