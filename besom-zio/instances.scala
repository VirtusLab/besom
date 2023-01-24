package besom.zio

import besom.internal.*

import zio.{ZIO, Task, Queue => ZQueue, Ref => ZRef, Semaphore}
import scala.util.Try
import scala.concurrent.Future
import zio.Zippable.Out

class ZIOMonad extends Monad[Task] {

  override def eval[A](a: => A): Task[A] = ZIO.succeed(a)

  override def unit: Task[Unit] = ZIO.unit

  override def recover[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] = fa.catchAll(f)

  override def map[A, B](fa: Task[A])(f: A => B): Task[B] = fa.map(f)

  override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = fa.flatMap(f)

  // TODO uncancellable both? parZip?
  override def product[A, B](fa: => Task[A], fb: => Task[B]): Task[(A, B)] =
    fa.uninterruptible.zipPar(fb.uninterruptible)

  override def sequence[A](vec: Vector[Task[A]]): Task[Vector[A]] = ZIO.collectAll(vec.map(_.uninterruptible))

  override def error[A](err: Throwable): Task[A] = ZIO.die(err)

  override def evalEither[A](a: => Either[Throwable, A]): Task[A] = ZIO.fromEither(a)

  override def fromFuture[A](futA: => Future[A]): Task[A] = ZIO.fromFuture(_ => futA)

  override def evalTry[A](a: => Try[A]): Task[A] = ZIO.fromTry(a)

  override def fork[A](fa: => Task[A]): Task[Fiber[Task, A]] = fa.uninterruptible.fork.map { zioFib =>
    new Fiber[Task, A]:
      def join: Task[A] = zioFib.await.flatMap { exit =>
        ZIO.fromEither(exit.toEither)
      }
  }

  override def promise[A]: Task[Promise[Task, A]] =
    zio.Promise.make[Throwable, A].map { internalPromise =>
      new Promise[Task, A]:
        override def get: Task[A] = internalPromise.await

        override def fulfill(a: A): Task[Unit] =
          internalPromise.succeed(a).unit // TODO maybe fail on false?

        override def fail(t: Throwable): Task[Unit] =
          internalPromise.fail(t).unit // TODO maybe fail on false?
    }

  override def queue[A]: Task[Queue[Task, A]] = ZQueue.unbounded[A].map { internalQueue =>
    new Queue[Task, A]:
      def offer(a: A): Task[Unit] = internalQueue.offer(a).unit // TODO boolean?
      def poll: Task[A]           = internalQueue.take
      def size: Task[Int]         = internalQueue.size
      def isEmpty: Task[Boolean]  = internalQueue.isEmpty
  }

  override def ref[A](initial: => A): Task[Ref[Task, A]] =
    ZRef.make[A](initial).map { internalRef =>
      new Ref[Task, A]:
        override def get: Task[A]                  = internalRef.get
        override def update(f: A => A): Task[Unit] = internalRef.update(f)
    }

  override def workgroup: Task[WorkGroup[Task]] = Semaphore.make(Int.MaxValue).map { semaphore =>
    new WorkGroup[Task]:
      def runInWorkGroup[A](eff: => Task[A]): Task[A] = semaphore.withPermit(eff)
      def waitForAll: Task[Unit]                      = semaphore.withPermits(Int.MaxValue)(ZIO.unit)
  }

}

trait ZIOEffectMonadModule extends BesomModule:
  override final type M[+A] = zio.Task[A]
  override val F: Monad[M] = new ZIOMonad

  // def run(program: Context ?=> Output[Outputs]): Task[Unit] = ???

object Pulumi extends ZIOEffectMonadModule
export Pulumi.*
