//> using toolkit 0.6.0
//> using dep com.lihaoyi::pprint:0.9.0
//> using dep com.softwaremill.ox::core:0.5.12

import upickle.default.*
import upickle.implicits.key
import scala.util.Try
import scala.util.Random
import ox.*

case class LoadedEnvParameter(value: String, parameterMetadata: Parameter.Env)

@key("from") sealed trait Parameter

object Parameter:
  @key("exec") case class Exec(
    cmd: Vector[String],
    where: String,
    name: Option[String] = None,
    to: Option[String] = None
  ) extends Parameter
      derives ReadWriter

  @key("env") case class Env(
    to: String,
    name: String,
    secret: Boolean = false
  ) extends Parameter
      derives ReadWriter

  @key("const") case class Const(
    to: String,
    name: String,
    value: String
  ) extends Parameter
      derives ReadWriter

  @key("generate") case class Generate(
    to: String,
    name: String,
    secret: Boolean = false
  ) extends Parameter
      derives ReadWriter

  @key("kubectl") case object Kubectl extends Parameter derives ReadWriter

  @key("docker") case object Docker extends Parameter derives ReadWriter

  given ReadWriter[Parameter] = macroRW[Parameter]

def readParameterData(path: os.Path, targetProjects: Set[String]): Map[String, Vector[Parameter]] =
  val requiredSecretsStr = os.read(path)
  val allParamsObj       = ujson.read(requiredSecretsStr).obj

  val parsedParamsPerProject = allParamsObj.flatMap { case (key, value) =>
    if targetProjects.isEmpty || targetProjects.contains(key) then Some(key -> read[Vector[Parameter]](value))
    else None
  }.toMap

  parsedParamsPerProject

def normalizeEnvKey(key: String): String =
  key.replace(":", "_").toUpperCase()

lazy val pulumiPresent: Boolean =
  Try(os.proc("pulumi", "version").call()).isSuccess

lazy val dockerPresent: Boolean =
  Try(os.proc("docker", "version").call()).isSuccess

lazy val kubectlPresent: Boolean =
  Try(os.proc("kubectl", "version").call()).isSuccess

val chars = """ABCDEFGHIJKLMNOPQRSTUVWXYZ
              |abcdefghijklmnopqrstuvwxyz
              |0123456789
              |!@#$%^&*()-_+=<>?{}[]|""".stripMargin.replace("\n", "").toCharArray()

def generatePasswordString: String =
  val length = Random.between(12, 25)
  Random.shuffle(chars).take(length).mkString

def retryWithExceptions[A](maxRetries: Int = 3)(f: => A): A =
  def retry(remainingRetries: Int, suppressedExceptions: Vector[Exception]): A =
    try f
    catch
      case e: Exception =>
        if remainingRetries <= 0 then
          println(s"Operation failed after $maxRetries attempts")
          suppressedExceptions.foreach(e.addSuppressed)
          throw e
        else
          println(s"Operation failed, ${remainingRetries} attempts remaining. Retrying...")
          retry(remainingRetries - 1, suppressedExceptions :+ e)

  retry(maxRetries, Vector.empty)

def verifyAllParametersArePresent(parsedParamsPerProject: Map[String, Vector[Parameter]]): Unit =
  val (errs, uniqueSecretKeys) =
    parsedParamsPerProject.foldLeft(Vector.empty[String] -> Set.empty[String]) {
      case ((errs, requiredSecretKeys), (project, projectParams)) =>
        val envKeys = projectParams
          .collect { case Parameter.Env(to, name, secret) => name }
          .map(normalizeEnvKey)

        val missingEnvKeys = envKeys.filterNot(key => sys.env.contains(key))

        val dockerErrors =
          if projectParams.contains(Parameter.Docker) && !dockerPresent then
            Vector(s"Docker capability requested for project $project but docker is not present!")
          else Vector.empty[String]

        val kubectlErrors =
          if projectParams.contains(Parameter.Kubectl) && !kubectlPresent then
            Vector(s"Kubectl capability requested for project $project but kubectl is not present!")
          else Vector.empty[String]

        if missingEnvKeys.nonEmpty || dockerErrors.nonEmpty || kubectlErrors.nonEmpty then
          val allErrors = errs ++
            missingEnvKeys.map(mk => s"Missing required secret '$mk' for project $project") ++
            dockerErrors ++
            kubectlErrors

          (
            allErrors,
            requiredSecretKeys
          )
        else (errs, requiredSecretKeys ++ envKeys)
    }

  if errs.nonEmpty then
    println(errs.mkString("\n"))
    sys.exit(1)
  else println("All required secrets are present in environment!")

def loadEnvParams(envParams: Vector[Parameter.Env]): Map[String, LoadedEnvParameter] =
  envParams.map { envParam =>
    val envKey = normalizeEnvKey(envParam.name)
    val value  = sys.env.getOrElse(envKey, sys.error(s"Missing required env variable '$envKey'"))
    envKey -> LoadedEnvParameter(value, envParam)
  }.toMap

