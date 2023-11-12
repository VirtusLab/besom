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
      case "" => Utils.indexModuleName
      case _  => module
    }

  def apply(token: String): PulumiToken = token match {
    case tokenPattern("pulumi", "providers", providerName) =>
      new PulumiToken(providerName, Utils.indexModuleName, Utils.providerTypeName)
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

  implicit class PulumiTokenOps(token: PulumiToken) {
    private def tokenToMethodName: String =
      token.name.split("/").map(_.capitalize).mkString("")

    def toFunctionCoordinates(
      moduleToPackageParts: String => Seq[String],
      providerToPackageParts: String => Seq[String],
      isMethod: Boolean
    ): PulumiDefinitionCoordinates = {
      val methodToken = if (isMethod) {
        token.copy(name = tokenToMethodName)
      } else {
        token
      }
      PulumiDefinitionCoordinates.fromToken(
        typeToken = methodToken,
        moduleToPackageParts = moduleToPackageParts,
        providerToPackageParts = providerToPackageParts
      )
    }
  }
}
