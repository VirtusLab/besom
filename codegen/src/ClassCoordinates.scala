package besom.codegen

import scala.meta._
import scala.meta.dialects.Scala33

case class ClassCoordinates private (
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  className: String
) {
  import ClassCoordinates._

  // only used for package parts sanitization
  // DO NOT use for splitting the package parts
  private def sanitizeParts(parts: Seq[String]): List[String] = {
    parts
      .toList
      .filterNot(_.isBlank)
      .map(_.replace("-", ""))
  }

  private def packageRef: Term.Ref = {
    try {
      // we remove index from the package, if necessary
      val moduleParts = modulePackageParts.toList match {
        case head :: tail if head == Utils.IndexModuleName => tail
        case p                                             => p
      }
      val partsHead :: partsTail = sanitizeParts(baseApiPackagePrefixParts ++ providerPackageParts ++ moduleParts)
      partsTail.foldLeft[Term.Ref](Term.Name(partsHead))((acc, name) => Term.Select(acc, Term.Name(name)))
    } catch {
      case e: org.scalameta.invariants.InvariantFailedException =>
        throw ClassCoordinatesError(
          s"Cannot generate package reference for className: $className, " +
            s"providerPackageParts: ${providerPackageParts.mkString("[", ",", "]")}, " +
            s"modulePackageParts: ${modulePackageParts.mkString("[", ",", "]")}",
          e
        )
    }
  }

  def fullyQualifiedTypeRef: Type.Ref = {
    Type.Select(packageRef, Type.Name(className))
  }

  def fullPackageName: String = {
    packageRef.syntax
  }

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
      case Nil => Utils.IndexModuleName :: Nil
      case p   => p
    }
    FilePath(Seq("src") ++ moduleParts ++ Seq(s"${className}.scala"))
  }
}

object ClassCoordinates {
  private val baseApiPackagePrefixParts: Seq[String] = Seq("besom", "api")

  @throws[ClassCoordinatesError]("if 'className' is empty")
  def apply(
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    className: String
  ): ClassCoordinates = {
    if (className.isBlank)
      throw ClassCoordinatesError(s"Cannot create ClassCoordinates with empty className")
    new ClassCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      className = className
    )
  }
}