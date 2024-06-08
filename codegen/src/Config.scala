package besom.codegen

import besom.model.SemanticVersion

case class Config(
  logLevel: Logger.Level = Logger.Level.Info,
  besomVersion: String = Config.DefaultBesomVersion,
  javaVersion: String = Config.DefaultJavaVersion,
  scalaVersion: String = Config.DefaultScalaVersion,
  schemasDir: os.Path = Config.DefaultSchemasDir,
  codegenDir: os.Path = Config.DefaultCodegenDir,
  overlaysDir: os.Path = Config.DefaultOverlaysDir,
  outputDir: Option[os.RelPath] = None,
  providers: String => Config.Provider = Config.DefaultProvidersConfigs,
  organization: String = Config.DefaultOrganization,
  url: String = Config.DefaultUrl,
  vcs: String = Config.DefaultVcs,
  license: String = Config.DefaultLicense,
  repository: String = Config.DefaultRepository,
  developers: List[String] = Config.DefaultDevelopersList
):
  val coreShortVersion: String = SemanticVersion
    .parseTolerant(besomVersion)
    .fold(
      e => throw GeneralCodegenException(s"Invalid besom version: ${besomVersion}", e),
      _.copy(patch = 0).toShortString
    )
end Config

// noinspection ScalaWeakerAccess
object Config {

  val DefaultJavaVersion  = "11"
  val DefaultScalaVersion = "3.3.1"

  val DefaultBesomVersion: String = {
    try {
      os.read(besomDir / "version.txt").trim
    } catch {
      case ex: java.nio.file.NoSuchFileException =>
        throw GeneralCodegenException(
          "Expected './version.txt' file or explicit 'besom.codegen.Config(besomVersion = \"1.2.3\")",
          ex
        )
    }
  }
  val DefaultSchemasDir: os.Path  = besomDir / ".out" / "schemas"
  val DefaultCodegenDir: os.Path  = besomDir / ".out" / "codegen"
  val DefaultOverlaysDir: os.Path = besomDir / "codegen" / "resources" / "overlays"

  def besomDir: os.Path =
    if os.pwd.last != "besom" then
      println("You have to run this command from besom project root directory")
      sys.exit(1)
    os.pwd

  case class Provider(
    nonCompiledModules: Seq[String] = Seq.empty,
    moduleToPackages: Map[String, String] = Map.empty
  )

  val DefaultProvidersConfigs: Map[String, Provider] = Map().withDefault(_ => Config.Provider())

  val DefaultOrganization: String = "org.virtuslab"

  val DefaultUrl: String = "https://github.com/VirtusLab/besom"

  val DefaultVcs: String = "github:VirtusLab/besom"

  val DefaultLicense = "Apache-2.0"

  val DefaultRepository = "central"

  val DefaultDevelopersList: List[String] = List(
    "lbialy|Łukasz Biały|https://github.com/lbialy",
    "prolativ|Michał Pałka|https://github.com/prolativ",
    "KacperFKorban|Kacper Korban|https://github.com/KacperFKorban",
    "pawelprazak|Paweł Prażak|https://github.com/pawelprazak"
  )
}
