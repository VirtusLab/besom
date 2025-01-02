package yaga.sbt.aws

import _root_.sbt.Project
import _root_.sbt._
import _root_.sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import java.nio.file.{Files, Path}

import YagaAwsLambdaPlugin.autoImport._

trait YagaAwsLambdaDependency extends yaga.sbt.YagaDependency

case class YagaAwsLambdaProjectDependency(
  project: Project,
  outputSubdirName: Option[String],
  packagePrefix: Option[String],
  withInfra: Boolean
) extends YagaAwsLambdaDependency {
  override def addSelfToProject(baseProject: Project): Project = {
    val codegenTask = Def.task {
      val outputSubdirectoryName = outputSubdirName.getOrElse((project / name).value)
      val codegenOutputDir = (baseProject / Compile / sourceManaged).value / "yaga-aws-codegen" / outputSubdirectoryName
      val pkgPrefix = packagePrefix.getOrElse("")

      val sources: Seq[Path] = Seq(
        (project / yagaAwsLambdaAssembly).value
      )
      val dependencyJarChanged = (project / yagaAwsLambdaAssembly).outputFileChanges.hasChanges
      val log = streams.value.log

      if (dependencyJarChanged || !Files.exists(codegenOutputDir.toPath)) {
        CodegenHelpers.runCodegen(localJarSources = sources, packagePrefix = pkgPrefix, outputDir = codegenOutputDir.toPath, withInfra = withInfra, log = log)
      }

      (codegenOutputDir ** "*.scala").get
    }

    baseProject.settings(
      yaga.sbt.YagaPlugin.autoImport.yagaGeneratedSources ++= codegenTask.value,

      libraryDependencies ++= {
        if (withInfra)
          Seq(YagaAwsLambdaPlugin.yagaBesomAwsSdkDep)
        else
          Seq.empty
      }
    )
  }
}

