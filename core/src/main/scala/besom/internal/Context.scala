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
  private[besom] def getParentURN: Result[URN]
  private[besom] def config: Config
  private[besom] def isDryRun: Boolean
  private[besom] def logger: BesomLogger
  private[besom] def pulumiOrganization: Option[NonEmptyString]
  private[besom] def pulumiProject: NonEmptyString
  private[besom] def pulumiStack: NonEmptyString

  private[besom] def registerComponentResource(
    name: NonEmptyString,
    typ: ResourceType
  )(using Context): Result[ComponentBase]

  private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
    typ: ProviderType,
    name: NonEmptyString,
    args: A,
    options: CustomResourceOptions
  )(using Context): Output[R]

  private[besom] def registerResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  )(using Context): Output[R]

  private[besom] def readResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  )(using Context): Output[R]

  private[besom] def registerResourceOutputs(
    name: NonEmptyString,
    typ: ResourceType,
    urnResult: Result[URN],
    outputs: Result[Struct]
  )(using Context): Result[Unit]

  private[besom] def invoke[A: ArgsEncoder, R: Decoder](
    token: FunctionToken,
    args: A,
    opts: InvokeOptions
  )(using Context): Output[R]
end Context

class ComponentContext(private val globalContext: Context, private val componentURN: Result[URN]) extends Context:
  export globalContext.{getParentURN => _, *}

  def getParentURN: Result[URN] = componentURN

class ContextImpl(
  private[besom] val runInfo: RunInfo,
  private[besom] val featureSupport: FeatureSupport,
  private[besom] val config: Config,
  private[besom] val logger: BesomLogger,
  private[besom] val monitor: Monitor,
  private[besom] val engine: Engine,
  private[besom] val taskTracker: TaskTracker,
  private[besom] val resources: Resources,
  private val stackPromise: Promise[Stack]
) extends Context
    with TaskTracker:

  export taskTracker.{registerTask, waitForAllTasks, fail}

  override private[besom] def isDryRun: Boolean = runInfo.dryRun

  override private[besom] def pulumiOrganization: Option[NonEmptyString] = runInfo.organization
  override private[besom] def pulumiProject: NonEmptyString              = runInfo.project
  override private[besom] def pulumiStack: NonEmptyString                = runInfo.stack

  override private[besom] def getParentURN: Result[URN] =
    stackPromise.get.flatMap(_.urn.getData).map(_.getValue).flatMap {
      case Some(urn) => Result.pure(urn)
      case None      => Result.fail(Exception("Stack urn is not available. This should not happen."))
    }

  override private[besom] def initializeStack: Result[Unit] =
    Stack.initializeStack(runInfo)(using this).flatMap(stackPromise.fulfill)

  override private[besom] def registerComponentResource(
    name: NonEmptyString,
    typ: ResourceType
  )(using Context): Result[ComponentBase] =
    val label = Label.fromNameAndType(name, typ)

    BesomMDC(Key.LabelKey, label) {
      ResourceOps().registerResourceInternal[ComponentBase, EmptyArgs](
        typ,
        name,
        EmptyArgs(),
        ComponentResourceOptions() // TODO pass initial ResourceTransformations here
      )
    }

  override private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
    typ: ProviderType,
    name: NonEmptyString,
    args: A,
    options: CustomResourceOptions
  )(using Context): Output[R] =
    BesomMDC(Key.LabelKey, Label.fromNameAndType(name, typ)) {
      Output.ofData(ResourceOps().registerResourceInternal[R, A](typ, name, args, options).map(OutputData(_)))
    }

  override private[besom] def registerResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  )(using Context): Output[R] =
    BesomMDC(Key.LabelKey, Label.fromNameAndType(name, typ)) {
      Output.ofData(ResourceOps().registerResourceInternal[R, A](typ, name, args, options).map(OutputData(_)))
    }

  override private[besom] def readResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  )(using Context): Output[R] = ???

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
      ResourceOps().invoke[A, R](token, args, opts)
    }
end ContextImpl

object Context:

  def apply(
    runInfo: RunInfo,
    featureSupport: FeatureSupport,
    config: Config,
    logger: BesomLogger,
    monitor: Monitor,
    engine: Engine,
    taskTracker: TaskTracker,
    resources: Resources,
    stackPromise: Promise[Stack]
  ): Context =
    new ContextImpl(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, resources, stackPromise)

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
      stackPromise <- Promise[Stack]()
    yield apply(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, resources, stackPromise)

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
