package besom.codegen

case class PulumiDefinitionCoordinates private (
  token: PulumiToken,
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  private val definitionName: String,
  private val wireName: Option[String]
) {
  import PulumiDefinitionCoordinates.*

  def topLevelPackage: String = modulePackageParts.headOption.getOrElse {
    throw new RuntimeException(s"No top level package for $token")
  }

  def className(asArgsType: Boolean)(implicit logger: Logger): String = {
    val classNameSuffix = if (asArgsType) "Args" else ""
    mangleTypeName(definitionName) ++ classNameSuffix
  }

  def asResourceClass(asArgsType: Boolean)(implicit logger: Logger): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = Some(className(asArgsType)),
      wireName = wireName
    )
  }
  def asObjectClass(asArgsType: Boolean)(implicit logger: Logger): ScalaDefinitionCoordinates = {
    // because we have no plain inputs we can simplify and all Args are inputs
    val packageSuffix = if asArgsType then inputsPackage else outputsPackage
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ packageSuffix,
      definitionName = Some(className(asArgsType)),
      wireName = wireName
    )
  }
  def asEnumClass(implicit logger: Logger): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ enumsPackage,
      definitionName = Some(className(asArgsType = false)),
      wireName = wireName
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

  def asFunctionClass(using Logger): ScalaDefinitionCoordinates = {
    val (methodPrefix, methodName) = splitMethodName(definitionName)
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = methodPrefix,
      selectionName = Some(mangleMethodName(methodName)),
      wireName = wireName
    )
  }
  def asFunctionArgsClass(using Logger): ScalaDefinitionCoordinates = {
    val (methodPrefix, methodName) = splitMethodName(definitionName)
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = Some((methodPrefix.toSeq :+ mangleTypeName(methodName)).mkString("") + "Args"),
      wireName = wireName
    )
  }
  def asFunctionResultClass(using Logger): ScalaDefinitionCoordinates = {
    val (methodPrefix, methodName) = splitMethodName(definitionName)
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = Some((methodPrefix.toSeq :+ mangleTypeName(methodName)).mkString("") + "Result"),
      wireName = wireName
    )
  }

  def asConfigClass(using Logger): ScalaDefinitionCoordinates = {
    ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = Some(mangleTypeName(definitionName)),
      wireName = wireName
    )
  }
}

object PulumiDefinitionCoordinates {
  def fromRawToken(
    typeToken: String,
    moduleToPackageParts: String => Seq[String],
    providerToPackageParts: String => Seq[String],
    overrideDefinitionName: Option[String] = None
  )(using Logger): PulumiDefinitionCoordinates = {
    fromToken(
      typeToken = PulumiToken(typeToken),
      moduleToPackageParts = moduleToPackageParts,
      providerToPackageParts = providerToPackageParts,
      overrideDefinitionName = overrideDefinitionName
    )
  }

  def fromToken(
    typeToken: PulumiToken,
    moduleToPackageParts: String => Seq[String],
    providerToPackageParts: String => Seq[String],
    overrideDefinitionName: Option[String] = None
  )(using logger: Logger): PulumiDefinitionCoordinates = {
    typeToken match {
      case PulumiToken("pulumi", "providers", providerName) =>
        PulumiDefinitionCoordinates(
          token = typeToken,
          providerPackageParts = providerName :: Nil,
          modulePackageParts = Utils.indexModuleName :: Nil,
          definitionName = Utils.providerTypeName,
          wireName = None
        )
      case PulumiToken(providerName, moduleName, definitionName) =>
        if overrideDefinitionName.isDefined then logger.warn(s"Overriding definition name for $typeToken to ${overrideDefinitionName.get}")

        PulumiDefinitionCoordinates(
          token = typeToken,
          providerPackageParts = providerToPackageParts(providerName),
          modulePackageParts = moduleToPackageParts(moduleName),
          definitionName = overrideDefinitionName.getOrElse(definitionName),
          wireName = overrideDefinitionName.map(_ => definitionName)
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

  private[codegen] def words(input: String): List[String] =
    import scala.collection.mutable
    val words = mutable.ArrayBuffer.empty[mutable.ArrayBuffer[Char]]
    for (c, i) <- input.zipWithIndex do
      def isWithinBounds(j: Int) = j >= 0 && j < input.length
      val prev                   = if isWithinBounds(i - 1) then Some(input(i - 1)) else None
      val next                   = if isWithinBounds(i + 1) then Some(input(i + 1)) else None

      (prev.map(_.isUpper), c.isUpper, next.map(_.isUpper)) match
        // word boundary - exclusive
        case _ if c.isWhitespace || !c.isLetterOrDigit /* special character */ =>
          words += mutable.ArrayBuffer.empty // new word
        // word boundary - inclusive
        case (None, _, _) /* first letter */
            | (Some(false), true, Some(false)) /* down-up-down */
            | (Some(true), true, Some(false)) /* up-up-down */
            | (Some(false), true, Some(true)) /* down-up-up */
            | (Some(false), true, None) /* down-up-nil */ =>
          words += mutable.ArrayBuffer.empty // new word
          words(words.length - 1) += c // append to the new word
        // word continuation
        case _ => words(words.length - 1) += c // append to the current word
    end for
    words.filter(_.nonEmpty).map(_.mkString).toList
  end words

  private[codegen] def normalize(input: String): String = words(input).map(_.toLowerCase.capitalize).mkString

  @throws[PulumiDefinitionCoordinatesError]("if 'definitionName' is empty")
  def apply(
    token: PulumiToken,
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    definitionName: String,
    wireName: Option[String]
  ): PulumiDefinitionCoordinates = {
    if (definitionName.isBlank)
      throw PulumiDefinitionCoordinatesError("Unexpected empty 'definitionName' parameter")
    new PulumiDefinitionCoordinates(token, providerPackageParts, modulePackageParts, definitionName, wireName)
  }
}
