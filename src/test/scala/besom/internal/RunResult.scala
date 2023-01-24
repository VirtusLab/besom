package besom.internal

import scala.concurrent._, duration._, ExecutionContext.Implicits.global

trait RunResult[F[+_]]:
  def run[A](result: Result[A]): A

object RunResult:
  given RunResult[Future] = new RunResult[Future]:
    given Runtime[Future]            = FutureRuntime()
    def run[A](result: Result[A]): A = Await.result(result.run, Duration.Inf)

  extension [F[+_], A](result: Result[A])(using rr: RunResult[F]) def unsafeRunSync(): A = rr.run(result)
