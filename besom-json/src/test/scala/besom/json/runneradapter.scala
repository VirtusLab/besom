import org.specs2.io.Output
import org.specs2.reporter.BufferedLineLogger

//noinspection ScalaFileName
class Spec2RunnerAdapter extends munit.FunSuite {

  test("spec2 tests") {
    runner.run(
      searchPath = os.pwd / "besom-json" / "src" / "test" / "scala",
      printer = s => println(s)
    )
  }
}

object runner extends org.specs2.runner.FilesRunner {

  import org.specs2.control.*
  import org.specs2.fp.Name
  import org.specs2.fp.syntax.*
  import org.specs2.io.DirectoryPath
  import org.specs2.main.*
  import org.specs2.runner.Runner.*
  import org.specs2.runner.SpecificationsFinder.*
  import org.specs2.runner.{ClassRunner, Runner, SpecificationsFinder}
  import org.specs2.specification.core.*
  import org.specs2.specification.process.Stats

  def printerLogger(printer: String => Unit): BufferedLineLogger with Output = new BufferedLineLogger with Output {
    protected def infoLine(msg: String): Unit    = printer("[info] " + msg)
    protected def errorLine(msg: String): Unit   = printer("[error] " + msg)
    protected def failureLine(msg: String): Unit = printer("[error] " + msg)
    protected def warnLine(msg: String): Unit    = printer("[warn] " + msg)

    override def printf(format: String, args: Any*): Unit = printer(format.format(args: _*))

    override def toString = "printerLogger"
  }

  def run(searchPath: os.Path, printer: String => Unit)(implicit
    loc: munit.Location
  ): Unit = {
    val env = Env(arguments = Arguments(), lineLogger = printerLogger(printer))

    try
      execute(run(env, searchPath), printer, env.arguments, exit = false)(env)
    finally
      env.shutdown()
  }

  def execute(action: Action[Stats], printer: String => Unit, arguments: Arguments, exit: Boolean)(env: Env)(implicit
    loc: munit.Location
  ): Unit = {
    val logging = (s: String) => Name(printer(s))

    ExecuteActions.attemptExecuteAction(action, printer)(env.specs2ExecutionEnv) match {
      case Left(t) =>
        logThrowable(t, arguments)(logging).value

      case Right((result, warnings)) =>
        result
          .fold(
            error =>
              error.fold(
                t =>
                  logUserWarnings(warnings)(logging)
                    >> logThrowable(t, arguments)(logging),
                m =>
                  logUserWarnings(warnings)(logging)
                    >> logging(m)
              ),
            ok =>
              logUserWarnings(warnings)(logging)
                >> {
                  if ok.isSuccess then Name(printer(ok.displayResults(arguments)))
                  else Name(munit.Assertions.fail(s"Failure: ${ok.displayResults(arguments)}"))
                }
          )
          .value
    }
  }

  def run(env: Env, searchPath: os.Path): Action[Stats] = {
    val specs = for {
      ss <- SpecificationsFinder
        .findSpecifications(
          basePath = DirectoryPath.unsafe(searchPath.toString)
        )
        .toAction
    } yield ss

    val args    = env.arguments
    val verbose = false
    for {
      _     <- beforeExecution(args, verbose).toAction
      ss    <- specs.map(sort(env))
      stats <- ss.toList.map(ClassRunner.report(env)).sequence
      _     <- afterExecution(ss, verbose).toAction
    } yield stats.foldMap(identity _)
  }
}
