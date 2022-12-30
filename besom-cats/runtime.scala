package besom.cats

import cats.effect.*
import besom.internal.*
import cats.effect.unsafe.IORuntime

object CatsRuntime extends Runtime[IO]:
  override def pure[A](a: A): IO[A]                                                  = IO(a)
  override def fail(err: Throwable): IO[Nothing]                                     = IO.raiseError(err)
  override def defer[A](thunk: => A): IO[A]                                          = IO(thunk)
  override def flatMapBoth[A, B](fa: IO[A])(f: Either[Throwable, A] => IO[B]): IO[B] = fa.attempt.flatMap(f)
  override def fromFuture[A](f: => scala.concurrent.Future[A]): IO[A]                = IO.fromFuture(IO(f))
  override def fork[A](fa: => IO[A]): IO[Unit]                                       = fa.start.void

  private[besom] override def unsafeRunSync[A](fa: IO[A]): Either[Throwable, A] =
    given IORuntime = cats.effect.unsafe.IORuntime.global
    fa.attempt.unsafeRunSync()
