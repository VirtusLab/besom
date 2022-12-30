package besom.internal

import scala.util.Try
import scala.concurrent.*

trait Runtime[F[+_]]:
  def pure[A](a: A): F[A]
  def fail(err: Throwable): F[Nothing]
  def defer[A](thunk: => A): F[A]
  def flatMapBoth[A, B](fa: F[A])(f: Either[Throwable, A] => F[B]): F[B]
  def fromFuture[A](f: => scala.concurrent.Future[A]): F[A]
  def fork[A](fa: => F[A]): F[Unit]

  private[besom] def unsafeRunSync[A](fa: F[A]): Either[Throwable, A]

class FutureRuntime(using ExecutionContext) extends Runtime[Future]:
  def pure[A](a: A): Future[A]              = Future.successful(a)
  def fail(err: Throwable): Future[Nothing] = Future.failed(err)
  def defer[A](thunk: => A): Future[A]      = Future(thunk)
  def flatMapBoth[A, B](fa: Future[A])(f: Either[Throwable, A] => Future[B]): Future[B] =
    fa.transformWith(t => f(t.toEither))
  def fromFuture[A](f: => scala.concurrent.Future[A]): Future[A] = Future(f).flatten
  def fork[A](fa: => Future[A]): Future[Unit]                    = Future.unit.flatMap(_ => fa.map(_ => ()))

  private[besom] def unsafeRunSync[A](fa: Future[A]): Either[Throwable, A] = ???

enum Result[+A]:
  case Suspend(thunk: () => Future[A])
  case Pure(a: A)
  case Defer(a: () => A)
  case Fail(t: Throwable) extends Result[Nothing]
  case BiFlatMap[B, A](r: Result[B], f: Either[Throwable, B] => Result[A]) extends Result[A]
  case Fork(r: Result[Unit]) extends Result[Unit]

  def flatMap[B](f: A => Result[B]): Result[B] = BiFlatMap(
    this,
    {
      case Right(a) => f(a)
      case Left(t)  => Fail(t)
    }
  )

  def recover[A2 >: A](f: Throwable => Result[A2]): Result[A2] = BiFlatMap(
    this,
    {
      case Right(a) => Pure(a)
      case Left(t)  => f(t)
    }
  )

  def transform[B](f: Either[Throwable, A] => Either[Throwable, B]): Result[B] = BiFlatMap(
    this,
    e => Result.evalEither(f(e))
  )

  def map[B](f: A => B): Result[B]                      = flatMap(a => Pure(f(a)))
  def product[B](rb: Result[B]): Result[(A, B)]         = flatMap(a => rb.map(b => (a, b)))
  def zip[B](rb: => Result[B])(using z: Zippable[A, B]) = product(rb).map((a, b) => z.zip(a, b))
  def void: Result[Unit]                                = flatMap(_ => Result.unit)
  def fork: Result[Unit]                                = Result.Fork(this.void)
  def tap(f: A => Result[Unit]): Result[A]              = flatMap(a => f(a).flatMap(_ => Result.pure(a)))

  def run[F[+_]](using F: Runtime[F]): F[A] = this match
    case Suspend(thunk)   => F.fromFuture(thunk())
    case Pure(value)      => F.pure(value)
    case Fail(err)        => F.fail(err).asInstanceOf[F[A]]
    case Defer(thunk)     => F.defer(thunk())
    case BiFlatMap(fa, f) => F.flatMapBoth(fa.run[F])(either => f(either).run[F])
    case Fork(fa)         => F.fork(fa.run[F])

object Result:
  import scala.collection.BuildFrom

  def apply[A](a: => A): Result[A]                           = defer(a)
  def defer[A](a: => A): Result[A]                           = Result.Defer(() => a)
  def pure[A](a: A): Result[A]                               = Result.Pure(a)
  def eval[F[_], A](fa: => F[A])(tf: ToFuture[F]): Result[A] = Result.Suspend(tf.eval(fa))
  def evalTry[A](tryA: => Try[A]): Result[A]                 = Result.Suspend(() => Future.fromTry(tryA))

  def evalEither[A](eitherA: => Either[Throwable, A]): Result[A] = Result.Suspend(() =>
    eitherA match
      case Right(r) => Future.successful(r)
      case Left(l)  => Future.failed(l)
  )

  def fail[A](err: Throwable): Result[A] = Result.Fail(err)

  final val unit: Result[Unit] = Result.Pure(())

  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[Result[A]]
  )(using bf: BuildFrom[CC[Result[A]], A, To]): Result[To] =
    coll.iterator
      .foldLeft(pure(bf.newBuilder(coll))) { (acc, curr) =>
        acc.product(curr).map { case (b, r) => b += r }
      }
      .map(_.result())

  def deferFuture[A](thunk: => Future[A]): Result[A] = Result.Suspend(() => thunk)

  trait WorkGroup:
    def runInWorkGroup[A](eff: => Result[A]): Result[A]
    def waitForAll: Result[Unit]

  object WorkGroup:
    def apply(): Result[WorkGroup] = pure {
      new WorkGroup:
        val semaphore = java.util.concurrent.Semaphore(Int.MaxValue, false)

        override def runInWorkGroup[A](result: => Result[A]): Result[A] =
          defer(semaphore.acquire()).flatMap { _ =>
            result.transform { res =>
              semaphore.release()
              res
            }
          }
        override def waitForAll: Result[Unit] = defer(semaphore.acquire(Int.MaxValue)).fork.void
    }

  trait Promise[A]:
    def get: Result[A]
    def fulfill(a: A): Result[Unit]
    def fail(t: Throwable): Result[Unit]

  object Promise:
    def apply[A]: Result[Promise[A]] = pure {
      new Promise:
        val internal = scala.concurrent.Promise[A]()
        override def get: Result[A] = deferFuture {
          println(s"Getting result of $this on thread ${Thread.currentThread()}")
          internal.future
        }
        override def fulfill(a: A): Result[Unit] = defer {
          println(s"Fulfilling $this with $a on thread ${Thread.currentThread()}")
          internal.success(a)
        }
        override def fail(t: Throwable): Result[Unit] = defer(internal.failure(t))
    }

  trait Ref[A]:
    def get: Result[A]
    def update(f: A => A): Result[Unit]

  object Ref:
    import java.util.concurrent.atomic.AtomicReference

    def apply[A](initialValue: => A): Result[Ref[A]] = pure {
      new Ref:
        val internalRef = new AtomicReference[A](initialValue)

        override def get: Result[A] = defer(internalRef.get)

        override def update(f: A => A): Result[Unit] = defer {
          var set = false
          while (!set) do
            val a = internalRef.get
            set = internalRef.compareAndSet(a, f(a))
        }

    }

  trait ToFuture[F[_]]:
    def eval[A](fa: => F[A]): () => Future[A]
