package besom.integration

import java.io.*
import scala.sys.process.*

class LoggerTest extends munit.FunSuite {

  test("logging should work") {
    var output = ""
    val logger = ProcessLogger { line =>
      println(line)
      output += line + "\n"
    }
    val loggerTestCmd =
      """|pulumi preview --cwd integration-tests/src/test/resources/logger-example --stack integration-tests-logger-example
         |""".stripMargin
    val result = loggerTestCmd.run(logger)
    assert(output.contains("Nothing here yet. It's waiting for you!"), s"Output:\n$output\n")
    assert(result.exitValue() == 0)
  }

}
