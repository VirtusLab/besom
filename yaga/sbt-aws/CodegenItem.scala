package yaga.sbt.aws

sealed trait CodegenItem {
  def packagePrefix: String
  def withInfra: Boolean
}

object CodegenItem {
  trait FromMaven extends CodegenItem {
    def artifactCoordinates: String
    def outputSubdirName: String
  }

  trait FromLocalJars extends CodegenItem {
    def absoluteJarsPaths: Seq[java.nio.file.Path]
    def outputSubdirName: String
  }

  // trait FromProject extends CodegenItem {
  //   def project: sbt.ProjectRef
  // }

  case class ModelFromMaven(
    artifactCoordinates: String,
    outputSubdirName: String,
    packagePrefix: String
  ) extends FromMaven {
    override def withInfra: Boolean = false
  }

  case class InfraFromMaven(
    artifactCoordinates: String,
    outputSubdirName: String,
    packagePrefix: String
  ) extends FromMaven {
    override def withInfra: Boolean = true
  }

  case class ModelFromLocalJars(
    absoluteJarsPaths: Seq[java.nio.file.Path],
    outputSubdirName: String,
    packagePrefix: String
  ) extends FromLocalJars {
    override def withInfra: Boolean = false
  }

  case class InfraFromLocalJars(
    absoluteJarsPaths: Seq[java.nio.file.Path],
    outputSubdirName: String,
    packagePrefix: String
  ) extends FromLocalJars {
    override def withInfra: Boolean = true
  }

  // case class ModelFromProject(
  //   project: sbt.ProjectRef,
  //   packagePrefix: String
  // ) extends FromProject {
  //   override def withInfra: Boolean = false
  // }

  // case class InfraFromProject(
  //   project: sbt.ProjectRef,
  //   packagePrefix: String
  // ) extends FromProject {
  //   override def withInfra: Boolean = true
  // }

}
