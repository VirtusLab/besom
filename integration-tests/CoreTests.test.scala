package besom.integration.core

import os.*
import besom.integration.common.*

//noinspection ScalaWeakerAccess,TypeAnnotation,ScalaFileName
class CoreTests extends munit.FunSuite {
  val wd = os.pwd / "integration-tests"

  FunFixture[pulumi.FixtureContext](
    setup = pulumi.fixture.setup(
      wd / "resources" / "logger-example"
    ),
    teardown = pulumi.fixture.teardown
  ).test("SDK logging be visible in Pulumi CLI") { ctx =>
    val result = pulumi.up(ctx.stackName).call(cwd = ctx.testDir, env = ctx.env)
    val output = result.out.text()
    assert(output.contains("Nothing here yet. It's waiting for you!"), s"Output:\n$output\n")
    assert(result.exitCode == 0)
  }

  FunFixture[pulumi.FixtureContext](
    setup = pulumi.fixture.setup(
      wd / "resources" / "config-example"
    ),
    teardown = pulumi.fixture.teardown
  ).test("SDK config and secrets should work with Pulumi CLI") { ctx =>
    pulumi.config(ctx.stackName, "name", "config value").call(cwd = ctx.testDir, env = ctx.env)
    pulumi.secret(ctx.stackName, "hush").call(cwd = ctx.testDir, env = ctx.env, stdin = "secret value\n")
    val result = pulumi.up(ctx.stackName).call(cwd = ctx.testDir, env = ctx.env)
    val output = result.out.text()
    assert(output.contains("config value"), s"Output:\n$output\n")
    assert(output.contains("[secret]"), s"Output:\n$output\n")
    assert(output.contains("default value"), s"Output:\n$output\n")
    assert(result.exitCode == 0)
  }

  FunFixture[pulumi.FixtureContext](
    setup = {
      val schemaName = "random"
      val result = codegen.generatePackage(schemaName, providerRandomSchemaVersion)
      scalaCli.publishLocal(result.outputDir).call()
      pulumi.fixture.setup(
        wd / "resources" / "random-example",
        projectFiles = Map(
          "project.scala" ->
            (defaultProjectFile + s"""//> using dep org.virtuslab::besom-$schemaName:$providerRandomVersion""")
        )
      )
    },
    teardown = pulumi.fixture.teardown
  ).test("random provider and memoization should work") { ctx =>
    val result = pulumi.up(ctx.stackName).call(cwd = ctx.testDir, env = ctx.env)
    val output = result.out.text()
    assert(output.contains("randomString:"), s"Output:\n$output\n")
    assert(result.exitCode == 0)
  }
}
