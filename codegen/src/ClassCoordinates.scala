package besom.codegen

import scala.meta._
import scala.meta.dialects.Scala33

object ClassCoordinates {
  val baseApiPackagePrefixParts = Seq("besom", "api")
}

case class ClassCoordinates(
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  val className: String,
) {
  import ClassCoordinates._

  private def packageRef: Term.Ref = {
    val moduleParts = if (modulePackageParts.head == Utils.indexModuleName) modulePackageParts.tail else modulePackageParts
    val partsHead :: partsTail = baseApiPackagePrefixParts ++ providerPackageParts ++ moduleParts
    partsTail.foldLeft[Term.Ref](Term.Name(partsHead))((acc, name) => Term.Select(acc, Term.Name(name)))
  }

  def fullyQualifiedTypeRef: Type.Ref = {
    Type.Select(packageRef, Type.Name(className))
  }

  def fullPackageName: String = {
    packageRef.syntax
  }

  def filePath(implicit providerConfig: Config.ProviderConfig): FilePath = {
    val moduleName = modulePackageParts.head
    val moduleParts =
      if (providerConfig.noncompiledModules.contains(moduleName))
        s".${moduleName}" +: modulePackageParts.tail // A leading dot excludes a directory from scala-cli sources
      else
        modulePackageParts
    FilePath(Seq("src") ++ moduleParts ++ Seq(s"${className}.scala"))
  }
}