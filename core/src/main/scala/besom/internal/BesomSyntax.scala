package besom.internal

import scala.reflect.Typeable
import besom.util.NonEmptyString
import besom.types.ResourceType
import besom.types.URN

/** This trait is the main export point that exposes Besom specific functions and types to the user. The only exception is the [[Output]]
  * object which is exposed in [[BesomModule]] which extends this trait.
  * @see
  *   [[besom.Pulumi]]
  * @see
  *   [[besom.internal.BesomModule]]
  * @see
  *   [[besom.internal.EffectBesomModule]]
  */
trait BesomSyntax:

  /** A dry run is a program evaluation for purposes of planning, instead of performing a true deployment.
    * @param ctx
    *   the Besom context
    * @return
    *   true if the current run is a dry run
    */
  def isDryRun(using ctx: Context): Boolean = ctx.isDryRun

  /** Returns the current project configuration.
    * @param ctx
    *   the Besom context
    * @return
    *   the current project [[Config]] instance
    */
  def config(using ctx: Context): Config = ctx.config

  /** Returns the current project logger.
    * @param ctx
    *   the Besom context
    * @return
    *   the current project [[besom.aliases.Logger]] instance
    */
  def log(using ctx: Context): besom.aliases.Logger =
    besom.internal.logging.UserLoggerFactory(using ctx)

  /** The current project [[besom.types.URN]]
    * @param ctx
    *   the Besom context
    * @return
    *   the current project [[besom.types.URN]] instance
    */
  def urn(using ctx: Context): Output[URN] =
    Output.ofData(ctx.getParentURN.map(OutputData(_)))

  /** @param ctx
    *   the Besom context
    * @return
    *   the organization of the current Pulumi stack.
    */
  def pulumiOrganization(using ctx: Context): Option[NonEmptyString] = ctx.pulumiOrganization

  /** @param ctx
    *   the Besom context
    * @return
    *   the project name of the current Pulumi stack.
    */
  def pulumiProject(using ctx: Context): NonEmptyString = ctx.pulumiProject

  /** @param ctx
    *   the Besom context
    * @return
    *   the stack name of the current Pulumi stack.
    */
  def pulumiStack(using ctx: Context): NonEmptyString = ctx.pulumiStack

  /** The [[Export]] instance that exposes [[besom.aliases.Output]] instances as Pulumi Stack outputs.
    *
    * All arguments of `exports(...)` must be explicitly named, because [[besom.internal.Exports]] are dynamic, e.g.:
    *
    * {{{
    * import besom.*
    * import besom.api.aws
    *
    * @main def run = Pulumi.run {
    *   for
    *     bucket <- aws.s3.Bucket("my-bucket")
    *   yield exports(
    *     bucketUrl = bucket.websiteEndpoint
    *   )
    * }
    * }}}
    */
  val exports: Export.type = Export

  /** Creates a new component resource.
    * @param name
    *   a unique resource name for this component
    * @param typ
    *   the Pulumi [[ResourceType]] of the component resource
    * @param f
    *   the block of code that defines all the resources that should belong to the component
    * @param ctx
    *   the Besom context
    * @tparam A
    *   the type of the component resource
    * @return
    *   the component resource instance
    */
  def component[A <: ComponentResource & Product: RegistersOutputs: Typeable](name: NonEmptyString, typ: ResourceType)(
    f: Context ?=> ComponentBase ?=> A | Output[A]
  )(using ctx: Context): Output[A] =
    Output.ofData {
      ctx
        .registerComponentResource(name, typ)
        .flatMap { componentBase =>
          val urnRes: Result[URN] = componentBase.urn.getValueOrFail {
            s"Urn for component resource $name is not available. This should not happen."
          }

          val componentContext = ComponentContext(ctx, urnRes)
          val componentOutput =
            try
              f(using componentContext)(using componentBase) match
                case output: Output[A] @unchecked => output
                case a: A                         => Output(Result.pure(a))
            catch case e: Exception => Output(Result.fail(e))

          val componentResult = componentOutput.getValueOrFail {
            s"Component resource $name of type $typ did not return a value. This should not happen."
          }

          componentResult.flatMap { a =>
            val serializedOutputs = RegistersOutputs[A].serializeOutputs(a)
            ctx.registerResourceOutputs(name, typ, urnRes, serializedOutputs) *> Result.pure(a)
          }
        }
        .map(OutputData(_))
    }
