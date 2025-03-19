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


////////////////////////////////////////////////////////////
// Core
////////////////////////////////////////////////////////////

lazy val `core-model` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("core/model"))
  .jvmSettings(CoreSettings.modelJvmSettings)
  .jsSettings(CoreSettings.modelJsSettings)

lazy val `core-codegen` = project
  .in(file("core/codegen"))
  .settings(CoreSettings.codegenSettings)

lazy val `core-sbt` = project
  .in(file("core/sbt"))
  .settings(CoreSettings.sbtPluginSettings)

lazy val `core` = project
  .in(file("core"))
  .aggregate(`core-model`.jvm, `core-model`.js, `core-codegen`, `core-sbt`)


////////////////////////////////////////////////////////////
// AWS Lambda
////////////////////////////////////////////////////////////

lazy val `aws-lambda-sdk` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("extensions/aws-lambda/sdk"))
  .jvmSettings(AwsLambdaSettings.sdkJvmSettings)
  .jsSettings(AwsLambdaSettings.sdkJsSettings)
  .dependsOn(`core-model`)

lazy val `aws-lambda-besom` = project
  .in(file("extensions/aws-lambda/besom"))
  .settings(AwsLambdaSettings.besomSettings)
  .dependsOn(`aws-lambda-sdk`.jvm) // Needs dependency only on the model part od the SDK - split modules?

lazy val `aws-lambda-codegen` = project
  .in(file("extensions/aws-lambda/codegen"))
  .settings(AwsLambdaSettings.codegenSettings)
  .dependsOn(`core-codegen`)

lazy val `aws-lambda-sbt` = project
  .in(file("extensions/aws-lambda/sbt"))
  .settings(AwsLambdaSettings.sbtPluginSettings)
  .dependsOn(`core-sbt`)

lazy val `aws-lambda` = project
  .in(file("extensions/aws-lambda"))
  .aggregate(`aws-lambda-sdk`.jvm, `aws-lambda-sdk`.js, `aws-lambda-besom`, `aws-lambda-codegen`, `aws-lambda-sbt`)
