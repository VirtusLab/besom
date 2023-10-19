package besom.codegen

object Config {
  sealed trait ModuleToPackage
  case object JavaModuleToPackage extends ModuleToPackage
  case object NodeJsModuleToPackage extends ModuleToPackage

  case class ProviderConfig(
    // Select the package mapping from the language part of the schema
    moduleToPackage: ModuleToPackage = JavaModuleToPackage,
    // Select modules to skip codegen for
    noncompiledModules: Seq[String] = Seq.empty
  )

  val providersConfigs: Map[String, ProviderConfig] = Map(
    "aws" -> ProviderConfig(
      noncompiledModules = Seq(
        "quicksight", // Module too large
        "wafv2" // Module too large
      )
    ),
    "kubernetes" -> ProviderConfig(
      moduleToPackage = NodeJsModuleToPackage
    )
  ).withDefaultValue(ProviderConfig())
}
