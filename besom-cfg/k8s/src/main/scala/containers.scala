package besom.cfg.containers

// this should be a separate package, base for all container integrations

import besom.cfg.internal.Schema
import besom.json.*
import scala.util.Try

val cacheDir = sys.props.get("java.io.tmpdir").getOrElse("/tmp")

def sanitizeImageName(image: String): String =
  image
    .replace("/", "_")
    .replace(":", "_")

def fetchFromCache(image: String): Option[String] =
  if image.endsWith(":latest") then None
  else
    val sanitized = sanitizeImageName(image)
    os.makeDir.all(os.Path(s"$cacheDir/besom-cfg"))
    Try(os.read(os.Path(s"$cacheDir/besom-cfg/$sanitized"))).toOption

def saveToCache(image: String, content: String): Unit =
  if !image.endsWith(":latest") then
    val sanitized = sanitizeImageName(image)
    os.makeDir.all(os.Path(s"$cacheDir/besom-cfg"))
    os.write.over(os.Path(s"$cacheDir/besom-cfg/$sanitized"), content)

def resolveMetadataFromImage(image: String): String =
  lazy val sbtNativePackagerFormatCall =
    os
      .proc("docker", "run", "--rm", "--entrypoint", "java", image, "-cp", "/opt/docker/lib/*", "besom.cfg.SummonConfiguration")
      .call(check = false)

  lazy val customDockerFormatCall =
    os
      .proc("docker", "run", "--rm", "--entrypoint", "java", image, "-cp", "/app/main", "besom.cfg.SummonConfiguration")
      .call(check = false)

  if sbtNativePackagerFormatCall.exitCode == 0 then sbtNativePackagerFormatCall.out.text().trim()
  else if customDockerFormatCall.exitCode == 0 then customDockerFormatCall.out.text().trim()
  else throw RuntimeException(s"Failed to get configuration from $image")

def getDockerImageMetadata(image: String): Either[Throwable, Schema] =
  Try {
    // 1. cache result per image in /tmp DONE
    // 2. verify the version of the library used, fail macro if we are older than it
    // 3. parse the json to correct structure DONE
    // next:
    // - support different image setups, autodetect which one is used somehow? somewhat DONE
    // - cp argument should be configurable
    val json = fetchFromCache(image) match {
      case Some(cachedJson) => cachedJson
      case None =>
        val json = resolveMetadataFromImage(image)

        saveToCache(image, json)

        json
    }

    summon[JsonFormat[Schema]].read(json.parseJson)
  }.toEither
