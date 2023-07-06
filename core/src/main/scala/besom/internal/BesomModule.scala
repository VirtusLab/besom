package besom.internal

import besom.internal.logging.{LocalBesomLogger => logger, BesomLogger}

trait BesomModule:
  type Eff[+A]

  given rt: Runtime[Eff]

  type Outputs = Map[String, Output[Any]]

  object Output extends OutputFactory

  def run(program: Context ?=> Output[Outputs]): Unit =
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
      userOutputs    <- program(using ctx).getValueOrElse(Map.empty) // TODO register outputs!!!
      _              <- ctx.waitForAllTasks
    yield ()

    rt.unsafeRunSync(everything.run(using rt)) match
      case Left(err) => throw err
      case Right(_)  => sys.exit(0)

  def exports(outputs: (String, Output[Any])*)(using Context): Map[String, Output[Any]] = outputs.toMap
