package besom.zio

import zio.{Runtime => _, Fiber => ZIOFiber, *}
import besom.internal.*
import zio.Promise

// TODO it would be good to make effects uninterruptible
class ZIORuntime(val debugEnabled: Boolean = false)(using rt: zio.Runtime[Any]) extends Runtime[Task]:
  override def pure[A](a: A): Task[A]                                                      = ZIO.succeed(a)
  override def fail(err: Throwable): Task[Nothing]                                         = ZIO.fail(err)
  override def defer[A](thunk: => A): Task[A]                                              = ZIO.attempt(thunk)
  override def flatMapBoth[A, B](fa: Task[A])(f: Either[Throwable, A] => Task[B]): Task[B] = fa.either.flatMap(f)
  override def fromFuture[A](f: => scala.concurrent.Future[A]): Task[A]                    = ZIO.fromFuture(_ => f)
  override def blocking[A](thunk: => A): Task[A]                                           = ZIO.attemptBlocking(thunk)
  override def fork[A](fa: => Task[A]): Task[Fiber[A]] =
    for
      promise <- Promise.make[Throwable, A]
      fib <- fa.fork.map(zioFib =>
        new Fiber[A]:
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

  override def sleep[A](fa: => Task[A], duration: Long): Task[A] = ZIO.sleep(duration.millis) *> fa

  private[besom] override def unsafeRunSync[A](fa: Task[A]): Either[Throwable, A] =
    Unsafe.unsafe { implicit unsafe =>
      rt.unsafe.run(fa).toEither
    }

trait ZIOModule extends BesomModule:
  import scala.concurrent.*
  override final type Eff[+A] = zio.Task[A]

  protected lazy val rt: Runtime[Eff] = ZIORuntime()(using zio.Runtime.default)

  given Result.ToFuture[Eff] = new Result.ToFuture[Task]:
    def eval[A](fa: => Task[A]): () => Future[A] = () =>
      Unsafe.unsafe { implicit unsafe =>
        zio.Runtime.default.unsafe.runToFuture(fa)
      }

  // override def run(program: Context ?=> Output[Exports]): Future[Unit] = ???

object Pulumi extends ZIOModule
export Pulumi.{ *, given }
