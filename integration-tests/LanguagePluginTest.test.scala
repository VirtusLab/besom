package besom.integration.languageplugin

import besom.codegen.PulumiPluginJSON
import besom.integration.common.*
import munit.Slow
import os.*

import scala.concurrent.duration.*

//noinspection ScalaWeakerAccess,TypeAnnotation,ScalaFileName
class LanguagePluginTest extends munit.FunSuite {
  override val munitTimeout = 5.minutes

  override def munitTests(): Seq[Test] = super
    .munitTests()
    .filterNot(tagsWhen(envVarOpt("CI").contains("true"))(LocalOnly))
    .filterNot(tags(Slow))

  val wd                  = os.pwd / "integration-tests"
  val besomVersion        = os.read(os.pwd / "version.txt").trim
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

  val gradleBuildFile =
    s"""|plugins {
        |    scala
        |    application
        |}
        |
        |repositories {
        |    mavenLocal()
        |    mavenCentral()
        |}
        |
        |dependencies {
        |    implementation("org.scala-lang:scala3-library_3:$scalaVersion")
        |    implementation("org.virtuslab:besom-core_3:$coreVersion")
        |    implementation("org.virtuslab:besom-fake-standard-resource_3:1.2.3-TEST")
        |    implementation("org.virtuslab:besom-fake-external-resource_3:2.3.4-TEST")
        |    if (project.hasProperty("besomBootstrapJar")) runtimeOnly(files(project.property("besomBootstrapJar") as String))
        |}
        |
        |application {
        |    mainClass.set(
        |            if (project.hasProperty("mainClass")) {
        |                project.property("mainClass") as String
        |            } else {
        |                "besom.languageplugin.test.pulumiapp.run"
        |            }
        |    )
        |}
        |""".stripMargin

  val mavenPomFile =
    s"""|<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        |         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
        |    <modelVersion>4.0.0</modelVersion>
        |
        |    <groupId>org.virtuslab.example</groupId>
        |    <artifactId>infra</artifactId>
        |    <version>1.0-SNAPSHOT</version>
        |
        |    <properties>
        |        <encoding>UTF-8</encoding>
        |        <maven.compiler.target>$javaVersion</maven.compiler.target>
        |        <maven.compiler.release>$javaVersion</maven.compiler.release>
        |        <mainClass>$${project.groupId}.$${project.artifactId}.run</mainClass>
        |    </properties>
        |
        |    <profiles>
        |        <profile>
        |            <!-- This profile is used to run the project with the besom bootstrap jar -->
        |            <id>default-pulumi-java-sdk-dependency</id>
        |            <activation>
        |                <property>
        |                    <name>besomBootstrapJar</name>
        |                </property>
        |            </activation>
        |            <dependencies>
        |                <dependency>
        |                    <groupId>org.virtuslab</groupId>
        |                    <artifactId>besom-bootstrap_3</artifactId>
        |                    <version>$coreVersion</version>
        |                    <scope>system</scope>
        |                    <systemPath>$${besomBootstrapJar}</systemPath>
        |                </dependency>
        |            </dependencies>
        |        </profile>
        |    </profiles>
        |
        |    <dependencies>
        |        <dependency>
        |            <groupId>org.scala-lang</groupId>
        |            <artifactId>scala3-library_3</artifactId>
        |            <version>$scalaVersion</version>
        |        </dependency>
        |        <dependency>
        |            <groupId>org.virtuslab</groupId>
        |            <artifactId>besom-core_3</artifactId>
        |            <version>$coreVersion</version>
        |        </dependency>
        |        <dependency>
        |            <groupId>org.virtuslab</groupId>
        |            <artifactId>besom-fake-standard-resource_3</artifactId>
        |            <version>1.2.3-TEST</version>
        |        </dependency>
        |        <dependency>
        |            <groupId>org.virtuslab</groupId>
        |            <artifactId>besom-fake-external-resource_3</artifactId>
        |            <version>2.3.4-TEST</version>
        |        </dependency>
        |    </dependencies>
        |
        |    <build>
        |        <sourceDirectory>src/main/scala</sourceDirectory>
        |        <plugins>
        |            <plugin>
        |                <groupId>net.alchim31.maven</groupId>
        |                <artifactId>scala-maven-plugin</artifactId>
        |                <version>4.8.1</version>
        |                <executions>
        |                    <execution>
        |                        <goals>
        |                            <goal>compile</goal>
        |                            <goal>testCompile</goal>
        |                        </goals>
        |                    </execution>
        |                </executions>
        |            </plugin>
        |            <plugin>
        |                <groupId>org.apache.maven.plugins</groupId>
        |                <artifactId>maven-wrapper-plugin</artifactId>
        |                <version>3.2.0</version>
        |                <configuration>
        |                    <mavenVersion>3.9.6</mavenVersion>
        |                </configuration>
        |            </plugin>
        |        </plugins>
        |    </build>
        |</project>
        |""".stripMargin

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

  def testExecutor(ctx: pulumi.FixtureContext, pluginsJson: String = "") =
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
    
    val expectedAboutPluginsVersions = Map("scala" -> besomVersion, "random" -> "4.3.1", "aci" -> "0.0.6")
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

  def publishLocalResourcePlugin(pluginName: String) =
    scalaCli.publishLocal(resourcesDir / pluginName).call()
    // publish to ~/.m2 for gradle and maven
    scalaCli.publishLocalMaven(resourcesDir / pluginName).call()

  def publishLocalResourcePlugins() =
    publishLocalResourcePlugin("fake-standard-resource-plugin")
    publishLocalResourcePlugin("fake-external-resource-plugin")

  override def beforeAll(): Unit =
    publishLocalResourcePlugins()
    // publish to ~/.m2 for gradle and maven
    scalaCli.publishLocalMaven("besom-json", s"--project-version=${coreVersion}").call()
    scalaCli.publishLocalMaven("core", s"--project-version=${coreVersion}").call()
  end beforeAll

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
    testExecutor(ctx)
  }

  FunFixture[pulumi.FixtureContext](
    setup = pulumi.fixture.setup(
      executorsDir / "gradle",
      projectFiles = Map("build.gradle.kts" -> gradleBuildFile)
    ),
    teardown = pulumi.fixture.teardown
  ).test("gradle") { ctx =>
    testExecutor(ctx)
  }

  FunFixture[pulumi.FixtureContext](
    setup = pulumi.fixture.setup(
      executorsDir / "maven",
      projectFiles = Map("pom.xml" -> mavenPomFile)
    ),
    teardown = pulumi.fixture.teardown
  ).test("maven") { ctx =>
    testExecutor(ctx)
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
