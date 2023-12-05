package besom.codegen

import scala.meta.*

case class ScalaDefinitionCoordinates private (
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  definitionName: String
) {
  import ScalaDefinitionCoordinates.*

  // only used for package parts sanitization
  // DO NOT use for splitting the package parts
  private def sanitizeParts(parts: Seq[String]): List[String] = {
    parts.toList
      .filterNot(_.isBlank)
      .map(_.replace("-", ""))
  }

  def packageRef: Term.Ref = {
    try {
      // we remove index from the package, if necessary
      val moduleParts = modulePackageParts.toList match {
        case head :: tail if head == Utils.indexModuleName => tail
        case p                                             => p
      }
      scalameta.ref(sanitizeParts(baseApiPackagePrefixParts ++ providerPackageParts ++ moduleParts))
    } catch {
      case e: org.scalameta.invariants.InvariantFailedException =>
        throw ScalaDefinitionCoordinatesError(
          s"Cannot generate package reference for definition: \"$definitionName\", " +
            s"providerPackageParts: ${providerPackageParts.mkString("[", ",", "]")}, " +
            s"modulePackageParts: ${modulePackageParts.mkString("[", ",", "]")}",
          e
        )
    }
  }

  def termName: Term.Name = Term.Name(definitionName)
  def typeName: Type.Name = Type.Name(definitionName)
  def termRef: Term.Ref = Term.Select(packageRef, termName)
  def typeRef: Type.Ref = Type.Select(packageRef, typeName)

  def filePath(implicit providerConfig: Config.ProviderConfig): FilePath = {
    // we DO NOT remove index from the file path, we add it if necessary
    val moduleParts = sanitizeParts(modulePackageParts) match {
      case moduleName :: tail =>
        // we need to exclude a module it does not compile
        if (providerConfig.noncompiledModules.contains(moduleName)) {
          println(s"Excluding module: $moduleName")
          s".${moduleName}" +: tail // A leading dot excludes a directory from scala-cli sources
        } else {
          moduleName :: tail
        }
      case Nil => Utils.indexModuleName :: Nil
    }
    FilePath(Seq("src") ++ moduleParts ++ Seq(s"${definitionName}.scala"))
  }
}

object ScalaDefinitionCoordinates {
  private val baseApiPackagePrefixParts: Seq[String] = Seq("besom", "api")

  @throws[ScalaDefinitionCoordinatesError]("if 'definitionName' is empty")
  def apply(
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    definitionName: String
  ): ScalaDefinitionCoordinates = {
    if (definitionName.isBlank)
      throw ScalaDefinitionCoordinatesError(s"Cannot create ScalaDefinitionCoordinates with empty definitionName")
    new ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = definitionName
    )
  }
}
