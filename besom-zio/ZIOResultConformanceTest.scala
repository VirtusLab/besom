//> using target.scope "test"

package besom.zio

import besom.internal.*
import zio.{Runtime => ZRuntime, *}

class CatsEffectResultConformanceTest extends ResultSpec:
  given rt: Runtime[Task] = ZIORuntime()(using zio.Runtime.default)
  def run[A](result: Result[A]): A =
    Unsafe.unsafe { implicit unsafe =>
      zio.Runtime.default.unsafe.run(result.run).getOrThrow()
    }
