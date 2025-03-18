package yaga.sbt.aws

import scala.language.implicitConversions

import _root_.sbt._
import _root_.sbt.Keys._
import _root_.sbt.AutoPlugin
import _root_.sbt.nio.{ file => _, * }
import java.nio.file.{Files, Path, Paths}
import sbtassembly.AssemblyPlugin.*
import sbtassembly.AssemblyPlugin.autoImport.*
import sbtassembly.MergeStrategy
import yaga.sbt.{MavenArtifactsHelpers, YagaDependency}
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import yaga.sbt.YagaPlugin.autoImport.yagaGeneratedSources
import com.typesafe.sbt.packager.graalvmnativeimage.GraalVMNativeImagePlugin
import GraalVMNativeImagePlugin.autoImport.{GraalVMNativeImage, graalVMNativeImageOptions, graalVMNativeImageGraalVersion/* , graalVMNativeImageCommand */}

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
    val yagaAwsDeployableLambdaArtifact = taskKey[Path]("Deployable AWS lambda artifact")
    val yagaAwsRunCodegen: TaskKey[Seq[File]] = taskKey[Seq[File]]("Generate code for yaga AWS")

    val yagaAwsLambdaGraalLambdaArchivePath = settingKey[File]("Path to the graal lambda archive")

    val yagaAwsLambdaGraaledImage = taskKey[Path]("Path to the graaled image")
    val yagaAwsLambdaGraalBootstrapFile = taskKey[Path]("Bootstrap file for graaled lambda")

    private def makeGraaledImageRemotely(
      remoteBuilderUser: String,
      remoteBuilderIp: String,
      fatJarLocalPath: Path,
      fatJarRemotePath: Path,
      graalVmNativeImageCommand: String,
      nativeImageExtraSettings: Seq[String],
      resultArtifactRemotePath: Path,
      resultArtifactLocalPath: Path
    ): Unit = {
      import scala.sys.process._

      val copyToRemoteCommand = Seq(
        "scp",
        fatJarLocalPath.toString,
        s"$remoteBuilderUser@$remoteBuilderIp:$fatJarRemotePath"
      )

      copyToRemoteCommand.!!

      val buildGraaledImageRemoteCommandParts = Seq(
        graalVmNativeImageCommand,
        "-jar", fatJarRemotePath.toString,
        "-o", resultArtifactRemotePath.toString,
        ) ++ nativeImageExtraSettings

      val buildGraaledImageCommand = Seq(
        "ssh",
        s"$remoteBuilderUser@$remoteBuilderIp",
        "-t",
        buildGraaledImageRemoteCommandParts.mkString(" ")
      )

      buildGraaledImageCommand.!!

      resultArtifactLocalPath.toFile.getParentFile().mkdirs()

      val copyFromRemoteCommand = Seq(
        "scp",
        s"$remoteBuilderUser@$remoteBuilderIp:$resultArtifactRemotePath",
        resultArtifactLocalPath.toString
      )

      copyFromRemoteCommand.!!

      // TODO clean up on remote
    }

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

      def awsGraalLambda() = {
        project
          .enablePlugins(GraalVMNativeImagePlugin)
          .settings(
            libraryDependencies ++= Seq(
              "org.virtuslab" %% "yaga-aws-lambda-sdk" % yagaAwsVersion,
              "com.amazonaws" % "aws-lambda-java-runtime-interface-client" % "2.4.2" // versions > 2.4.2 <= 2.6.0 are buggy and don't work with GraalVM
            ),
            Compile / mainClass := Some("com.amazonaws.services.lambda.runtime.api.client.AWSLambda"),
            graalVMNativeImageOptions ++= Seq(
              "--verbose",
              "-H:+ReportExceptionStackTraces",
              "--no-fallback",
              "--initialize-at-build-time=org.slf4j",
              "--enable-url-protocols=http",
              "'-H:IncludeResources=jni/.*'", // Asterisk had to be escaped - TODO?
              "--add-opens=java.base/java.util=ALL-UNNAMED"
            ),
            graalVMNativeImageGraalVersion := Some("ol8-java11-22.3.3"),

            yagaAwsLambdaRuntime := "graal",

            yagaAwsLambdaGraalBootstrapFile := {
              val fileContent = """#!/usr/bin/env bash
                                  |
                                  |./lambda $_HANDLER""".stripMargin

              val filePath = (Compile / target).value / "yaga" / "graal" / "bootstrap"
              IO.write(filePath, fileContent)
              filePath.setExecutable(true, false) // executable for everyone
              filePath.toPath
            },

            yagaAwsLambdaGraaledImage := Def.taskDyn {
              val remoteBuilderUser = sys.env.getOrElse("YAGA_AWS_LAMBDA_GRAAL_REMOTE_USER", throw new Exception("YAGA_AWS_LAMBDA_GRAAL_REMOTE_USER is not set"))
              val remoteBuilderIp = sys.env.getOrElse("YAGA_AWS_LAMBDA_GRAAL_REMOTE_IP", throw new Exception("YAGA_AWS_LAMBDA_GRAAL_REMOTE_IP is not set"))
              val fatJarLocalPath = yagaAwsLambdaAssembly.value
              val projectName = (Compile / name).value
              val tmpName = s"lambda-${projectName}" // TODO Find better way to assure no race conditions when building in parallel
              val fatJarRemotePath = Paths.get(s"/tmp/${tmpName}.jar")
              val resultArtifactRemotePath = Paths.get(s"/tmp/${tmpName}")
              val resultArtifactLocalPath = (target.value / "yaga" / "graal" / "lambda").toPath
              val graalVmNativeImageCommand = "native-image" //GraalVMNativeImage / graalVMNativeImageCommand
              val nativeImageExtraSettings = graalVMNativeImageOptions.value

              lazy val dependenciesChanged = yagaAwsLambdaAssembly.outputFileChanges.hasChanges

              if (Files.exists(resultArtifactLocalPath) && !dependenciesChanged) {
                Def.task {
                  resultArtifactLocalPath
                }
              } else {
                Def.task {
                  makeGraaledImageRemotely(
                    remoteBuilderUser = remoteBuilderUser,
                    remoteBuilderIp = remoteBuilderIp,
                    fatJarLocalPath = fatJarLocalPath,
                    fatJarRemotePath = fatJarRemotePath,
                    graalVmNativeImageCommand = graalVmNativeImageCommand,
                    nativeImageExtraSettings = nativeImageExtraSettings,
                    resultArtifactRemotePath = resultArtifactRemotePath,
                    resultArtifactLocalPath = resultArtifactLocalPath
                  )

                  resultArtifactLocalPath
                }
              }
            }.value,

            yagaAwsLambdaGraalLambdaArchivePath := target.value / "yaga-graal" / "function.zip",

            // TODO Add caching
            yagaAwsDeployableLambdaArtifact := {
              val zipFile = yagaAwsLambdaGraalLambdaArchivePath.value

              val zipInputs = Seq(
                yagaAwsLambdaGraaledImage.value.toFile -> "lambda",
                yagaAwsLambdaGraalBootstrapFile.value.toFile -> "bootstrap"
              )
              sbt.io.IO.zip(zipInputs, zipFile, time = None)
              zipFile.toPath
            }
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