package besom.codegen

case class PulumiDefinitionCoordinates private (
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  private val definitionName: String
) {
  import PulumiDefinitionCoordinates._

  def className(asArgsType: Boolean)(implicit logger: Logger): String = {
    val classNameSuffix = if (asArgsType) "Args" else ""
    mangleTypeName(definitionName) ++ classNameSuffix
  }

  def asResourceClass(asArgsType: Boolean)(implicit logger: Logger): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = className(asArgsType)
    )
  }
  def asObjectClass(asArgsType: Boolean)(implicit logger: Logger): ScalaDefinitionCoordinates = {
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

  private def splitMethodName(definitionName: String): (Seq[String], String) = {
    val methodNameParts = definitionName.split("/")
    val methodName = methodNameParts.last
    val methodPrefix = methodNameParts.init.filterNot(_ == methodName) // filter out the function name duplication
    (methodPrefix, methodName)
  }

  def resourceMethod(implicit logger: Logger): ScalaDefinitionCoordinates = {
    val (methodPrefix, methodName) = splitMethodName(definitionName)
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts ++ methodPrefix,
      definitionName = mangleMethodName(methodName)
    )
  }
  def methodArgsClass(implicit logger: Logger): ScalaDefinitionCoordinates = {
    val (methodPrefix, methodName) = splitMethodName(definitionName)
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = (methodPrefix :+ mangleTypeName(methodName)).mkString("") + "Args"
    )
  }
  def methodResultClass(implicit logger: Logger): ScalaDefinitionCoordinates = {
    val (methodPrefix, methodName) = splitMethodName(definitionName)
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = (methodPrefix :+ mangleTypeName(methodName)).mkString("") + "Result"
    )
  }
}

object PulumiDefinitionCoordinates {

  def fromRawToken(
    typeToken: String,
    moduleToPackageParts: String => Seq[String],
    providerToPackageParts: String => Seq[String]
  ): PulumiDefinitionCoordinates = {
    fromToken(PulumiToken(typeToken), moduleToPackageParts, providerToPackageParts)
  }

  def fromToken(
    typeToken: PulumiToken,
    moduleToPackageParts: String => Seq[String],
    providerToPackageParts: String => Seq[String]
  ): PulumiDefinitionCoordinates = {
    val PulumiToken(providerName, modulePortion, definitionName) = typeToken
    PulumiDefinitionCoordinates(
      providerPackageParts = providerToPackageParts(providerName),
      modulePackageParts = moduleToPackageParts(modulePortion),
      definitionName = definitionName
    )
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
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    definitionName: String
  ): PulumiDefinitionCoordinates = {
    if (definitionName.isBlank)
      throw PulumiDefinitionCoordinatesError("Unexpected empty 'definitionName' parameter")
    new PulumiDefinitionCoordinates(providerPackageParts, modulePackageParts, definitionName)
  }
}
