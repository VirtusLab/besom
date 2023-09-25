package besom.internal

import scala.util.Try
import scala.concurrent.*
import scala.util.Failure
import scala.util.Success
import scala.concurrent.duration.Duration

import logging.{LocalBesomLogger => logger}
import scala.annotation.implicitNotFound
import besom.util.Zippable

trait Fiber[+A]:
  def join: Result[A]

object Fiber:
  def unit: Fiber[Unit] = new Fiber:
    override def join: Result[Unit] = Result.unit

trait Queue[A]:
  def offer(a: A): Result[Unit]
  def take: Result[A]

object Queue:
  sealed trait Stop
  case object Stop extends Stop

  import java.util.concurrent.ArrayBlockingQueue

  def apply[A](capacity: Int): Result[Queue[A]] = Result.defer {
    new Queue:
      private val internal = ArrayBlockingQueue[A](capacity)

      override def offer(a: A): Result[Unit] = Result.blocking(internal.offer(a))

      override def take: Result[A] = Result.blocking(internal.take)
  }

trait WorkGroup:
  def runInWorkGroup[A](eff: => Result[A]): Result[A]
  def waitForAll: Result[Unit]
  def reset: Result[Unit]

object WorkGroup:
  def apply(): Result[WorkGroup] = Result.defer {
    new WorkGroup:
      val semaphore = java.util.concurrent.Semaphore(Int.MaxValue, false)

      override def runInWorkGroup[A](result: => Result[A]): Result[A] =
        Result
          .defer {
            semaphore.acquire()
          }
          .flatMap { _ =>
            result.transform { res =>
              semaphore.release()
              res
            }
          }
      override def waitForAll: Result[Unit] = Result.defer(semaphore.acquire(Int.MaxValue))

      override def reset: Result[Unit] = Result.defer(semaphore.release(Int.MaxValue))
  }

// TODO: implementations of Promise for CE and ZIO?
trait Promise[A]:
  def get: Result[A]
  def isCompleted: Result[Boolean]
  def fulfill(a: A): Result[Unit]
  def fulfillAny(a: Any): Result[Unit] = fulfill(a.asInstanceOf[A]) // TODO fix this in CustomPropertyExtractor
  def fail(t: Throwable): Result[Unit]

object Promise:
  def apply[A](): Result[Promise[A]] = Result.defer {
    new Promise:
      private val internal                          = scala.concurrent.Promise[A]()
      override def get: Result[A]                   = Result.deferFuture(internal.future)
      override def isCompleted: Result[Boolean]     = Result.defer(internal.isCompleted)
      override def fulfill(a: A): Result[Unit]      = Result.defer(internal.success(a))
      override def fail(t: Throwable): Result[Unit] = Result.defer(internal.failure(t))
  }

trait Ref[A]:
  def get: Result[A]
  def update(f: A => A): Result[Unit]

object Ref:
  import java.util.concurrent.atomic.AtomicReference

  def apply[A](initialValue: => A): Result[Ref[A]] = Result.defer {
    new Ref:
      private val internalRef = new AtomicReference[A](initialValue)

      override def get: Result[A] = Result.defer(internalRef.get)

      override def update(f: A => A): Result[Unit] = Result.defer {
        var set = false
        while (!set) do
          val a = internalRef.get
          set = internalRef.compareAndSet(a, f(a))
      }

  }

