package besom.zio

import zio.{Runtime => _, *}
import besom.internal.*

object ZIORuntime extends Runtime[Task]:
  override def pure[A](a: A): Task[A]                                                      = ZIO.succeed(a)
  override def fail(err: Throwable): Task[Nothing]                                         = ZIO.die(err)
  override def defer[A](thunk: => A): Task[A]                                              = ZIO.succeed(thunk)
  override def flatMapBoth[A, B](fa: Task[A])(f: Either[Throwable, A] => Task[B]): Task[B] = fa.either.flatMap(f)
  override def fromFuture[A](f: => scala.concurrent.Future[A]): Task[A]                    = ZIO.fromFuture(_ => f)
  override def fork[A](fa: => Task[A]): Task[Unit]                                         = fa.fork.unit

  private[besom] override def unsafeRunSync[A](fa: Task[A]): Either[Throwable, A] =
    Unsafe.unsafe { implicit unsafe =>
      zio.Runtime.default.unsafe.run(fa).toEither
    }
