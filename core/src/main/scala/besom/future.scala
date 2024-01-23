package besom

import besom.internal.*
import scala.concurrent.*

/** A module that provides a runtime for the [[scala.concurrent.Future]] monad.
  */
trait FutureMonadModule extends BesomModule:
  override final type Eff[+A] = scala.concurrent.Future[A]
  given ExecutionContext                 = scala.concurrent.ExecutionContext.global
  protected lazy val rt: Runtime[Future] = FutureRuntime()

  implicit val toFutureFuture: Result.ToFuture[Eff] = new Result.ToFuture[Future]:
    def eval[A](fa: => Future[A]): () => Future[A] = () => fa

/** Besom API entry point for the [[scala.concurrent.Future]] monad.
  *
  * All Pulumi programs are executed in Besom context [[besom.Context]]
  *
  * Most notable methods exposed by [[besom.Pulumi]] are:
  *   - [[besom.internal.BesomModule.run]] - the Pulumi program function
  *   - [[besom.internal.BesomSyntax.config]] - configuration and secrets
  *   - [[besom.internal.BesomSyntax.log]] - all your logging needs
  *
  * Inside `Pulumi.run` block you can use all methods without `Pulumi.` prefix. All functions that belong to Besom program but are defined
  * outside the `Pulumi.run` block should have the following using clause: `(using Context)` or `(using besom.Context)` using a fully
  * qualified name of the type.
  *
  * The hello world example:
  * {{{
  * import besom.*
  *
  * @main def main = Pulumi.run {
  *   val message = log.warn("Nothing's here yet, it's waiting for you to write some code!")
  *   Stack(dependsOn = message)
  * }
  * }}}
  */
object Pulumi extends FutureMonadModule
export Pulumi.{*, given}