trait Runtime[F[+_]]:
  def pure[A](a: A): F[A]
  def fail(err: Throwable): F[Nothing]
  def defer[A](thunk: => A): F[A]
  def blocking[A](thunk: => A): F[A]
  def flatMapBoth[A, B](fa: F[A])(f: Either[Throwable, A] => F[B]): F[B]
  def fromFuture[A](f: => scala.concurrent.Future[A]): F[A]
  def fork[A](fa: => F[A]): F[Fiber[A]]
  def sleep[A](fa: => F[A], duration: Long): F[A]

  private[internal] final case class M[+A](inner: Finalizers => F[A])
  private[internal] def pureM[A](a: A): M[A]              = M(_ => pure(a))
  private[internal] def failM(err: Throwable): M[Nothing] = M(_ => fail(err))
  private[internal] def deferM[A](thunk: => A): M[A]      = M(_ => defer(thunk))
  private[internal] def blockingM[A](thunk: => A): M[A]   = M(_ => blocking(thunk))
  private[internal] def flatMapBothM[A, B](fa: M[A])(f: Either[Throwable, A] => M[B]): M[B] = M { finalizers =>
    flatMapBoth(fa.inner(finalizers)) { either =>
      f(either).inner(finalizers)
    }
  }
  private[internal] def fromFutureM[A](f: => scala.concurrent.Future[A]): M[A] = M(_ => fromFuture(f))
  private[internal] def forkM[A](fa: => M[A]): M[Fiber[A]] = M { finalizers =>
    fork(fa.inner(finalizers))
  }
  private[internal] def sleepM[A](fa: => M[A], duration: Long): M[A] = M { finalizers =>
    sleep(fa.inner(finalizers), duration)
  }
  private[internal] def getFinalizersM: M[Finalizers] = M(fs => pure(fs))

  private[besom] def debugEnabled: Boolean

  private[besom] def unsafeRunSync[A](fa: F[A]): Either[Throwable, A]

import sourcecode.*

case class Debug(name: FullName, file: File, line: Line):
  override def toString: String = s"${name.value} at ${file.value}:${line.value}"
object Debug:
  inline given (using inline fullName: FullName, inline file: File, inline line: Line): Debug =
    new Debug(fullName, file, line)
  def apply()(using d: Debug): Debug = d

private[internal] type Finalizers = Ref[Map[Scope, List[Result[Unit]]]]
@implicitNotFound(s"""|Resource can only be used inside a block with a defined Scope.
                      |
                      |To start a new scope use `Result.scoped { ... }`
                      |Otherwise add a `using Scope` parameter to your method.""".stripMargin)
private[internal] trait Scope

