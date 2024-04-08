package besom

import besom.internal.*
import scala.concurrent.*

/** A module that provides a runtime for the [[scala.concurrent.Future]] monad.
  */
trait FutureMonadModule extends BesomModule:
  override final type Eff[+A] = scala.concurrent.Future[A]
  given ExecutionContext = ExecutionContext.fromExecutorService(
    null, // FJP does seem to swallow fatals
    (t: Throwable) =>
      // TODO this has to contain a link to github issue tracker to allow user to easily create a bug report, this is EXTREMELY IMPORTANT
      scribe.error("Uncaught fatal error in Future Runtime", t)
      t.printStackTrace()
      sys.exit(1)
  )
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
export Pulumi.{component => _, *, given}

import scala.reflect.Typeable

// this proxy is only necessary due to https://github.com/scala/scala3/issues/17930
/** Creates a new component resource.
  *
  * @param name
  *   The unique name of the resource.
  * @param typ
  *   The type of the resource.
  * @param opts
  *   A bag of options that control this resource's behavior.
  * @param f
  *   The function that will create the component resource.
  * @tparam A
  *   The type of the component resource.
  * @return
  *   The component resource.
  */
def component[A <: ComponentResource & Product: RegistersOutputs: Typeable](using ctx: Context)(
  name: NonEmptyString,
  typ: ResourceType,
  opts: ComponentResourceOptions = ComponentResourceOptions()
)(
  f: Context ?=> ComponentBase ?=> A
): Output[A] = Pulumi.component(name, typ, opts)(f)
