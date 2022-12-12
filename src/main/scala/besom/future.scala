package besom

import internal.*
import scala.util.*

import scala.concurrent.{ExecutionContext, Future, Promise}
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import scala.collection.BuildFrom

class FutureMonad(using ec: ExecutionContext) extends Monad[Future]:

  override def eval[A](a: => A): Future[A] = Future(a)

  override def unit: Future[Unit] = Future.unit

  override def evalTry[A](a: => Try[A]): Future[A] = Future(a).flatMap {
    case Success(v) => Future.successful(v)
    case Failure(t) => Future.failed(t)
  }

  override def evalEither[A](a: => Either[Throwable, A]): Future[A] = Future(a).flatMap {
    case Right(r) => Future.successful(r)
    case Left(l)  => Future.failed(l)
  }

  override def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

  override def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

  override def product[A, B](fa: => Future[A], fb: => Future[B]): Future[(A, B)] = fa.zip(fb)

  override def sequence[A](vec: Vector[Future[A]]): Future[Vector[A]] = Future.sequence(vec)

  override def error[A](err: Throwable): Future[A] = Future.failed(err)

  override def recover[A](fa: Future[A])(f: Throwable => Future[A]): Future[A] =
    fa.recoverWith(PartialFunction.fromFunction(f))

  def fork[A](fa: => Future[A]): Future[Fiber[Future, A]] = Future.unit.map { _ =>
    new Fiber[Future, A]:
      val fut             = fa
      def join: Future[A] = fut
  }

  override def fromFuture[A](futA: => Future[A]): Future[A] = Future.unit.flatMap(_ => futA)

  override def queue[A]: Future[Queue[Future, A]] = Future.successful {
    new Queue[Future, A]:
      val internalQ                          = new ConcurrentLinkedQueue[A]
      override def offer(a: A): Future[Unit] = Future(internalQ.offer(a))
      override def poll: Future[A]           = Future(internalQ.poll())
      override def size: Future[Int]         = Future(internalQ.size())
      override def isEmpty: Future[Boolean]  = Future(internalQ.isEmpty())
  }

  override def promise[A]: Future[internal.Promise[Future, A]] = Future.successful {
    new internal.Promise[Future, A]:
      val internalPromise         = Promise[A]
      override def get: Future[A] = internalPromise.future
      override def fulfill(a: A): Future[Unit] =
        val maybeSuccess = Try(internalPromise.success(a))
        Future.fromTry(maybeSuccess.map(_ => ()))
      override def fail(t: Throwable): Future[Unit] =
        val maybeFail = Try(internalPromise.failure(t))
        Future.fromTry(maybeFail.map(_ => ()))
  }

  override def ref[A](initial: => A): F[Ref[Future, A]] = Future.successful {
    new internal.Ref[Future, A]:
      val internalRef = new AtomicReference[A](initial)

      override def get: Future[A] = Future.successful(internalRef.get)

      override def update(f: A => A): Future[Unit] = Future.successful {
        var set = false
        while (!set) do
          val a = internalRef.get
          set = internalRef.compareAndSet(a, f(a))
      }
  }

  override def workgroup: Future[WorkGroup[Future]] = Future.successful {
    new internal.WorkGroup[Future]:
      val semaphore = java.util.concurrent.Semaphore(Int.MaxValue, false)
      def runInWorkGroup[A](eff: => Future[A]): Future[A] = Future(semaphore.acquire()).flatMap { _ =>
        eff.transform { res => // Future evaluation point
          semaphore.release()
          res
        }
      }
      def waitForAll: Future[Unit] = Future(semaphore.acquire(Int.MaxValue))
  }

trait FutureMonadModule extends BesomModule:
  type F[A] = scala.concurrent.Future[A]
  given ExecutionContext = scala.concurrent.ExecutionContext.global
  given F: Monad[F]      = FutureMonad()

  // override def run(program: Context ?=> Output[Outputs]): Future[Unit] = ???

object Pulumi extends FutureMonadModule
export Pulumi.*
