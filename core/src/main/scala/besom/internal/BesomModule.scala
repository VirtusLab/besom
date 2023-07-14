package besom.internal

import com.google.protobuf.struct.*
import besom.internal.logging.{LocalBesomLogger => logger, BesomLogger}
import besom.util.NonEmptyString
import besom.util.Types.ResourceType

trait BesomModule:
  type Eff[+A]

  given rt: Runtime[Eff]

  object Output extends OutputFactory

  def run(program: Context ?=> Output[Exports]): Unit =
    val everything: Result[Unit] = for
      _              <- BesomLogger.setupLogger()
      runInfo        <- RunInfo.fromEnv
      _              <- logger.info(s"Besom starting up in ${if runInfo.dryRun then "dry run" else "live"} mode.")
      taskTracker    <- TaskTracker()
      monitor        <- Monitor(runInfo.monitorAddress)
      engine         <- Engine(runInfo.engineAddress)
      _              <- logger.info(s"Established connections to monitor and engine, spawning streaming pulumi logger.")
      logger         <- BesomLogger(engine, taskTracker)
      config         <- Config(runInfo.project)
      featureSupport <- FeatureSupport(monitor)
      _              <- logger.info(s"Resolved feature support, spawning context and executing user program.")
      ctx            <- Context(runInfo, taskTracker, monitor, engine, logger, featureSupport, config)
      userOutputs    <- program(using ctx).map(_.toResult).getValueOrElse(Result.pure(Struct()))
      -              <- Stack.registerStackOutputs(runInfo, userOutputs)(using ctx)
      _              <- ctx.waitForAllTasks
      _              <- ctx.close
    yield ()

    rt.unsafeRunSync(everything.run(using rt)) match
      case Left(err) => throw err
      case Right(_)  => sys.exit(0)

  def isDryRun(using ctx: Context): Boolean = ctx.isDryRun

  def log(using ctx: Context): BesomLogger = ctx.logger

  def urn(using ctx: Context): Output[String] =
    besom.internal.Output.ofData(ctx.getParentURN.map(OutputData(_)))

  val exports: Export.type = Export

  def component[A <: ComponentResource & Product: RegistersOutputs](name: NonEmptyString, typ: ResourceType)(
    f: Context ?=> ComponentBase ?=> Output[A]
  )(using ctx: Context): Output[A] =
    besom.internal.Output.ofData {
      ctx
        .registerComponentResource(name, typ)
        .flatMap { componentBase =>
          val urnRes: Result[String] = componentBase.urn.getValueOrFail {
            s"Urn for component resource $name is not available. This should not happen."
          }

          val componentContext = ComponentContext(ctx, urnRes)
          val componentOutput  = f(using componentContext)(using componentBase)

          componentOutput
            .getValueOrFail {
              "Component resource is not available. This should not happen."
            }
            .flatMap { a =>
              val serializedOutputs = RegistersOutputs[A].serializeOutputs(a)
              ctx.registerResourceOutputs(name, typ, urnRes, serializedOutputs) *> Result.pure(a)
            }
        }
        .map(OutputData(_))
    }