case class RunConfig(
  interactive: Boolean = false,
  skipUntil: Int = 0,
  targetProjects: Set[String] = Set.empty
)

def parseArgs(args: Array[String]): RunConfig =
  args
    .foldLeft((RunConfig(), 0)) { case ((config, skip), arg) =>
      arg match
        case "-i" | "--interactive" =>
          (config.copy(interactive = true), skip)
        case "--skip-until" =>
          (config, 1)
        case n if skip == 1 =>
          (config.copy(skipUntil = n.toInt), 0)
        case arg if !arg.startsWith("-") =>
          (config.copy(targetProjects = config.targetProjects + arg), skip)
        case unknown =>
          println(s"Unknown argument: $unknown")
          sys.exit(1)
    }
    ._1

def runExample(
  project: String,
  params: Vector[Parameter],
  loadedEnvParams: Map[String, LoadedEnvParameter],
  config: RunConfig
): Unit =
  var stepNumber = 1

  def confirmStep(description: String): Unit =
    if config.interactive && stepNumber >= config.skipUntil then
      println(s"\nStep $stepNumber: $description")
      println("Press Enter to continue or Ctrl+C to abort...")
      scala.io.StdIn.readLine()
    else if config.interactive then println(s"\nStep $stepNumber: $description (skipped)")
    stepNumber += 1

  println(s"\n========== Project $project ==========")

  val projectPath = os.pwd / os.RelPath(project)
  if (!os.exists(projectPath)) {
    println(s"Project directory not found: $project")
    return
  }

  // Collect exec commands

  // Collect parameters by their target (env or config)
  val toEnvParams = params.collect[Parameter.Const | Parameter.Generate | Parameter.Env | Parameter.Exec] {
    case p: Parameter.Env if p.to == "env"                        => p
    case c: Parameter.Const if c.to == "env"                      => c
    case g: Parameter.Generate if g.to == "env"                   => g
    case e: Parameter.Exec if e.to.isDefined && e.to.get == "env" => e
  }

  val toConfigParams = params.collect[Parameter.Const | Parameter.Generate | Parameter.Env | Parameter.Exec] {
    case p: Parameter.Env if p.to == "config"                        => p
    case c: Parameter.Const if c.to == "config"                      => c
    case g: Parameter.Generate if g.to == "config"                   => g
    case e: Parameter.Exec if e.to.isDefined && e.to.get == "config" => e
  }

  val toExecCmds = params.collect {
    case e: Parameter.Exec if !e.name.isDefined || !e.to.isDefined => e
  }

  // Prepare environment parameters
  val preparedEnvParams = toEnvParams.map {
    case p: Parameter.Env      => p.name -> loadedEnvParams(normalizeEnvKey(p.name)).value
    case p: Parameter.Const    => p.name -> p.value
    case p: Parameter.Generate => p.name -> generatePasswordString
    case p: Parameter.Exec =>
      val r = os.proc(p.cmd).call(cwd = os.pwd / os.RelPath(p.where))
      p.name.get -> r.out.text()
  }

  // Prepare config parameters with their secret status
  val preparedConfigParams = toConfigParams.map {
    case p: Parameter.Env      => (p.name, loadedEnvParams(normalizeEnvKey(p.name)).value, p.secret)
    case p: Parameter.Const    => (p.name, p.value, false)
    case p: Parameter.Generate => (p.name, generatePasswordString, p.secret)
    case p: Parameter.Exec =>
      val r = os.proc(p.cmd).call(cwd = os.pwd / os.RelPath(p.where))
      (p.name.get, r.out.text(), true)
  }

  if toExecCmds.nonEmpty then
    toExecCmds.foreach { exec =>
      os.proc(exec.cmd).call(cwd = os.pwd / os.RelPath(exec.where))
    }

  confirmStep("Initialize project environment")
  println("Setting up project environment...")

  // Create environment map
  val envMap: Map[String, String] = sys.env ++ preparedEnvParams.toMap

  try {
    confirmStep("Initialize and select Pulumi stack")
    println("Initializing Pulumi stack...")
    val stackName = s"testing-${project.replace("/", "-")}"

    // Clean up any existing stack first
    try {
      os.proc("pulumi", "stack", "rm", "--yes", "--force", stackName, "--cwd", projectPath)
        .call(check = false, env = envMap)
    } catch {
      case _: Exception => // Ignore errors if stack doesn't exist
    }

    os.proc("pulumi", "stack", "init", stackName, "--cwd", projectPath)
      .call(check = false, env = envMap)
    os.proc("pulumi", "stack", "select", stackName, "--cwd", projectPath)
      .call(check = false, env = envMap)

    // Set up Pulumi config
    if (preparedConfigParams.nonEmpty) {
      confirmStep("Set up Pulumi configuration")
      println("Setting up Pulumi configuration...")
      preparedConfigParams.foreach { case (name, value, isSecret) =>
        println(s"  Setting config: $name${if (isSecret) " (secret)" else s" with value $value"}")
        val cmd =
          if (isSecret)
            os.proc("pulumi", "config", "set", "--secret", name, value, "--cwd", projectPath)
          else
            os.proc("pulumi", "config", "set", name, value, "--cwd", projectPath)
        cmd.call(check = false, env = envMap)
      }
    }

    confirmStep("Deploy the stack")
    println(s"Deploying example...")
    os.proc("pulumi", "up", "--yes", "--cwd", projectPath).call(env = envMap)
    println(s"Successfully deployed example: $project")

  } catch {
    case e: os.SubprocessException =>
      println(s"Failed to deploy example $project: Process failed with output:\n${e.toString()}")
      throw e
    case e: Exception =>
      println(s"Failed to deploy example $project: ${e.getMessage}")
      throw e
  } finally {
    confirmStep("Clean up resources")
    println(s"Cleaning up example: $project")
    // Destroy the stack with retries
    retryWithExceptions() {
      os.proc("pulumi", "destroy", "--yes", "--cwd", projectPath)
        .call(env = envMap)
    }

    // Remove the stack (no retries needed)
    try {
      os.proc("pulumi", "stack", "rm", "--yes", "--cwd", projectPath)
        .call(env = envMap)
    } catch {
      case e: os.SubprocessException =>
        println(s"Failed to remove stack: Process failed with output:\n${e.toString()}")
      case e: Exception =>
        println(s"Failed to remove stack: ${e.getMessage}")
    }
  }
