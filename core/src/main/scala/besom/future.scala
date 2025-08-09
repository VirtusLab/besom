package besom

import besom.internal.*
import scala.concurrent.*

/** A module that provides a runtime for the [[scala.concurrent.Future]] monad.
  */
trait FutureMonadModule extends BesomModule:
  override final type Eff[+A] = scala.concurrent.Future[A]
  given ExecutionContext = ExecutionContext.fromExecutorService(
    null, // global Future EC reports fatals and uncaught exceptions to stderr, we want to quit on fatals
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
  * `Pulumi.run` is the entry point for your Pulumi program. Inside the `Pulumi.run` a `besom.Context` is available, which is used by
  * methods that provide metadata about the current Pulumi stack. These methods are:
  *   - [[besom.internal.BesomSyntax.isDryRun]] - is the current run a dry run (planning instead of deployment)
  *   - [[besom.internal.BesomSyntax.config]] - provides access to the configuration and secrets
  *   - [[besom.internal.BesomSyntax.log]] - all your logging needs
  *   - [[besom.internal.BesomSyntax.urn]] - the URN of the current Pulumi stack
  *   - [[besom.internal.BesomSyntax.pulumiOrganization]] - the organization of the current Pulumi stack
  *   - [[besom.internal.BesomSyntax.pulumiProject]] - the project name of the current Pulumi stack
  *   - [[besom.internal.BesomSyntax.pulumiStack]] - the stack name of the current Pulumi stack
  *
  * Inside `Pulumi.run` block you can use all of the abovemethods without the `Pulumi.` prefix. Should you need to use one of these methods
  * outside of the `Pulumi.run` block, you can use the following using clauses:
  *   - `(using Context)` if you have a `import besom.*` clause on top of the file or
  *   - `(using besom.Context)` using a fully qualified name of the type.
  *
  * There are also two other functions exported by [[besom.Pulumi]]:
  *   - [[besom.internal.BesomModule.component]] - creates a new component resource
  *   - [[besom.internal.BesomModule.opts]] - shortcut function allowing for uniform resource options syntax
  *
  * The hello world example:
  *
  * {{{
  * import besom.*
  *
  * @main def main = Pulumi.run {
  *   val message = log.warn("Nothing's here yet, it's waiting for you to write some code!")
  *   Stack(dependsOn = message)
  * }
  * }}}
  *
  * Notice that Besom is purely functional and therefore evaluation is lazy - you need to reference all the `Output`s you want evaluated. In
  * the above example, we reference the `Output[Unit]` returned by the `log.warn` method as one of the dependencies of the `Stack` resource.
  * Should we not do this, the logging statement will not be evaluated (it will be a no-op).
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
def component[A <: ComponentResource & Product: RegistersOutputs: Typeable](
  name: NonEmptyString,
  typ: ResourceType,
  opts: ComponentResourceOptions = ComponentResourceOptions()
)(
  f: Context ?=> ComponentBase ?=> A
)(using ctx: Context): Output[A] = Pulumi.component(name, typ, opts)(f)
