package besom.internal

import scala.concurrent.ExecutionContext

import scala.concurrent.*

// trait MonadHelpers[F[+_]](using F: Monad[F]):
//   def sleep(durationMs: Long): F[Unit]

//   extension [A](fa: F[A]) def delay(durationMs: Long): F[A] = sleep(durationMs).flatMap(_ => fa)

// trait FutureMonadHelpers(using ec: ExecutionContext) extends MonadHelpers[Future]:
//   def sleep(durationMs: Long): Future[Unit] = Future(Thread.sleep(durationMs))
