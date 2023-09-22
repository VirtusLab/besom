package besom.integration

import java.io.*
import scala.sys.process.*

class CompilerPluginTest extends munit.FunSuite {

  test("compilation should fail with pulumi compiler plugin when using output parameter in an s interpolator") {
    var output = ""
    val logger = ProcessLogger { line =>
      println(line)
      output += line + "\n"
    }
    val compilePluginTestStr =
      """|scala-cli compile
         |integration-tests/src/test/resources/compilerplugintest.scala
         |""".stripMargin
    val result = compilePluginTestStr.!(logger)
    assert(output.contains("is used in a default string interpolator."))
  }

}
