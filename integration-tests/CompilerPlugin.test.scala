package besom.integration.compilerplugin

import besom.integration.common.*

import scala.concurrent.duration.*

//noinspection ScalaFileName,TypeAnnotation
class CompilerPluginTest extends munit.FunSuite {
  override val munitTimeout = 1.minute

  test("compilation should fail with pulumi compiler plugin when using output parameter in an s interpolator") {
    val result = scalaCli
      .compile(os.pwd / "integration-tests" / "resources" / "compiler-plugin")
      .call(cwd = os.pwd, check = false, mergeErrIntoOut = true)
    val output = result.out.text()
    assert(output.contains("is used in a default string interpolator."), clue = output)
    assertEquals(result.exitCode, 1)
  }
}
