package besom.internal

import com.google.protobuf.struct.*
import besom.util.*
import besom.types.{ResourceType, FunctionToken, URN, Label, ProviderType}
import besom.internal.logging.*
import scala.annotation.implicitNotFound

case class InvokeOptions(parent: Option[Resource] = None, provider: Option[ProviderResource] = None, version: Option[String] = None)

@implicitNotFound(s"""|Pulumi code has to be written with a Context in scope.
                      |
                      |Context is available by default in your main pulumi function, inside of `Pulumi.run`.
                      |NOTE: Every pulumi program should only have ONE `Pulumi.run` call.
                      |
                      |If you are writing code outside of `Pulumi.run`, you can pass a Context explicitly.
                      |This can be done by just adding a `(using Context)` clause to your function.""".stripMargin)
trait Context extends TaskTracker:
  private[besom] def initializeStack: Result[Unit]
  private[besom] def featureSupport: FeatureSupport
  private[besom] def resources: Resources
  private[besom] def runInfo: RunInfo
  private[besom] def monitor: Monitor
  private[besom] def memo: Memo
  private[besom] def getParentURN: Result[URN]
  private[besom] def getParent: Option[Resource]
  private[besom] def config: Config
  private[besom] def isDryRun: Boolean
  private[besom] def logger: BesomLogger
  private[besom] def pulumiOrganization: Option[NonEmptyString]
  private[besom] def pulumiProject: NonEmptyString
  private[besom] def pulumiStack: NonEmptyString

  /** This is only called by user's component constructor functions. see [[BesomSyntax#component]].
    */
  private[besom] def registerComponentResource(
    name: NonEmptyString,
    typ: ResourceType,
    options: ComponentResourceOptions
  )(using Context): Result[ComponentBase]

  /** This is only called by codegened remote component constructor functions.
    */
  private[besom] def registerRemoteComponentResource[R <: RemoteComponentResource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ComponentResourceOptions
  )(using Context): Output[R]

  /** This is only called by codegened provider constructor functions.
    */
  private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
    typ: ProviderType,
    name: NonEmptyString,
    args: A,
    options: CustomResourceOptions
  )(using Context): Output[R]

  /** This is called by codegened custom resource & stack reference constructor functions, get functions and rehydrated resources decoders.
    */
  private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  )(using Context): Output[R]

  /** This is only called by Component API, see [[BesomSyntax#component]]
    */
  private[besom] def registerResourceOutputs(
    name: NonEmptyString,
    typ: ResourceType,
    urnResult: Result[URN],
    outputs: Result[Struct]
  )(using Context): Result[Unit]

  /** This is only called by codegened functions.
    */
  private[besom] def invoke[A: ArgsEncoder, R: Decoder](
    token: FunctionToken,
    args: A,
    opts: InvokeOptions
  )(using Context): Output[R]

  /** This is only called by codegened methods.
    */
  private[besom] def call[A: ArgsEncoder, R: Decoder, T <: Resource](
    token: FunctionToken,
    args: A,
    resource: T,
    opts: InvokeOptions
  )(using Context): Output[R]

end Context

class ComponentContext(
  private val globalContext: Context,
  private val componentURN: Result[URN],
  private val componentBase: ComponentBase
) extends Context:
  export globalContext.{getParentURN => _, getParent => _, *}

  def getParentURN: Result[URN] = componentURN

  // components provide themselves as the parent to facilitate provider inheritance
  def getParent: Option[Resource] = Some(componentBase)

