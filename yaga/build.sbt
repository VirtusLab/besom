////////////////////////////////////////////////////////////
// Root
////////////////////////////////////////////////////////////

lazy val root = project
  .in(file("."))
  .settings(
    name := "yaga",
    publish / skip := true
  )
  .aggregate(`core`, `aws-lambda`)


////////////////////////////////////////////////////////////
// Commons
////////////////////////////////////////////////////////////

ThisBuild / organization := "org.virtuslab"
ThisBuild / version := "0.4.0-SNAPSHOT"
ThisBuild / developers := List(
  Developer(id = "lbialy", name = "Łukasz Biały", email = "lbialy@virtuslab.com", url = url("https://github.com/lbialy")),
  Developer(id = "prolativ", name = "Michał Pałka", email = "mpalka@virtuslab.com", url = url("https://github.com/prolativ"))
)


val scala3LTS = "3.3.5"
val scala3Next = "3.6.4"

val sdkModuleSettings = Seq(
  scalaVersion := scala3LTS,
)

val besomModuleSettings = Seq(
  scalaVersion := scala3LTS,
)

val codegenModuleSettings = Seq(
  scalaVersion := scala3Next,
)

val sbtPluginModuleSettings = Seq(
  sbtPlugin := true
)


////////////////////////////////////////////////////////////
// Core
////////////////////////////////////////////////////////////

lazy val `core-model` = project
  .in(file("core/model"))
  .settings(sdkModuleSettings)
  .settings(CoreSettings.modelSettings)

lazy val `core-codegen` = project
  .in(file("core/codegen"))
  .settings(codegenModuleSettings)
  .settings(CoreSettings.codegenSettings)

lazy val `core-sbt` = project
  .in(file("core/sbt"))
  .settings(sbtPluginModuleSettings)
  .settings(CoreSettings.sbtPluginSettings)

lazy val `core` = project
  .in(file("core"))
  .aggregate(`core-model`, `core-codegen`, `core-sbt`)


////////////////////////////////////////////////////////////
// AWS Lambda
////////////////////////////////////////////////////////////

lazy val `aws-lambda-sdk` = project
  .in(file("extensions/aws-lambda/sdk"))
  .settings(sdkModuleSettings)
  .settings(AwsLambdaSettings.sdkSettings)
  .dependsOn(`core-model`)

lazy val `aws-lambda-besom` = project
  .in(file("extensions/aws-lambda/besom"))
  .settings(besomModuleSettings)
  .settings(AwsLambdaSettings.besomSettings)
  .dependsOn(`aws-lambda-sdk`)

lazy val `aws-lambda-codegen` = project
  .in(file("extensions/aws-lambda/codegen"))
  .settings(codegenModuleSettings)
  .settings(AwsLambdaSettings.codegenSettings)
  .dependsOn(`core-codegen`)

lazy val `aws-lambda-sbt` = project
  .in(file("extensions/aws-lambda/sbt"))
  .settings(sbtPluginModuleSettings)
  .settings(AwsLambdaSettings.sbtPluginSettings)
  .dependsOn(`core-sbt`)

lazy val `aws-lambda` = project
  .in(file("extensions/aws-lambda"))
  .aggregate(`aws-lambda-sdk`, `aws-lambda-besom`, `aws-lambda-codegen`, `aws-lambda-sbt`)
