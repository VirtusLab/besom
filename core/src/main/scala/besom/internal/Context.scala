package besom.internal

import besom.util.*, Types.*
import besom.internal.logging.*

case class InvokeOptions()

type Providers = Map[String, ProviderResource]

class Context(
  private[besom] val runInfo: RunInfo,
  private[besom] val featureSupport: FeatureSupport,
  val config: Config,
  val logger: BesomLogger,
  private[besom] val monitor: Monitor,
  private[besom] val engine: Engine,
  private[besom] val taskTracker: TaskTracker,
  private[besom] val stackPromise: Promise[Stack],
  private[besom] val resources: Resources
) extends TaskTracker:

  val projectName: NonEmptyString = runInfo.project
  val stackName: NonEmptyString   = runInfo.stack

  export taskTracker.{registerTask, waitForAllTasks}
  export resources.getStateFor

  private[besom] def isDryRun: Boolean = runInfo.dryRun

  private[besom] def getStack: Result[Stack] = stackPromise.get

  private[besom] def initializeStack: Result[Unit] =
    val rootPulumiStackName = projectName +++ "-" +++ stackName

    given Context = this

    val label = Label.fromNameAndType(rootPulumiStackName, Stack.RootPulumiStackTypeName)

    MDC(Key.LabelKey, label) {
      for
        stack <- ResourceOps().registerResourceInternal[Stack, EmptyArgs](
          Stack.RootPulumiStackTypeName,
          rootPulumiStackName,
          EmptyArgs(),
          ComponentResourceOptions(using this)() // TODO pass initial ResourceTransformations here
        )
        _ <- registerResourceOutputsInternal()
        _ <- stackPromise.fulfill(stack)
      yield ()
    }

  private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
    typ: ProviderType,
    name: NonEmptyString,
    args: A,
    options: CustomResourceOptions
  ): Output[R] =
    given Context = this

    MDC(Key.LabelKey, Label.fromNameAndType(name, typ)) {
      Output(ResourceOps().registerResourceInternal[R, A](typ, name, args, options).map(OutputData(_)))
    }

  private[besom] def registerResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  ): Output[R] =
    given Context = this
    MDC(Key.LabelKey, Label.fromNameAndType(name, typ)) {
      Output(ResourceOps().registerResourceInternal[R, A](typ, name, args, options).map(OutputData(_)))
    }

  private[besom] def readResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  ): Output[R] = ???
  // summon[ResourceDecoder[R]].makeFulfillable(using this) match
  //  case (r, fulfillable) =>

  private[besom] def close: Result[Unit] =
    for
      _ <- monitor.close()
      _ <- engine.close()
    yield ()

  // TODO move out to ops
  private[besom] def registerResourceOutputsInternal(): Result[Unit] = Result.unit // TODO

  private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  ): Output[R] =
    // val effect: Output[R] = ???
    // registerResourceCreation(typ, name, effect) // put into ConcurrentHashMap eagerly!
    // effect
    ???

object Context:

  def apply(
    runInfo: RunInfo,
    featureSupport: FeatureSupport,
    config: Config,
    logger: BesomLogger,
    monitor: Monitor,
    engine: Engine,
    taskTracker: TaskTracker,
    stackPromise: Promise[Stack],
    resources: Resources
  ): Context =
    new Context(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, stackPromise, resources)

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
      stack     <- Promise[Stack]()
      resources <- Resources()
    yield apply(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, stack, resources)

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
