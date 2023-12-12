package besom.integration.languageplugin

import besom.codegen.PulumiPluginJSON
import besom.integration.common.*
import munit.Slow
import os.*

//noinspection ScalaWeakerAccess,TypeAnnotation,ScalaFileName
class LanguagePluginTest extends munit.FunSuite {
  override def munitTests(): Seq[Test] = super
    .munitTests()
    .filterNot(tagsWhen(envVarOpt("CI").contains("true"))(LocalOnly))
    .filterNot(tags(Slow))

  val wd                  = os.pwd / "integration-tests"
  val resourcesDir        = wd / "resources"
  val executorsDir        = resourcesDir / "executors"
  val bootstrapLibJarPath = languagePluginDir / "bootstrap.jar"

  val projectFile =
    s"""|//> using scala $scalaVersion
        |//> using options -java-output-version:$javaVersion
        |//> using plugin "org.virtuslab::besom-compiler-plugin:$coreVersion"
        |//> using dep "org.virtuslab::besom-core:$coreVersion"
        |//> using dep "org.virtuslab::besom-fake-standard-resource:1.2.3-TEST"
        |//> using dep "org.virtuslab::besom-fake-external-resource:2.3.4-TEST"
        |""".stripMargin

  val sbtBuildFile =
    s"""|lazy val root = project
        |  .in(file("."))
        |  .settings(
        |    scalaVersion := "$scalaVersion",
        |    scalacOptions ++= Seq("-java-output-version", "$javaVersion"),
        |    javacOptions in (Compile, compile) ++= Seq("-source", "$javaVersion", "-target", "$javaVersion"),
        |    libraryDependencies ++= Seq(
        |      "org.virtuslab" %% "besom-core" % "$coreVersion",
        |      "org.virtuslab" %% "besom-fake-standard-resource" % "1.2.3-TEST",
        |      "org.virtuslab" %% "besom-fake-external-resource" % "2.3.4-TEST"
        |    )
        |  )
        |""".stripMargin

  def publishLocalResourcePlugin(pluginName: String) =
    scalaCli.publishLocal(".").call(cwd = resourcesDir / pluginName)

  def publishLocalResourcePlugins() =
    publishLocalResourcePlugin("fake-standard-resource-plugin")
    publishLocalResourcePlugin("fake-external-resource-plugin")

  val expectedBootstrapPluginsJson = List(
    PulumiPluginJSON(
      resource = true,
      name = Some(
        value = "random"
      ),
      version = Some(
        value = "4.3.1"
      ),
      server = None
    ),
    PulumiPluginJSON(
      resource = true,
      name = Some(
        value = "aci"
      ),
      version = Some(
        value = "0.0.6"
      ),
      server = Some(
        value = "github://api.github.com/netascode/pulumi-aci"
      )
    )
  )

  def testExecutor(ctx: pulumi.FixtureContext, pluginsJson: String) =
    if pluginsJson.nonEmpty then
      val actual = PulumiPluginJSON.listFrom(pluginsJson)
      assert {
        clue(pluginsJson)
        clue(actual.length) == expectedBootstrapPluginsJson.length
        clue(actual) == clue(expectedBootstrapPluginsJson)
      }

    val pulumiUpOutput =
      pulumi
        .up(ctx.stackName, "--skip-preview")
        .call(cwd = ctx.programDir, env = ctx.env)
        .out
        .text()

    val expectedError = "scala executor test got executed"
    assert(clue(pulumiUpOutput).contains(expectedError))

    val aboutInfoJson: String =
      pproc("pulumi", "--non-interactive", "about", "--json").call(cwd = ctx.programDir, env = ctx.env).out.text()

    val aboutPluginsVersions: Map[String, String] = ujson
      .read(aboutInfoJson)("plugins")
      .arr
      .map(plugin => plugin("name").str -> plugin("version").strOpt.getOrElse("null"))
      .toMap

    val expectedAboutPluginsVersions = Map("scala" -> "null" /* FIXME */, "random" -> "4.3.1", "aci" -> "0.0.6")
    assert {
      clue(aboutInfoJson)
      clue(aboutPluginsVersions) == clue(expectedAboutPluginsVersions)
    }

    val pluginsLsJson =
      pproc("pulumi", "--non-interactive", "plugin", "ls", "--json").call(cwd = ctx.programDir, env = ctx.env).out.text()

    val installedPluginsVersions = ujson
      .read(pluginsLsJson)
      .arr
      .map(plugin => plugin("name").str -> plugin("version").str)
      .toMap

    val expectedInstalledPluginsVersions = Map("scala" -> scalaPluginVersion, "random" -> "4.3.1", "aci" -> "0.0.6")

    assert {
      clue(pluginsLsJson)
      clue(installedPluginsVersions) == clue(expectedInstalledPluginsVersions)
    }
  end testExecutor

  override def beforeAll(): Unit =
    publishLocalResourcePlugins()

  FunFixture[pulumi.FixtureContext](
    setup = pulumi.fixture.setup(
      testDir = executorsDir / "scala-cli",
      projectFiles = Map("project.scala" -> projectFile)
    ),
    teardown = pulumi.fixture.teardown
  ).test("scala-cli") { ctx =>
    val pluginsJson = pproc(
      "scala-cli",
      "run",
      ".",
      "--jar",
      bootstrapLibJarPath,
      "--main-class",
      "besom.bootstrap.PulumiPluginsDiscoverer"
    ).call(cwd = ctx.programDir, env = ctx.env).out.text()

    testExecutor(ctx, pluginsJson)
  }

  FunFixture[pulumi.FixtureContext](
    setup = pulumi.fixture.setup(
      executorsDir / "sbt",
      projectFiles = Map("build.sbt" -> sbtBuildFile)
    ),
    teardown = pulumi.fixture.teardown
  ).test("sbt".tag(LocalOnly)) { ctx =>
    testExecutor(ctx, "") // we skip bootstrap test for sbt due to practical reasons for now
  }

  FunFixture[pulumi.FixtureContext](
    setup = pulumi.fixture.setup(
      executorsDir / "jar",
      projectFiles = Map()
    ),
    teardown = pulumi.fixture.teardown
  ).test("jar") { ctx =>
    // Prepare the binary
    val tmpBuildDir = os.temp.dir()
    os.list(executorsDir / "scala-cli").foreach(file => os.copy.into(file, tmpBuildDir))
    pproc("scala-cli", "--power", "package", ".", "--assembly", "-o", "app.jar").call(cwd = tmpBuildDir)
    os.copy.into(tmpBuildDir / "app.jar", ctx.programDir, replaceExisting = true)

    val binaryPath = ctx.programDir / "app.jar"
    val pluginsJson =
      pproc("java", "-cp", s"$bootstrapLibJarPath:$binaryPath", "besom.bootstrap.PulumiPluginsDiscoverer")
        .call(cwd = ctx.programDir, env = ctx.env)
        .out
        .text()

    testExecutor(ctx, pluginsJson)
  }
}
