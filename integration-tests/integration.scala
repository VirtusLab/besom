package besom.integration.common

import besom.codegen.Config.CodegenConfig
import besom.codegen.{Config, PackageMetadata}
import besom.codegen.generator.Result
import munit.{Tag, Test}
import os.Shellable

import scala.util.{Failure, Success, Try}

case object LocalOnly extends munit.Tag("LocalOnly")

val javaVersion                 = Config.DefaultJavaVersion
val scalaVersion                = Config.DefaultScalaVersion
val coreVersion                 = os.read(os.pwd / "version.txt").trim
val scalaPluginVersion          = coreVersion
val providerRandomSchemaVersion = "4.14.0"
val providerTlsSchemaVersion    = "4.11.1"
val providerRandomVersion       = s"$providerRandomSchemaVersion-core.$coreVersion"
val providerTlsVersion          = s"$providerTlsSchemaVersion-core.$coreVersion"

val languagePluginDir = os.pwd / ".out" / "language-plugin"

val defaultProjectFile =
  s"""|//> using scala $scalaVersion
      |//> using options -java-output-version:$javaVersion -Werror -Wunused:all -Wvalue-discard -Wnonunit-statement
      |//> using plugin org.virtuslab::besom-compiler-plugin:$coreVersion
      |//> using dep org.virtuslab::besom-core:$coreVersion
      |""".stripMargin

def sanitizeName(name: String): String = name.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase().take(32).stripSuffix("-")
def testToStack(name: String): String  = "tests-" + sanitizeName(name)

//noinspection TypeAnnotation,ScalaWeakerAccess
object pulumi {
  def login(pulumiHome: os.Path) = pproc("pulumi", "--non-interactive", "--logtostderr", "login", s"file://$pulumiHome")

  def logout(pulumiHome: os.Path) = pproc("pulumi", "--non-interactive", "--logtostderr", "logout", s"file://$pulumiHome")

  def stackInit(stackName: String) =
    pproc("pulumi", "--non-interactive", "--logtostderr", "stack", "init", "--stack", stackName)

  def stackRm(stackName: String) =
    pproc("pulumi", "--non-interactive", "--logtostderr", "stack", "rm", "-y", "--stack", stackName)

  def preview(stackName: String, additional: os.Shellable*) =
    pproc("pulumi", "--non-interactive", "--logtostderr", "preview", "--stack", stackName, additional)

  def up(stackName: String, additional: os.Shellable*) = pproc(
    "pulumi",
    "--non-interactive",
    "--logtostderr",
    "up",
    "--stack",
    stackName,
    "--yes",
    additional
  )

  def destroy(stackName: String, additional: os.Shellable*) = pproc(
    "pulumi",
    "--non-interactive",
    "--logtostderr",
    "destroy",
    "--stack",
    stackName,
    "--yes",
    additional
  )

  def config(stackName: String, additional: os.Shellable*) = pproc(
    "pulumi",
    "--non-interactive",
    "--logtostderr",
    "config",
    "--stack",
    stackName,
    "set",
    additional
  )

  def secret(stackName: String, additional: os.Shellable*) = pproc(
    "pulumi",
    "--non-interactive",
    "--logtostderr",
    "config",
    "--stack",
    stackName,
    "set",
    "--secret",
    additional
  )

  def outputs(stackName: String, additional: os.Shellable*) = pproc(
    "pulumi",
    "--non-interactive",
    "--logtostderr",
    "stack",
    "output",
    "--stack",
    stackName,
    "--json",
    additional
  )

  def installScalaPlugin() =
    pproc(
      "pulumi",
      "--non-interactive",
      "--logtostderr",
      "plugin",
      "install",
      "language",
      "scala",
      scalaPluginVersion,
      "--file",
      languagePluginDir,
      "--reinstall"
    )

  // noinspection ScalaWeakerAccess
  case class FixtureArgs(
                          testDir: os.Path,
                          projectFiles: Map[String, String] = Map("project.scala" -> defaultProjectFile),
                          pulumiEnv: Map[String, String] = Map()
                        )
  case class FixtureContext(stackName: String, testDir: os.Path, env: Map[String, String], pulumiHome: os.Path)

  object fixture {
    def setup(
               args: FixtureArgs*
             )(
               test: munit.TestOptions
             ): Vector[FixtureContext] =
      args
        .map(setup(_)(test))
        .foldLeft(Vector.empty[FixtureContext])(_ :+ _)

    def setup(
      testDir: os.Path,
      projectFiles: Map[String, String] = Map("project.scala" -> defaultProjectFile),
      pulumiEnv: Map[String, String] = Map()
             ): munit.TestOptions => FixtureContext =
      setup(FixtureArgs(testDir, projectFiles, pulumiEnv))

