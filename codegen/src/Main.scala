package besom.codegen

import besom.codegen.UpickleApi.*
import besom.codegen.{PackageVersion, SchemaFile}

object Main {
  def main(args: Array[String]): Unit = {
    val (tracing, remainingArgs) = args.toList match {
      case lst if lst.contains("--trace") => (true, lst.filterNot(_ == "--trace"))
      case lst                            => (false, lst)
    }

    val result = remainingArgs match {
      case "named" :: name :: version :: Nil =>
        given Config = Config(tracing = tracing)
        generator.generatePackageSources(metadata = PackageMetadata(name, version))
      case "named" :: name :: version :: outputDir :: Nil =>
        given Config = Config(outputDir = Some(os.rel / outputDir), tracing = tracing)
        generator.generatePackageSources(metadata = PackageMetadata(name, version))
      case "metadata" :: metadataPath :: Nil =>
        given Config = Config(tracing = tracing)
        generator.generatePackageSources(metadata = PackageMetadata.fromJsonFile(os.Path(metadataPath)))
      case "metadata" :: metadataPath :: outputDir :: Nil =>
        given Config = Config(outputDir = Some(os.rel / outputDir), tracing = tracing)
        generator.generatePackageSources(metadata = PackageMetadata.fromJsonFile(os.Path(metadataPath)))
      case "schema" :: name :: version :: schemaPath :: Nil =>
        given Config = Config(tracing = tracing)
        generator.generatePackageSources(
          metadata = PackageMetadata(name, version),
          schema = Some(os.Path(schemaPath))
        )
      case "schema" :: name :: version :: schemaPath :: organization :: url :: vcs :: license :: repository :: developer :: Nil =>
        given Config = Config(
          organization = organization,
          url = url,
          vcs = vcs,
          license = license,
          repository = repository,
          developers = List(developer),
          tracing = tracing
        )
        generator.generatePackageSources(
          metadata = PackageMetadata(name, version),
          schema = Some(os.Path(schemaPath))
        )
      case _ =>
        System.err.println(
          s"""|Unknown arguments: '${args.mkString(" ")}'
              |
              |Usage:
              |  named <name> <version> [outputDir] [--trace]               - Generate package from name and version
              |  metadata <metadataPath> [outputDir] [--trace]              - Generate package from metadata file
              |  schema <name> <version> <schemaPath> [outputDir] [--trace] - Generate package from schema file
              |  schema <name> <version> <schemaPath> <organization> <url> <vcs> <license> <repository> <developer> [outputDir] [--trace] - Generate package from schema file
              |""".stripMargin
        )
        sys.exit(1)
    }
    System.out.println(result.asString)
  }
}

object generator {
  case class Result(
    metadata: PackageMetadata,
    outputDir: os.Path,
    total: Int
  ):
    def asString: String =
      s"""|Generated package:
          |  name: ${this.metadata.name}
          |  version: ${this.metadata.version.getOrElse("none")}
          |  outputDir: ${this.outputDir.relativeTo(os.pwd)}
          |  total: ${this.total}
          |${
           if this.metadata.dependencies.nonEmpty
           then
             this.metadata.dependencies
               .map { case PackageMetadata(name, version, _, _) => s"  - $name:${version.orDefault}" }
               .mkString("  dependencies:\n", "\n", "")
           else ""
         }
          |""".stripMargin.trim

  end Result

