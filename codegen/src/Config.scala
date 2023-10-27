package besom.codegen

import ujson.{Str, Value}

object Config {
  case class ProviderConfig(
    noncompiledModules: Seq[String] = Seq.empty,
    transformSchema: Option[Value.Value => Value.Value] = None
  )

  val providersConfigs: Map[String, ProviderConfig] = Map(
    "aws" -> ProviderConfig(
      noncompiledModules = Seq(
        "quicksight", // Module too large
        "wafv2" // Module too large
      )
    ),
    "digitalocean" -> ProviderConfig(
      transformSchema = Some { schema =>
        val urn = schema.obj("resources")("digitalocean:index/reservedIp:ReservedIp")("properties")("urn")
        schema.obj("resources")("digitalocean:index/reservedIp:ReservedIp")("properties")("reservedIpUrn") = urn
        schema.obj("resources")("digitalocean:index/reservedIp:ReservedIp")("properties").obj.remove("urn")
        schema
      }
    ),
  ).withDefaultValue(ProviderConfig())
}
