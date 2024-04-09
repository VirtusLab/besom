package besom.codegen

import scala.meta.*

case class ScalaDefinitionCoordinates private (
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  definitionName: Option[String],
  selectionName: Option[String]
) {
  import ScalaDefinitionCoordinates.*

  def withSelectionName(selectionName: Option[String]): ScalaDefinitionCoordinates =
    copy(selectionName = selectionName)

  // only used for package parts sanitization
  // DO NOT use for splitting the package parts
  private def sanitizeParts(parts: Seq[String]): List[String] = {
    parts.toList
      .filterNot(_.isBlank)
      .map(_.replace("-", ""))
      .map(_.replace(".", ""))
      .map(_.toLowerCase)
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
          s"Cannot generate package reference for definitionName: '$definitionName', " +
            s"selectionName: '${selectionName.getOrElse("no selection")}', " +
            s"providerPackageParts: ${providerPackageParts.mkString("[", ",", "]")}, " +
            s"modulePackageParts: ${modulePackageParts.mkString("[", ",", "]")}",
          e
        )
    }
  }

  def definitionTermName: Option[Term.Name] = definitionName.map(Term.Name(_))
  def definitionTypeName: Option[Type.Name] = definitionName.map(Type.Name(_))
  def selectionTermName: Option[Term.Name]  = selectionName.map(Term.Name(_))
  def selectionTypeName: Option[Type.Name]  = selectionName.map(Type.Name(_))

  def termRef: Term.Ref = (definitionTermName, selectionTermName) match {
    case (Some(d), Some(s)) => Term.Select(Term.Select(packageRef, d), s)
    case (Some(d), None)    => Term.Select(packageRef, d)
    case (None, Some(s))    => Term.Select(packageRef, s)
    case (None, None)       => packageRef
  }
  def typeRef: Type.Ref = (definitionTypeName, selectionTypeName) match {
    case (Some(d), Some(s)) => Type.Select(Term.Select(packageRef, Term.Name(d.value)), s)
    case (Some(d), None)    => Type.Select(packageRef, d)
    case (None, Some(s))    => Type.Select(packageRef, s)
    case (None, None) =>
      throw ScalaDefinitionCoordinatesError(
        s"Cannot generate type reference for definitionName: '$definitionName', selectionName: '${selectionName}'"
      )
  }

  def filePath(using providerConfig: Config.Provider): FilePath = {
    // we DO NOT remove index from the file path, we add it if necessary
    val moduleParts = sanitizeParts(modulePackageParts) match {
      case moduleName :: tail =>
        // HACK: we need to exclude a module it does not compile
        if (providerConfig.nonCompiledModules.contains(moduleName)) {
          println(s"Excluding module: $moduleName")
          s".${moduleName}" +: tail // A leading dot excludes a directory from scala-cli sources
        } else {
          moduleName :: tail
        }
      case Nil => Utils.indexModuleName :: Nil
    }
    val fileName = definitionName.getOrElse(definitionName.orElse(selectionName).getOrElse("package"))
    FilePath(Seq("src") ++ moduleParts ++ Seq(s"${fileName}.scala"))
  }
}

object ScalaDefinitionCoordinates {
  private val baseApiPackagePrefixParts: Seq[String] = Seq("besom", "api")

  @throws[ScalaDefinitionCoordinatesError]("if 'definitionName' is empty")
  def apply(
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    definitionName: Option[String],
    selectionName: Option[String] = None
  ): ScalaDefinitionCoordinates = {
    if definitionName.map(_.isBlank).getOrElse(false)
    then throw ScalaDefinitionCoordinatesError(s"Cannot create ScalaDefinitionCoordinates with blank definitionName: $definitionName")
    new ScalaDefinitionCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      definitionName = definitionName,
      selectionName = selectionName
    )
  }
}
