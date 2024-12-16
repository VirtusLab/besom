package yaga.sbt.aws

import sbt._
import sbt.Keys._

private[aws] object MavenArtifactsHelpers {
  // TODO Deduplicate with ContextSetup.scala and LambdaHandlerUtils.scala?
  def jarPathsFromMavenCoordinates(orgName: String, moduleName: String , version: String): List[java.nio.file.Path] = {
    import coursier._

    // TODO Verify performance and try to speed up compilation
    Fetch()
      .withRepositories(Seq(
        // TODO allow customization of repositories
        LocalRepositories.ivy2Local,
        Repositories.central
      ))
      .addDependencies(
        Dependency(Module(Organization(orgName), ModuleName(moduleName)), version)
      )
      .run()
      .map(_.toPath)
      .toList
  }

  def fatJarPathFromMavenCoordinates(orgName: String, moduleName: String , version: String): java.nio.file.Path = {
    val fetchedFiles: List[java.nio.file.Path] = jarPathsFromMavenCoordinates(orgName, moduleName, version)

    val expectedJarFileName = s"${moduleName}.jar"
    val filteredFiles: List[java.nio.file.Path] = fetchedFiles.filter(file => file.getFileName.toString == expectedJarFileName)

    filteredFiles match {
      case Nil =>
        throw new Exception(s"No file with name $expectedJarFileName found when getting paths of resolved dependencies. All paths:\n ${fetchedFiles.mkString("\n")}.")
      case file :: Nil => file
      case files =>
        throw new Exception(s"Expected exactly one file with name $expectedJarFileName when getting paths of resolved dependencies. Instead found: ${files.mkString(", ")}.")
    }
  }

  def runMavenArtifactMainWithArgs(orgName: String, moduleName: String, version: String, mainClass: String, args: Seq[String]): Unit = {
    val dependencyPaths = jarPathsFromMavenCoordinates(orgName, moduleName, version)
    val commandParts = Seq(
      "java",
      "-cp", dependencyPaths.mkString(":"),
      mainClass
    ) ++ args

    import scala.sys.process._

    commandParts.!! // TODO Handle errors
  }
}