package besom.cats

// import besom.internal.*

// import cats.syntax.parallel.*
// import cats.effect.{Ref => CERef, Fiber => _, *}
// import cats.effect.implicits.*
// import cats.effect.std.{Queue => CEQueue, Semaphore}
// import scala.util.Try
// import scala.concurrent.Future
// import scala.collection.BuildFrom
// import cats.effect.kernel.Outcome.*

// class IOMonad extends Monad[IO]:

//   override def eval[A](a: => A): IO[A] = IO(a)

//   override def unit: IO[Unit] = IO.unit

//   override def recover[A](fa: IO[A])(f: Throwable => IO[A]): IO[A] = fa.redeemWith(f, IO.apply)

//   override def map[A, B](fa: IO[A])(f: A => B): IO[B] = fa.map(f)

//   override def flatMap[A, B](fa: IO[A])(f: A => IO[B]): IO[B] = fa.flatMap(f)

//   // TODO uncancellable both? parZip?
//   override def product[A, B](fa: => IO[A], fb: => IO[B]): IO[(A, B)] =
//     (fa.uncancelable, fb.uncancelable).parMapN((a, b) => a -> b)

//   override def sequence[A](vec: Vector[IO[A]]): IO[Vector[A]] =
//     IO.parSequenceN(Int.MaxValue)(vec.map(_.uncancelable))

//   override def error[A](err: Throwable): IO[A] = IO.raiseError[A](err)

//   override def evalEither[A](a: => Either[Throwable, A]): IO[A] = IO.fromEither(a)

//   override def fromFuture[A](futA: => Future[A]): IO[A] = IO.fromFuture(IO(futA))

//   override def evalTry[A](a: => Try[A]): IO[A] = IO.fromTry(a)

//   override def fork[A](fa: => IO[A]): IO[Fiber[IO, A]] = fa.uncancelable.start.map { catsFib =>
//     new Fiber[IO, A]:
//       def join: IO[A] = catsFib.join.flatMap {
//         case Succeeded(fa) => fa
//         case Errored(e)    => IO.raiseError(e)
//         case Canceled()    => IO.raiseError(new Exception("Unexpected cancelation!"))
//       }
//   }

//   override def promise[A]: IO[Promise[IO, A]] = Deferred[IO, Either[Throwable, A]].map { internalDeferred =>
//     new Promise[IO, A]:
//       override def get: IO[A] = internalDeferred.get.flatMap(_.fold(IO.raiseError, IO.pure))

//       override def fulfill(a: A): IO[Unit] =
//         internalDeferred.complete(Right(a)).void // TODO maybe fail on false?

//       override def fail(t: Throwable): IO[Unit] =
//         internalDeferred.complete(Left(t)).void // TODO maybe fail on false?
//   }

//   override def queue[A]: IO[Queue[IO, A]] = CEQueue.unbounded[IO, A].map { internalQueue =>
//     new Queue[IO, A]:
//       def offer(a: A): IO[Unit] = internalQueue.offer(a).void // TODO boolean?
//       def poll: IO[A]           = internalQueue.take
//       def size: IO[Int]         = internalQueue.size
//       def isEmpty: IO[Boolean]  = internalQueue.size.map(_ == 0)
//   }

//   override def ref[A](initial: => A): IO[Ref[IO, A]] = CERef.of[IO, A](initial).map { internalRef =>
//     new Ref[IO, A]:
//       override def get: IO[A]                  = internalRef.get
//       override def update(f: A => A): IO[Unit] = internalRef.update(f)
//   }

//   override def workgroup: IO[WorkGroup[IO]] = Semaphore[IO](Int.MaxValue).map { semaphore =>
//     new WorkGroup[IO]:
//       def runInWorkGroup[A](eff: => IO[A]): IO[A] = semaphore.permit.use(_ => eff)
//       def waitForAll: IO[Unit]                    = semaphore.acquireN(Int.MaxValue)
//   }

// trait CatsEffectMonadModule extends BesomModule:
//   override final type M[+A] = cats.effect.IO[A]
//   override val F: Monad[M] = new IOMonad

//   // def run(program: Context ?=> Output[Outputs]): IO[Unit] = ???

// object Pulumi extends CatsEffectMonadModule
// export Pulumi.*
