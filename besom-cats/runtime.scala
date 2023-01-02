package besom.cats

import cats.effect.*
import besom.internal.*
import cats.effect.unsafe.IORuntime
import cats.effect.kernel.Outcome.*

class CatsRuntime(val debugEnabled: Boolean = false)(using ioRuntime: IORuntime) extends Runtime[IO]:
  override def pure[A](a: A): IO[A]                                                  = IO(a)
  override def fail(err: Throwable): IO[Nothing]                                     = IO.raiseError(err)
  override def defer[A](thunk: => A): IO[A]                                          = IO(thunk)
  override def flatMapBoth[A, B](fa: IO[A])(f: Either[Throwable, A] => IO[B]): IO[B] = fa.attempt.flatMap(f)
  override def fromFuture[A](f: => scala.concurrent.Future[A]): IO[A]                = IO.fromFuture(IO(f))
  override def fork[A](fa: => IO[A]): IO[Result.Fiber[A]] =
    for
      promise <- Deferred[IO, Either[Throwable, A]]
      fib <- fa.start.map(catsFib =>
        new Result.Fiber[A]:
          def join: Result[A] = Result.deferFuture(
            catsFib.join
              .flatMap {
                case Succeeded(fa) => fa
                case Errored(e)    => IO.raiseError(e)
                case Canceled()    => IO.raiseError(new Exception("Unexpected cancelation!"))
              }
              .unsafeToFuture()
          )
      )
    yield fib

  private[besom] override def unsafeRunSync[A](fa: IO[A]): Either[Throwable, A] =
    fa.attempt.unsafeRunSync()
