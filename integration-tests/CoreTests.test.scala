package besom.integration.core

import besom.codegen.PackageMetadata
import besom.integration.common.*
import os.*

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
    setup = {
      val schemaName = "random"
      val result = codegen.generatePackage(PackageMetadata(schemaName, providerRandomSchemaVersion))
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
