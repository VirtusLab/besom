//> using toolkit "latest"

import sttp.client4.quick.*
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.immutable.Stream.Cons

val packagesRepoApi = "https://api.github.com/repos/pulumi/registry/contents/themes/default/data/registry/packages"

val pulumiverseApiUrl = "github://api.github.com/pulumiverse"

def githubToken = sys.env.getOrElse("GITHUB_TOKEN", throw new Exception("Missing GITHUB_TOKEN"))

def headers = Map("Authorization" -> s"token $githubToken")

def getSchema(packageName: String, version: String): os.CommandResult =
  os.proc("just", "get-schema", packageName, version).call(check = false)

def fetchPluginDownloadURLFromSchema(packageName: String, version: String): String =
  val schema = ujson.read(os.read(os.pwd / ".out" / "schemas" / packageName / version / "schema.json"))
  schema("pluginDownloadURL").str

def installPlugin(
  packageName: String,
  version: String,
  server: Option[String],
  silent: Boolean = true
): os.CommandResult =
  val stderr = if silent then os.Pipe else os.Inherit
  os
    .proc("just", "get-schema", packageName, version)
    .call(
      check = false,
      env = server.map(s => Map("PULUMI_PLUGIN_INSTALL_ARGS" -> s"--server $s")).getOrElse(Map.empty),
      stderr = stderr
    )

def generateProvider(packageName: String, version: String, silent: Boolean = true): os.CommandResult =
  val stderr = if silent then os.Pipe else os.Inherit
  os.proc("just", "generate-provider-sdk", packageName, version).call(check = false, stderr = stderr)

def publishProviderLocally(packageName: String, version: String, silent: Boolean = true): os.CommandResult =
  val stderr = if silent then os.Pipe else os.Inherit
  os.proc("just", "publish-local-provider-sdk", packageName, version).call(check = false, stderr = stderr)

def publishProviderToMaven(packageName: String, version: String, silent: Boolean = true): os.CommandResult =
  val stderr = if silent then os.Pipe else os.Inherit
  os.proc("just", "publish-maven-provider-sdk", packageName, version).call(check = false, stderr = stderr)

def findRepoUrl(yaml: String): Option[String] =
  yaml.split("\n").find(_.startsWith("repo_url:")).map(_.stripPrefix("repo_url: "))

def findVersion(yaml: String): Option[String] =
  yaml.split("\n").find(_.startsWith("version: ")).map(_.stripPrefix("version: "))

def noPluginsInstalled: Boolean =
  os.proc("pulumi", "plugin", "ls").call().out.lines().map(_.trim).contains("TOTAL plugin cache size: 0 B")

def fetchPackageInfo(): Map[String, String] =
  val response  = quickRequest.get(uri"$packagesRepoApi").headers(headers).send()
  val files     = ujson.read(response.body).arr
  val yamlFiles = files.filter(file => file("name").str.endsWith(".yaml"))

  withProgress("Fetching package info", yamlFiles.size) {
    yamlFiles
      .parMap(10) { file =>
        val packageName  = file("name").str.stripSuffix(".yaml")
        val yamlUrl      = file("download_url").str
        val yamlResponse = quickRequest.get(uri"$yamlUrl").headers(headers).send()

        reportProgress

        packageName -> yamlResponse.body
      }
      .toMap
  }

enum Server:
  case Registry
  case Pulumiverse
  case Custom(url: String)

extension (srv: Server)
  def show: String =
    srv match
      case Server.Registry    => "registry"
      case Server.Pulumiverse => pulumiverseApiUrl
      case Server.Custom(url) => url

def tryToFindServer(packageName: String, packageVersion: String, repoUrl: String): Option[Server] =
  val main = installPlugin(packageName, packageVersion, None)

  if main.exitCode == 0 then Some(Server.Registry)
  else
    val pv = installPlugin(packageName, packageVersion, Some(pulumiverseApiUrl))

    if (pv.exitCode == 0) then Some(Server.Pulumiverse)
    else
      val reformattedUrl = repoUrl.stripPrefix("\"").stripSuffix("\"").replaceFirst("https://", "github://api.")
      val custom         = installPlugin(packageName, packageVersion, Some(reformattedUrl))

      if custom.exitCode == 0 then Some(Server.Custom(reformattedUrl))
      else None

@main def registry(command: String): Unit =
  command match
    case "packages" =>
      if !noPluginsInstalled then
        println(
          "Warning: this command does not work properly when resource plugins are installed. " +
            "It is advised to run `pulumi plugin rm --all` before running this command. Press enter to continue."
        )
        scala.io.StdIn.readLine

      val packageInfos = fetchPackageInfo().toVector

      val index = AtomicInteger(0)

      withProgress("Finding servers", packageInfos.size) {
        packageInfos
          .parMap(10) { case (packageName, packageDetails) =>
            val version =
              findVersion(packageDetails).getOrElse(throw Exception(s"Failed to find version for $packageName"))
            val repoUrl =
              findRepoUrl(packageDetails).getOrElse(throw Exception(s"Failed to find repo_url for $packageName"))
            val maybeServer = tryToFindServer(packageName, version, repoUrl)

            val current = index.incrementAndGet()

            reportProgress

            (packageName, version, maybeServer)
          }
      }
        .sortBy(_._3.isDefined)
        .foreach {
          case (packageName, version, None) => println(s"Failed to find server for: $packageName $version")
          case (packageName, version, Some(serverUrl)) =>
            println(s"package: $packageName, version: $version, server: ${serverUrl.show}")
        }
