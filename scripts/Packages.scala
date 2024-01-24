package besom.scripts

import besom.codegen.Config.CodegenConfig
import besom.codegen.{PackageMetadata, UpickleApi, generator}
import org.virtuslab.yaml.*

import java.util.Date
import scala.collection.mutable
import scala.util.*
import scala.util.control.NonFatal

//noinspection ScalaWeakerAccess,TypeAnnotation
object Packages:
  def main(args: String*): Unit =
    args match
      case "metadata-all" :: Nil => downloadPackagesMetadata(packagesDir)
      case "metadata" :: tail => downloadPackagesMetadata(packagesDir, selected = tail)
      case "generate-all" :: Nil => generateAll(packagesDir)
      case "generate" :: tail => generateSelected(packagesDir, tail)
      case "publish-local-all" :: Nil => publishLocalAll(generatedFile)
      case "publish-local" :: tail => publishLocalSelected(generatedFile, tail)
      case "publish-maven-all" :: Nil => publishMavenAll(generatedFile)
      case "publish-maven" :: tail => publishMavenSelected(generatedFile, tail)
      case _ => println(s"Unknown command: $args, expected one of: " +
        s"metadata-all, metadata, generate-all, generate, publish-local-all, publish-local, publish-maven-all, publish-maven")
  end main

  val cwd = besomDir
  val codegenDir = cwd / ".out" / "codegen"
  val packagesDir = cwd / ".out" / "packages"
  val publishLocalDir = cwd / ".out" / "publishLocal"
  val publishMavenDir = cwd / ".out" / "publishMaven"

  val generatedFile = codegenDir / "generated.json"
  val publishedLocalFile = publishLocalDir / "published.json"
  val publishedMavenFile = publishMavenDir / "published.json"

  private val pluginDownloadProblemPackages = Vector(
    "aws-miniflux", // lack of darwin/arm64 binary
    "aws-s3-replicated-bucket", // lack of darwin/arm64 binary
    "aws-quickstart-aurora-postgres", // lack of darwin/arm64 binary
    "aws-quickstart-redshift", // lack of darwin/arm64 binary
    "aws-quickstart-vpc", // lack of darwin/arm64 binary
    "azure-native-v1", // deprecated, lack of darwin/arm64 binary
    "azure-quickstart-acr-geo-replication", // lack of darwin/arm64 binary
    "gcp-global-cloudrun", // lack of darwin/arm64 binary
    "iosxe", // schemaOnlyProvider does not implement runtime operation InitLogging
    "kubernetes-coredns", // lack of darwin/arm64 binary
    "nxos", // schemaOnlyProvider does not implement runtime operation InitLogging
    "packet" // deprecated, lack of darwin/arm64 binary
  )

  private val codegenProblemPackages = Vector()

  private val compileProblemPackages = Vector(
    "azure-native", // takes too long to compile
    "alicloud", // schema error, ListenerXforwardedForConfig vs ListenerxForwardedForConfig
    "aws-iam", // id parameter, schema error - components should make this viable
    "aws-static-website", // version confusion
    "azure-justrun", // version confusion
    "databricks", // 'scala' symbol in source code
    "rootly" // version confusion
  )

  def generateAll(targetPath: os.Path): os.Path = {
    val metadata = generate(readPackagesMetadata(targetPath))
      .filterNot(m => codegenProblemPackages.contains(m.name))
    os.write.over(generatedFile, PackageMetadata.toJson(metadata), createFolders = true)
    generatedFile
  }

  def generateSelected(targetPath: os.Path, packages: List[String]): os.Path = {
    val selectedPackages = readPackagesMetadata(targetPath, selected = packages)
      .filter(p => packages.contains(p.name))
    val metadata = generate(selectedPackages)
    os.write.over(generatedFile, PackageMetadata.toJson(metadata), createFolders = true)
    generatedFile
  }

  def publishLocalAll(sourceFile: os.Path): os.Path = {
    val generated = PackageMetadata
      .fromJsonList(os.read(sourceFile: os.Path))
      .filterNot(m => compileProblemPackages.contains(m.name))
    val published = publishLocal(generated)
    os.write.over(publishedLocalFile, PackageMetadata.toJson(published), createFolders = true)
    publishedLocalFile
  }

  def publishLocalSelected(sourceFile: os.Path, packages: List[String]): os.Path = {
    val generated = PackageMetadata
      .fromJsonList(os.read(sourceFile))
      .filter(p => packages.contains(p.name))
    val published = publishLocal(generated)
    os.write.over(publishedLocalFile, PackageMetadata.toJson(published), createFolders = true)
    publishedLocalFile
  }

  def publishMavenAll(sourceFile: os.Path): os.Path = {
    val generated = PackageMetadata
      .fromJsonList(os.read(sourceFile: os.Path))
      .filterNot(m => compileProblemPackages.contains(m.name))
    val published = publishMaven(generated)
    os.write.over(publishedMavenFile, PackageMetadata.toJson(published), createFolders = true)
    publishedMavenFile
  }

  def publishMavenSelected(sourceFile: os.Path, packages: List[String]): os.Path = {
    val generated = PackageMetadata
      .fromJsonList(os.read(sourceFile))
      .filter(p => packages.contains(p.name))
    val published = publishMaven(generated)
    os.write.over(publishedMavenFile, PackageMetadata.toJson(published), createFolders = true)
    publishedMavenFile
  }

  type PackageId = (String, Option[String])

  def generate(metadata: Vector[PackageMetadata]): Vector[PackageMetadata] = {
    val seen = mutable.HashSet.empty[PackageId]
    val todo = mutable.Queue.empty[PackageMetadata]
    val done = mutable.ListBuffer.empty[PackageMetadata]

    todo.enqueueAll(metadata)
    withProgress("Generating packages from metadata", todo.size) {
      while todo.nonEmpty do
        val m             = todo.dequeue()
        val id: PackageId = (m.name, m.version.map(_.asString))
        if !seen.contains(id) then
          seen.add(id)
          val versionOrLatest = m.version.getOrElse("latest")
          Progress.report(label = s"${m.name}:${versionOrLatest}")
          try
            implicit val codegenConfig: CodegenConfig = CodegenConfig()
            val result                                = generator.generatePackageSources(metadata = m)
            val version = result.metadata.version.getOrElse(throw Exception("Package version must be present after generating")).asString

            println(s"[${new Date}] Successfully generated provider '${m.name}' version '${version}' [${new Date}]")
            println(result.asString)
            println()

            todo.enqueueAll(result.metadata.dependencies)
            done += result.metadata
            Progress.total(todo.size)
          catch
            case NonFatal(_) =>
              Progress.fail(s"[${new Date}] Code generation failed for provider '${m.name}' version '${versionOrLatest}'")
          finally Progress.end
    }
    done.toVector
  }

  def publishLocal(generated: Vector[PackageMetadata]): Vector[PackageMetadata] = {
    val seen = mutable.HashSet.empty[PackageId]
    val todo = mutable.Queue.empty[PackageMetadata]
    val done = mutable.ListBuffer.empty[PackageMetadata]

    os.remove.all(publishLocalDir) // make sure there is no dirty state from previous runs
    os.makeDir.all(publishLocalDir)

    generated.foreach(m => {
      todo.enqueueAll(m.dependencies) // dependencies first to avoid missing dependencies during compilation
      todo.enqueue(m)
    })
    withProgress("Publishing packages locally", todo.size) {
      while todo.nonEmpty do
        val m             = todo.dequeue()
        val id: PackageId = (m.name, m.version.map(_.asString))
        if !seen.contains(id) then
          seen.add(id)
          val version = m.version.getOrElse(throw Exception("Package version must be provided for publishing")).asString
          Progress.report(label = s"${m.name}:${version}")
          val logFile = publishLocalDir / s"${m.name}-${version}.log"
          try
            os.proc("just", "publish-local-provider", m.name, version).call(stdout = logFile, mergeErrIntoOut = true)
            println(s"[${new Date}] Successfully published locally provider '${m.name}' version '${version}'")

            done += m
            Progress.total(todo.size)
          catch
            case NonFatal(_) =>
              Progress.fail(s"[${new Date}] Publish failed for provider '${m.name}' version '${version}', logs: ${logFile}")
          finally Progress.end
    }
    done.toVector
  }

  def publishMaven(generated: Vector[PackageMetadata]): Vector[PackageMetadata] = {
    val seen = mutable.HashSet.empty[PackageId]
    val todo = mutable.Queue.empty[PackageMetadata]
    val done = mutable.ListBuffer.empty[PackageMetadata]

    os.remove.all(publishMavenDir) // make sure there is no dirty state from previous runs
    os.makeDir.all(publishMavenDir)

    generated.foreach(m => {
      todo.enqueueAll(m.dependencies) // dependencies first to avoid missing dependencies during compilation
      todo.enqueue(m)
    })
    withProgress("Publishing packages to Maven", todo.size) {
      while todo.nonEmpty do
        val m             = todo.dequeue()
        val id: PackageId = (m.name, m.version.map(_.asString))
        if !seen.contains(id) then
          seen.add(id)
          val version = m.version.getOrElse(throw Exception("Package version must be provided for publishing")).asString
          Progress.report(label = s"${m.name}:${version}")
          val logFile = publishMavenDir / s"${m.name}-${version}.log"
          try
            os.proc("just", "publish-maven-provider", m.name, version).call(stdout = logFile, mergeErrIntoOut = true)
            println(s"[${new Date}] Successfully published provider '${m.name}' version '${version}'")

            done += m
            Progress.total(todo.size)
          catch
            case NonFatal(_) =>
              Progress.fail(s"[${new Date}] Publish failed for provider '${m.name}' version '${version}', logs: ${logFile}")
          finally Progress.end
    }
    done.toVector
  }

  def readPackagesMetadata(targetPath: os.Path, selected: List[String] = Nil): Vector[PackageMetadata] = {
    if !os.exists(targetPath) then downloadPackagesMetadata(targetPath, selected)
    val metadataFiles = os.list(targetPath).filter(_.last.endsWith("metadata.json"))
    val metadata = metadataFiles
      .filter(p => selected.contains(p.last.stripSuffix(".metadata.json")))
      .map(PackageMetadata.fromJsonFile)
      .collect {
        case metadata if !pluginDownloadProblemPackages.contains(metadata.name) => metadata
      }
      .toVector

    if metadata.isEmpty then throw Exception(s"No packages metadata found in: '$targetPath'")
    metadata
  }

  def downloadPackagesMetadata(targetPath: os.Path, selected: List[String] = Nil): Unit = {
    os.remove.all(targetPath)

    val packagesRepoApi = "https://api.github.com/repos/pulumi/registry/contents/themes/default/data/registry/packages"

    val token      = sys.env.getOrElse("GITHUB_TOKEN", {
      System.err.println("\nExpected GITHUB_TOKEN environment variable to be set, hint: export GITHUB_TOKEN=$(gh auth token)")
      sys.exit(1)
    })
    val authHeader = Map("Authorization" -> s"token $token")

    val packagesResponse = requests.get(packagesRepoApi, headers = authHeader)
    if packagesResponse.statusCode != 200
    then throw Exception(s"Failed to fetch packages list from: '$packagesRepoApi'")

    case class PackageSource(name: String, download_url: String, sha: String) derives UpickleApi.ReadWriter
    object PackageSource {
      def fromJsonArray(json: ujson.Readable): List[PackageSource] = UpickleApi.read(json, trace = true)
    }

    val packages: List[PackageSource] = PackageSource.fromJsonArray(packagesResponse.text())
    if packages.isEmpty
    then throw Exception(s"No packages found using: '$packagesRepoApi'")
    else println(s"Found ${packages.size} packages total")

    type Error = String

    case class PackageYAML(name: String, repo_url: String, schema_file_path: String, version: String) derives YamlCodec

    val size = if selected.isEmpty then packages.size else selected.size

    // fetch all production schemas
    withProgress(s"Downloading $size packages metadata", size) {
      packages
        .map { p =>
          val packageName = p.name.stripSuffix(".yaml")
          packageName -> p
        }
        .filter { (name, _) =>
          selected match
            case Nil      => true
            case selected => selected.contains(name)
        }
        .foreach { (packageName: String, p: PackageSource) =>
          Progress.report(label = packageName)

          val metadataFile = p.name
          val metadataPath = targetPath / metadataFile
          val shaPath      = targetPath / s"${p.name}.sha"

          val hasChanged: Boolean =
            if os.exists(shaPath) then
              val sha = os.read(shaPath).split(" ").head
              sha != p.sha
            else true // no sha file, assume it has changed

          val metadataRaw: Either[Error, String] = {
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
          }

          val metadata: Either[Error, PackageMetadata] =
            metadataRaw.flatMap(_.as[PackageYAML]) match
              case Left(error) =>
                Left(s"failed to deserialize metadata for package: '${p.name}', error: $error")
              case Right(m: PackageYAML) =>
                Right(PackageMetadata(m.name, m.version).withUrl(m.repo_url))

          metadata match
            case Left(error) => Progress.fail(error)
            case Right(value) =>
              os.write.over(targetPath / s"${packageName}.metadata.json", value.toJson, createFolders = true)

          Progress.end
        }
    }

    println(s"Packages directory: '$targetPath'")
  }
