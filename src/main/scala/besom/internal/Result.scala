//> using lib "com.lihaoyi::sourcecode:0.3.0"

package besom.internal

import scala.util.Try
import scala.concurrent.*
import scala.util.Failure
import scala.util.Success
import scala.concurrent.duration.Duration

trait Fiber[A]:
  def join: Result[A]

trait WorkGroup:
  def runInWorkGroup[A](eff: => Result[A]): Result[A]
  def waitForAll: Result[Unit]

object WorkGroup:
  def apply(): Result[WorkGroup] = Result.defer {
    new WorkGroup:
      val semaphore = java.util.concurrent.Semaphore(Int.MaxValue, false)

      override def runInWorkGroup[A](result: => Result[A]): Result[A] =
        Result.defer(semaphore.acquire()).flatMap { _ =>
          result.transform { res =>
            semaphore.release()
            res
          }
        }
      override def waitForAll: Result[Unit] = Result.defer(semaphore.acquire(Int.MaxValue)).void
  }

trait Promise[A]:
  def get: Result[A]
  def fulfill(a: A): Result[Unit]
  def fail(t: Throwable): Result[Unit]

object Promise:
  def apply[A]: Result[Promise[A]] = Result.defer {
    new Promise:
      val internal                                  = scala.concurrent.Promise[A]()
      override def get: Result[A]                   = Result.deferFuture(internal.future)
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
      val internalRef = new AtomicReference[A](initialValue)

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
  def flatMapBoth[A, B](fa: F[A])(f: Either[Throwable, A] => F[B]): F[B]
  def fromFuture[A](f: => scala.concurrent.Future[A]): F[A]
  def fork[A](fa: => F[A]): F[Fiber[A]]
  def sleep[A](fa: => F[A], duration: Long): F[A]

  private[besom] def debugEnabled: Boolean

  private[besom] def unsafeRunSync[A](fa: F[A]): Either[Throwable, A]

import sourcecode.*

case class Debug(name: FullName, file: File, line: Line):
  override def toString: String = s"${name.value} at ${file.value}:${line.value}"
object Debug:
  def !(using name: FullName, file: File, line: Line): Debug = new Debug(name, file, line)

enum Result[+A]:
  case Suspend(thunk: () => Future[A], debug: Debug)
  case Pure(a: A, debug: Debug)
  case Defer(a: () => A, debug: Debug)
  case Fail(t: Throwable, debug: Debug) extends Result[Nothing]
  case BiFlatMap[B, A](r: Result[B], f: Either[Throwable, B] => Result[A], debug: Debug) extends Result[A]
  case Fork[A](r: Result[A], debug: Debug) extends Result[Fiber[A]]
  case Sleep(r: () => Result[A], duration: Long, debug: Debug)

  def flatMap[B](f: A => Result[B])(using FullName, File, Line): Result[B] = BiFlatMap(
    this,
    {
      case Right(a) => f(a)
      case Left(t)  => Fail(t, Debug.!)
    },
    Debug.!
  )

  def recover[A2 >: A](f: Throwable => Result[A2])(using FullName, File, Line): Result[A2] = BiFlatMap(
    this,
    {
      case Right(a) => Pure(a, Debug.!)
      case Left(t)  => f(t)
    },
    Debug.!
  )

  def transform[B](f: Either[Throwable, A] => Either[Throwable, B])(using FullName, File, Line): Result[B] = BiFlatMap(
    this,
    e => Result.evalEither(f(e)),
    Debug.!
  )

  def map[B](f: A => B)(using FullName, File, Line): Result[B]              = flatMap(a => Pure(f(a), Debug.!))
  def product[B](rb: Result[B])(using FullName, File, Line): Result[(A, B)] = flatMap(a => rb.map(b => (a, b)))
  def zip[B](rb: => Result[B])(using z: Zippable[A, B])(using FullName, File, Line) =
    product(rb).map((a, b) => z.zip(a, b))
  def void(using FullName, File, Line): Result[Unit]                   = flatMap(_ => Result.unit)
  def fork[A2 >: A](using FullName, File, Line): Result[Fiber[A2]]     = Result.Fork(this, Debug.!)
  def tap(f: A => Result[Unit])(using FullName, File, Line): Result[A] = flatMap(a => f(a).flatMap(_ => Result.pure(a)))
  def delay(duration: Long)(using FullName, File, Line): Result[A]     = Result.Sleep(() => this, duration, Debug.!)

  def run[F[+_]](using F: Runtime[F]): F[A] = this match
    case Suspend(thunk, debug) =>
      if (F.debugEnabled) (s"interpreting Suspend from $debug")
      F.fromFuture(thunk())
    case Pure(value, debug) =>
      if (F.debugEnabled) println(s"interpreting Pure from $debug")
      F.pure(value)
    case Fail(err, debug) =>
      if (F.debugEnabled) println(s"interpreting Fail from $debug")
      F.fail(err).asInstanceOf[F[A]]
    case Defer(thunk, debug) =>
      if (F.debugEnabled) println(s"interpreting Defer from $debug")
      F.defer(thunk())
    case BiFlatMap(fa, f, debug) =>
      if (F.debugEnabled) println(s"interpreting BiFlatMap from $debug")
      F.flatMapBoth(fa.run[F])(either => f(either).run[F])
    case Fork(fa, debug) =>
      if (F.debugEnabled) println(s"interpreting Fork from $debug")
      F.fork(fa.run[F])
    case Sleep(fa, duration, debug) =>
      if (F.debugEnabled) println(s"interpreting Sleep from $debug")
      F.sleep(fa().run[F], duration)

object Result:
  import scala.collection.BuildFrom

  def apply[A](a: => A)(using FullName, File, Line): Result[A] = defer(a)
  def defer[A](a: => A)(using FullName, File, Line): Result[A] = Result.Defer(() => a, Debug.!)
  def pure[A](a: A)(using FullName, File, Line): Result[A]     = Result.Pure(a, Debug.!)
  def eval[F[_], A](fa: => F[A])(tf: ToFuture[F])(using FullName, File, Line): Result[A] =
    Result.Suspend(tf.eval(fa), Debug.!)
  def evalTry[A](tryA: => Try[A])(using FullName, File, Line): Result[A] =
    Result.Suspend(() => Future.fromTry(tryA), Debug.!)

  def evalEither[A](eitherA: => Either[Throwable, A]): Result[A] = Result.Suspend(
    () =>
      eitherA match
        case Right(r) => Future.successful(r)
        case Left(l)  => Future.failed(l)
    ,
    Debug.!
  )

  def sleep(duration: Long): Result[Unit] = Result.Sleep(() => unit, duration, Debug.!)

  def fail[A](err: Throwable)(using FullName, File, Line): Result[A] = Result.Fail(err, Debug.!)

  def unit(using FullName, File, Line): Result[Unit] = Result.Pure((), Debug.!)

  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[Result[A]]
  )(using bf: BuildFrom[CC[Result[A]], A, To]): Result[To] =
    coll.iterator
      .foldLeft(pure(bf.newBuilder(coll))) { (acc, curr) =>
        acc.product(curr).map { case (b, r) => b += r }
      }
      .map(_.result())

  def deferFuture[A](thunk: => Future[A])(using FullName, File, Line): Result[A] = Result.Suspend(() => thunk, Debug.!)

  trait ToFuture[F[_]]:
    def eval[A](fa: => F[A]): () => Future[A]

class FutureRuntime(val debugEnabled: Boolean = false)(using ExecutionContext) extends Runtime[Future]:
  def pure[A](a: A): Future[A]              = Future.successful(a)
  def fail(err: Throwable): Future[Nothing] = Future.failed(err)
  def defer[A](thunk: => A): Future[A]      = Future(thunk)
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

  def sleep[A](fa: => Future[A], duration: Long): Future[A] = Future(Thread.sleep(duration)).flatMap(_ => fa)

  private[besom] def unsafeRunSync[A](fa: Future[A]): Either[Throwable, A] = Try(
    Await.result(fa, Duration.Inf)
  ).toEither
