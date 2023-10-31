package besom.codegen

import scala.util.matching.Regex

case class PulumiToken private (provider: String, module: String, name: String) {
  def asString: String = s"${provider}:${module}:${name}"
}

object PulumiToken {
  private val tokenFmt: String    = "(.*):(.*)?:(.*)" // provider:module:name
  private val tokenPattern: Regex = ("^" + tokenFmt + "$").r

  private def enforceNonEmptyModule(module: String): String =
    module match {
      case "" => "index"
      case _  => module
    }

  def apply(token: String): PulumiToken = token match {
    case tokenPattern(provider, module, name) =>
      new PulumiToken(
        provider = provider,
        module = enforceNonEmptyModule(module),
        name = name
      )
    case _ => throw TypeMapperError(s"Cannot parse Pulumi token: $token, tokenPattern: $tokenPattern")
  }

  def apply(provider: String, module: String, name: String): PulumiToken = {
    new PulumiToken(
      provider = provider,
      module = enforceNonEmptyModule(module),
      name = name
    )
  }
}
