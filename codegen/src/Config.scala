package besom.codegen

object Config {
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
