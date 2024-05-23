package besom.internal

import scala.concurrent.Future

trait RunOutput[F[+_]]:
  def run[A](output: Output[A]): Option[A]

object RunOutput:
  given runOutputForFuture(using RunResult[Future]): RunOutput[Future] = new RunOutput[Future]:
    def run[A](output: Output[A]): Option[A] = RunResult.unsafeRunSync(output.getData)().getValue

  extension [F[+_], A](output: Output[A])(using ro: RunOutput[F]) def unsafeRunSync(): Option[A] = ro.run(output)
  extension [F[+_], A](result: Result[A])(using rr: RunResult[F]) def unsafeRunSync(): A         = rr.run(result)
