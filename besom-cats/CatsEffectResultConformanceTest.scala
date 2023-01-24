//> using target.scope "test"

package besom.cats

import besom.internal.*
import cats.effect.*, unsafe.IORuntime.global

given RunResult[IO] = new RunResult[IO]:
  given Runtime[IO]                = CatsRuntime()(using global)
  def run[A](result: Result[A]): A = result.run.unsafeRunSync()(using global)

class CatsEffectResultConformanceTest extends ResultSpec[IO]:
  given Runtime[IO]                = CatsRuntime()(using global)
  def run[A](result: Result[A]): A = result.run.unsafeRunSync()(using global)
