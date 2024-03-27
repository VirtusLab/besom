package besom.scripts

import besom.codegen.*
import besom.model.SemanticVersion
import coursier.error.ResolutionError.CantDownloadModule
import org.virtuslab.yaml.*
import os.{Path, Shellable}

import java.util.Date
import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.concurrent.duration.*
import scala.sys.exit
import scala.util.*
import scala.util.control.NonFatal

//noinspection ScalaWeakerAccess,TypeAnnotation
object Packages:
  def main(args: String*): Unit =
    args match
      case "list" :: Nil                    => listLatestPackages(packagesDir)
      case "list-published-to-maven" :: Nil => listPublishedPackages(publishedMavenFile)
      case "local-all" :: Nil               => publishLocalAll(generateAll(packagesDir))
      case "local" :: tail                  => publishLocalSelected(generateSelected(packagesDir, tail), tail)
      case "maven-all" :: Nil               => publishMavenAll(generateAll(packagesDir))
      case "maven" :: tail                  => publishMavenSelected(generateSelected(packagesDir, tail), tail)
      case "metadata-all" :: Nil            => downloadPackagesMetadata(packagesDir)
      case "metadata" :: tail               => downloadPackagesMetadata(packagesDir, selected = tail)
      case "generate-all" :: Nil            => generateAll(packagesDir)
      case "generate" :: tail               => generateSelected(packagesDir, tail)
      case "publish-local-all" :: Nil       => publishLocalAll(generatedFile)
      case "publish-local" :: tail          => publishLocalSelected(generatedFile, tail)
      case "publish-maven-all" :: Nil       => publishMavenAll(generatedFile)
      case "publish-maven" :: tail          => publishMavenSelected(generatedFile, tail)
      case "compile" :: tail                => compileSelected(generatedFile, tail)
      case "publish-maven-no-deps" :: tail  => publishMavenSelectedNoDeps(generatedFile, tail)
      case cmd =>
        println(s"Unknown command: $cmd\n")
        println(
          s"""Usage: packages <command>
             |
             |Commands:
             |  list                               - list latest packages from Pulumi repository
             |  local-all                          - generate and publish all packages locally
             |  local <package>...                 - generate and publish selected packages locally
             |  maven-all                          - generate and publish all packages to Maven
             |  maven <package>...                 - generate and publish selected packages to Maven
             |  metadata-all                       - download all packages metadata
             |  metadata <package>...              - download selected packages metadata
             |  generate-all                       - generate all packages source code
             |  generate <package>...              - generate selected packages source code
             |  publish-local-all                  - publish all packages locally
             |  publish-local <package>...         - publish selected packages locally
             |  publish-maven-all                  - publish all packages to Maven
             |  publish-maven <package>...         - publish selected packages to Maven
             |  compile <package>...               - compile selected packages
             |  publish-maven-no-deps <package>... - publish selected packages to Maven without dependencies
             |""".stripMargin
        )
        exit(1)
  end main

  val cwd             = besomDir
  val schemasDir      = cwd / ".out" / "schemas"
  val codegenDir      = cwd / ".out" / "codegen"
  val packagesDir     = cwd / ".out" / "packages"
  val publishLocalDir = cwd / ".out" / "publishLocal"
  val publishMavenDir = cwd / ".out" / "publishMaven"

  val generatedFile      = codegenDir / "generated.json"
  val publishedLocalFile = publishLocalDir / "published.json"
  val publishedMavenFile = publishMavenDir / "published.json"

  def compileOpts(heapMaxGb: Int = 32): Vector[os.Shellable] =
    println(s"Compiling with max heap size: ${heapMaxGb}G")
    Vector(
      "--server=false",
      "--javac-opt=-verbose",
      s"--javac-opt=-J-XX:MaxHeapSize=${heapMaxGb}G",
      "--javac-opt=-J-XX:NewRatio=1", // increase young vs old gen size, default is 2
      "--javac-opt=-J-XX:+UseParallelGC"
    )

  def publishOpts(heapMaxGb: Int = 32, jarCompression: Int = 1, sources: Boolean = false, docs: Boolean = false): Vector[os.Shellable] =
    compileOpts(heapMaxGb) ++ Vector(
      s"--sources=$sources",
      s"--doc=$docs"
    )

  def mavenAuthOpts(pgpKey: String): Vector[os.Shellable] = Vector(
    "--user=env:OSSRH_USERNAME",
    "--password=env:OSSRH_PASSWORD",
    s"--gpg-key=$pgpKey",
    "--gpg-option=--pinentry-mode=loopback",
    "--gpg-option=--passphrase-fd=0" // read passphrase from stdin
  )

  lazy val compileLocalOpts: Vector[os.Shellable] =
    if isCI
    then compileOpts(heapMaxGb = 16)
    else compileOpts(heapMaxGb = 32)

  lazy val localOpts: Vector[os.Shellable] =
    if isCI
    then publishOpts(heapMaxGb = 16, jarCompression = 1)
    else publishOpts(heapMaxGb = 32, jarCompression = 1)

  lazy val mavenOpts: Vector[os.Shellable] = {
    if isCI
    then publishOpts(heapMaxGb = 16, jarCompression = 9, sources = true, docs = true)
    else publishOpts(heapMaxGb = 32, jarCompression = 9, sources = true, docs = true)
  } ++ mavenAuthOpts(pgpKey = envOrExit("PGP_KEY_ID"))
    ++ Vector(
      "--repository=sonatype:snapshots",
      "--connection-timeout-retries=10", // up from default 3
      "--staging-repo-retries=10", // up from default 3
      s"--staging-repo-wait-time-milis=${15.seconds.toMillis}" // up from default 10s
    )

  private val blockedPackages = Vector(
    "azure-native-v1", // deprecated
    "aws-quickstart-aurora-postgres", // archived
    "aws-quickstart-redshift", // archived
    "aws-quickstart-vpc" // archived
  )

  private val pluginDownloadProblemPackages = blockedPackages ++ Vector()

  private val codegenProblemPackages = blockedPackages ++ Vector()

  private val compileProblemPackages = blockedPackages ++ Vector(
    "aws-iam", // id parameter, schema error - components should make this viable
    "nuage" // id parameter, schema error - components should make this viable
  )

  def generateAll(targetPath: os.Path): os.Path = {
    val metadata = readPackagesMetadata(targetPath)
      .filter {
        case m if (codegenProblemPackages ++ compileProblemPackages).contains(m.name) =>
          println(s"Skipping problematic package generation: ${m.name}")
          false
        case _ => true
      }
    upsertGeneratedFile(generate(metadata))
  }

  def publishLocalAll(sourceFile: os.Path): os.Path = {
    val generated = PackageMetadata
      .fromJsonList(os.read(sourceFile: os.Path))
      .filter {
        case m if compileProblemPackages.contains(m.name) =>
          println(s"Skipping problematic package publishing: ${m.name}")
          false
        case _ => true
      }
    val published = publishLocal(generated)
    upsertPublishedFile(publishedLocalFile, published)
  }

  def publishMavenAll(sourceFile: os.Path): os.Path = {
    val generated = PackageMetadata
      .fromJsonList(os.read(sourceFile: os.Path))
      .filter {
        case m if compileProblemPackages.contains(m.name) =>
          println(s"Skipping problematic package publishing: ${m.name}")
          false
        case _ => true
      }
    val published = publishMaven(generated)
    upsertPublishedFile(publishedMavenFile, published)
  }

  def generateSelected(targetPath: os.Path, selected: List[String]): os.Path = {
    val readPackages = readPackagesMetadata(targetPath, selected)
    val packages = selected.map { p =>
      PackageId.parse(p) match {
        case Right(name, Some(version)) =>
          readPackages
            .find(p => p.name == name && p.version.contains(PackageVersion(version)))
            .getOrElse(PackageMetadata(name, PackageVersion(version)))
        case Right(name, None) =>
          readPackages
            .find(_.name == name)
            .getOrElse(throw Exception(s"Package '$name' not found in the generated packages (${readPackages.size})"))
        case Left(e) => throw e
      }
    }.toVector

    val metadata = generate(packages)
    upsertGeneratedFile(metadata)
  }

  def publishLocalSelected(sourceFile: os.Path, packages: List[String]): os.Path = {
    val selectedPackages = resolvePackageVersions(sourceFile, packages)
    val published        = publishLocal(selectedPackages)
    upsertPublishedFile(publishedLocalFile, published)
  }

  def publishMavenSelected(sourceFile: os.Path, packages: List[String]): os.Path = {
    val selectedPackages = resolvePackageVersions(sourceFile, packages)
    val published        = publishMaven(selectedPackages)
    upsertPublishedFile(publishedMavenFile, published)
  }

  def resolvePackageVersions(sourceFile: os.Path, packages: List[String]): Vector[PackageMetadata] = {
    lazy val generated = PackageMetadata.fromJsonList(os.read(sourceFile))
    packages.map {
      PackageId.parse(_) match
        case Right((name, Some(version))) => PackageMetadata(name, PackageVersion(version))
        case Right((name, None)) =>
          generated
            .find(_.name == name)
            .getOrElse(throw Exception(s"Package '$name' not found in the generated packages (${generated.size})"))
        case Left(e) => throw e
    }.toVector
  }

  def compileSelected(sourceFile: os.Path, packages: List[String]) =
    val selectedPackages = resolvePackageVersions(sourceFile, packages)
    withProgress("Compiling packages", selectedPackages.size) {
      selectedPackages.foreach { m =>
        Progress.report(label = s"${m.name}:${m.version.getOrElse("latest")}")
        val version = m.version.getOrElse(throw Exception("Package version must be provided for publishing")).asString
        try
          val args: Seq[os.Shellable] = Seq(
            "scala-cli",
            "--power",
            "compile",
            codegenDir / m.name / version,
            "--suppress-experimental-feature-warning",
            "--suppress-directives-in-multiple-files-warning"
          )
          os.proc(args ++ compileLocalOpts).call(stdout = os.Inherit, mergeErrIntoOut = true)
          Progress.total(selectedPackages.size)
        catch
          case _: os.SubprocessException =>
            Progress.fail(s"[${new Date}] Compilation failed for provider '${m.name}' version '${version}', error: sub-process failed")
          case NonFatal(_) =>
            Progress.fail(s"[${new Date}] Compilation failed for provider '${m.name}' version '${version}'")
        finally Progress.end
      }
    }
  end compileSelected

  def publishMavenSelectedNoDeps(sourceFile: os.Path, packages: List[String]) =
    val selectedPackages = resolvePackageVersions(sourceFile, packages)
    withProgress("Publishing packages to Maven without dependencies", selectedPackages.size) {
      selectedPackages.foreach { m =>
        Progress.report(label = s"${m.name}:${m.version.getOrElse("latest")}")
        val version = m.version.getOrElse(throw Exception("Package version must be provided for publishing")).asString
        try
          awaitPublishedToMaven(m)
          val args: Seq[os.Shellable] = Seq(
            "scala-cli",
            "--power",
            "publish",
            codegenDir / m.name / version,
            "--suppress-experimental-feature-warning",
            "--suppress-directives-in-multiple-files-warning"
          )
          os.proc(args ++ mavenOpts).call(stdin = envOrExit("PGP_PASSWORD"), stdout = os.Inherit, mergeErrIntoOut = true)
          println(s"[${new Date}] Successfully published provider '${m.name}' version '${version}'")
        catch
          case _: os.SubprocessException =>
            Progress.fail(s"[${new Date}] Publish failed for provider '${m.name}' version '${version}', error: sub-process failed")
          case NonFatal(_) =>
            Progress.fail(s"[${new Date}] Publish failed for provider '${m.name}' version '${version}'")
        finally Progress.end
      }
    }
  end publishMavenSelectedNoDeps

  type PackageId = (String, Option[String])
  object PackageId:
    def apply(name: String, version: Option[String]): PackageId = (name, version)
    def parse(value: String): Either[Exception, PackageId] = value.split(":").toList match
      case name :: version :: Nil => Right((name, PackageVersion(version)))
      case name :: Nil            => Right((name, None))
      case _                      => Left(Exception(s"Invalid package format: '$value'"))
  end PackageId

  def generate(metadata: Vector[PackageMetadata]): Vector[PackageMetadata] = {
    val seen = mutable.HashSet.empty[PackageId]
    val todo = mutable.Queue.empty[PackageMetadata]
    val done = mutable.ListBuffer.empty[PackageMetadata]

    todo.enqueueAll(metadata)
    withProgress(s"Generating ${todo.size} packages from metadata", todo.size) {
      while todo.nonEmpty do
        val m             = todo.dequeue()
        val id: PackageId = (m.name, m.version.map(_.asString))
        if !seen.contains(id) then
          seen.add(id)
          val versionOrLatest = m.version.getOrElse("latest")
          Progress.report(label = s"${m.name}:${versionOrLatest}")
          try
            given Config = Config(
              schemasDir = schemasDir,
              codegenDir = codegenDir
            )
            val result  = generator.generatePackageSources(metadata = m)
            val version = result.metadata.version.getOrElse(throw Exception("Package version must be present after generating")).asString

            println(s"[${new Date}] Successfully generated provider '${m.name}' version '${version}' [${new Date}]")
            println(result.asString)
            println()

            todo.enqueueAll(result.metadata.dependencies)
            done += result.metadata
            Progress.total(todo.size)
          catch
            case _: os.SubprocessException =>
              Progress.fail(
                s"[${new Date}] Code generation failed for provider '${m.name}' version '${versionOrLatest}', " +
                  s"error: sub-process failed"
              )
            case NonFatal(e) =>
              Progress.fail(
                s"[${new Date}] Code generation failed for provider '${m.name}' version '${versionOrLatest}', " +
                  s"error [${e.getClass.getSimpleName}]: ${e.getMessage}"
              )
          finally Progress.end
    }
    done.toVector
  }

  def publishLocal(generated: Vector[PackageMetadata]): Vector[PackageMetadata] = {
    val seen = mutable.HashSet.empty[PackageId]
    val todo = mutable.Queue.empty[PackageMetadata]
    val done = mutable.ListBuffer.empty[PackageMetadata]

    os.makeDir.all(publishLocalDir)

    generated.foreach(m => {
      todo.enqueueAll(m.dependencies) // dependencies first to avoid missing dependencies during compilation
      todo.enqueue(m)
    })
    withProgress(s"Publishing ${todo.size} packages locally", todo.size) {
      while todo.nonEmpty do
        val m             = todo.dequeue()
        val id: PackageId = (m.name, m.version.map(_.asString))
        if !seen.contains(id) then
          seen.add(id)
          val version = m.version.getOrElse(throw Exception("Package version must be provided for publishing")).asString
          Progress.report(label = s"${m.name}:${version}")
          val logFile = publishLocalDir / s"${m.name}-${version}.log"
          try
            val args: Seq[os.Shellable] = Seq(
              "scala-cli",
              "--power",
              "publish",
              "local",
              codegenDir / m.name / version,
              "--suppress-experimental-feature-warning",
              "--suppress-directives-in-multiple-files-warning"
            )
            os.proc(args ++ localOpts).call(stdout = logFile, mergeErrIntoOut = true)
            println(s"[${new Date}] Successfully published locally provider '${m.name}' version '${version}'")

            done += m
            Progress.total(todo.size)
          catch
            case _: os.SubprocessException =>
              Progress.fail(
                s"[${new Date}] Publish failed for provider '${m.name}' version '${version}', logs: ${logFile}, " +
                  s"error: sub-process failed, see logs for details"
              )
            case NonFatal(e) =>
              Progress.fail(
                s"[${new Date}] Publish failed for provider '${m.name}' version '${version}', logs: ${logFile}, " +
                  s"error [${e.getClass.getSimpleName}]: ${e.getMessage}"
              )
          finally Progress.end
    }
    done.toVector
  }

  def publishMaven(generated: Vector[PackageMetadata]): Vector[PackageMetadata] = {
    val seen = mutable.HashSet.empty[PackageId]
    val todo = mutable.Queue.empty[PackageMetadata]
    val done = mutable.ListBuffer.empty[PackageMetadata]

    os.makeDir.all(publishMavenDir)

    generated.foreach(m => {
      todo.enqueueAll(m.dependencies) // dependencies first to avoid missing dependencies during compilation
      todo.enqueue(m)
    })
    withProgress(s"Publishing ${todo.size} packages to Maven", todo.size) {
      while todo.nonEmpty do
        val m             = todo.dequeue()
        val id: PackageId = (m.name, m.version.map(_.asString))
        if !seen.contains(id) then
          seen.add(id)
          val version = m.version.getOrElse(throw Exception("Package version must be provided for publishing")).asString
          Progress.report(label = s"${m.name}:${version}")
          val logFile = publishMavenDir / s"${m.name}-${version}.log"
          try
            awaitPublishedToMaven(m)
            val args: Seq[Shellable] = Seq(
              "scala-cli",
              "--power",
              "publish",
              codegenDir / m.name / version,
              "--suppress-experimental-feature-warning",
              "--suppress-directives-in-multiple-files-warning"
            )
            os.proc(args ++ mavenOpts).call(stdin = envOrExit("PGP_PASSWORD"), stdout = logFile, mergeErrIntoOut = true)
            println(s"[${new Date}] Successfully published provider '${m.name}' version '${version}'")

            done += m
            Progress.total(todo.size)
          catch
            case e: CantDownloadModule =>
              e.printStackTrace()
              Progress.fail(
                s"[${new Date}] Publish failed for provider '${m.name}' version '${version}' " +
                  s"error: failed to download dependency, message: ${e.getMessage}"
              )
            case _: os.SubprocessException =>
              Progress.fail(
                s"[${new Date}] Publish failed for provider '${m.name}' version '${version}', logs: ${logFile}, " +
                  s"error: sub-process failed, see logs for details"
              )
            case NonFatal(e) =>
              Progress.fail(
                s"[${new Date}] Publish failed for provider '${m.name}' version '${version}', logs: ${logFile}, " +
                  s"error [${e.getClass.getSimpleName}]: ${e.getMessage}"
              )
          finally Progress.end
          end try
        end if
    }
    done.toVector
  }

  private def awaitPublishedToMaven(m: PackageMetadata): Unit = {
    // Check if dependencies are available in Maven Central before publishing
    if m.dependencies.nonEmpty then
      val timeout  = 15.minutes
      val interval = 15.second
      val deadline = timeout.fromNow
      println(s"Checking for dependencies with timeout of ${timeout}")

      // TODO: clean this up, it's a bit of a mess
      val defaultScalaVersion = Config.DefaultBesomVersion
      val shortCoreVersion = SemanticVersion
        .parseTolerant(defaultScalaVersion)
        .fold(
          e => Left(Exception(s"Invalid besom version: ${defaultScalaVersion}", e)),
          v => Right(v.copy(patch = 0).toShortString /* drop patch version */ )
        )

      def dependency(m: PackageMetadata): String =
        s"org.virtuslab::besom-${m.name}:${m.version.get}-core.${shortCoreVersion.toTry.get}"

      val allDependencies = m.dependencies.map(dependency)
      val found           = mutable.Map.empty[String, Boolean]

      def foundAll = found.values.forall(identity)

      allDependencies.foreach { d =>
        found(d) = false
      }
      while (deadline.hasTimeLeft && !foundAll) {
        allDependencies.foreach { d =>
          resolveMavenPackage(d, defaultScalaVersion).fold(
            _ => println(s"Dependency '${d}' not found"),
            _ => found(d) = true
          )
        }
        if !foundAll then
          println(s"Waiting for ${interval} to allow the publish to propagate")
          Thread.sleep(interval.toMillis)
      }
    end if
  }

  def readPackagesMetadata(targetPath: os.Path, selected: List[String] = Nil): Vector[PackageMetadata] = {
    // download metadata if none found
    if !os.exists(targetPath) then downloadPackagesMetadata(targetPath, selected)

    def read(path: os.Path): ListMap[String, Path] = ListMap.from(
      os
        .list(path)
        .filter(_.last.endsWith("metadata.json"))
        .map(p => p.last.stripSuffix(".metadata.json") -> p)
    )

    // read cached metadata
    val cached = read(targetPath)

    // download all metadata if selected packages are not found
    val selectedNames             = selected.map(_.takeWhile(_ != ':')).toSet
    val selectedAreSubsetOfCached = selectedNames.subsetOf(cached.keys.toSet)
    val downloaded =
      if !selectedAreSubsetOfCached
      then
        downloadPackagesMetadata(targetPath, selected)
        read(targetPath)
      else cached

    // double check if selected packages are found
    selectedNames.map { p =>
      downloaded.keys
        .find(_ == p)
        .getOrElse(throw Exception(s"Package '$p' not found in downloaded packages metadata (${downloaded.size})"))
    }.toVector

    val metadata = downloaded
      .filter { (name, _) =>
        selectedNames match
          case _ if selectedNames.isEmpty => true
          case s                          => s.contains(name) // filter out selected packages only if selected is not empty
      }
      .map((_, p) => PackageMetadata.fromJsonFile(p))
      .collect {
        case metadata if selected.nonEmpty => metadata
        case metadata if !pluginDownloadProblemPackages.contains(metadata.name) =>
          metadata // filter out packages with known problems only if selected is not empty
      }
      .toVector

    if metadata.isEmpty then throw Exception(s"No packages metadata found in: '$targetPath'")
    metadata
  }

  // downloads latest package metadata and schemas using Pulumi packages repository
  def downloadPackagesMetadata(targetPath: os.Path, selected: List[String] = Nil): Unit = {
    os.remove.all(targetPath)
    os.makeDir.all(targetPath)

    val packagesRepoApi = "https://api.github.com/repos/pulumi/registry/contents/themes/default/data/registry/packages"

    val token      = envOrExit("GITHUB_TOKEN")
    val authHeader = Map("Authorization" -> s"token $token")

    val packagesResponse = requests.get(packagesRepoApi, headers = authHeader)
    if packagesResponse.statusCode != 200
    then throw Exception(s"Failed to fetch packages list from: '$packagesRepoApi'")

    case class PackageSource(name: String, download_url: String, sha: String) derives UpickleApi.ReadWriter
    object PackageSource {
      def fromJsonArray(json: ujson.Readable): List[PackageSource] = UpickleApi.read(json, trace = true)
    }

    val packages: ListMap[String, PackageSource] = ListMap.from(
      PackageSource
        .fromJsonArray(packagesResponse.text())
        .map { p =>
          val packageName = p.name.stripSuffix(".yaml")
          packageName -> p
        }
    )

    if packages.isEmpty
    then throw Exception(s"No packages found using: '$packagesRepoApi'")
    else println(s"Found ${packages.size} packages total")

    type Error = String

    case class PackageYAML(name: String, repo_url: String, schema_file_path: String, version: String) derives YamlCodec

    val selectedNames = selected.map(_.takeWhile(_ != ':'))
    val size          = if selectedNames.isEmpty then packages.size else selectedNames.size
    println(selectedNames)

    // fetch all production schema metadata
    val downloaded: Vector[PackageYAML] =
      withProgress(s"Downloading $size packages metadata", size) {
        packages
          .filter { (name, _) =>
            selectedNames match
              case Nil => true
              case s   => s.contains(name)
          }
          .filterNot((name, _) => blockedPackages.contains(name))
          .map { (packageName: String, p: PackageSource) =>
            Progress.report(label = packageName)
            try
              val metadataFile = p.name
              val metadataPath = targetPath / metadataFile
              val shaPath      = targetPath / s"${p.name}.sha"

              val hasChanged: Boolean =
                if os.exists(shaPath) then
                  val sha = os.read(shaPath).split(" ").head
                  sha != p.sha
                else true // no sha file, assume it has changed

              val metadataRaw: Either[Error, PackageYAML] = {
                if hasChanged || !os.exists(metadataPath) then
                  val schemaResponse = requests.get(p.download_url, headers = authHeader)
                  schemaResponse.statusCode match
                    case 200 =>
                      val metadata = schemaResponse.text()
                      os.write.over(metadataPath, metadata, createFolders = true)
                      os.write.over(shaPath, s"${p.sha}  $metadataFile", createFolders = true)
                      Right(metadata)
                    case _ =>
                      Left(s"failed to download metadata for package: '${p.name}'")
                else Right(os.read(metadataPath))
              }.flatMap {
                _.as[PackageYAML].fold(
                  error => Left(s"failed to deserialize metadata for package: '${p.name}', error: ${error}"),
                  p => Right(p)
                )
              }

              val metadata: Either[Error, PackageMetadata] =
                metadataRaw.map(m => PackageMetadata(m.name, m.version).withUrl(m.repo_url))

              metadata match
                case Left(error) => Progress.fail(error)
                case Right(value) =>
                  os.write.over(targetPath / s"${packageName}.metadata.json", value.toJson, createFolders = true)

              metadataRaw
            catch
              case NonFatal(e) =>
                val msg = s"Failed to download metadata for package: '${p.name}', error: ${e.getMessage}"
                Progress.fail(msg)
                Left(msg)
            finally Progress.end
            end try
          }
          .map(_.toOption)
          .collect { case Some(p) => p }
          .toVector
      }

    // pre-fetch all available production schemas, if any are missing here code generation will use a fallback mechanism
    withProgress(s"Downloading $size packages schemas", size) {
      downloaded.foreach(p => {
        Progress.report(label = p.name)
        try
          val ext        = if p.schema_file_path.endsWith(".yaml") || p.schema_file_path.endsWith(".yml") then "yaml" else "json"
          val schemaPath = schemasDir / p.name / PackageVersion(p.version).get / s"schema.$ext"
          if !os.exists(schemaPath) then
            val rawUrlPrefix   = p.repo_url.replace("https://github.com/", "https://raw.githubusercontent.com/")
            val url            = s"$rawUrlPrefix/${p.version}/${p.schema_file_path}"
            val schemaResponse = requests.get(url)
            if schemaResponse.statusCode == 200
            then os.write.over(schemaPath, schemaResponse.text(), createFolders = true)
            else
              Progress.fail(
                s"Failed to download schema for package: '${p.name}' from: '$url', " +
                  s"error[${schemaResponse.statusCode}]: ${schemaResponse.statusMessage}"
              )
        catch
          case NonFatal(e) =>
            Progress.fail(s"Failed to download schema for package: '${p.name}', error: ${e.getMessage}")
        finally Progress.end
      })
    }

    println(s"Packages directory: '$targetPath'")
    println(s"Schemas directory: '$schemasDir'")
  }

  def listLatestPackages(targetPath: os.Path): Unit = {
    val metadata = readPackagesMetadata(targetPath)
    val active = metadata
      .filterNot(m => blockedPackages.contains(m.name))
      .filterNot(m => codegenProblemPackages.contains(m.name))
      .filterNot(m => compileProblemPackages.contains(m.name))
      .size
    println(s"Found ${metadata.size} (${active} active) packages in Pulumi repository")
    metadata.foreach(m =>
      println {
        s"${m.name}:${m.version.getOrElse("latest")}" + {
          if blockedPackages.contains(m.name) then " (blocked)" else ""
        } + {
          if codegenProblemPackages.contains(m.name) then " (codegen problem)" else ""
        } + {
          if compileProblemPackages.contains(m.name) then " (compile problem)" else ""
        }
      }
    )
  }

  def listPublishedPackages(file: os.Path): Unit =
    val metadata = PackageMetadata.fromJsonList(os.read(file))
    println(s"Found ${metadata.size} packages in logs published to Maven")
    metadata.foreach(m => println(s"${m.name}:${m.version.getOrElse("unknown")}"))
  end listPublishedPackages

  def upsertGeneratedFile(metadata: Vector[PackageMetadata]): os.Path = {
    val generated =
      if os.exists(generatedFile)
      then PackageMetadata.fromJsonList(os.read(generatedFile))
      else Vector.empty
    val all = deduplicate(generated ++ metadata)
    os.write.over(generatedFile, PackageMetadata.toJson(all), createFolders = true)
    generatedFile
  }

  def upsertPublishedFile(file: os.Path, metadata: Vector[PackageMetadata]): os.Path = {
    val published =
      if os.exists(file)
      then PackageMetadata.fromJsonList(os.read(file))
      else Vector.empty
    val all = deduplicate(published ++ metadata)
    os.write.over(file, PackageMetadata.toJson(all), createFolders = true)
    file
  }

  def deduplicate(metadata: Iterable[PackageMetadata]): Vector[PackageMetadata] = {
    val seen     = mutable.HashSet.empty[PackageId]
    val elements = mutable.ListBuffer.empty[PackageMetadata]
    for m <- metadata do
      val id: PackageId = (m.name, m.version.map(_.asString))
      if !seen.contains(id) then
        seen.add(id)
        elements += m
    elements.toVector
  }

end Packages
