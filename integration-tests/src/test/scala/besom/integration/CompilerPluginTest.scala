package besom.integration

import java.io.*
import scala.sys.process.*

class CompilerPluginTest extends munit.FunSuite {

  extension (errorLineCounts: Map[Int, Int])
    def showForMsg: String =
      errorLineCounts
        .toSeq
        .sortBy(_._1)
        .map { case (line, count) => s"- line: $line, count: $count" }
        .mkString("\n")

  /**
    * Returns every line in a given file that contains "// error" (with the number of occurances)
    */
  def getErrorLines(file: String): Map[Int, Int] = {
    val source = scala.io.Source.fromFile(file)
    val lines = source.getLines().toList
    source.close()
    lines.zipWithIndex
      .filter { case (line, _) => line.contains("// error") }
      .map { case (line, index) => (index + 1, line.sliding("// error".size).count(_ == "// error")) }
      .toMap
  }

  test("compilation should fail with pulumi compiler plugin when using output parameter in an s interpolator") {
    val testSourceFile = "integration-tests/src/test/resources/compilerplugintest.scala"
    val testSourceFileName = testSourceFile.split("/").last
    def containsErrorPattern(input: String, lineno: Option[Int] = None): Boolean = {
      input.contains("error") && input.contains(testSourceFileName)
        && lineno.forall { line =>
          input.contains(s"$testSourceFileName:$line")
        }
    }
    def getErrorLineFromErrorMsg(input: String): Option[Int] = {
      input
        .split("\n")
        .filter(containsErrorPattern(_))
        .headOption
        .flatMap { line =>
          val lineNo = line.split(":").reverse(1).toInt
          if (lineNo > 0) Some(lineNo) else None
        }
    }
    val lineErrors = getErrorLines(testSourceFile)
    var output = ""
    val logger = ProcessLogger { line =>
      println(line)
      output += line + "\n"
    }
    val compilePluginTestStr =
      s"""|scala-cli compile
         |$testSourceFile
         |""".stripMargin
    val result = compilePluginTestStr.!(logger)
    val allErrorsMap =
      output
        .split("\n")
        .flatMap(getErrorLineFromErrorMsg)
        .groupBy(identity)
        .map { case (line, errors) => (line, errors.size) }
    lineErrors.toSeq.foreach { (line, count) =>
      val actualErrorsCount = output
        .split("\n")
        .filter(containsErrorPattern(_, Some(line)))
        .size
      assertEquals(actualErrorsCount, count, s"Expected $count errors on line $line, got $actualErrorsCount")
    }
    assertEquals(
      allErrorsMap.values.sum,
      lineErrors.values.sum,
      s"""|Expected ${lineErrors.values.sum} errors, got ${allErrorsMap.values.sum}
          |All expected errors:
          |${lineErrors.showForMsg}
          |All encountered errors:
          |${allErrorsMap.showForMsg}
          |""".stripMargin
    )

    // assert(output.contains("is used in a default string interpolator."))
    // assert(output.contains("""val anUnusedOutput = Output("unused")"""))
    // assert(output.contains("""case out => Output("XD")"""))
  }

}
