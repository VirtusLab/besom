package besom.codegen

import scala.util.matching.Regex

/** The parsed Pulumi type token used in Pulumi schema in a clean, canonical form, that enforces all three parts present
  * @param provider
  *   the provider name
  * @param module
  *   the module name
  * @param name
  *   the type name
  * @param raw
  *   the raw Pulumi type token
  */
case class PulumiToken private (provider: String, module: String, name: String, raw: String) extends PulumiToken.RawToken:
  def asString: String = PulumiToken.concat(provider, module, name)

  /** Transform the Pulumi type token to a Pulumi definition coordinates
    * @param pulumiPackageInfo
    *   the Pulumi package schema information
    * @return
    *   the Pulumi definition coordinates for the given Pulumi type token and Pulumi package schema information
    */
  @throws[TypeMapperError]("if the package name is not allowed in the context of the given Pulumi package")
  def toCoordinates(pulumiPackageInfo: PulumiPackageInfo): PulumiDefinitionCoordinates =
    this match
      case t @ PulumiToken("pulumi", "providers", providerName) =>
        validatePackageName(pulumiPackageInfo.name, pulumiPackageInfo.allowedPackageNames)(providerName)
        PulumiDefinitionCoordinates(
          token = t,
          providerPackageParts = providerName :: Nil,
          modulePackageParts = Utils.indexModuleName :: Nil,
          definitionName = Utils.providerTypeName
        )
      case t @ PulumiToken(providerName, moduleName, definitionName) =>
        validatePackageName(pulumiPackageInfo.name, pulumiPackageInfo.allowedPackageNames)(providerName)
        PulumiDefinitionCoordinates(
          token = t,
          providerPackageParts = pulumiPackageInfo.providerToPackageParts(providerName),
          modulePackageParts = pulumiPackageInfo.moduleToPackageParts(moduleName),
          definitionName = definitionName
        )
  end toCoordinates

  private def validatePackageName(packageName: String, allowedPackageNames: Set[String])(name: String): Unit = {
    val allNames = allowedPackageNames + packageName
    if (!allNames.contains(name)) {
      throw TypeMapperError(
        s"$this has invalid package name '$name', in context of package '${packageName}', " +
          s"allowed names for the current package: ${allNames.mkString("'", "', '", "'")}"
      )
    }
  }
end PulumiToken

object PulumiToken {
  private val tokenFmtShort: String    = "([^:]*):([^:]*)" // provider:name
  private val tokenPatternShort: Regex = ("^" + tokenFmtShort + "$").r
  private val tokenFmt: String         = "([^:]*):([^:]*)?:([^:]*)" // provider:module:name
  private val tokenPattern: Regex      = ("^" + tokenFmt + "$").r

  private def concat(provider: String, module: String, name: String) = s"${provider}:${module}:${name}"

  private def enforceNonEmptyModule(module: String): String =
    module match {
      case "" => Utils.indexModuleName
      case _  => module
    }

  def apply(token: String): PulumiToken = token match {
    case tokenPattern(provider, module, name) =>
      new PulumiToken(
        provider = provider,
        module = enforceNonEmptyModule(module),
        name = name,
        raw = token
      )
    case tokenPatternShort(provider, name) =>
      new PulumiToken(
        provider = provider,
        module = Utils.indexModuleName,
        name = name,
        raw = token
      )
    case _ => throw TypeMapperError(s"Cannot parse Pulumi token: $token, tokenPattern: $tokenPattern")
  }

  def apply(
    provider: String,
    module: String,
    name: String
  ): PulumiToken = {
    new PulumiToken(
      provider = provider,
      module = enforceNonEmptyModule(module),
      name = name,
      raw = concat(provider, module, name)
    )
  }

  def unapply(token: PulumiToken): Some[(String, String, String)] = Some((token.provider, token.module, token.name))

  /** The raw, non-uniform Pulumi token from Pulumi schema, it represents "dirty state"
    */
  trait RawToken {

    /** @return
      *   the raw Pulumi type token
      */
    def raw: String

    /** Lookup key is used during Pulumi schema parsing to recognize known elements
      *
      * @return
      *   the uniform representation of raw token
      */
    def asLookupKey: String = raw.toLowerCase
  }
}
