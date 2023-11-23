package besom.codegen

import besom.codegen.metaschema.PulumiPackage

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
case class PulumiToken private (provider: String, module: String, name: String, raw: String) extends PulumiToken.RawToken {
  def asString: String = PulumiToken.concat(provider, module, name)

  /** Transform the Pulumi type token to a Pulumi definition coordinates
    * @param pulumiPackage
    *   the Pulumi package containing the schema information
    * @return
    *   the Pulumi definition coordinates for the given Pulumi type token
    */
  def toCoordinates(pulumiPackage: PulumiPackage)(implicit logger: Logger): PulumiDefinitionCoordinates = {
    import besom.codegen.Utils.PulumiPackageOps

    PulumiDefinitionCoordinates.fromToken(
      typeToken = this,
      moduleToPackageParts = pulumiPackage.moduleToPackageParts,
      providerToPackageParts = pulumiPackage.providerToPackageParts
    )
  }
}

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
    case _ => throw TypeError(s"Cannot parse Pulumi token: $token, tokenPattern: $tokenPattern")
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

  def unapply(token: PulumiToken): Option[(String, String, String)] = Some((token.provider, token.module, token.name))

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
