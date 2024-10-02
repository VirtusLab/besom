package besom.integration.compilerplugin

import besom.integration.common.*

import scala.concurrent.duration.*

//noinspection ScalaFileName,TypeAnnotation
class CompilerPluginTest extends munit.FunSuite {
  override val munitTimeout = 1.minute

  test("compilation should fail with pulumi compiler plugin when using output parameter in an s interpolator") {
    val path = os.pwd / "integration-tests" / "resources" / "compiler-plugin"
    pulumi.fixture.setupProject(path)
    val result = scalaCli.compile(path).call(cwd = os.pwd, check = false, mergeErrIntoOut = true)
    val output = result.out.text()
    assert(output.contains("is used in a default string interpolator."), clue = output)
    assertEquals(result.exitCode, 1)
  }
}