class ContextImpl(
  private[besom] val runInfo: RunInfo,
  private[besom] val featureSupport: FeatureSupport,
  private[besom] val config: Config,
  private[besom] val logger: BesomLogger,
  private[besom] val monitor: Monitor,
  private[besom] val engine: Engine,
  private[besom] val taskTracker: TaskTracker,
  private[besom] val resources: Resources,
  private[besom] val memo: Memo,
  private val stackPromise: Promise[StackResource]
) extends Context
    with TaskTracker:

  export taskTracker.{registerTask, waitForAllTasks, fail}

  override private[besom] def isDryRun: Boolean = runInfo.dryRun

  override private[besom] def pulumiOrganization: Option[NonEmptyString] = runInfo.organization
  override private[besom] def pulumiProject: NonEmptyString              = runInfo.project
  override private[besom] def pulumiStack: NonEmptyString                = runInfo.stack

  override private[besom] def getParentURN: Result[URN] =
    stackPromise.get.flatMap(_.urn.getData(using this)).map(_.getValue).flatMap {
      case Some(urn) => Result.pure(urn)
      case None      => Result.fail(Exception("Stack urn is not available. This should not happen."))
    }

  // top level Context does not return a parent (stack is the top level resource and it's providers are default provider instances)
  override private[besom] def getParent: Option[Resource] = None

  override private[besom] def initializeStack: Result[Unit] =
    StackResource.initializeStack(runInfo)(using this).flatMap(stackPromise.fulfill)

  override private[besom] def registerComponentResource(
    name: NonEmptyString,
    typ: ResourceType,
    options: ComponentResourceOptions
  )(using Context): Result[ComponentBase] =
    val label = Label.fromNameAndType(name, typ)

    BesomMDC(Key.LabelKey, label) {
      ResourceOps().readOrRegisterResourceInternal[ComponentBase, EmptyArgs](
        typ,
        name,
        EmptyArgs(),
        options, // TODO pass initial ResourceTransformations here
        remote = false // all user components are local components
      )
    }

  override private[besom] def registerRemoteComponentResource[R <: RemoteComponentResource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ComponentResourceOptions
  )(using Context): Output[R] =
    val label = Label.fromNameAndType(name, typ)

    BesomMDC(Key.LabelKey, label) {
      Output.ofData {
        ResourceOps()
          .readOrRegisterResourceInternal[R, A](
            typ,
            name,
            args,
            options, // TODO pass initial ResourceTransformations here
            remote = true // all codegened components are remote components
          )
          .map(OutputData(_))
      }
    }

  override private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
    typ: ProviderType,
    name: NonEmptyString,
    args: A,
    options: CustomResourceOptions
  )(using Context): Output[R] =
    BesomMDC(Key.LabelKey, Label.fromNameAndType(name, typ)) {
      Output.ofData(ResourceOps().readOrRegisterResourceInternal[R, A](typ, name, args, options, remote = false).map(OutputData(_)))
    }

  override private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  )(using Context): Output[R] =
    BesomMDC(Key.LabelKey, Label.fromNameAndType(name, typ)) {
      Output.ofData(ResourceOps().readOrRegisterResourceInternal[R, A](typ, name, args, options, remote = false).map(OutputData(_)))
    }

  override private[besom] def registerResourceOutputs(
    name: NonEmptyString,
    typ: ResourceType,
    urnResult: Result[URN],
    outputs: Result[Struct]
  )(using Context): Result[Unit] =
    BesomMDC(Key.LabelKey, Label.fromNameAndType(name, typ)) {
      ResourceOps().registerResourceOutputsInternal(urnResult, outputs)
    }

  override private[besom] def invoke[A: ArgsEncoder, R: Decoder](
    token: FunctionToken,
    args: A,
    opts: InvokeOptions
  )(using Context): Output[R] =
    BesomMDC(Key.LabelKey, Label.fromFunctionToken(token)) {
      ResourceOps().invokeInternal[A, R](token, args, opts)
    }

  override private[besom] def call[A: ArgsEncoder, R: Decoder, T <: Resource](
    token: FunctionToken,
    args: A,
    resource: T,
    opts: InvokeOptions
  )(using Context): Output[R] =
    BesomMDC(Key.LabelKey, Label.fromFunctionToken(token)) {
      ResourceOps().callInternal[A, R](token, args, resource, opts)
    }

end ContextImpl

object Context:
  def apply()(using Context): Context = summon[Context]

  def create(
    runInfo: RunInfo,
    featureSupport: FeatureSupport,
    config: Config,
    logger: BesomLogger,
    monitor: Monitor,
    engine: Engine,
    taskTracker: TaskTracker,
    resources: Resources,
    memo: Memo,
    stackPromise: Promise[StackResource]
  ): Context =
    new ContextImpl(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, resources, memo, stackPromise)

  def apply(
    runInfo: RunInfo,
    featureSupport: FeatureSupport,
    config: Config,
    logger: BesomLogger,
    monitor: Monitor,
    engine: Engine,
    taskTracker: TaskTracker
  ): Result[Context] =
    for
      resources    <- Resources()
      memo         <- Memo()
      stackPromise <- Promise[StackResource]()
    yield Context.create(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, resources, memo, stackPromise)

  def apply(
    runInfo: RunInfo,
    taskTracker: TaskTracker,
    monitor: Monitor,
    engine: Engine,
    logger: BesomLogger,
    featureSupport: FeatureSupport,
    config: Config
  ): Result[Context] =
    for
      ctx <- apply(runInfo, featureSupport, config, logger, monitor, engine, taskTracker)
      _   <- ctx.initializeStack
    yield ctx
end Context
