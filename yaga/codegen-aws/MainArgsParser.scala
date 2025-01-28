package yaga.codegen.aws

import yaga.codegen.core.extractor.CodegenSource
import java.nio.file.Paths

private[aws] case class MainArgsParser(
  codegenSources: List[CodegenSource],
  handlerClassFullName: Option[String],
  packagePrefix: Option[String],
  generateInfra: Option[Boolean],
  outputDir: Option[String],
  summaryFile: Option[String],
  noCleanup: Option[Boolean]
):
  // TODO don't allow overriding non-repeated parameters
  def parseArgs(args: List[String]): CodegenMainArgs = 
    args match
      case "--maven-artifact" :: artifactMavenCoordinates :: rest =>
        val mavenSource = CodegenSource.MavenArtifact.parseCoordinates(artifactMavenCoordinates)
        this.copy(
          codegenSources = codegenSources :+ mavenSource
        ).parseArgs(rest)
      case "--local-jar" :: filePath :: rest =>
        val path = Paths.get(filePath)
        assert(path.isAbsolute, s"Path ${filePath} is not absolute")
        val localSource = CodegenSource.LocalJar(absolutePath = path)
        this.copy(
          codegenSources = codegenSources :+ localSource
        ).parseArgs(rest)
      case "--handler-class" :: handlerClassFullName :: rest =>
        this.copy(
          handlerClassFullName = Some(handlerClassFullName)
        ).parseArgs(rest)
      case "--package-prefix" :: packagePrefix :: rest =>
        this.copy(
          packagePrefix = Some(packagePrefix)
        ).parseArgs(rest)
      case "--with-infra" :: rest =>
        this.copy(
          generateInfra = Some(true)
        ).parseArgs(rest)
      case "--output-dir" :: outputDir :: rest =>
        this.copy(
          outputDir = Some(outputDir)
        ).parseArgs(rest)
      case "--summary-file" :: summaryFile :: rest =>
        this.copy(
          summaryFile = Some(summaryFile)
        ).parseArgs(rest)
      case "--no-cleanup" :: rest =>
        this.copy(
          noCleanup = Some(true)
        ).parseArgs(rest)
      case Nil =>
        assert(codegenSources.nonEmpty, "Missing codegen sources")
        CodegenMainArgs(
          codegenSources = codegenSources,
          handlerClassFullName = handlerClassFullName,
          packagePrefix = packagePrefix.getOrElse(throw Exception("Missing package prefix")),
          generateInfra = generateInfra.getOrElse(false),
          outputDir = outputDir.getOrElse(throw Exception("Missing output dir")),
          summaryFile = summaryFile,
          noCleanup = noCleanup.getOrElse(false)
        )
      case _ =>
        throw Exception(s"Wrong main arguments: ${args}")
  

object MainArgsParser:
  def parse(args: Seq[String]): CodegenMainArgs =
    val emptyParser = MainArgsParser(
      codegenSources = Nil,
      handlerClassFullName = None,
      packagePrefix = None,
      generateInfra = None,
      outputDir = None,
      summaryFile = None,
      noCleanup = None
    )
    emptyParser.parseArgs(args.toList)
