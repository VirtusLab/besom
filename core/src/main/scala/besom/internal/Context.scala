package besom.internal

import com.google.protobuf.struct.*
import besom.util.*, Types.*
import besom.internal.logging.*
import scala.annotation.implicitNotFound
import besom.internal.ComponentResource

case class InvokeOptions()

type Providers = Map[String, ProviderResource]

@implicitNotFound("TODO write a good message here") // TODO
trait Context extends TaskTracker:
  private[besom] def initializeStack: Result[Unit]
  private[besom] def featureSupport: FeatureSupport
  private[besom] def resources: Resources
  private[besom] def runInfo: RunInfo
  private[besom] def monitor: Monitor
  private[besom] def getParentURN: Result[String]

  private[besom] def isDryRun: Boolean
  private[besom] def logger: BesomLogger

  private[besom] def registerComponentResource[A <: ComponentResource: ResourceDecoder](
    name: NonEmptyString,
    typ: ResourceType
  ): Result[A]

  private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
    typ: ProviderType,
    name: NonEmptyString,
    args: A,
    options: CustomResourceOptions
  ): Output[R]
  private[besom] def registerResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  ): Output[R]
  private[besom] def readResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  ): Output[R]
  private[besom] def registerResourceOutputs(
    urnResult: Result[String],
    outputs: Map[String, Result[(Set[Resource], Value)]]
  ): Result[Unit]

  private[besom] def close: Result[Unit]

class ComponentContext(private val globalContext: Context, private val componentURN: Result[String]) extends Context:
  export globalContext.{getParentURN => _, *}

  def getParentURN: Result[String] = componentURN

class ContextImpl(
  private[besom] val runInfo: RunInfo,
  private[besom] val featureSupport: FeatureSupport,
  val config: Config,
  val logger: BesomLogger,
  private[besom] val monitor: Monitor,
  private[besom] val engine: Engine,
  private[besom] val taskTracker: TaskTracker,
  private[besom] val stackPromise: Promise[Stack],
  private[besom] val resources: Resources
) extends Context
    with TaskTracker:

  val projectName: NonEmptyString = runInfo.project
  val stackName: NonEmptyString   = runInfo.stack

  export taskTracker.{registerTask, waitForAllTasks}

  private[besom] def isDryRun: Boolean = runInfo.dryRun

  private[besom] def getParentURN: Result[String] =
    stackPromise.get.flatMap(_.urn.getData).map(_.getValue).flatMap {
      case Some(urn) => Result.pure(urn)
      case None      => Result.fail(Exception("Stack urn is not available. This should not happen."))
    }

  private[besom] def initializeStack: Result[Unit] =
    val rootPulumiStackName = projectName +++ "-" +++ stackName
    val typ                 = Stack.RootPulumiStackTypeName

    for
      stack <- registerComponentResource[Stack](rootPulumiStackName, typ)
      _     <- stackPromise.fulfill(stack)
    yield ()

  private[besom] def registerComponentResource[A <: ComponentResource: ResourceDecoder](
    name: NonEmptyString,
    typ: ResourceType
  ): Result[A] =
    given Context = this

    val label = Label.fromNameAndType(name, typ)

    MDC(Key.LabelKey, label) {
      for
        comp <- ResourceOps().registerResourceInternal[A, EmptyArgs](
          typ,
          name,
          EmptyArgs(),
          ComponentResourceOptions(using this)() // TODO pass initial ResourceTransformations here
        )

        urn <- comp.urn.getValue
      yield comp
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

  private[besom] def registerResourceOutputs(
    urnResult: Result[String],
    outputs: Map[String, Result[(Set[Resource], Value)]]
  ): Result[Unit] =
    for _ <- Result.unit
    yield ()

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
    new ContextImpl(runInfo, featureSupport, config, logger, monitor, engine, taskTracker, stackPromise, resources)

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
