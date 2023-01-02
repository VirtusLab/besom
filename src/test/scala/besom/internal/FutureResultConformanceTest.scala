package besom.internal

import scala.concurrent._, duration._, ExecutionContext.Implicits.global

class FutureResultConformanceTest extends ResultSpec:
  given Runtime[Future]            = FutureRuntime()
  def run[A](result: Result[A]): A = Await.result(result.run, Duration.Inf)
