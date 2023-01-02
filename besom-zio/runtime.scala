package besom.zio

import zio.{Runtime => _, *}
import besom.internal.*
import zio.Promise

class ZIORuntime(val debugEnabled: Boolean = false)(using rt: zio.Runtime[Any]) extends Runtime[Task]:
  override def pure[A](a: A): Task[A]                                                      = ZIO.succeed(a)
  override def fail(err: Throwable): Task[Nothing]                                         = ZIO.die(err)
  override def defer[A](thunk: => A): Task[A]                                              = ZIO.succeed(thunk)
  override def flatMapBoth[A, B](fa: Task[A])(f: Either[Throwable, A] => Task[B]): Task[B] = fa.either.flatMap(f)
  override def fromFuture[A](f: => scala.concurrent.Future[A]): Task[A]                    = ZIO.fromFuture(_ => f)
  override def fork[A](fa: => Task[A]): Task[Result.Fiber[A]] =
    for
      promise <- Promise.make[Throwable, A]
      fib <- fa.fork.map(zioFib =>
        new Result.Fiber[A]:
          def join: Result[A] = Result.deferFuture(
            Unsafe.unsafe { implicit unsafe =>
              rt.unsafe.runToFuture {
                zioFib.await
                  .flatMap { exit =>
                    ZIO.fromEither(exit.toEither)
                  }
              }
            }
          )
      )
    yield fib

  private[besom] override def unsafeRunSync[A](fa: Task[A]): Either[Throwable, A] =
    Unsafe.unsafe { implicit unsafe =>
      rt.unsafe.run(fa).toEither
    }
