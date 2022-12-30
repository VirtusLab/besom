package besom.internal

import scala.util.Try
import scala.concurrent.Future

trait Queue[F[+_], A]:
  def offer(a: A): F[Unit]
  def poll: F[A]
  def size: F[Int]
  def isEmpty: F[Boolean]

object Queue:
  def apply[F[+_], A](using F: Monad[F]): F[Queue[F, A]] = F.queue

trait Promise[F[+_], A]:
  def get: F[A]
  def fulfill(a: A): F[Unit]
  def fail(t: Throwable): F[Unit]

object Promise:
  def apply[F[+_], A](using F: Monad[F]): F[Promise[F, A]] = F.promise

trait Ref[F[+_], A]:
  def get: F[A]
  def update(f: A => A): F[Unit]

object Ref:
  def apply[F[+_], A](initialValue: => A)(using F: Monad[F]): F[Ref[F, A]] = F.ref(initialValue)

trait WorkGroup[F[+_]]:
  def runInWorkGroup[A](eff: => F[A]): F[A]
  def waitForAll: F[Unit]

object WorkGroup:
  def apply[F[+_]](using F: Monad[F]): F[WorkGroup[F]] = F.workgroup

trait Fiber[F[+_], A]:
  def join: F[A]

trait Zippable[-A, -B]:
  type Out
  def zip(left: A, right: B): Out

object Zippable extends ZippableLowPrio:
  given append[A <: Tuple, B]: (Zippable[A, B] { type Out = Tuple.Append[A, B] }) =
    (left, right) => left :* right

trait ZippableLowPrio:
  given pair[A, B]: (Zippable[A, B] { type Out = (A, B) }) =
    (left, right) => (left, right)

trait Monad[F[+_]]:
  def eval[A](a: => A): F[A]
  def evalTry[A](a: => Try[A]): F[A]
  def evalEither[A](a: => Either[Throwable, A]): F[A]
  def unit: F[Unit]

  def map[A, B](fa: F[A])(f: A => B): F[B]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def product[A, B](fa: => F[A], fb: => F[B]): F[(A, B)]

  def zip[A, B](a: => F[A], b: => F[B])(using z: Zippable[A, B]) =
    map(product(a, b))((a, b) => z.zip(a, b))

  def sequence[A](vec: Vector[F[A]]): F[Vector[A]]

  def error[A](err: Throwable): F[A]
  def recover[A](fa: F[A])(f: Throwable => F[A]): F[A]

  def fork[A](fa: => F[A]): F[Fiber[F, A]]

  def fromFuture[A](futA: => scala.concurrent.Future[A]): F[A]

  def queue[A]: F[Queue[F, A]]

  def promise[A]: F[Promise[F, A]]

  def ref[A](initial: => A): F[Ref[F, A]]

  def workgroup: F[WorkGroup[F]]

object Monad:
  def apply[F[+_]: Monad]: Monad[F] = summon[Monad[F]]

  inline given fromContext[F[+_]](using ctx: Context.Of[F]): Monad[F] = ctx.monad

extension [F[+_], A](fa: F[A])
  inline def map[B](f: A => B)(using F: Monad[F]): F[B]                = F.map(fa)(f)
  inline def flatMap[B](f: A => F[B])(using F: Monad[F]): F[B]         = F.flatMap(fa)(f)
  inline def void(using F: Monad[F]): F[Unit]                          = F.map(fa)(_ => ())
  inline def tap(f: A => F[Unit])(using F: Monad[F]): F[A]             = fa.flatMap(a => f(a).flatMap(_ => F.eval(a)))
  inline def recover(f: Throwable => F[A])(using F: Monad[F]): F[A]    = F.recover(fa)(f)
  inline def product[B](fb: => F[B])(using F: Monad[F]): F[(A, B)]     = F.product(fa, fb)
  inline def zip[B](fb: => F[B])(using F: Monad[F], z: Zippable[A, B]) = F.zip(fa, fb)
  inline def fork(using F: Monad[F]): F[Fiber[F, A]]                   = F.fork(fa)

  inline def background(using F: Monad[F]): F[Promise[F, A]] =
    for
      p <- Promise[F, A]
      _ <- fa.flatMap(a => p.fulfill(a)).recover(t => p.fail(t)).fork
    yield p
