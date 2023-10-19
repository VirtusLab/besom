package besom.codegen

import scala.meta._
import scala.meta.dialects.Scala33

case class ClassCoordinates private (
  private val providerPackageParts: Seq[String],
  private val modulePackageParts: Seq[String],
  className: String
) {
  // "index" is a placeholder module for classes that should be in
  // the root package (according to pulumi's convention)
  // Needs to be used in Pulumi types, but should NOT be translated to Scala code

  import ClassCoordinates._

  private def sanitizeParts(parts: Seq[String]): Seq[String] = {
    parts
      .filterNot(_.isBlank)
      .map(_.replace("-", ""))
      .flatMap(_.split("/"))
  }

  private def packageRef: Term.Ref = {
    try {
      val moduleParts = modulePackageParts match {
        case "index" :: tail => tail
        case p               => p
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
    val moduleParts = modulePackageParts match {
      case "index" :: tail => tail
      case moduleName :: tail =>
        if (providerConfig.noncompiledModules.contains(moduleName))
          s".${moduleName}" +: tail // A leading dot excludes a directory from scala-cli sources
        else
          moduleName :: tail
      case p => p
    }
    FilePath(Seq("src") ++ sanitizeParts(moduleParts) ++ Seq(s"${className}.scala"))
  }
}

object ClassCoordinates {
  private val baseApiPackagePrefixParts: Seq[String]                  = Seq("besom", "api")
  private def validate[A](test: Boolean, left: => A): Either[A, Unit] = Either.cond(test, (), left)

  def apply(
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    className: String
  ): Either[ClassCoordinatesError, ClassCoordinates] =
    for {
      _ <- validate(className.nonEmpty, ClassCoordinatesError("Unexpected empty 'className' parameter"))
    } yield new ClassCoordinates(providerPackageParts, modulePackageParts, className)
}
