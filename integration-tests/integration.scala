package besom.integration.common

import besom.codegen.generator.Result
import besom.codegen.{Config, PackageMetadata}
import besom.scripts.Version
import munit.{Tag, Test}
import os.Shellable

import scala.util.{Failure, Success, Try}

case object LocalOnly extends munit.Tag("LocalOnly")

val javaVersion        = Config.DefaultJavaVersion
val scalaVersion       = Config.DefaultScalaVersion
val coreVersion        = Config.DefaultBesomVersion
val scalaPluginVersion = coreVersion

val languagePluginDir = os.pwd / ".out" / "language-plugin"

val defaultProjectFile =
  s"""|//> using scala $scalaVersion
      |//> using options -java-output-version:$javaVersion -Werror -Wunused:all -Wvalue-discard -Wnonunit-statement
      |//> using plugin org.virtuslab::besom-compiler-plugin:$coreVersion
      |//> using dep org.virtuslab::besom-core:$coreVersion
      |
      |//> using repository sonatype:snapshots
      |""".stripMargin

def sanitizeName(name: String): String = name.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase().take(40).stripSuffix("-")
def testToStack(name: String): String  = "tests-" + sanitizeName(name)

//noinspection TypeAnnotation,ScalaWeakerAccess
object pulumi {
  val verboseLogging = List("-v=10", "--logflow")

  def login(pulumiHome: os.Path) = pproc("pulumi", "--non-interactive", "--logtostderr", verboseLogging *, "login", s"file://$pulumiHome")

  def logout(pulumiHome: os.Path) =
    pproc("pulumi", "--non-interactive", "--logtostderr", verboseLogging *, "logout", s"file://$pulumiHome")

  def stackInit(stackName: String) =
    pproc("pulumi", "--non-interactive", "--logtostderr", verboseLogging *, "stack", "init", "--stack", stackName)

  def stackRm(stackName: String) =
    pproc("pulumi", "--non-interactive", "--logtostderr", verboseLogging *, "stack", "rm", "-y", "--stack", stackName)

  def stackLs() =
    pproc("pulumi", "--non-interactive", "--logtostderr", verboseLogging *, "stack", "ls", "--json")

  def preview(stackName: String, additional: os.Shellable*) =
    pproc("pulumi", "--non-interactive", "--logtostderr", verboseLogging *, "preview", "--stack", stackName, additional)

