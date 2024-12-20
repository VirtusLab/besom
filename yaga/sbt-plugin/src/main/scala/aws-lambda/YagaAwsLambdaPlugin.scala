package yaga.sbt.aws

import scala.language.implicitConversions

import sbt._
import sbt.Keys._
import sbt.AutoPlugin
import java.nio.file.{Files, Path}
import sbtassembly.AssemblyPlugin._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy
import yaga.sbt.{MavenArtifactsHelpers, YagaDependency}

object YagaAwsLambdaPlugin extends AutoPlugin {
  val yagaVersion = "0.4.0-SNAPSHOT"
  val yagaAwsSdkDep = "org.virtuslab" %% "yaga-aws" % yagaVersion
  val yagaBesomAwsSdkDep = "org.virtuslab" %% "yaga-besom-aws" % yagaVersion

  override def requires = sbtassembly.AssemblyPlugin
  override def trigger = allRequirements

  object autoImport {
    val yagaDebug = taskKey[Unit]("Debug mode for yaga AWS codegen")
    val yagaAwsCodegenItems: TaskKey[Seq[CodegenItem]] = taskKey[Seq[CodegenItem]]("Configuration entries for Yaga AWS codegen") // TODO when referring to the setting use a narrower scope (e.g. Compile)?
    val yagaAwsRunCodegen: TaskKey[Seq[File]] = taskKey[Seq[File]]("Generate code for yaga AWS")

    implicit class ProjectYagaDependencyOps(project: Project) {
      def awsLambda = {
        project.settings(
          libraryDependencies += yagaAwsSdkDep
        )
      }

      def awsLambdaModel(outputSubdirName: Option[String] = None, packagePrefix: Option[String] = None) = {
        YagaAwsLambdaProjectDependency(
          project = project,
          outputSubdirName = outputSubdirName,
          packagePrefix = packagePrefix,
          withInfra = false
        )
      }

      def awsLambdaInfra(outputSubdirName: Option[String] = None, packagePrefix: Option[String] = None) = {
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
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    yagaAwsCodegenItems := Seq.empty,
    yagaAwsRunCodegen := helperGenerator.map(_.flatten).value,
    Compile / sourceGenerators += yagaAwsRunCodegen.taskValue,
    yagaDebug := {

    }
  )

  def defaultAssemblyMergeStrategy = Def.setting[String => MergeStrategy] {
    (path: String) => path match {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  }

  private def flattenTasks[A](tasks: Seq[Def.Initialize[Task[A]]]): Def.Initialize[Task[List[A]]] =
    tasks.toList match {
      case Nil => Def.task { Nil }
      case x :: xs => Def.taskDyn { flattenTasks(xs) map (x.value :: _) }
  }

  private def undefinedKeyError[A](key: AttributeKey[A]): A = {
    sys.error(s"Please declare a value for the `${key.label}` key. " +
      s"Description: '${key.description.getOrElse("A required key")}'"
    )
  }

  private def codegenResultPathsFromSummaryFile(file: File): Seq[File] = {
    val lines = scala.io.Source.fromFile(file).getLines
    lines.map(new File(_)).toList
  }

  private def runCodegen(localJarSources: Seq[Path], packagePrefix: String, outputDir: Path, summaryFile: Path, withInfra: Boolean): Seq[File] = {
    val mainArgs = codegenMainArgs(localJarSources, packagePrefix, outputDir, summaryFile, withInfra)

    println(s"Running yaga AWS codegen with args: ${mainArgs.mkString(" ")}")

    MavenArtifactsHelpers.runMavenArtifactMainWithArgs(
      "org.virtuslab", "yaga-codegen-aws_3", yagaVersion,
      "yaga.codegen.aws.runCodegen",
      mainArgs
    )

    codegenResultPathsFromSummaryFile(summaryFile.toFile)
  }

  private def codegenMainArgs(localJarSources: Seq[Path], packagePrefix: String, outputDir: Path, summaryFile: Path, withInfra: Boolean): Seq[String] = {
    val infraMainArgs =
      if (withInfra)
        Seq("--with-infra")
      else
        Seq.empty

    val sourcesOptions = localJarSources.flatMap(path => Seq("--local-jar", path.toString))

    val mainArgs = sourcesOptions ++ Seq(
      "--package-prefix", packagePrefix,
      "--output-dir", outputDir.toString,
      "--summary-file", summaryFile.toString,
    ) ++ infraMainArgs
    mainArgs
  }

  private def helperGenerator: Def.Initialize[Task[List[Seq[File]]]] = Def.taskDyn {
    val items = yagaAwsCodegenItems.value

    val subtasks: Seq[Def.Initialize[Task[Seq[File]]]] = items.map { case item =>
      val outputSubdirName = item match {
        case item: CodegenItem.FromMaven => item.outputSubdirName
        case item: CodegenItem.FromLocalJars => item.outputSubdirName
      }

      val codegenOutputDir = (Compile / sourceManaged).value / "yaga-aws-codegen" / outputSubdirName
      val summaryFile = Files.createTempFile("codegen-summary-", ".txt")
      val sources: Seq[Path] = item match {
        case item: CodegenItem.FromMaven =>
          val List(orgName, moduleName, version) = item.artifactCoordinates.split(":").toList
          val jarPath = MavenArtifactsHelpers.fatJarPathFromMavenCoordinates(orgName = orgName, moduleName = moduleName, version = version).toAbsolutePath
          Seq(jarPath)

        case item: CodegenItem.FromLocalJars =>
          item.absoluteJarsPaths
      }

      val packagePrefix = item.packagePrefix
      val withInfra = item.withInfra

      Def.task {
        runCodegen(localJarSources = sources, packagePrefix = packagePrefix, outputDir = codegenOutputDir.toPath, summaryFile = summaryFile, withInfra = withInfra)
      }
    }

    flattenTasks(subtasks)
  }
}