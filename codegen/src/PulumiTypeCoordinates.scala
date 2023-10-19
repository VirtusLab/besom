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

  def asResourceClass(
    asArgsType: Boolean
  )(implicit logger: Logger): Either[PulumiTypeCoordinatesError, ClassCoordinates] = {
    ClassCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts,
      className = className(asArgsType)
    ) match {
      case Left(e) =>
        Left(
          PulumiTypeCoordinatesError(
            s"Cannot generate resource class coordinates, typeName: $typeName, " +
              s"modulePackageParts: ${modulePackageParts.mkString("[", ",", "]")}" +
              s"providerPackageParts: ${providerPackageParts.mkString("[", ",", "]")}",
            e
          )
        )
      case Right(c) => Right(c)
    }
  }

  def asObjectClass(
    asArgsType: Boolean
  )(implicit logger: Logger): Either[PulumiTypeCoordinatesError, ClassCoordinates] = {
    val packageSuffix = if (asArgsType) "inputs" else "outputs"
    ClassCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ packageSuffix,
      className = className(asArgsType)
    ) match {
      case Left(e) =>
        Left(
          PulumiTypeCoordinatesError(
            s"Cannot generate object class coordinates, typeName: $typeName, " +
              s"modulePackageParts: ${modulePackageParts.mkString("[", ",", "]")}" +
              s"providerPackageParts: ${providerPackageParts.mkString("[", ",", "]")}",
            e
          )
        )
      case Right(c) => Right(c)
    }
  }

  def asEnumClass(implicit logger: Logger): Either[PulumiTypeCoordinatesError, ClassCoordinates] = {
    ClassCoordinates(
      providerPackageParts = providerPackageParts,
      modulePackageParts = modulePackageParts :+ "enums",
      className = mangleTypeName(typeName)
    ) match {
      case Left(e) =>
        Left(
          PulumiTypeCoordinatesError(
            s"Cannot generate enum class coordinates, typeName: $typeName, " +
              s"modulePackageParts: ${modulePackageParts.mkString("[", ",", "]")}" +
              s"providerPackageParts: ${providerPackageParts.mkString("[", ",", "]")}",
            e
          )
        )
      case Right(c) => Right(c)
    }
  }
}

object PulumiTypeCoordinates {
  private def capitalize(s: String) = s(0).toUpper.toString ++ s.substring(1, s.length)
  private val maxNameLength         = 200

  // This naïvely tries to avoid the limitation on the length of file paths in a file system
  // TODO: truncated file names with a suffix might still potentially clash with each other
  private def uniquelyTruncateTypeName(name: String)(implicit logger: Logger) =
    if (name.length <= maxNameLength) {
      name
    } else {
      val preservedPrefix   = name.substring(0, maxNameLength)
      val removedSuffixHash = Math.abs(name.substring(maxNameLength, name.length).hashCode)
      val truncatedName     = s"${preservedPrefix}__${removedSuffixHash}__"

      truncatedName
    }

  private def mangleTypeName(name: String)(implicit logger: Logger) = {
    val mangledName = capitalize(uniquelyTruncateTypeName(name))
    if (mangledName != name)
      logger.warn(s"Mangled type name '$name' as '$mangledName'")
    mangledName
  }

  def apply(
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    typeName: String
  ): Either[PulumiTypeCoordinatesError, PulumiTypeCoordinates] =
    for {
      _ <- validate(typeName.nonEmpty, PulumiTypeCoordinatesError("Unexpected empty 'typeName' parameter"))
    } yield new PulumiTypeCoordinates(providerPackageParts, modulePackageParts, typeName)

  private def validate[A](test: Boolean, left: => A): Either[A, Unit] = Either.cond(test, (), left)
}
