package besom.cfg.containers

import besom.cfg.internal.Schema
import besom.cfg.Configured
import besom.model.SemanticVersion
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

def resolveMetadataFromImage(image: String, overrideClasspathPath: Option[String] = None): String =
  lazy val sbtNativePackagerFormatCall =
    val classpathPath = overrideClasspathPath.getOrElse("/opt/docker/lib/*")
    os
      .proc("docker", "run", "--rm", "--entrypoint", "java", image, "-cp", classpathPath, "besom.cfg.SummonConfiguration")
      .call(check = false)

  lazy val customDockerFormatCall =
    val classpathPath = overrideClasspathPath.getOrElse("/app/main")
    os
      .proc("docker", "run", "--rm", "--entrypoint", "java", image, "-cp", classpathPath, "besom.cfg.SummonConfiguration")
      .call(check = false)

  if sbtNativePackagerFormatCall.exitCode == 0 then sbtNativePackagerFormatCall.out.text().trim()
  else if customDockerFormatCall.exitCode == 0 then customDockerFormatCall.out.text().trim()
  else throw RuntimeException(s"Failed to get configuration from $image")

def getDockerImageMetadata(image: String, dontUseCache: Boolean, overrideClasspathPath: Option[String] = None): Either[Throwable, Schema] =
  Try {
    val maybeCachedJson = if dontUseCache then None else fetchFromCache(image)

    val json = maybeCachedJson match {
      case Some(cachedJson) => cachedJson
      case None =>
        val json = resolveMetadataFromImage(image, overrideClasspathPath)

        saveToCache(image, json)

        json
    }

    val schema = summon[JsonFormat[Schema]].read(json.parseJson)
    val obtainedSchemaVersion = SemanticVersion.parse(schema.version).toTry.get
    val besomCfgVersionFromClasspath = SemanticVersion.parse(besom.cfg.Version).toTry.get

    if obtainedSchemaVersion > besomCfgVersionFromClasspath then
      throw Exception(
        s"Version of besom-cfg-lib used in image $image is '$obtainedSchemaVersion' and is newer than the present library version '$besomCfgVersionFromClasspath'. Please update the besom-cfg extension in your besom project."
      )
    else schema

  }.toEither
