//> using scala 3.3.1

//> using dep com.lihaoyi::os-lib:0.9.2
//> using dep com.lihaoyi::requests:0.8.0
//> using dep com.lihaoyi::upickle:3.1.3
//> using dep org.virtuslab::scala-yaml:0.0.8
//> using dep org.virtuslab::besom-codegen:0.1.1-SNAPSHOT
//> using file common.scala

package besom.scripts

import besom.codegen.Config.CodegenConfig
import besom.codegen.{PackageMetadata, UpickleApi, generator}
import org.virtuslab.yaml.*

import java.util.Date
import scala.collection.mutable
import scala.util.*
import scala.util.control.NonFatal

@main def run(args: String*): Unit =
  val packagesDir   = os.pwd / ".out" / "packages"
  val generatedFile = os.pwd / ".out" / "codegen" / "generated.json"

  val pluginDownloadProblemPackages = Vector(
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

  val codegenProblemPackages = Vector()

  val compileProblemPackages = Vector(
    "azure-native", // decoder error
    "alicloud", // schema error, ListenerXforwardedForConfig vs ListenerxForwardedForConfig
//    "aws-apigateway", // decoder error
    "aws-iam", // id parameter
    "aws-native", // decoder error
    "aws-static-website", // version confusion
    "azure-justrun", // version confusion
    "databricks", // 'scala' symbol in source code
    "digitalocean", // urn parameter
//    "eks", // decoder error
    "rootly" // version confusion
  )

  args match
    case "metadata-all" :: Nil      => downloadPackagesMetadata(packagesDir)
    case "generate-all" :: Nil      => generateAll(packagesDir)
    case "publish-local-all" :: Nil => publishLocalAll()
    case "publish-maven-all" :: Nil => publishMavenAll(packagesDir)
    case _                          => println(s"Unknown command: $args")

  def generateAll(targetPath: os.Path): Unit = {
    val metadata = generate(readAllPackagesMetadata(targetPath))
    os.write(generatedFile, PackageMetadata.toJson(metadata), createFolders = true)
  }

  def generate(metadata: Vector[PackageMetadata]): Vector[PackageMetadata] = {
    val results = withProgress("Generating packages from metadata", metadata.size) {
      metadata
        .filterNot(m => codegenProblemPackages.contains(m.name))
        .flatMap {
          case m @ PackageMetadata(name, Some(version), _, _) =>
            Progress.report(label = s"$name:$version")
            try
              implicit val codegenConfig: CodegenConfig = CodegenConfig()

              val result = generator.generatePackageSources(metadata = m)
              println(new Date)
              println(s"Successfully generated provider '${name}' version '${version}'")
              println(result.asString)
              println()

              Vector(result.metadata)
            catch
              case NonFatal(_) =>
                Progress.fail(s"Code generation failed for provider '${name}' version '${version}'")
                Vector()
            finally Progress.end
          case PackageMetadata(name, None, _, _) =>
            Progress.report(label = s"$name")
            Progress.fail(s"Code generation failed for provider '${name}', version is not defined")
            Progress.end
            Vector()
        }
    }
    val (dependencies, packages) = extractDependencies(results)
    deDuplicate(generate(dependencies)) ++ packages
  }

  def publishLocalAll(): Unit = {
    val generated = PackageMetadata.fromJsonList(os.read(generatedFile))
    publishLocal(generated)
  }

  def publishLocal(generated: Vector[PackageMetadata]): Unit = {
    val logDir = os.pwd / ".out" / "publishLocal"
    os.remove.all(logDir)
    os.makeDir.all(logDir)

    val (dependencies, packages) = extractDependencies(generated)
    publishLocal(dependencies)

    withProgress("Publishing packages locally", packages.size) {
      packages
        .filterNot(m => compileProblemPackages.contains(m.name))
        .foreach {
          case PackageMetadata(name, Some(version), _, _) =>
            Progress.report(label = s"$name:$version")
            try
              val logFile = logDir / s"${name}-${version}.log"
              os.proc("just", "publish-local-provider-sdk", name, version.asString)
                .call(stdout = logFile, stderr = logFile)
              println(new Date)
              println(s"Successfully published locally provider '${name}' version '${version}'")
            catch
              case NonFatal(_) =>
                Progress.fail(s"Publish failed for provider '${name}' version '${version}'")
            finally Progress.end
          case PackageMetadata(name, None, _, _) =>
            Progress.report(label = s"$name")
            Progress.fail(s"Publish failed for provider '${name}', version is not defined")
            Progress.end
        }
    }
  }

  def publishMavenAll(targetPath: os.Path): Unit = {
    val logDir = os.pwd / ".out" / "publishMaven"
    os.remove.all(logDir)
    os.makeDir.all(logDir)

    val metadata = readAllPackagesMetadata(targetPath)
    withProgress("Publishing packages to Maven", metadata.size) {
      metadata.foreach {
        case PackageMetadata(name, Some(version), _, _) =>
          Progress.report(label = s"$name:$version")
          try
            val logFile = logDir / s"${name}-${version}.log"
            os.proc("just", "publish-maven-provider-sdk", name, version.asString)
              .call(stdout = logFile, stderr = logFile)
            println(new Date)
            println(s"Successfully published provider '${name}' version '${version}'")
          catch
            case NonFatal(_) =>
              Progress.fail(s"Publish failed for provider '${name}' version '${version}'")
          finally Progress.end
        case PackageMetadata(name, None, _, _) =>
          Progress.report(label = s"$name")
          Progress.fail(s"Publish failed for provider '${name}', version is not defined")
          Progress.end
      }
    }
  }

  def readAllPackagesMetadata(targetPath: os.Path): Vector[PackageMetadata] = {
    val metadataFiles = os.list(targetPath).filter(_.last.endsWith("metadata.json"))
    val metadata = metadataFiles
      .map(PackageMetadata.fromJsonFile)
      .collect {
        case metadata if !pluginDownloadProblemPackages.contains(metadata.name) => metadata
      }
      .toVector

    if metadata.isEmpty then throw Exception(s"No packages metadata found in: '$targetPath'")
    metadata
  }

  def downloadPackagesMetadata(targetPath: os.Path): Unit = {
    os.remove.all(targetPath)

    val packagesRepoApi = "https://api.github.com/repos/pulumi/registry/contents/themes/default/data/registry/packages"

    val token      = sys.env.getOrElse("GITHUB_TOKEN", sys.error("Expected GITHUB_TOKEN environment variable to be set"))
    val authHeader = Map("Authorization" -> s"token $token")

    val packagesResponse = requests.get(packagesRepoApi, headers = authHeader)
    if packagesResponse.statusCode != 200
    then throw Exception(s"Failed to fetch packages list from: '$packagesRepoApi'")

    case class PackageSource(name: String, download_url: String, sha: String) derives UpickleApi.ReadWriter
    object PackageSource {
      def fromJsonArray(json: ujson.Readable): List[PackageSource] = UpickleApi.read(json, trace = true)
    }

    val packages: List[PackageSource] = PackageSource.fromJsonArray(packagesResponse.text())
    if packages.isEmpty then throw Exception(s"No packages found using: '$packagesRepoApi'")

    type Error = String

    case class PackageYAML(name: String, repo_url: String, schema_file_path: String, version: String) derives YamlCodec

    // fetch all production schemas
    withProgress("Downloading packages metadata", packages.size) {
      packages.foreach { (p: PackageSource) =>
        val packageName = p.name.stripSuffix(".yaml")
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

  def extractDependencies(metadata: Vector[PackageMetadata]): (Vector[PackageMetadata], Vector[PackageMetadata]) =
    val (dependencies, packages) = metadata.foldLeft((Vector.empty[PackageMetadata], Vector.empty[PackageMetadata])) { (acc, m) =>
      (acc._1 ++ m.dependencies, acc._2 :+ m)
    }
    (deDuplicate(dependencies), packages)
  end extractDependencies

  def deDuplicate[A](items: Vector[A]): Vector[A] =
    val seen = mutable.HashSet.empty[A]
    items.filter { item =>
      val isNew = !seen.contains(item)
      seen.add(item)
      isNew
    }

end run