  // noinspection ScalaWeakerAccess
  def generatePackageSources(
    metadata: PackageMetadata,
    schema: Option[SchemaFile] = None
  )(using config: Config): Result = {
    given logger: Logger                            = Logger()
    given schemaProvider: DownloadingSchemaProvider = DownloadingSchemaProvider()

    // detect possible problems with GH API throttling
    // noinspection ScalaUnusedSymbol
    if !sys.env.contains("GITHUB_TOKEN") then logger.warn("Setting GITHUB_TOKEN environment variable might solve some problems")

    val packageInfo    = schemaProvider.packageInfo(metadata, schema)
    val packageName    = packageInfo.name
    val packageVersion = packageInfo.version

    given typeMapper: TypeMapper = TypeMapper(packageInfo)

    val outputDir: os.Path =
      config.outputDir.getOrElse(os.rel / packageName / packageVersion.asString).resolveFrom(config.codegenDir)

    val total = generatePackageSources(packageInfo, outputDir)
    logger.info(s"Finished generating package '$packageName:$packageVersion' codebase (${total} files)")

    val dependencies = schemaProvider.dependencies(packageName, packageVersion).map { case (name, version) =>
      PackageMetadata(name, Some(version))
    }
    logger.debug(
      s"Dependencies: \n${dependencies.map { case PackageMetadata(name, version, _, _) => s"- $name:$version\n" }}"
    )

    Result(
      metadata = packageInfo.asPackageMetadata.withDependencies(dependencies.toVector),
      outputDir = outputDir,
      total = total
    )
  }

  private def generatePackageSources(
    packageInfo: PulumiPackageInfo,
    outputDir: os.Path
  )(using
    logger: Logger,
    config: Config,
    schemaProvider: SchemaProvider,
    typeMapper: TypeMapper
  ): Int = {
    // Print diagnostic information
    logger.info {
      val relOutputDir = outputDir.relativeTo(os.pwd)
      s"""|Generating package '${packageInfo.name}:${packageInfo.version}' into '$relOutputDir'
          | - Besom version          : ${config.besomVersion}
          | - Scala version          : ${config.scalaVersion}
          | - Java version           : ${config.javaVersion}
          | - Java target version    : ${config.javaTargetVersion}
          |
          | - Resources: ${packageInfo.pulumiPackage.resources.size}
          | - Types    : ${packageInfo.pulumiPackage.types.size}
          | - Functions: ${packageInfo.pulumiPackage.functions.size}
          | - Config   : ${packageInfo.pulumiPackage.config.variables.size}
          |""".stripMargin
    }

    // make sure we don't have a dirty state
    os.remove.all(outputDir)
    os.makeDir.all(outputDir)

    val codeGen = new CodeGen
    try {
      val sources = codeGen.sourcesFromPulumiPackage(packageInfo)
      sources
        .foreach { sourceFile =>
          val filePath = outputDir / sourceFile.filePath.osSubPath
          os.makeDir.all(filePath / os.up)
          logger.debug(s"Writing source file: '${filePath.relativeTo(os.pwd)}'")
          try {
            os.write(filePath, sourceFile.sourceCode, createFolders = true)
          } catch {
            case e: java.nio.file.FileAlreadyExistsException =>
              // write the duplicate class for debugging purposes
              val fileDuplicate = filePath / os.up / s"${filePath.last}.duplicate"
              os.write(fileDuplicate, sourceFile.sourceCode, createFolders = true)
              val message = s"Duplicate file '${fileDuplicate.relativeTo(os.pwd)}' while, " +
                s"generating package '${packageInfo.name}:${packageInfo.version}', error: ${e.getMessage}"

              throw Exception(message, e)
          }
        }
      sources.size
    } catch {
      case e: Throwable =>
        logger.error(s"Error generating package '${packageInfo.name}:${packageInfo.version}', error: ${e.getMessage}")
        throw e
    } finally {
      val logFile = outputDir / ".codegen-log.txt"

      if logger.hasErrors
      then logger.error(s"Some problems were encountered during the code generation. See ${logFile.relativeTo(os.pwd)}")
      else if logger.hasWarnings
      then logger.warn(s"Some warnings were encountered during the code generation. See ${logFile.relativeTo(os.pwd)}")
      else logger.info(s"Code generation finished successfully. See ${logFile.relativeTo(os.pwd)}")

      logger.writeToFile(logFile)
    }
  }
}
