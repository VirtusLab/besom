package besom.integration.core

import os.*

class CoreTests extends munit.FunSuite {
  val coreVersion = os.read(os.pwd / "version.txt").trim
  val deps = s"""|//> using scala "3.3.1"
                 |//> using plugin "org.virtuslab::besom-compiler-plugin:$coreVersion"
                 |//> using dep "org.virtuslab::besom-core:$coreVersion"
                 |//> using options -Werror -Wunused:all -Wvalue-discard -Wnonunit-statement
                 |""".stripMargin

  val wd = os.pwd / "integration-tests" / "resources" / "logger-example"

  def sanitizeName(name: String): String = name.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase().take(64).stripSuffix("-")
  def testToStack(name: String): String  = "tests-" + sanitizeName(name)

  val stack = FunFixture[String](
    setup = { test =>
      val stackName = testToStack(test.name)
      println(s"Test stack: ${stackName}")
      os.write.over(wd / "project.scala", deps)
      val code = os.proc("pulumi", "stack", "init", "--cwd", wd, "--stack", stackName).call().exitCode
      assert(code == 0)
      stackName
    },
    teardown = { stackName =>
      // Always gets called, even if test failed.
      val code = os.proc("pulumi", "stack", "rm", "-y", "--cwd", wd, "--stack", stackName).call().exitCode
      assert(code == 0)
      // purposely not deleting project.scala to make editing easier
    }
  )

  stack.test("SDK logging be visible in Pulumi CLI") { stackName =>
    val result = os.proc("pulumi", "preview", "--cwd", wd, "--stack", stackName).call()
    val output = result.out.text()
    assert(output.contains("Nothing here yet. It's waiting for you!"), s"Output:\n$output\n")
    assert(result.exitCode == 0)
  }
}
