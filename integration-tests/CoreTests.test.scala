package besom.integration.core

import besom.codegen.PackageMetadata
import besom.integration.common.*
import besom.integration.common.pulumi.{FixtureArgs, FixtureOpts}
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
    val result = pulumi.up(ctx.stackName).call(cwd = ctx.programDir, env = ctx.env)
    val output = result.out.text()
    assert(output.contains("Nothing here yet. It's waiting for you!"), s"Output:\n$output\n")
    assert(output.contains("Interpolated value"), s"Output:\n$output\n")
    assert(result.exitCode == 0)
  }

  FunFixture[pulumi.FixtureContext](
    setup = pulumi.fixture.setup(
      wd / "resources" / "config-example"
    ),
    teardown = pulumi.fixture.teardown
  ).test("SDK config and secrets should work with Pulumi CLI") { ctx =>
    pulumi.config(ctx.stackName, "name", "config value").call(cwd = ctx.programDir, env = ctx.env)
    pulumi.secret(ctx.stackName, "hush").call(cwd = ctx.programDir, env = ctx.env, stdin = "secret value\n")

    pulumi.config(ctx.stackName, "--path", "names[0]", "a").call(cwd = ctx.programDir, env = ctx.env)
    pulumi.config(ctx.stackName, "--path", "names[1]", "b").call(cwd = ctx.programDir, env = ctx.env)
    pulumi.config(ctx.stackName, "--path", "names[2]", "c").call(cwd = ctx.programDir, env = ctx.env)
    pulumi.secret(ctx.stackName, "--path", "names[3]", "super secret").call(cwd = ctx.programDir, env = ctx.env)

    pulumi.config(ctx.stackName, "--path", "foo.name", "Name").call(cwd = ctx.programDir, env = ctx.env)
    pulumi.config(ctx.stackName, "--path", "foo.age", "23").call(cwd = ctx.programDir, env = ctx.env)

    val upResult = pulumi.up(ctx.stackName).call(cwd = ctx.programDir, env = ctx.env)
    println(upResult.out.text())
    assert(upResult.exitCode == 0)

    val outResult = pulumi.outputs(ctx.stackName).call(cwd = ctx.programDir, env = ctx.env)
    assert(outResult.exitCode == 0)

    val output  = outResult.out.text()
    val outputs = upickle.default.read[Map[String, ujson.Value]](output)

    assertEquals(outputs("name").str, "config value", s"Output:\n$output\n")
    assertEquals(outputs("notThere").str, "default value", s"Output:\n$output\n")
    assertEquals(outputs("codeSecret").str, "[secret]", s"Output:\n$output\n")
    assertEquals(outputs("foo").obj.mkString(","), "age -> 23,name -> \"Name\"", s"Output:\n$output\n")
    // FIXME: https://github.com/pulumi/pulumi/issues/14576
//    assertEquals(outputs("hush"),"[secret]", s"Output:\n$output\n")
//    assertEquals(outputs("names").arr.map(_.str).toList, List("a","b","c","[secret]"), s"Output:\n$output\n")

    val outResult2 = pulumi.outputs(ctx.stackName, "--show-secrets").call(cwd = ctx.programDir, env = ctx.env)
    assert(outResult.exitCode == 0)

    val output2  = outResult2.out.text()
    val outputs2 = upickle.default.read[Map[String, ujson.Value]](output2)

    assertEquals(outputs2("hush").str, "secret value", s"Output:\n$output2\n")
    assertEquals(outputs2("codeSecret").str, "secret code", s"Output:\n$output2\n")
    assertEquals(outputs2("names").arr.map(_.str).toList, List("a", "b", "c", "super secret"), s"Output:\n$output2\n")
  }

  FunFixture[pulumi.FixtureContext](
    setup = {
      val schemaName = "random"
      val result     = codegen.generatePackage(PackageMetadata(schemaName, providerRandomSchemaVersion))
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
    val result = pulumi.up(ctx.stackName).call(cwd = ctx.programDir, env = ctx.env)
    val output = result.out.text()
    assert(output.contains("randomString:"), s"Output:\n$output\n")
    assert(result.exitCode == 0)
  }

  FunFixture[pulumi.FixtureContext](
    setup = {
      val schemaName = "tls"
      val result     = codegen.generatePackage(PackageMetadata(schemaName, providerTlsSchemaVersion))
      scalaCli.publishLocal(result.outputDir).call()
      pulumi.fixture.setup(
        wd / "resources" / "tls-example",
        projectFiles = Map(
          "project.scala" ->
            (defaultProjectFile + s"""//> using dep org.virtuslab::besom-$schemaName:$providerTlsVersion""")
        )
      )
    },
    teardown = pulumi.fixture.teardown
  ).test("tls provider should work with function") { ctx =>
    val result = pulumi.up(ctx.stackName).call(cwd = ctx.programDir, env = ctx.env)
    assert(result.exitCode == 0)
  }

  FunFixture[pulumi.FixtureMultiContext](
    setup = {
      val schemaName = "tls"
      val result     = codegen.generatePackage(PackageMetadata(schemaName, providerTlsSchemaVersion))
      scalaCli.publishLocal(result.outputDir).call()
      pulumi.fixture.setup(
        FixtureOpts(),
        FixtureArgs(
          wd / "resources" / "references" / "source-stack",
          projectFiles = Map(
            "project.scala" ->
              (defaultProjectFile + s"""//> using dep org.virtuslab::besom-$schemaName:$providerTlsVersion""")
          )
        ),
        FixtureArgs(
          wd / "resources" / "references" / "target-stack",
          projectFiles = Map(
            "project.scala" ->
              (defaultProjectFile + s"""//> using dep org.virtuslab::besom-$schemaName:$providerTlsVersion""")
          )
        )
      )
    },
    teardown = pulumi.fixture.teardown
  ).test("stack outputs and references should work") {
    case pulumi.FixtureMultiContext(ctx, Vector(ctx1, ctx2)) =>
      println(s"Source stack name: ${ctx1.stackName}, pulumi home: ${ctx.home}")
      pulumi.up(ctx1.stackName).call(cwd = ctx1.programDir, env = ctx1.env)
      println(pulumi.outputs(ctx1.stackName).call(cwd = ctx1.programDir, env = ctx1.env).out.text())
      println(pulumi.stackLs().call(cwd = ctx2.programDir, env = ctx2.env, check = false).out.text())
      println(s"Target stack name: ${ctx2.stackName}, pulumi home: ${ctx.home}")
      pulumi
        .up(ctx2.stackName, "--config", s"sourceStack=organization/source-stack-test/${ctx1.stackName}")
        .call(cwd = ctx2.programDir, env = ctx2.env)

    case _ => throw new Exception("Invalid number of contexts")
  }
}