    def setup(args: FixtureArgs)(test: munit.TestOptions): FixtureContext = {
      val stackName = testToStack(test.name) + sha1(args.testDir.relativeTo(os.pwd).toString)
      val tmpPulumiHome = os.temp.dir()
      val allEnv: Map[String, String] =
        Map(
          "PULUMI_CONFIG_PASSPHRASE" -> envVarOpt("PULUMI_CONFIG_PASSPHRASE").getOrElse("")
        ) ++ args.pulumiEnv ++ Map( // don't override test-critical env vars
          "PULUMI_HOME" -> tmpPulumiHome.toString,
          "PULUMI_STACK" -> stackName,
          "PULUMI_SKIP_UPDATE_CHECK" -> "true"
        )

      println(s"Test stack: $stackName")
      args.projectFiles.foreach { case (name, content) =>
        val file = args.testDir / name
        println(s"Writing test file: ${file.relativeTo(os.pwd)}")
        os.write.over(file, content)
      }
      pulumi.login(tmpPulumiHome).call(cwd = args.testDir, env = allEnv)
      pulumi.stackInit(stackName).call(cwd = args.testDir, env = allEnv)
      pulumi.installScalaPlugin().call(cwd = args.testDir, env = allEnv)
      FixtureContext(stackName, args.testDir, allEnv, tmpPulumiHome)
    }

    def teardown(ctx: FixtureContext): Unit = {
      pulumi.destroy(ctx.stackName).call(cwd = ctx.testDir, env = ctx.env)
      pulumi.stackRm(ctx.stackName).call(cwd = ctx.testDir, env = ctx.env)
      pulumi.logout(ctx.pulumiHome).call(cwd = ctx.testDir, env = ctx.env)
      // purposely not deleting project.scala to make editing tests easier
    }
  }
}

//noinspection TypeAnnotation
object scalaCli {
  def compile(additional: os.Shellable*) = pproc(
    "scala-cli",
    "--power",
    "compile",
    "--suppress-experimental-feature-warning",
    "--suppress-directives-in-multiple-files-warning",
    "--jvm=17",
    "--bloop-jvm=17",
    "--bloop-java-opt=-XX:-UseZGC",
    "--bloop-java-opt=-XX:+UseUseParallelGC",
    "--bloop-java-opt=-XX:ParallelGCThreads=120",
    additional
  )

  def publishLocal(additional: os.Shellable*) = pproc(
    "scala-cli",
    "--power",
    "publish",
    "local",
    "--suppress-experimental-feature-warning",
    "--suppress-directives-in-multiple-files-warning",
    "--jvm=17",
    "--bloop-jvm=17",
    "--bloop-java-opt=-XX:-UseZGC",
    "--bloop-java-opt=-XX:+UseUseParallelGC",
    "--bloop-java-opt=-XX:ParallelGCThreads=120",
    additional
  )
}

object codegen {
  import besom.codegen.generator

  def generateLocalPackage(
    metadata: PackageMetadata
  ): Unit = {
    val result = codegen.generatePackage(metadata)
    scalaCli.publishLocal(result.outputDir).call(check = false) // compilation will fail anyway
    if (result.dependencies.nonEmpty)
      println(s"\nCompiling dependencies for ${result.schemaName}:${result.packageVersion}...")
    for (dep <- result.dependencies) {
      generateLocalPackage(dep)
    }
  }

  def generatePackage(
    metadata: PackageMetadata,
    outputDir: Option[os.RelPath] = None
  ): generator.Result = {
    // noinspection TypeAnnotation
    implicit val config = CodegenConfig(outputDir = outputDir)
    generator.generatePackageSources(metadata)
  }

  def generatePackageFromSchema(
    metadata: PackageMetadata,
    schema: os.Path,
    outputDir: Option[os.RelPath] = None
  ): generator.Result = {
    // noinspection TypeAnnotation
    implicit val config = CodegenConfig(outputDir = outputDir)
    generator.generatePackageSources(metadata, Some(schema))
  }
}

def pproc(command: Shellable*) = {
  val cmd = os.proc(command)
  println(cmd.commandChunks.mkString(" "))
  cmd
}

def envVar(name: String): Try[String] =
  sys.env.get(name) match
    case Some(v) =>
      Option(v).filter(_.trim.nonEmpty) match
        case Some(value) => Success(value)
        case None        => Failure(new Exception(s"Environment variable $name is empty"))
    case None => Failure(new Exception(s"Environment variable $name is not set"))

def envVarOpt(name: String): Option[String] =
  sys.env.get(name).map(_.trim).filter(_.nonEmpty)

def tagsWhen(condition: Boolean)(excludedTag: Tag): Test => Boolean =
  if condition then tags(excludedTag)(_)
  else _ => false

def tags(excludedTag: Tag): Test => Boolean = _.tags.contains(excludedTag)

private def sha1(s: String): String = {
  import java.security.MessageDigest
  import java.util.HexFormat

  val bytes = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))
  HexFormat.of().withLowerCase().formatHex(bytes)
}