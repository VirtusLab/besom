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

case class Flags(
  force: Boolean = false
)

//noinspection ScalaWeakerAccess,TypeAnnotation
object Packages:
  def main(args: String*): Unit =
    val (params, flags) = Args.parse(args, monoFlags = Vector("force", "f"))

    given Config = Config()
    given Flags = Flags(
      force = flags.get("force").orElse(flags.get("f")) match
        case Some(v: String) => v.toBoolean
        case Some(v: Int)    => v > 0
        case None            => false
    )

    params.toList match
      case "local-all" :: Nil         => publishLocalAll(generateAll(packagesDir))
      case "local" :: tail            => publishLocalSelected(generateSelected(packagesDir, tail), tail)
      case "maven-all" :: Nil         => publishMavenAll(generateAll(packagesDir))
      case "maven" :: tail            => publishMavenSelected(generateSelected(packagesDir, tail), tail)
      case "metadata-all" :: Nil      => downloadPackagesMetadataAndSchema(packagesDir, selected = Nil)
      case "metadata" :: tail         => downloadPackagesMetadataAndSchema(packagesDir, selected = tail)
      case "generate-all" :: Nil      => generateAll(packagesDir)
      case "generate" :: tail         => generateSelected(packagesDir, tail)
      case "publish-local-all" :: Nil => publishLocalAll(generatedFile)
      case "publish-local" :: tail    => publishLocalSelected(generatedFile, tail)
      case "publish-maven-all" :: Nil => publishMavenAll(generatedFile)
      case "publish-maven" :: tail    => publishMavenSelected(generatedFile, tail)
      // less common use cases
      case "list" :: Nil                    => listLatestPackages(packagesDir)
      case "compile" :: tail                => compileSelected(generatedFile, tail)
      case "list-published-to-maven" :: Nil => listPublishedPackages(publishedMavenFile)
      case "resolve-maven" :: tail          => resolveMavenSelected(packagesDir, tail)
      case "resolve-maven-all" :: Nil       => resolveMavenAll(packagesDir)
      case cmd =>
        println(s"Unknown command: $cmd\n")
        println(
          s"""Usage: packages <command>
             |
             |Commands:
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
             |  
             |  compile <package>...               - compile selected packages
             |  list                               - list latest packages from Pulumi repository
             |  list-published-to-maven            - list packages published to Maven (from local cache)
             |  resolve-maven <package>...         - resolve selected packages from Maven
             |  resolve-maven-all                  - resolve all packages from Maven
             |""".stripMargin
        )
        exit(1)
    end match
  end main

  val cwd             = Config.besomDir
  val packagesDir     = cwd / ".out" / "packages"
  val publishLocalDir = cwd / ".out" / "publishLocal"
  val publishMavenDir = cwd / ".out" / "publishMaven"

  def generatedFile(using config: Config) = config.codegenDir / "generated.json"
  val publishedLocalFile                  = publishLocalDir / "published.json"
  val publishedMavenFile                  = publishMavenDir / "published.json"

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
    "azure-quickstart-acr-geo-replication", // uses deprecated azure-native-v1
    "aws-quickstart-aurora-postgres", // archived
    "aws-quickstart-redshift", // archived
    "aws-quickstart-vpc" // archived
  )

  private val pluginDownloadProblemPackages = blockedPackages ++ Vector()

  private val codegenProblemPackages = blockedPackages ++ Vector(
    "lbrlabs-eks" // schema error: https://github.com/lbrlabs/pulumi-lbrlabs-eks/issues/110
  )

  private val compileProblemPackages = blockedPackages ++ Vector(
    "aws-iam", // id parameter, schema error - https://github.com/pulumi/pulumi-aws-iam/issues/18
    "nuage", // id parameter, schema error - https://github.com/nuage-studio/pulumi-nuage/issues/50
    "ovh", // urn parameter, schema error - https://github.com/ovh/pulumi-ovh/issues/139
    "fortios" // method collision - https://github.com/VirtusLab/besom/issues/458
  )

  def generateAll(targetPath: os.Path)(using Config, Flags): os.Path = {
    val metadata = readOrFetchPackagesMetadata(targetPath, Nil)
      .filter {
        case m if (codegenProblemPackages ++ compileProblemPackages).contains(m.name) =>
          println(s"Skipping problematic package generation: ${m.name}")
          false
        case _ => true
      }
    upsertGeneratedFile(generate(metadata))
  }

  def publishLocalAll(sourceFile: os.Path)(using Config, Flags): os.Path = {
    val generated = getPackages(sourceFile)
      .filter {
        case m if compileProblemPackages.contains(m.name) =>
          println(s"Skipping problematic package publishing: ${m.name}")
          false
        case _ => true
      }
    val published = publishLocal(generated)
    upsertPublishedFile(publishedLocalFile, published)
  }

  def publishMavenAll(sourceFile: os.Path)(using Config, Flags): os.Path = {
    val generated = getPackages(sourceFile)
      .filter {
        case m if compileProblemPackages.contains(m.name) =>
          println(s"Skipping problematic package publishing: ${m.name}")
          false
        case _ => true
      }
    val published = publishMaven(generated)
    upsertPublishedFile(publishedMavenFile, published)
  }

  def generateSelected(targetPath: os.Path, selected: List[String])(using config: Config, flags: Flags): os.Path = {
    val readPackages = readOrFetchPackagesMetadata(targetPath, selected)
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

  def publishLocalSelected(sourceFile: os.Path, packages: List[String])(using Config, Flags): os.Path = {
    val selectedPackages = resolvePackageVersions(sourceFile, packages)
    val published        = publishLocal(selectedPackages)
    upsertPublishedFile(publishedLocalFile, published)
  }

  def publishMavenSelected(sourceFile: os.Path, packages: List[String])(using Config, Flags): os.Path = {
    val selectedPackages = resolvePackageVersions(sourceFile, packages)
    val published        = publishMaven(selectedPackages)
    upsertPublishedFile(publishedMavenFile, published)
  }

  def resolvePackageVersions(sourceFile: os.Path, packages: List[String])(using Config): Vector[PackageMetadata] =
    lazy val metadata = getPackages(sourceFile)
    packages.map {
      PackageId.parse(_) match
        case Right((name, Some(version))) => PackageMetadata(name, PackageVersion(version))
        case Right((name, None)) =>
          metadata
            .find(_.name == name)
            .getOrElse(throw Exception(s"Package '$name' not found in the generated packages (${metadata.size})"))
        case Left(e) => throw e
    }.toVector
  end resolvePackageVersions

  def compileSelected(sourceFile: os.Path, packages: List[String])(using config: Config) =
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
            config.codegenDir / m.name / version,
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

  private def parsePackages(packages: List[String]): Vector[PackageMetadata] =
    packages.map {
      PackageId.parse(_) match
        case Right((name, Some(version))) => PackageMetadata(name, PackageVersion(version))
        case Right((name, None))          => PackageMetadata(name, None)
        case Left(e)                      => throw e
    }.toVector

  def resolveMavenAll(targetDir: Path)(using config: Config): Unit =
    val _        = downloadPackagesMetadata(targetDir, Nil)
    val latest   = readPackagesMetadata(targetDir).map(_.copy(version = None))
    val resolved = resolveMavenVersions(targetDir, latest)
    compareWithLatest(targetDir, resolved, latest)
  end resolveMavenAll

  def resolveMavenSelected(targetDir: Path, selected: List[String])(using config: Config): Unit =
    val selectedPackages = parsePackages(selected)
    val resolved         = resolveMavenVersions(targetDir, selectedPackages)

    val _              = downloadPackagesMetadata(targetDir, selected)
    val latestPackages = readPackagesMetadata(targetDir)
    compareWithLatest(targetDir, resolved, latestPackages)
  end resolveMavenSelected

  def resolveMavenVersions(targetDir: os.Path, packages: Vector[PackageMetadata])(using config: Config): Map[String, SemanticVersion] =
    withProgress("Resolving packages from Maven", packages.size) {
      packages.map { m =>
        val dep = dependency(m)
        val ver = m.version.getOrElse("latest")
        Thread.sleep(100) // avoid rate limiting
        val res = resolveMavenPackageVersion(dep)
          .fold(
            e => {
              Progress.fail(s"Can't resolve '${m.name}' version '${ver}':\n ${e.getMessage}")
              m.name -> None
            },
            v => {
              SemanticVersion.parseTolerant(v) match
                case Left(e) =>
                  Progress.fail(s"Can't parse resolved version '${v}' for '${m.name}':\n ${e.getMessage}")
                  m.name -> None
                case Right(v) =>
                  Progress.report(label = s"${m.name}:${v}")
                  m.name -> Some(v)
            }
          )
        Progress.end
        res
      }
    }.collect({ case (name, Some(version)) => name -> version }).toMap
  end resolveMavenVersions

  def compareWithLatest(
    targetDir: os.Path,
    resolved: Map[String, SemanticVersion],
    latestPackages: Vector[PackageMetadata]
  )(using config: Config): Unit =
    withProgress("Comparing with Pulumi repository versions", resolved.size) {
      resolved.foreach { case (name, l) =>
        val v = SemanticVersion(l.major, l.minor, l.patch)
        val latest =
          latestPackages
            .find(_.name == name)
            .flatMap(_.version)
            .map(SemanticVersion.parseTolerant)
        (v, latest) match
          case (maven: SemanticVersion, Some(Right(upstream: SemanticVersion))) if maven < upstream =>
            Progress.report(label = s"${name}: ${maven} < ${upstream}")
            Progress.end
          case (maven: SemanticVersion, Some(Right(upstream: SemanticVersion))) if maven >= upstream =>
            Progress.report(label = s"${name}: ${maven} >= ${upstream}")
            Progress.end
          case (m, u) =>
            Progress.fail(s"${name}: one or both not found: ${m} ${u}")
            Progress.end
      }
    }
  end compareWithLatest

  type PackageId = (String, Option[String])
  object PackageId:
    def apply(name: String, version: Option[String]): PackageId = (name, version)
    def parse(value: String): Either[Exception, PackageId] = value.split(":").toList match
      case name :: version :: Nil => Right((name, PackageVersion(version)))
      case name :: Nil            => Right((name, None))
      case _                      => Left(Exception(s"Invalid package format: '$value'"))
  end PackageId

  def generate(metadata: Vector[PackageMetadata])(using Config): Vector[PackageMetadata] = {
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

  def publishLocal(generated: Vector[PackageMetadata])(using config: Config, flags: Flags): Vector[PackageMetadata] = {
    val seen = mutable.HashSet.empty[PackageId]
    val todo = mutable.Queue.empty[PackageMetadata]
    val done = mutable.ListBuffer.empty[PackageMetadata]

    os.makeDir.all(publishLocalDir)

    generated.foreach(m => {
      // add dependencies first to avoid missing dependencies during compilation
      for d <- m.dependencies do if !todo.contains(d) then todo.enqueue(d)
      if !todo.contains(m) then todo.enqueue(m)
    })
    withProgress(s"Publishing ${todo.size} packages locally", todo.size) {
      while todo.nonEmpty do
        val m             = todo.dequeue()
        val id: PackageId = (m.name, m.version.map(_.asString))
        lazy val resolved = resolveLocalPackageVersion(dependency(m))
        if !flags.force && !seen.contains(id) && resolved.isRight then
          seen.add(id)
          val v = resolved.getOrElse(throw Exception("Resolved version must be provided at this point"))
          Progress.report(label = s"${m.name}:${v}")
          println(s"[${new Date}] Skipping provider '${m.name}' version '${v}' (already published locally)")
          done += m
          Progress.end
        end if

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
              config.codegenDir / m.name / version,
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

  def publishMaven(generated: Vector[PackageMetadata])(using config: Config, flags: Flags): Vector[PackageMetadata] = {
    val seen = mutable.HashSet.empty[PackageId]
    val todo = mutable.Queue.empty[PackageMetadata]
    val done = mutable.ListBuffer.empty[PackageMetadata]

    os.makeDir.all(publishMavenDir)

    generated.foreach(m => {
      // add dependencies first to avoid missing dependencies during compilation
      for d <- m.dependencies do if !todo.contains(d) then todo.enqueue(d)
      if !todo.contains(m) then todo.enqueue(m)
    })
    withProgress(s"Publishing ${todo.size} packages to Maven", todo.size) {
      while todo.nonEmpty do
        val m             = todo.dequeue()
        val id: PackageId = (m.name, m.version.map(_.asString))
        lazy val resolved = resolveMavenPackageVersion(dependency(m))
        if !flags.force && !seen.contains(id) && resolved.isRight then
          seen.add(id)
          val v = resolved.getOrElse(throw Exception("Resolved version must be provided at this point"))
          Progress.report(label = s"${m.name}:${v}")
          println(s"[${new Date}] Skipping provider '${m.name}' version '${v}' (already published to Maven)")
          done += m
          Progress.end
        end if

        if !seen.contains(id) then
          seen.add(id)
          val version = m.version.getOrElse(throw Exception("Package version must be provided for publishing")).asString
          Progress.report(label = s"${m.name}:${version}")
          val logFile = publishMavenDir / s"${m.name}-${version}.log"
          try
            awaitDependenciesPublishedToMaven(m)
            val args: Seq[Shellable] = Seq(
              "scala-cli",
              "--power",
              "publish",
              config.codegenDir / m.name / version,
              "--suppress-experimental-feature-warning",
              "--suppress-directives-in-multiple-files-warning"
            )
            os.proc(args ++ mavenOpts).call(stdin = envOrExit("PGP_PASSWORD"), stdout = logFile, mergeErrIntoOut = true)
            println(s"[${new Date}] Successfully published provider '${m.name}' version '${version}'")

            done += m
            Progress.total(todo.size)
          catch
            case e: CantDownloadModule =>
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

  def dependency(m: PackageMetadata)(using config: Config): String =
    val version = m.version
      .map(version => s"${version}-core.${config.coreShortVersion}")
      .getOrElse("[0.+,999999.+)")
    s"org.virtuslab::besom-${m.name}:${version}"

  private def awaitDependenciesPublishedToMaven(m: PackageMetadata)(using config: Config): Unit = {
    // Check if dependencies are available in Maven Central before publishing
    if m.dependencies.nonEmpty then
      val timeout  = 15.minutes
      val interval = 15.second
      val deadline = timeout.fromNow
      println(s"Checking for dependencies with timeout of ${timeout}")

      val shortCoreVersion = SemanticVersion
        .parseTolerant(config.besomVersion)
        .fold(
          e => Left(Exception(s"Invalid besom version: ${config.besomVersion}", e)),
          v => Right(v.copy(patch = 0).toShortString /* drop patch version */ )
        )
      val allDependencies = m.dependencies.map(dependency(_))
      val found           = mutable.Map.empty[String, Boolean]

      def foundAll = found.values.forall(identity)

      allDependencies.foreach { d => found(d) = false }
      while (deadline.hasTimeLeft && !foundAll) {
        allDependencies.foreach { d =>
          resolveMavenPackageVersion(d).fold(
            e => println(s"Dependency '${d}' not found: ${e.getMessage}"),
            _ => found(d) = true
          )
        }
        if !foundAll then
          println(s"Waiting for ${interval} to allow the publish to propagate")
          Thread.sleep(interval.toMillis)
      }
      if foundAll then println("All dependencies found in Maven Central")
    end if
  }

  private def readPackageMetadataFiles(path: os.Path): Map[String, Path] = ListMap.from(
    os
      .list(path)
      .filter(_.last.endsWith("metadata.json"))
      .map(p => p.last.stripSuffix(".metadata.json") -> p)
  )

  def readPackagesMetadata(targetPath: os.Path): Vector[PackageMetadata] =
    readPackageMetadataFiles(targetPath)
      .map((_, p) => PackageMetadata.fromJsonFile(p))
      .toVector

  def readOrFetchPackagesMetadata(
    targetPath: os.Path,
    selected: List[String]
  )(using config: Config, flags: Flags): Vector[PackageMetadata] = {
    // download metadata if none found
    if flags.force || !os.exists(targetPath) then
      println(s"No packages metadata found in: '$targetPath', downloading...")
      downloadPackagesMetadataAndSchema(targetPath, selected)
    else println(s"Reading packages metadata from: '$targetPath'")

    // read cached metadata
    val cached = readPackageMetadataFiles(targetPath)

    // download all metadata if selected packages are not found
    val selectedNames             = selected.map(_.takeWhile(_ != ':')).toSet
    val selectedAreSubsetOfCached = selectedNames.subsetOf(cached.keys.toSet)
    if selected.nonEmpty then
      println(s"Selected: ${selected.mkString(", ")}, cached: ${cached.keys.mkString(", ")}, subset: $selectedAreSubsetOfCached")
    val downloaded =
      if !selectedAreSubsetOfCached
      then
        downloadPackagesMetadataAndSchema(targetPath, selected)
        readPackageMetadataFiles(targetPath)
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

  private case class PackageYAML(name: String, repo_url: String, schema_file_path: String, version: String) derives YamlCodec

  // downloads latest package metadata and schemas using Pulumi packages repository
  def downloadPackagesMetadataAndSchema(targetPath: os.Path, selected: List[String])(using config: Config): Unit =
    downloadPackagesSchema(downloadPackagesMetadata(targetPath, selected))
  end downloadPackagesMetadataAndSchema

  private def downloadPackagesMetadata(targetPath: os.Path, selected: List[String])(using config: Config): Vector[PackageYAML] =
    println("Downloading packages metadata...")

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

    val selectedNames = selected.map(_.takeWhile(_ != ':'))
    val size          = if selectedNames.isEmpty then packages.size else selectedNames.size
    if selectedNames.nonEmpty then println(s"Selected for download: ${selectedNames.mkString(", ")}")

    // fetch all production schema metadata
    val downloaded = withProgress(s"Downloading $size packages metadata", size) {
      packages
        .filter { (name, _) =>
          selectedNames match
            case Nil => true
            case s   => s.contains(name)
        }
        .filterNot((name, _) => {
          val block = blockedPackages.contains(name)
          if block then Progress.fail(s"Skipping package: '$name', it's known to have problems")
          block
        })
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
    println(s"Packages directory: '$targetPath'")
    downloaded
  end downloadPackagesMetadata

  private def downloadPackagesSchema(downloaded: Vector[PackageYAML])(using config: Config): Unit = {
    // pre-fetch all available production schemas, if any are missing here code generation will use a fallback mechanism
    withProgress(s"Downloading ${downloaded.size} packages schemas", downloaded.size) {
      downloaded.foreach(p => {
        Progress.report(label = p.name)
        try
          val ext        = if p.schema_file_path.endsWith(".yaml") || p.schema_file_path.endsWith(".yml") then "yaml" else "json"
          val schemaPath = config.schemasDir / p.name / PackageVersion(p.version).get / s"schema.$ext"
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

    println(s"Schemas directory: '${config.schemasDir}'")
  }

  def listLatestPackages(targetPath: os.Path)(using config: Config, flags: Flags): Unit = {
    val metadata = readOrFetchPackagesMetadata(targetPath, Nil)
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

  def getPackages(file: os.Path): Vector[PackageMetadata] =
    if os.exists(file)
    then PackageMetadata.fromJsonList(os.read(file))
    else
      println(s"File not found: $file, returning empty list")
      Vector.empty
  end getPackages

  def listPublishedPackages(file: os.Path): Unit =
    val metadata = getPackages(file)
    println(s"Found ${metadata.size} packages in logs published to Maven")
    metadata.foreach(m => println(s"${m.name}:${m.version.getOrElse("unknown")}"))
  end listPublishedPackages

  def upsertGeneratedFile(metadata: Vector[PackageMetadata])(using Config): os.Path = {
    val generated = getPackages(generatedFile)
    val all       = deduplicate(generated ++ metadata)
    os.write.over(generatedFile, PackageMetadata.toJson(all), createFolders = true)
    generatedFile
  }

  def upsertPublishedFile(file: os.Path, metadata: Vector[PackageMetadata]): os.Path = {
    val published = getPackages(file)
    val all       = deduplicate(published ++ metadata)
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
