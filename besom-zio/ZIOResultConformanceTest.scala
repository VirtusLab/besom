//> using target.scope "test"

package besom.zio

import besom.internal.*
import zio.{Runtime => ZRuntime, *}

given RunResult[Task] = new RunResult[Task]:
  given rt: Runtime[Task] = ZIORuntime()(using zio.Runtime.default)
  def run[A](result: Result[A]): A =
    Unsafe.unsafe { implicit unsafe =>
      zio.Runtime.default.unsafe.run(result.run).getOrThrow()
    }

class ZIOResultConformanceTest extends ResultSpec[Task]
