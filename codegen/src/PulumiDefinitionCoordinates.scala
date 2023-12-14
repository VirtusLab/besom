package besom.codegen

case class PulumiDefinitionCoordinates private (
  token: PulumiToken,
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  private val definitionName: String
) {
  import PulumiDefinitionCoordinates.*

  def className(asArgsType: Boolean)(implicit logger: Logger): String = {
    val classNameSuffix = if (asArgsType) "Args" else ""
    mangleTypeName(definitionName) ++ classNameSuffix
  }

  def asResourceClass(asArgsType: Boolean)(implicit logger: Logger): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = Some(className(asArgsType))
    )
  }
  def asObjectClass(asArgsType: Boolean)(implicit logger: Logger): ScalaDefinitionCoordinates = {
    val packageSuffix = if (asArgsType) inputsPackage else outputsPackage
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ packageSuffix,
      definitionName = Some(className(asArgsType))
    )
  }
  def asEnumClass(implicit logger: Logger): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ enumsPackage,
      definitionName = Some(mangleTypeName(definitionName)),
      isEnum = true
    )
  }

  private def splitMethodName(definitionName: String): (Option[String], String) = {
    definitionName.split('/') match
      case Array(methodName) =>
        (None, methodName)
      case Array(className, methodName) =>
        // filter out the function name duplication
        (Some(className).filterNot(_ == methodName), methodName)
      case _ => throw PulumiDefinitionCoordinatesError(s"Invalid definition name '$definitionName'")
  }

  def resourceMethod(implicit logger: Logger): ScalaDefinitionCoordinates = {
    val (methodPrefix, methodName) = splitMethodName(definitionName)
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = methodPrefix,
      selectionName = Some(mangleMethodName(methodName))
    )
  }
  def methodArgsClass(implicit logger: Logger): ScalaDefinitionCoordinates = {
    val (methodPrefix, methodName) = splitMethodName(definitionName)
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = Some((methodPrefix.toSeq :+ mangleTypeName(methodName)).mkString("") + "Args")
    )
  }
  def methodResultClass(implicit logger: Logger): ScalaDefinitionCoordinates = {
    val (methodPrefix, methodName) = splitMethodName(definitionName)
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = Some((methodPrefix.toSeq :+ mangleTypeName(methodName)).mkString("") + "Result")
    )
  }

  def asConfigClass(implicit logger: Logger): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = Some(mangleTypeName(definitionName))
    )
  }
}

object PulumiDefinitionCoordinates {
  def fromRawToken(
    typeToken: String,
    moduleToPackageParts: String => Seq[String],
    providerToPackageParts: String => Seq[String]
  ): PulumiDefinitionCoordinates = {
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
  ): PulumiDefinitionCoordinates = {
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
    val normalized = normalize(truncated)
    if (normalized != truncated)
      logger.debug(s"Mangled type name '$name' as '$normalized' (normalized)")

    normalized
  }

  private def mangleMethodName(name: String)(implicit logger: Logger) = {
    val decapitalized = decapitalize(name)
    if (decapitalized != name)
      logger.debug(s"Mangled method name '$name' as '$decapitalized' (decapitalized)")
    decapitalized
  }

  private[codegen] def normalize(input: String): String =
    val head = input.head.toUpper
    val it   = input.tail.iterator.buffered
    val tail =
      for c <- it
      yield
        if it.headOption.map(_.isUpper).getOrElse(true)
        then c.toLower
        else c

    (head +: tail.toArray).mkString
  end normalize

  @throws[PulumiDefinitionCoordinatesError]("if 'definitionName' is empty")
  def apply(
    token: PulumiToken,
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    definitionName: String
  ): PulumiDefinitionCoordinates = {
    if (definitionName.isBlank)
      throw PulumiDefinitionCoordinatesError("Unexpected empty 'definitionName' parameter")
    new PulumiDefinitionCoordinates(token, providerPackageParts, modulePackageParts, definitionName)
  }
}
