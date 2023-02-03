package besom

import besom.internal.*
import scala.concurrent.*

trait FutureMonadModule extends BesomModule:
  override final type Eff[+A] = scala.concurrent.Future[A]
  given ExecutionContext = scala.concurrent.ExecutionContext.global
  given Runtime[Future]  = FutureRuntime()

  given Result.ToFuture[Eff] = new Result.ToFuture[Future]:
    def eval[A](fa: => Future[A]): () => Future[A] = () => fa

  // override def run(program: Context ?=> Output[Outputs]): Future[Unit] = ???

object Pulumi extends FutureMonadModule
export Pulumi.*
