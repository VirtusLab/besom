package yaga.sbt.aws

sealed trait CodegenItem {
  def packagePrefix: String
  def withInfra: Boolean
}

object CodegenItem {
  case class FromMaven(
    artifactCoordinates: String,
    outputSubdirName: String,
    packagePrefix: String,
    withInfra: Boolean
  ) extends CodegenItem

  case class FromLocalJars(
    absoluteJarsPaths: Seq[java.nio.file.Path],
    outputSubdirName: String,
    packagePrefix: String,
    withInfra: Boolean
  ) extends CodegenItem
}
