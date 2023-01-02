//> using target.scope "test"

package besom.cats

import besom.internal.*
import cats.effect.*, unsafe.IORuntime.global

class CatsEffectResultConformanceTest extends ResultSpec:
  given Runtime[IO]                = CatsRuntime()(using global)
  def run[A](result: Result[A]): A = result.run.unsafeRunSync()(using global)
