scalaVersion := "3.3.3"

libraryDependencies += ("org.virtuslab" %% "yaga-besom-aws" % "0.4.0-SNAPSHOT")

enablePlugins(YagaAwsCodeGenPlugin)

import yaga.sbt.aws.CodegenItem

yagaAwsCodegenItems := Seq(
  CodegenItem.InfraFromMaven(
    artifactCoordinates = "org.virtuslab:child-lambda_3:0.0.1-SNAPSHOT",
    outputSubdirName = "child-lambda",
    packagePrefix = "childlambda"
  )
)