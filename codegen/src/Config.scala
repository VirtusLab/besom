package besom.codegen

object Config {

  // noinspection ScalaWeakerAccess
  val DefaultJavaVersion = "11"
  // noinspection ScalaWeakerAccess
  val DefaultScalaVersion = "3.3.1"

  case class CodegenConfig(
    besomVersion: String,
    schemasDir: os.Path,
    codegenDir: os.Path,
    outputDir: Option[os.RelPath] = None,
    scalaVersion: String = DefaultScalaVersion,
    javaVersion: String = DefaultJavaVersion
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