  def up(stackName: String, additional: os.Shellable*) = pproc(
    "pulumi",
    "--non-interactive",
    "--logtostderr",
    verboseLogging *,
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
    verboseLogging *,
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
    verboseLogging *,
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
    verboseLogging *,
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
    verboseLogging *,
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
      verboseLogging *,
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
  case class FixtureOpts(
    pulumiHomeDir: os.Path = os.temp.dir(),
    pulumiEnv: Map[String, String] = Map()
  )
  case class FixtureArgs(
    programDir: os.Path,
    projectFiles: Map[String, String] = Map("project.scala" -> defaultProjectFile)
  )

  case class ProgramContext(
    stackName: String,
    programDir: os.Path,
    env: Map[String, String]
  )

  case class PulumiContext(
    home: os.Path,
    env: Map[String, String]
  )

  case class FixtureContext(
    pulumi: PulumiContext,
    program: ProgramContext
  ) {
    def stackName: String = program.stackName

    def programDir: os.Path = program.programDir

    def env: Map[String, String] = program.env
  }

  case class FixtureMultiContext(
    pulumi: PulumiContext,
    program: Vector[ProgramContext]
  )

  object fixture {
    def setup(
      opts: FixtureOpts,
      args: FixtureArgs*
    )(
      test: munit.TestOptions
    ): FixtureMultiContext =
      val pulumiContext = init(opts)
      val programContexts = args
        .map(setup(pulumiContext, _)(test))
        .foldLeft(Vector.empty[ProgramContext])(_ :+ _)
      FixtureMultiContext(pulumiContext, programContexts)

    def setup(
      testDir: os.Path,
      projectFiles: Map[String, String] = Map("project.scala" -> defaultProjectFile),
      pulumiHomeDir: os.Path = os.temp.dir(),
      pulumiEnv: Map[String, String] = Map()
    ): munit.TestOptions => FixtureContext =
      val pulumiContext  = init(FixtureOpts(pulumiHomeDir, pulumiEnv))
      val programContext = setup(pulumiContext, FixtureArgs(testDir, projectFiles))
      (test: munit.TestOptions) => FixtureContext(pulumiContext, programContext(test))

    def init(opts: FixtureOpts): PulumiContext = {
      val allEnv: Map[String, String] =
        opts.pulumiEnv ++ Map(
          "PULUMI_HOME" -> (opts.pulumiHomeDir / ".pulumi").toString,
          "PULUMI_SKIP_UPDATE_CHECK" -> "true"
        )
      pulumi.login(opts.pulumiHomeDir).call(cwd = opts.pulumiHomeDir, env = allEnv)
      pulumi.installScalaPlugin().call(cwd = opts.pulumiHomeDir, env = allEnv)
      PulumiContext(opts.pulumiHomeDir, allEnv)
    }

    def setup(pulumiContext: PulumiContext, args: FixtureArgs)(test: munit.TestOptions): ProgramContext = {
      val stackName = testToStack(test.name) + sha1(args.programDir.relativeTo(os.pwd).toString)
      val allEnv: Map[String, String] =
        Map(
          "PULUMI_CONFIG_PASSPHRASE" -> envVarOpt("PULUMI_CONFIG_PASSPHRASE").getOrElse("")
        ) ++ pulumiContext.env ++ Map( // don't override test-critical env vars
          "PULUMI_STACK" -> stackName
        )

      println(s"Test stack: $stackName")
      args.projectFiles.foreach { case (name, content) =>
        val file = args.programDir / name
        println(s"Writing test file: ${file.relativeTo(os.pwd)}")
        os.write.over(file, content)
      }
      pulumi.stackInit(stackName).call(cwd = args.programDir, env = allEnv)
      ProgramContext(stackName, args.programDir, allEnv)
    }

    def teardown(ctx: FixtureContext): Unit = {
      destroy(ctx.program)
      logout(ctx.pulumi)
    }

    def teardown(ctx: FixtureMultiContext): Unit = {
      ctx.program.foreach(destroy)
      logout(ctx.pulumi)
    }

    def destroy(ctx: ProgramContext): Unit = {
      pulumi.destroy(ctx.stackName).call(cwd = ctx.programDir, env = ctx.env)
      pulumi.stackRm(ctx.stackName).call(cwd = ctx.programDir, env = ctx.env)
      // purposely not deleting project.scala to make editing tests easier
    }

    def logout(ctx: PulumiContext): Unit = {
      pulumi.logout(ctx.home).call(cwd = ctx.home, env = ctx.env)
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
    "--server=false",
    "--javac-opt=-verbose",
    "--javac-opt=-J-XX:MaxHeapSize=32G",
    "--javac-opt=-J-XX:+UseParallelGC",
    additional
  )

  def publishLocal(additional: os.Shellable*) = pproc(
    "scala-cli",
    "--power",
    "publish",
    "local",
    "--sources=false",
    "--doc=false",
    "--suppress-experimental-feature-warning",
    "--suppress-directives-in-multiple-files-warning",
    "--server=false",
    "--javac-opt=-verbose",
    "--javac-opt=-J-XX:MaxHeapSize=32G",
    "--javac-opt=-J-XX:+UseParallelGC",
    additional
  )

  def publishLocalMaven(additional: os.Shellable*) = pproc(
    "scala-cli",
    "--power",
    "publish",
    s"--publish-repository=file://${os.home}/.m2/repository",
    "--sources=false",
    "--doc=false",
    "--suppress-experimental-feature-warning",
    "--suppress-directives-in-multiple-files-warning",
    "--server=false",
    "--javac-opt=-verbose",
    "--javac-opt=-J-XX:MaxHeapSize=32G",
    "--javac-opt=-J-XX:+UseParallelGC",
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
    if (result.metadata.dependencies.nonEmpty)
      println(s"\nCompiling dependencies for ${result.metadata.name}:${result.metadata.version.getOrElse("none")}...")
    for (dep <- result.metadata.dependencies) {
      generateLocalPackage(dep)
    }
  }

  def generatePackage(
    metadata: PackageMetadata,
    outputDir: Option[os.RelPath] = None
  ): generator.Result = {
    given Config = Config(outputDir = outputDir)
    generator.generatePackageSources(metadata)
  }

  def generatePackageFromSchema(
    metadata: PackageMetadata,
    schema: os.Path,
    outputDir: Option[os.RelPath] = None
  ): generator.Result = {
    given Config = Config(outputDir = outputDir)
    generator.generatePackageSources(metadata, Some(schema))
  }
}

def pproc(command: Shellable*) = {
  val cmd = os.proc(command, env = Map("TF_LOG" -> "TRACE"))
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

  val bytes = MessageDigest.getInstance("SHA-1").digest(s.getBytes("UTF-8"))
  String.format("%x", new java.math.BigInteger(1, bytes))
}
