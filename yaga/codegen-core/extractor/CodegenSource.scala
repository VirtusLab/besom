package yaga.codegen.core.extractor

sealed trait CodegenSource
object CodegenSource:
  case class MavenArtifact(
    orgName: String,
    moduleName: String,
    version: String,
  ) extends CodegenSource

  object MavenArtifact:
    def parseCoordinates(mavenCoordinates: String): MavenArtifact =
      val List(orgName, moduleName, version) = mavenCoordinates.split(":").toList
      MavenArtifact(orgName, moduleName, version)

  case class LocalJar(
    absolutePath: java.nio.file.Path,
  ) extends CodegenSource
