//> using toolkit "latest"
//> using lib "org.virtuslab::scala-yaml:0.0.8"

import sttp.client4.quick.*
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.immutable.Stream.Cons
import java.net.URLEncoder
import org.virtuslab.yaml.*
import scala.util.*

case class PulumiPackage(
  category: String,
  component: Boolean,
  description: String,
  featured: Boolean,
  logo_url: String,
  name: String,
  native: Boolean,
  package_status: String,
  publisher: String,
  repo_url: String,
  schema_file_path: String,
  title: String,
  updated_on: Long,
  version: String
) derives YamlCodec

val packagesRepoApi = "https://api.github.com/repos/pulumi/registry/contents/themes/default/data/registry/packages"

def githubToken = sys.env.getOrElse("GITHUB_TOKEN", throw new Exception("Missing GITHUB_TOKEN"))

def headers = Map("Authorization" -> s"token $githubToken")

def getSchema(packageName: String, version: String): os.CommandResult =
  os.proc("just", "get-schema", packageName, version).call(check = false)

def fetchPluginDownloadURLFromSchema(packageName: String, version: String): String =
  val schema = ujson.read(os.read(os.pwd / ".out" / "schemas" / packageName / version / "schema.json"))
  schema("pluginDownloadURL").str

def fetchSchemaAndExtractPluginDownloadURL(pulumiPackage: PulumiPackage): Try[String] =
  if pulumiPackage.schema_file_path.endsWith(".yaml") then return Failure(Exception("YAML schemas are not supported"))

  val Array(owner, repo) = pulumiPackage.repo_url.stripPrefix("https://github.com/").split("/", 2)
  val version            = pulumiPackage.version
  val schemaFilePath     = pulumiPackage.schema_file_path

  val schemaUrl = s"https://raw.githubusercontent.com/$owner/$repo/$version/$schemaFilePath"

  print(s" Fetching schema from: $schemaUrl")

  val response = quickRequest.get(uri"$schemaUrl").headers(headers).send()
  val schema   = ujson.read(response.body)

  Try(schema("pluginDownloadURL").str) match
    case Failure(t) =>
      val tmpFile = os.root / "tmp" / URLEncoder.encode(schemaUrl.replace(" ", "_"))
      os.write.over(tmpFile, response.body)
      print(s" Wrote schema to $tmpFile")
      Failure(t)
    case success => success

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

def noPluginsInstalled: Boolean =
  os.proc("pulumi", "plugin", "ls").call().out.lines().map(_.trim).contains("TOTAL plugin cache size: 0 B")

def fetchPackageInfo(): Map[String, PulumiPackage] =
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

        packageName -> yamlResponse.body.as[PulumiPackage].getOrElse {
          throw Exception(s"Failed to parse package details for $packageName:\n${yamlResponse.body}")
        }
      }
      .toMap
  }

enum Server:
  case Registry
  case Community(url: String)

extension (srv: Server)
  def show: String =
    srv match
      case Server.Registry       => "registry"
      case Server.Community(url) => url

def tryToFindServer(
  packageName: String,
  pulumiPackage: PulumiPackage
): Try[Server] =
  val main = installPlugin(packageName, pulumiPackage.version, None)

  if main.exitCode == 0 then Success(Server.Registry)
  else
    fetchSchemaAndExtractPluginDownloadURL(pulumiPackage).flatMap { pluginDownloadUrl =>
      val communityPlugin = installPlugin(packageName, pulumiPackage.version, Some(pluginDownloadUrl))

      if communityPlugin.exitCode == 0
      then Success(Server.Community(pluginDownloadUrl))
      else Failure(Exception(s"Failed to install plugin for $packageName ${pulumiPackage.version}"))
    }

    // throw Exception(s"Failed to find plugin download URL for $packageName ${pulumiPackage.version}")

@main def registry(command: String): Unit =
  command match
    case "fetch-packages" =>
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
          .parMap(10) { case (packageName, pulumiPackage) =>
            val maybeServer = tryToFindServer(packageName, pulumiPackage)

            val current = index.incrementAndGet()

            reportProgress

            (packageName, pulumiPackage.version, maybeServer)
          }
      }
        .sortBy(_._3.isSuccess)
        .foreach {
          case (packageName, version, Failure(t)) =>
            println(s"Failed to find server for: $packageName $version: ${t.getMessage}")
          case (packageName, version, Success(serverUrl)) =>
            println(s"package: $packageName, version: $version, server: ${serverUrl.show}")
        }
