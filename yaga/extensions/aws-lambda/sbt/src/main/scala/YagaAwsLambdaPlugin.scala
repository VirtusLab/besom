package yaga.sbt.aws

import scala.language.implicitConversions

import _root_.sbt._
import _root_.sbt.Keys._
import _root_.sbt.AutoPlugin
import _root_.sbt.nio.{ file => _, _ }
import java.nio.file.{Files, Path}
import sbtassembly.AssemblyPlugin._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy
import yaga.sbt.{MavenArtifactsHelpers, YagaDependency}

object YagaAwsLambdaPlugin extends AutoPlugin {
  val yagaVersion = "0.4.0-SNAPSHOT" // TODO Extract from root build definition
  val yagaAwsSdkDep = "org.virtuslab" %% "yaga-aws-lambda-sdk" % yagaVersion
  val yagaBesomAwsSdkDep = "org.virtuslab" %% "yaga-aws-lambda-besom" % yagaVersion // TODO should be suffixed with besom core minor version?

  override def requires = sbtassembly.AssemblyPlugin && yaga.sbt.YagaPlugin
  override def trigger = allRequirements

  object autoImport {
    val yagaAwsLambdaAssembly = taskKey[Path]("Assembled AWS lambda jar")
    val yagaAwsRunCodegen: TaskKey[Seq[File]] = taskKey[Seq[File]]("Generate code for yaga AWS")

    implicit class ProjectYagaDependencyOps(project: Project) {
      def awsLambda = {
        project.settings(
          libraryDependencies += yagaAwsSdkDep
        )
      }

      def awsLambdaModel(outputSubdirName: Option[String] = None, packagePrefix: String = ""): YagaAwsLambdaProjectDependency = {
        YagaAwsLambdaProjectDependency(
          project = project,
          outputSubdirName = outputSubdirName,
          packagePrefix = packagePrefix,
          withInfra = false
        )
      }

      def awsLambdaInfra(outputSubdirName: Option[String] = None, packagePrefix: String = ""): YagaAwsLambdaProjectDependency = {
        YagaAwsLambdaProjectDependency(
          project = project,
          outputSubdirName = outputSubdirName,
          packagePrefix = packagePrefix,
          withInfra = true
        )
      }
    }
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    yagaAwsLambdaAssembly := {
      assembly.value.toPath
    },
    assembly / assemblyMergeStrategy := defaultAssemblyMergeStrategy.value,
  )

  def defaultAssemblyMergeStrategy = Def.setting[String => MergeStrategy] {
    (path: String) => path match {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  }
}