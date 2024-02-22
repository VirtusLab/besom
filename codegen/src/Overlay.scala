package besom.codegen

object Overlay:
  def readFiles(
    packageInfo: PulumiPackageInfo,
    token: PulumiToken,
    scalaDefinitions: Vector[ScalaDefinitionCoordinates]
  )(using config: Config, logger: Logger): Vector[SourceFile] =
    given Config.Provider = packageInfo.providerConfig

    scalaDefinitions.flatMap { scalaDefinition =>
      val relativeFilePath = scalaDefinition.filePath.copy(scalaDefinition.filePath.pathParts.tail) // drop "src" from the path
      val path             = config.overlaysDir / packageInfo.name / relativeFilePath.osSubPath
      if os.exists(path) then
        val content = os.read(path)
        Vector(SourceFile(scalaDefinition.filePath, content))
      else
        logger.info(s"Token '${token.asString}' was not generated because it was marked as overlay and not found at '${path}'")
        Vector.empty
    }
