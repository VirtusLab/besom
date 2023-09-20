package besom.cats

import cats.effect.{Fiber => CatsFiber, *}
import besom.internal.*
import cats.effect.unsafe.IORuntime
import cats.effect.kernel.Outcome.*
import scala.concurrent.duration.*

// TODO it would be good to make effects uncancelable
class CatsRuntime(val debugEnabled: Boolean = false)(using ioRuntime: IORuntime) extends Runtime[IO]:
  override def pure[A](a: A): IO[A]                                                  = IO(a)
  override def fail(err: Throwable): IO[Nothing]                                     = IO.raiseError(err)
  override def defer[A](thunk: => A): IO[A]                                          = IO(thunk)
  override def flatMapBoth[A, B](fa: IO[A])(f: Either[Throwable, A] => IO[B]): IO[B] = fa.attempt.flatMap(f)
  override def fromFuture[A](f: => scala.concurrent.Future[A]): IO[A]                = IO.fromFuture(IO(f))
  override def blocking[A](thunk: => A): IO[A]                                       = IO.blocking(thunk)
  override def fork[A](fa: => IO[A]): IO[Fiber[A]] =
    for
      promise <- Deferred[IO, Either[Throwable, A]]
      fib <- fa.start.map(catsFib =>
        new Fiber[A]:
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

  override def sleep[A](fa: => IO[A], duration: Long): IO[A] = fa.delayBy(duration.millis)

  private[besom] override def unsafeRunSync[A](fa: IO[A]): Either[Throwable, A] =
    fa.uncancelable.attempt.unsafeRunSync()

trait CatsEffectModule extends BesomModule:
  import scala.concurrent.*
  override final type Eff[+A] = IO[A]

  def ioRuntime: IORuntime = cats.effect.unsafe.IORuntime.global

  protected lazy val rt: Runtime[Eff] = CatsRuntime()(using ioRuntime)

  implicit val toFutureCatsEffectIO: Result.ToFuture[Eff] = new Result.ToFuture[IO]:
    def eval[A](fa: => IO[A]): () => Future[A] = () => fa.uncancelable.unsafeToFuture()(using ioRuntime)

  // override def run(program: Context ?=> Output[Exports]): Future[Unit] = ???

object Pulumi extends CatsEffectModule
export Pulumi.{ *, given }
