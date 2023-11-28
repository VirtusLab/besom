package besom.codegen

case class PulumiDefinitionCoordinates private (
  token: PulumiToken,
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  private val definitionName: String
)(implicit logger: Logger) {
  import PulumiDefinitionCoordinates._

  def className(asArgsType: Boolean): String = {
    val classNameSuffix = if (asArgsType) "Args" else ""
    mangleTypeName(definitionName) ++ classNameSuffix
  }

  def asResourceClass(asArgsType: Boolean): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = className(asArgsType)
    )
  }
  def asObjectClass(asArgsType: Boolean): ScalaDefinitionCoordinates = {
    val packageSuffix = if (asArgsType) inputsPackage else outputsPackage
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ packageSuffix,
      definitionName = className(asArgsType)
    )
  }
  def asEnumClass(implicit logger: Logger): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ enumsPackage,
      definitionName = mangleTypeName(definitionName)
    )
  }

  def topLevelMethod(implicit logger: Logger): ScalaDefinitionCoordinates =
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = mangleMethodName(definitionName)
    )

  def methodArgsClass(implicit logger: Logger): ScalaDefinitionCoordinates =
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = mangleTypeName(capitalize(definitionName)) + "Args"
    )

  def methodResultClass(implicit logger: Logger): ScalaDefinitionCoordinates =
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = mangleTypeName(capitalize(definitionName)) + "Result"
    )

  def asConfigClass(implicit logger: Logger): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = mangleTypeName(definitionName)
    )
  }
}

object PulumiDefinitionCoordinates {
  def fromRawToken(
    typeToken: String,
    moduleToPackageParts: String => Seq[String],
    providerToPackageParts: String => Seq[String]
  )(implicit logger: Logger): PulumiDefinitionCoordinates = {
    fromToken(
      typeToken = PulumiToken(typeToken),
      moduleToPackageParts = moduleToPackageParts,
      providerToPackageParts = providerToPackageParts
    )
  }

  def fromToken(
    typeToken: PulumiToken,
    moduleToPackageParts: String => Seq[String],
    providerToPackageParts: String => Seq[String]
  )(implicit logger: Logger): PulumiDefinitionCoordinates = {
    typeToken match {
      case PulumiToken("pulumi", "providers", providerName) =>
        PulumiDefinitionCoordinates(
          token = typeToken,
          providerPackageParts = providerName :: Nil,
          modulePackageParts = Utils.indexModuleName :: Nil,
          definitionName = Utils.providerTypeName
        )
      case PulumiToken(providerName, moduleName, definitionName) =>
        PulumiDefinitionCoordinates(
          token = typeToken,
          providerPackageParts = providerToPackageParts(providerName),
          modulePackageParts = moduleToPackageParts(moduleName),
          definitionName = definitionName
        )
    }
  }

  private def inputsPackage  = "inputs"
  private def outputsPackage = "outputs"
  private def enumsPackage   = "enums"

  private def capitalize(s: String)   = s(0).toUpper.toString ++ s.substring(1, s.length)
  private def decapitalize(s: String) = s(0).toLower.toString ++ s.substring(1, s.length)
  private val maxNameLength           = 200

  // This naïvely tries to avoid the limitation on the length of file paths in a file system
  // TODO: truncated file names with a suffix might still potentially clash with each other
  private def uniquelyTruncateTypeName(name: String) = {
    val preservedPrefix   = name.substring(0, maxNameLength)
    val removedSuffixHash = Math.abs(name.substring(maxNameLength, name.length).hashCode)
    val truncatedName     = s"${preservedPrefix}__${removedSuffixHash}__"

    truncatedName
  }

  private def mangleTypeName(name: String)(implicit logger: Logger) = {
    val truncated = if (name.length <= maxNameLength) {
      name
    } else {
      val truncated = uniquelyTruncateTypeName(name)
      logger.warn(s"Mangled type name '$name' as '$truncated' (truncated)")
      truncated
    }
    val capitalized = capitalize(truncated)
    if (capitalized != truncated)
      logger.debug(s"Mangled type name '$name' as '$capitalized' (capitalized)")

    capitalized
  }

  private def mangleMethodName(name: String)(implicit logger: Logger) = {
    val decapitalized = decapitalize(name)
    if (decapitalized != name)
      logger.debug(s"Mangled method name '$name' as '$decapitalized' (decapitalized)")
    decapitalized
  }

  @throws[PulumiDefinitionCoordinatesError]("if 'definitionName' is empty")
  def apply(
    token: PulumiToken,
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    definitionName: String
  )(implicit logger: Logger): PulumiDefinitionCoordinates = {
    if (definitionName.isBlank)
      throw PulumiDefinitionCoordinatesError("Unexpected empty 'definitionName' parameter")
    new PulumiDefinitionCoordinates(token, providerPackageParts, modulePackageParts, definitionName)
  }
}
