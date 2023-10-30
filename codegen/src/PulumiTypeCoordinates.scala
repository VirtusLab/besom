package besom.codegen

case class PulumiTypeCoordinates private (
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  private val typeName: String
) {
  import PulumiTypeCoordinates._

  def className(asArgsType: Boolean)(implicit logger: Logger): String = {
    val classNameSuffix = if (asArgsType) "Args" else ""
    mangleTypeName(typeName) ++ classNameSuffix
  }

  def asResourceClass(asArgsType: Boolean)(implicit logger: Logger): ClassCoordinates = {
    ClassCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      className = className(asArgsType)
    )
  }
  def asObjectClass(asArgsType: Boolean)(implicit logger: Logger): ClassCoordinates = {
    val packageSuffix = if (asArgsType) "inputs" else "outputs"
    ClassCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ packageSuffix,
      className = className(asArgsType)
    )
  }
  def asEnumClass(implicit logger: Logger): ClassCoordinates = {
    ClassCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ "enums",
      className = mangleTypeName(typeName)
    )
  }
}

object PulumiTypeCoordinates {
  private def capitalize(s: String) = s(0).toUpper.toString ++ s.substring(1, s.length)
  private val maxNameLength         = 200

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

  @throws[PulumiTypeCoordinatesError]("if 'typeName' is empty")
  def apply(
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    typeName: String
  ): PulumiTypeCoordinates = {
    if (typeName.isBlank)
      throw PulumiTypeCoordinatesError("Unexpected empty 'typeName' parameter")
    new PulumiTypeCoordinates(providerPackageParts, modulePackageParts, typeName)
  }
}
