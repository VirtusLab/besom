package yaga.sbt.aws

import _root_.sbt.Project
import _root_.sbt._
import _root_.sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._

import YagaAwsLambdaPlugin.autoImport._

trait YagaAwsLambdaDependency extends yaga.sbt.YagaDependency

case class YagaAwsLambdaProjectDependency(
  project: Project,
  outputSubdirName: Option[String],
  packagePrefix: Option[String],
  withInfra: Boolean
) extends YagaAwsLambdaDependency {
  override def addSelfToProject(baseProject: Project): Project = {
    baseProject.settings(
      yagaAwsCodegenItems += CodegenItem.FromLocalJars(
        absoluteJarsPaths = Seq(
          (project / assembly).value.toPath
        ),
        outputSubdirName = outputSubdirName.getOrElse((project / name).value),
        packagePrefix = packagePrefix.getOrElse(""),
        withInfra = withInfra
      ),
      libraryDependencies ++= {
        if (withInfra)
          Seq(YagaAwsLambdaPlugin.yagaBesomAwsSdkDep)
        else
          Seq.empty
      }
    )
  }
}

