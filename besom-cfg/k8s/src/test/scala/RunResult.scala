package besom.internal

import scala.concurrent._, duration._

trait RunResult[F[+_]]:
  def run[A](result: Result[A]): A

object RunResult:
  given RunResult[Future] = new RunResult[Future]:
    given ExecutionContext = ExecutionContext.fromExecutorService(
      null, // FJP does seem to swallow fatals
      (t: Throwable) =>
        // TODO this has to contain a link to github issue tracker to allow user to easily create a bug report, this is EXTREMELY IMPORTANT
        scribe.error("Uncaught fatal error in Future Runtime", t)
        t.printStackTrace()
        sys.exit(1)
    )
    given Runtime[Future] = FutureRuntime()
    def run[A](result: Result[A]): A = Await.result(result.run, Duration.Inf)

  extension [F[+_], A](result: Result[A])(using rr: RunResult[F]) def unsafeRunSync(): A = rr.run(result)
