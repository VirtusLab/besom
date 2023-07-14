package besom.internal

import com.google.protobuf.struct.*
import besom.internal.logging.{LocalBesomLogger => logger, BesomLogger}
import besom.util.NonEmptyString
import besom.util.Types.ResourceType

trait BesomModule extends BesomSyntax:
  type Eff[+A]

  protected val rt: Runtime[Eff]

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
      _              <- ctx.close() // TODO fix this via finalizers issue #105
      _              <- logger.close() // TODO fix this via finalizers issue #105
    yield ()

    rt.unsafeRunSync(everything.run(using rt)) match
      case Left(err) => throw err
      case Right(_)  => sys.exit(0)
