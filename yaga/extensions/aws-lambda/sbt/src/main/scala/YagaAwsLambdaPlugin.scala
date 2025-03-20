package yaga.sbt.aws

import scala.language.implicitConversions

import _root_.sbt._
import _root_.sbt.Keys._
import _root_.sbt.AutoPlugin
import _root_.sbt.nio.{ file => _, * }
import java.nio.file.{Files, Path}
import sbtassembly.AssemblyPlugin.*
import sbtassembly.AssemblyPlugin.autoImport.*
import sbtassembly.MergeStrategy
import yaga.sbt.{MavenArtifactsHelpers, YagaDependency}
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import yaga.sbt.YagaPlugin.autoImport.yagaGeneratedSources

object YagaAwsLambdaPlugin extends AutoPlugin {
  val yagaAwsVersion = "0.4.0-SNAPSHOT"
  val yagaBesomAwsSdkDep = "org.virtuslab" %% "yaga-aws-lambda-besom" % yagaAwsVersion

  val jsoniterVersion = "2.33.2"
  val jsoniterMacrosDep = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.33.2" % "compile-internal"

  override def requires = sbtassembly.AssemblyPlugin && yaga.sbt.YagaPlugin
  override def trigger = allRequirements

  object autoImport {
    // TODO Hide keys from users if they don't have to be set manually

    val yagaAwsLambdaRuntime = settingKey[String]("Yaga AWS Lambda runtime") // TODO make it more typesafe
    val yagaAwsLambdaHandlerClassName = settingKey[String]("Fully qualified name (with package) of yaga AWS Lambda handler class name")
    val yagaAwsLambdaAssembly = taskKey[Path]("Assembled AWS lambda jar")
    //val yagaAwsDeployableLambdaArtifactPath = settingKey[File]("Path to the lambda archive")
    val yagaAwsDeployableLambdaArtifact = taskKey[Path]("Deployable AWS lambda artifact")
    val yagaAwsRunCodegen: TaskKey[Seq[File]] = taskKey[Seq[File]]("Generate code for yaga AWS")

    implicit class ProjectYagaDependencyOps(project: Project) {
      def awsJvmLambda() = {
        project.settings(
          libraryDependencies ++= Seq(
            "org.virtuslab" %% "yaga-aws-lambda-sdk" % yagaAwsVersion,
            jsoniterMacrosDep
          ),
          yagaAwsLambdaRuntime := "java21",
          yagaAwsDeployableLambdaArtifact := yagaAwsLambdaAssembly.value,
        )
      }

      def awsJsLambda(
        handlerClass: String
      ) = {
        project
          .enablePlugins(ScalaJSPlugin)
          .settings(
            libraryDependencies ++= Seq(
              "org.virtuslab" %% "yaga-aws-lambda-sdk_sjs1" % yagaAwsVersion, // TODO use %%%
              jsoniterMacrosDep
            ),
            yagaAwsLambdaRuntime := "nodejs22.x",
            yagaAwsLambdaHandlerClassName := handlerClass,
            scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
            yagaGeneratedSources ++= {
              val handlerClassName = (project / yagaAwsLambdaHandlerClassName).value
              val file = (Compile / sourceManaged).value / "yaga-aws-js" / "HandlerProxy.scala"
              IO.write(file, jsProxyHandlerCode(handlerClassName))
              Seq(file)
            },
            yagaAwsDeployableLambdaArtifact := {
              val fullLinkOutputDir = (Compile / fullLinkJSOutput).value
              val fullLinkMainFile = fullLinkOutputDir / "main.js"
              val zipFile = (Compile / target).value / "yaga" / "lambda-aws" / "lambda.zip"
              val zipInputs = Seq(
                fullLinkMainFile -> "index.js"
              )
              sbt.io.IO.zip(zipInputs, zipFile, time = None)
              zipFile.toPath
            },
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

  def jsProxyHandlerCode(handlerClassName: String): String = {
    val handlerInstance = "new com.virtuslab.child_lambda_a.ChildLambdaA"
    s"""
       |import scala.scalajs.js
       |import scala.scalajs.js.annotation.JSExportTopLevel
       |import scala.scalajs.js.JSConverters._
       |import scala.concurrent.Future
       |import scala.concurrent.ExecutionContext.Implicits.global
       |import yaga.extensions.aws.lambda.LambdaContext
       |
       |@JSExportTopLevel("handlerInstance")
       |val handlerInstance = new $handlerClassName
       |
       |@JSExportTopLevel("handler")
       |def handler(event: js.Any, context: LambdaContext.UnderlyingContext): Any =
       |  handlerInstance.handleRequest(event, context)
       |""".stripMargin
  }
}