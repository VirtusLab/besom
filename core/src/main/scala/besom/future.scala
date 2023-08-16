package besom

import besom.internal.*
import scala.concurrent.*

trait FutureMonadModule extends BesomModule:
  override final type Eff[+A] = scala.concurrent.Future[A]
  given ExecutionContext      = scala.concurrent.ExecutionContext.global
  protected lazy val rt: Runtime[Future] = FutureRuntime()

  given Result.ToFuture[Eff] = new Result.ToFuture[Future]:
    def eval[A](fa: => Future[A]): () => Future[A] = () => fa

object Pulumi extends FutureMonadModule
export Pulumi.{ *, given }
