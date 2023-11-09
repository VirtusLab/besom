//> using scala 3.3.1

//> using dep com.lihaoyi::os-lib:0.9.2
//> using dep com.lihaoyi::requests:0.8.0
//> using dep com.lihaoyi::upickle:3.1.3
//> using dep org.virtuslab::scala-yaml:0.0.8
//> using file common.scala

import java.util.Date
import scala.util.control.NonFatal
import org.virtuslab.yaml.*

@main def run(args: String*): Unit =
  val packagesDir = os.pwd / ".out" / "packages"

  args match
    case "generate-all" :: Nil      => generateAll(packagesDir)
    case "publish-local-all" :: Nil => publishLocalAll(packagesDir)
    case "publish-maven-all" :: Nil => publishMavenAll(packagesDir)
    case "metadata-all" :: Nil      => downloadPackagesMetadata(packagesDir)
    case _                          => println(s"Unknown command: $args")

  def generateAll(targetPath: os.Path): Unit = {
    val metadata = readAllPackagesMetadata(targetPath)
    withProgress("Generating packages from metadata", metadata.size) {
      metadata.foreach { case (PackageMetadata(name, version, _), path: os.Path) =>
        Progress.report(label = s"$name:$version")
        try
          os.proc(
            "scala-cli",
            "--power",
            "run",
            "codegen",
            "--suppress-experimental-feature-warning",
            "--",
            "metadata",
            path
          ).call()
        catch
          case NonFatal(_) =>
            Progress.failure(s"Code generation failed for provider '${name}' version '${version}'")
        finally Progress.summary
      }
    }
  }

  def publishLocalAll(targetPath: os.Path): Unit = {
    val logDir = os.pwd / ".out" / "publishLocal"
    os.remove.all(logDir)
    os.makeDir.all(logDir)

    val metadata = readAllPackagesMetadata(targetPath)
    withProgress("Publishing packages locally", metadata.size) {
      metadata.foreach { case (PackageMetadata(name, version, _), _: os.Path) =>
        Progress.report(label = s"$name:$version")
        try
          val logFile = logDir / s"${name}-${version}.log"
          os.proc("just", "publish-local-provider-sdk", name, version)
            .call(stdout = logFile, stderr = logFile)
        catch
          case NonFatal(_) =>
            Progress.failure(s"Publish failed for provider '${name}' version '${version}'")
        finally Progress.summary
      }
    }
  }

  def publishMavenAll(targetPath: os.Path): Unit = {
    val logDir = os.pwd / ".out" / "publishMaven"
    os.makeDir.all(logDir)

    val metadata = readAllPackagesMetadata(targetPath)
    withProgress("Publishing packages to Maven", metadata.size) {
      metadata.foreach { case (PackageMetadata(name, version, _), _: os.Path) =>
        Progress.report(label = s"$name:$version")
        try
          val logFile = logDir / s"${name}-${version}.log"
          os.proc("time", "-h", "just", "publish-maven-provider-sdk", name, version)
            .call(stdout = logFile, stderr = logFile)
          println(new Date)
          println(s"Successfully published provider '${name}' version '${version}'")
        catch
          case NonFatal(_) =>
            Progress.failure(s"Publish failed for provider '${name}' version '${version}'")
        finally Progress.summary
      }
    }
  }

  def readAllPackagesMetadata(targetPath: os.Path): List[(PackageMetadata, os.Path)] = {
    val metadataFiles = os.list(targetPath).filter(_.last.endsWith("metadata.json"))
    val metadata = metadataFiles.map { path =>
      val metadata = upickle_.read[PackageMetadata](os.read(path))
      (metadata, path)
    }.toList
    if metadata.isEmpty then throw Exception(s"No packages metadata found in: '$targetPath'")
    metadata
  }

  def downloadPackagesMetadata(targetPath: os.Path): Unit = {
    os.remove.all(targetPath)

    val packagesRepoApi = "https://api.github.com/repos/pulumi/registry/contents/themes/default/data/registry/packages"

    val token = sys.env.getOrElse("GITHUB_TOKEN", sys.error("Expected GITHUB_TOKEN environment variable to be set"))
    val authHeader = Map("Authorization" -> s"token $token")

    val packagesResponse = requests.get(packagesRepoApi, headers = authHeader)
    if packagesResponse.statusCode != 200 then
      throw Exception(s"Failed to fetch packages list from: '$packagesRepoApi'")

    case class PackageSource(name: String, download_url: String, sha: String) derives upickle_.ReadWriter
    object PackageSource {
      def fromList(json: ujson.Readable): List[PackageSource] = upickle_.read(json, trace = true)
    }

    val packages: List[PackageSource] = PackageSource.fromList(packagesResponse.text())
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
          case Left(error) => Progress.failure(error)
          case Right(value) =>
            os.write.over(targetPath / s"${packageName}.metadata.json", value.toJson, createFolders = true)

        Progress.summary
      }
    }

    println(s"Packages directory: '$targetPath'")
  }

// synchronize with codegen/src/PackageMetadata.scala
private case class PackageMetadata private (
  name: String,
  version: String,
  server: Option[String] = None
) derives upickle_.ReadWriter {
  require(name.nonEmpty, "name cannot be empty")
  require(version.nonEmpty, "version cannot be empty")

  def withUrl(url: String): PackageMetadata = {
    val server = url match {
      case s"https://github.com/pulumi/pulumi-${_}" => None // use default
      case s"https://github.com/$org/$name"         => Some(s"github://api.github.com/$org/$name")
      case _                                        => throw Exception(s"Unknown repo url format: ${url}")
    }
    PackageMetadata(name, version, server)
  }

  def toJson: String = upickle_.write(this)
}
object PackageMetadata:
  def apply(name: String, version: String, server: Option[String] = None): PackageMetadata =
    new PackageMetadata(name, version.trim.stripPrefix("v"), server)

private object upickle_ extends upickle.AttributeTagged {
  override implicit def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]] {
      case None    => null.asInstanceOf[T]
      case Some(x) => x
    }

  override implicit def OptionReader[T: Reader]: Reader[Option[T]] = {
    new Reader.Delegate[Any, Option[T]](implicitly[Reader[T]].map(Some(_))) {
      override def visitNull(index: Int): Option[Nothing] = None
    }
  }
}
