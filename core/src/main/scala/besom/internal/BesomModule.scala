package besom.internal

import besom.internal.logging.{LocalBesomLogger => logger, BesomLogger}
import besom.util.NonEmptyString
import besom.util.Types.ResourceType

trait BesomModule:
  type Eff[+A]

  given rt: Runtime[Eff]

  type Outputs = Map[String, (Encoder[?], Output[Any])]

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
      userOutputs    <- program(using ctx).getValueOrElse(Map.empty)
      -              <- ctx.registerResourceOutputs(ctx.getParentURN, userOutputs) // TODO register outputs!!!
      _              <- ctx.waitForAllTasks
      _              <- ctx.close
    yield ()

    rt.unsafeRunSync(everything.run(using rt)) match
      case Left(err) => throw err
      case Right(_)  => sys.exit(0)

  def isDryRun(using ctx: Context): Boolean = ctx.isDryRun

  def log(using ctx: Context): BesomLogger = ctx.logger

  def urn(using ctx: Context): Output[String] = Output(ctx.getParentURN.map(OutputData(_)))

  def exports(outputs: (String, Output[Any])*)(using Context): Output[Map[String, Output[Any]]] = Output(outputs.toMap)

  def component[A <: ComponentResource & Product: RegistersOutputs](name: NonEmptyString, typ: ResourceType)(
    f: Context ?=> Output[A]
  )(using ctx: Context): Output[A] =
    Output {
      ctx
        .registerComponentResource[ComponentUrn](name, typ)
        .flatMap { componentURN =>
          val urnRes: Result[String] = componentURN.urn.getValue.flatMap { // TODO include getValueOrFail
            case Some(urn) => Result.pure(urn)
            case None =>
              Result.fail(Exception(s"Urn for component resource $name is not available. This should not happen."))
          }

          val componentContext = ComponentContext(ctx, urnRes)
          val componentOutput  = f(using componentContext)
          componentOutput.getValue
            .flatMap { // TODO include getValueOrFail
              case Some(component) => Result.pure(component)
              case None => Result.fail(Exception("Component resource is not available. This should not happen."))
            }
            .flatMap { a =>
              val componentOutputs = RegistersOutputs[A].toMapOfOutputs(a)
              ctx.registerResourceOutputs(urnRes, componentOutputs) *> Result.pure(a)
            }
        }
        .map(OutputData(_))
    }
