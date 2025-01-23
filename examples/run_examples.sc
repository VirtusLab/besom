//> using toolkit 0.6.0
//> using dep com.lihaoyi::pprint:0.9.0

import upickle.default.*
import upickle.implicits.key
import scala.util.Try
import scala.util.Random

case class LoadedEnvParameter(value: String, parameterMetadata: Parameter.Env)

@key("from") sealed trait Parameter

object Parameter:
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

def readParameterData(path: os.Path): Map[String, Vector[Parameter]] =
  val requiredSecretsStr = os.read(path)
  val allParamsObj = ujson.read(requiredSecretsStr).obj

  val parsedParamsPerProject = allParamsObj.map { case (key, value) =>
    key -> read[Vector[Parameter]](value)
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
    val value = sys.env.getOrElse(envKey, sys.error(s"Missing required env variable '$envKey'"))
    envParam.name -> LoadedEnvParameter(value, envParam)
  }.toMap

def 

def main() =
  val parsedParamsPerProject = readParameterData(os.pwd / "required_secrets.json")
  verifyAllParametersArePresent(parsedParamsPerProject)

  if !pulumiPresent then
    println("Pulumi is not present in the system!")
    sys.exit(1)

  val envParams = parsedParamsPerProject.values.flatten.collect { case p: Parameter.Env => p }.toVector
  val loadedEnvParams = loadEnvParams(envParams)

  parsedParamsPerProject.foreach { case (project, params) =>
    val toEnvParams = params.collect[Parameter.Const | Parameter.Generate | Parameter.Env] {
      case p: Parameter.Env if p.to == "env"      => p
      case c: Parameter.Const if c.to == "env"    => c
      case g: Parameter.Generate if g.to == "env" => g
    }

    val toConfigParams = params.collect[Parameter.Const | Parameter.Generate | Parameter.Env] {
      case p: Parameter.Env if p.to == "config"      => p
      case c: Parameter.Const if c.to == "config"    => c
      case g: Parameter.Generate if g.to == "config" => g
    }

    val preparedEnvParams = toEnvParams.map {
      case p: Parameter.Env      => p.name -> loadedEnvParams(normalizeEnvKey(p.name)).value
      case p: Parameter.Const    => p.name -> p.value
      case p: Parameter.Generate => p.name -> generatePasswordString
    }

    val preparedConfigParams = toConfigParams.map {
      case p: Parameter.Env      => p.name -> loadedEnvParams(normalizeEnvKey(p.name)).value
      case p: Parameter.Const    => p.name -> p.value
      case p: Parameter.Generate => p.name -> generatePasswordString
    }



  }

main()