end runExample

def readSuccessfulProjects(): Set[String] =
  try
    val successesPath = os.pwd / "successes.json"
    if os.exists(successesPath) then read[Set[String]](os.read(successesPath))
    else Set.empty
  catch case _: Exception => Set.empty

def writeSuccessfulProject(project: String): Unit =
  val successesPath    = os.pwd / "successes.json"
  val currentSuccesses = readSuccessfulProjects()
  val updatedSuccesses = currentSuccesses + project
  os.write.over(successesPath, write(updatedSuccesses, indent = 2))

val projectsToSkip = Set(
  "azure-aks-managed-identity", // does not have authorization to perform action 'Microsoft.Authorization/roleAssignments/write' over scope
  "azure-synapse", // does not have authorization to perform action 'Microsoft.Authorization/roleAssignments/write' over scope
  "kubernetes-keycloak" // a lot of manual setup dependencies
)

def main(args: Array[String]) =
  val config                 = parseArgs(args)
  val parsedParamsPerProject = readParameterData(os.pwd / "required_secrets.json", config.targetProjects)
  verifyAllParametersArePresent(parsedParamsPerProject)

  if !pulumiPresent then
    println("Pulumi is not present in the system!")
    sys.exit(1)

  // If specific projects are provided, verify they exist in the configuration
  config.targetProjects.foreach { project =>
    if !parsedParamsPerProject.contains(project) then
      println(s"Project '$project' not found in required_secrets.json")
      sys.exit(1)
  }

  println()
  println(s"Skipping disabled projects:\n${projectsToSkip.mkString(" - ", "\n - ", "")}")

  // Determine which projects to compile and run
  val projectsToProcess =
    (if config.targetProjects.nonEmpty then parsedParamsPerProject.filter((proj, _) => config.targetProjects.contains(proj))
     else parsedParamsPerProject).filterNot((proj, _) => projectsToSkip.contains(proj))

  // Compile selected projects before running examples
  println("\nCompiling projects...")
  val compilationFailures = projectsToProcess
    .mapPar(projectsToProcess.size) { case (project, _) =>
      val projectPath = os.pwd / os.RelPath(project)
      if (!os.exists(projectPath)) {
        println(s"Project directory not found: $project")
        Some(project)
      } else {
        println(s"\nCompiling project: $project")
        try {
          os.proc("scala", "compile", ".").call(cwd = projectPath)
          None
        } catch {
          case e: os.SubprocessException =>
            println(s"Failed to compile project $project: ${e.toString()}")
            Some(project)
        }
      }
    }
    .foldLeft(Vector.empty[String]) {
      case (failures, Some(failed)) => failures :+ failed
      case (failures, None)         => failures
    }

  if (compilationFailures.nonEmpty) {
    println(s"\nCompilation failed for the following projects:\n${compilationFailures.mkString("\n")}")
    sys.exit(1)
  }
  println("\nAll selected projects compiled successfully!")

  val envParams          = parsedParamsPerProject.values.flatten.collect { case p: Parameter.Env => p }.toVector
  val loadedEnvParams    = loadEnvParams(envParams)
  val successfulProjects = readSuccessfulProjects()

  // Process each selected project that hasn't been successful yet
  projectsToProcess.foreach { case (project, params) =>
    if successfulProjects.contains(project) then println(s"\nSkipping already successful project: $project")
    else
      try
        runExample(project, params, loadedEnvParams, config)
        writeSuccessfulProject(project)
      catch
        case e: Exception =>
          println(s"Failed to process example $project: ${e.getMessage}")
          e.printStackTrace()
          sys.exit(1)
          throw Exception("NOOOOO")
  }
end main

main(args)
