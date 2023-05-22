package besom.internal

trait BesomModule:
  type Eff[+A]

  given rt: Runtime[Eff]

  type Outputs = Map[String, Output[Any]]

  object Output extends OutputFactory

  def run(program: Context ?=> Output[Outputs]): Unit =
    val everything: Result[Unit] = for
      ri          <- RunInfo.fromEnv
      ctx         <- Context(ri)
      userOutputs <- program(using ctx).getValueOrElse(Map.empty) // TODO register outputs!!!
      // _           <- Result.sleep(2000) // TODO DEBUG DELETE
      // _ = throw new Exception("ONIXPECTED!") // TODO DEBUG DELETE
      _ <- ctx.waitForAllTasks
    yield ()

    rt.unsafeRunSync(everything.run(using rt)) match
      case Left(err) => throw err
      case Right(_)  => sys.exit(0)

  def exports(outputs: (String, Output[Any])*)(using Context): Output[Map[String, Output[Any]]] = Output(outputs.toMap)