enum Result[+A]:
  case Suspend(thunk: () => Future[A], debug: Debug)
  case Pure(a: A, debug: Debug)
  case Defer(a: () => A, debug: Debug)
  case Blocking(a: () => A, debug: Debug)
  case Fail(t: Throwable, debug: Debug) extends Result[Nothing]
  case BiFlatMap[B, A](r: Result[B], f: Either[Throwable, B] => Result[A], debug: Debug) extends Result[A]
  case Fork(r: Result[A], debug: Debug) extends Result[Fiber[A]]
  case Sleep(r: () => Result[A], duration: Long, debug: Debug)
  case GetFinalizers(debug: Debug) extends Result[Finalizers]

  def flatMap[B](f: A => Result[B])(using Debug): Result[B] = BiFlatMap(
    this,
    {
      case Right(a) => f(a)
      case Left(t)  => Fail(t, Debug())
    },
    Debug()
  )

  def recover[A2 >: A](f: Throwable => Result[A2])(using Debug): Result[A2] = BiFlatMap(
    this,
    {
      case Right(a) => Pure(a, Debug())
      case Left(t)  => f(t)
    },
    Debug()
  )

  def transform[B](f: Either[Throwable, A] => Either[Throwable, B])(using Debug): Result[B] = BiFlatMap(
    this,
    e => Result.evalEither(f(e)),
    Debug()
  )

  def transformM[B](f: Either[Throwable, A] => Result[Either[Throwable, B]])(using Debug): Result[B] = BiFlatMap(
    this,
    e => f(e).flatMap(Result.evalEither),
    Debug()
  )

  def either(using Debug): Result[Either[Throwable, A]] =
    BiFlatMap(
      this,
      e => Result.Pure(e, Debug()),
      Debug()
    )

  def delay(duration: Long)(using Debug): Result[A] = Result.Sleep(() => this, duration, Debug())
  def fork[A2 >: A](using Debug): Result[Fiber[A2]] = Result.Fork(this, Debug())

  def map[B](f: A => B)(using Debug): Result[B]              = flatMap(a => Pure(f(a), Debug()))
  def product[B](rb: Result[B])(using Debug): Result[(A, B)] = flatMap(a => rb.map(b => (a, b)))
  def zip[B](rb: => Result[B])(using z: Zippable[A, B])(using Debug) =
    product(rb).map((a, b) => z.zip(a, b))
  def void(using Debug): Result[Unit]                   = flatMap(_ => Result.unit)
  def tap(f: A => Result[Unit])(using Debug): Result[A] = flatMap(a => f(a).flatMap(_ => Result.pure(a)))
  def tapBoth(f: Either[Throwable, A] => Result[Unit])(using Debug): Result[A] =
    transformM(e => f(e) *> Result.pure(e))
  def as[B](b: B)(using Debug): Result[B] = map(_ => b)

  inline def *>[B](rb: => Result[B])(using Debug): Result[B] = flatMap(_ => rb)
  inline def <*[B](rb: => Result[B])(using Debug): Result[A] = flatMap(a => rb.as(a))

  def run[F[+_]](using F: Runtime[F]): F[A] =
    F.flatMapBoth(Ref(Map.empty[Scope, List[Result[Unit]]]).runF[F]) {
      case Right(finalizersRef) => this.runM[F].inner(finalizersRef)
      case Left(err)            => F.fail(err)
    }

  private def runF[F[+_]](using F: Runtime[F]): F[A] = this match
    case Suspend(thunk, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Suspend from $debug")
      F.fromFuture(thunk())
    case Pure(value, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Pure from $debug")
      F.pure(value)
    case Fail(err, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Fail from $debug")
      F.fail(err).asInstanceOf[F[A]]
    // TODO test that Defer catches for all implementations always
    case Defer(thunk, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Defer from $debug")
      F.defer(thunk())
    case Blocking(thunk, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Blocking from $debug")
      F.blocking(thunk())
    case BiFlatMap(fa, f, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting BiFlatMap from $debug")
      F.flatMapBoth(fa.runF[F])(either => f(either).runF[F].asInstanceOf[F[A]])
    case Fork(fa, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Fork from $debug")
      F.fork(fa.runF[F]).asInstanceOf[F[A]]
    case Sleep(fa, duration, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Sleep from $debug")
      F.sleep(fa().runF[F], duration)
    case GetFinalizers(debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting GetFinalizers from $debug")
      F.fail(new RuntimeException("Panic: GetFinalizers should not be called on base monad F")).asInstanceOf[F[A]]

  private def runM[F[+_]](using F: Runtime[F]): F.M[A] = this match
    case Suspend(thunk, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Suspend from $debug")
      F.fromFutureM(thunk())
    case Pure(value, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Pure from $debug")
      F.pureM(value)
    case Fail(err, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Fail from $debug")
      F.failM(err).asInstanceOf[F.M[A]]
    // TODO test that Defer catches for all implementations always
    case Defer(thunk, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Defer from $debug")
      F.deferM(thunk())
    case Blocking(thunk, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Blocking from $debug")
      F.blockingM(thunk())
    case BiFlatMap(fa, f, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting BiFlatMap from $debug")
      F.flatMapBothM(fa.runM[F])(either => f(either).runM[F].asInstanceOf[F.M[A]])
    case Fork(fa, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Fork from $debug")
      F.forkM(fa.runM[F]).asInstanceOf[F.M[A]]
    case Sleep(fa, duration, debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting Sleep from $debug")
      F.sleepM(fa().runM[F], duration)
    case GetFinalizers(debug) =>
      if (F.debugEnabled) logger.trace(s"interpreting GetFinalizers from $debug")
      F.getFinalizersM

object Result:
  import scala.collection.BuildFrom

  def apply[A](a: => A)(using Debug): Result[A]    = defer(a)
  def defer[A](a: => A)(using Debug): Result[A]    = Result.Defer(() => a, Debug())
  def blocking[A](a: => A)(using Debug): Result[A] = Result.Blocking(() => a, Debug())
  def pure[A](a: A)(using Debug): Result[A]        = Result.Pure(a, Debug())

  def eval[F[_], A](fa: => F[A])(using tf: ToFuture[F])(using Debug): Result[A] =
    // if F.eagerEvaluation then // TODO this is one of the possible ways to implement eager evaluation, other being the CHM approach
    //   val pendingFuture = tf.eval(fa)()
    //   Result.Suspend(() => pendingFuture, Debug())
    // else
    Result.Suspend(tf.eval(fa), Debug())

  def evalTry[A](tryA: => Try[A])(using Debug): Result[A] =
    Result.Suspend(() => Future.fromTry(tryA), Debug())

  def evalEither[A](eitherA: => Either[Throwable, A])(using Debug): Result[A] = Result.Suspend(
    () =>
      eitherA match
        case Right(r) => Future.successful(r)
        case Left(l)  => Future.failed(l)
    ,
    Debug()
  )

  def sleep(duration: Long)(using Debug): Result[Unit] = Result.Sleep(() => unit, duration, Debug())

  def fail[A](err: Throwable)(using Debug): Result[A] = Result.Fail(err, Debug())

  def unit(using Debug): Result[Unit] = Result.Pure((), Debug())

  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[Result[A]]
  )(using bf: BuildFrom[CC[Result[A]], A, To], d: Debug): Result[To] =
    coll.iterator
      .foldLeft(pure(bf.newBuilder(coll))) { (acc, curr) =>
        acc.product(curr).map { case (b, r) => b += r }
      }
      .map(_.result())

  def sequenceMap[K, V](map: Map[K, Result[V]])(using Debug): Result[Map[K, V]] =
    sequence(map.view.map { case (k, v) => v.map(k -> _) }.toVector).map(_.toMap)

  def deferFuture[A](thunk: => Future[A])(using Debug): Result[A] = Result.Suspend(() => thunk, Debug())

  def bracket[A, B](acquire: => Result[A])(release: A => Result[Unit])(use: A => Result[B])(using Debug): Result[B] =
    acquire.flatMap { a =>
      use(a).transformM(r => release(a).as(r))
    }

  def getFinalizers(using Debug): Result[Finalizers] = GetFinalizers(Debug())

  def scoped[A](inner: Scope ?=> Result[A])(using Debug): Result[A] =
    given scope: Scope = new Scope {}
    inner.transformM { a =>
      for
        finalizersRef <- getFinalizers
        finalizers    <- finalizersRef.get
        currentFinalizers = finalizers.getOrElse(scope, Nil)
        _ <- Result.sequence(currentFinalizers)
        _ <- finalizersRef.update(_.removed(scope))
      yield a
    }

  def resource[A](acquire: => Result[A])(release: A => Result[Unit])(using scope: Scope)(using Debug): Result[A] =
    for
      a             <- acquire
      finalizersRef <- getFinalizers
      _ <- finalizersRef.update(_.updatedWith(scope)(finalizers => Some(release(a) :: finalizers.toList.flatten)))
    yield a

  trait ToFuture[F[_]]:
    def eval[A](fa: => F[A]): () => Future[A]

class FutureRuntime(val debugEnabled: Boolean = false)(using ExecutionContext) extends Runtime[Future]:

  def pure[A](a: A): Future[A]              = Future.successful(a)
  def fail(err: Throwable): Future[Nothing] = Future.failed(err)
  def defer[A](thunk: => A): Future[A]      = Future(thunk)
  def blocking[A](thunk: => A): Future[A]   = Future(concurrent.blocking(thunk))

  def flatMapBoth[A, B](fa: Future[A])(f: Either[Throwable, A] => Future[B]): Future[B] =
    fa.transformWith(t => f(t.toEither))
  def fromFuture[A](f: => scala.concurrent.Future[A]): Future[A] = Future(f).flatten
  def fork[A](fa: => Future[A]): Future[Fiber[A]] =
    val p = scala.concurrent.Promise[A]()
    fa.onComplete(p.complete)
    Future(
      new Fiber[A]:
        def join: Result[A] = Result.deferFuture(p.future)
    )

  def sleep[A](fa: => Future[A], duration: Long): Future[A] =
    Future(concurrent.blocking(Thread.sleep(duration))).flatMap(_ => fa)

  private[besom] def unsafeRunSync[A](fa: Future[A]): Either[Throwable, A] = Try(
    Await.result(fa, Duration.Inf)
  ).toEither
