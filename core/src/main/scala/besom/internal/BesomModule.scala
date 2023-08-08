package besom.internal

import com.google.protobuf.struct.*
import besom.internal.logging.{LocalBesomLogger => logger, BesomLogger}

trait EffectBesomModule extends BesomSyntax:
  type Eff[+A]

  protected lazy val rt: Runtime[Eff]

  def run(program: Context ?=> Output[Exports]): Unit =
    val everything: Result[Unit] = Result.scoped {
      for
        _              <- BesomLogger.setupLogger()
        runInfo        <- RunInfo.fromEnv
        _              <- logger.info(s"Besom starting up in ${if runInfo.dryRun then "dry run" else "live"} mode.")
        taskTracker    <- TaskTracker()
        monitor        <- Result.resource(Monitor(runInfo.monitorAddress))(_.close())
        engine         <- Result.resource(Engine(runInfo.engineAddress))(_.close())
        _              <- logger.info(s"Established connections to monitor and engine, spawning streaming pulumi logger.")
        logger         <- Result.resource(BesomLogger(engine, taskTracker))(_.close())
        config         <- Config.forProject(runInfo.project)
        featureSupport <- FeatureSupport(monitor)
        _              <- logger.info(s"Resolved feature support, spawning context and executing user program.")
        ctx            <- Result.resource(Context(runInfo, taskTracker, monitor, engine, logger, featureSupport, config))(_.close())
        userOutputs    <- program(using ctx).map(_.toResult).getValueOrElse(Result.pure(Struct()))
        _              <- Stack.registerStackOutputs(runInfo, userOutputs)(using ctx)
        _              <- ctx.waitForAllTasks
      yield ()
    }

    rt.unsafeRunSync(everything.run(using rt)) match
      case Left(err) => throw err
      case Right(_)  => sys.exit(0)

trait BesomModule extends EffectBesomModule {
  export besom.types.{ *, given }
  export besom.util.interpolator.{ *, given }
  export besom.util.{ -> }
}
