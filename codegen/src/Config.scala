package besom.codegen

// noinspection ScalaWeakerAccess
object Config {

  val DefaultJavaVersion  = "11"
  val DefaultScalaVersion = "3.3.1"

  val DefaultBesomVersion: String = {
    try {
      os.read(os.pwd / "version.txt").trim
    } catch {
      case ex: java.nio.file.NoSuchFileException =>
        throw GeneralCodegenException(
          "Expected './version.txt' file or explicit 'besom.codegen.Config.CodegenConfig(besomVersion = \"1.2.3\")",
          ex
        )
    }
  }
  val DefaultSchemasDir: os.Path = os.pwd / ".out" / "schemas"
  val DefaultCodegenDir: os.Path = os.pwd / ".out" / "codegen"

  case class CodegenConfig(
    besomVersion: String = DefaultBesomVersion,
    schemasDir: os.Path = DefaultSchemasDir,
    codegenDir: os.Path = DefaultCodegenDir,
    outputDir: Option[os.RelPath] = None,
    scalaVersion: String = DefaultScalaVersion,
    javaVersion: String = DefaultJavaVersion,
    logLevel: Logger.Level = Logger.Level.Info,
  )

  case class ProviderConfig(
    noncompiledModules: Seq[String] = Seq.empty
  )

  val providersConfigs: Map[String, ProviderConfig] = Map(
    "aws" -> ProviderConfig(
      noncompiledModules = Seq(
        "quicksight", // Module too large
        "wafv2" // Module too large
      )
    )
  ).withDefaultValue(ProviderConfig())
}
