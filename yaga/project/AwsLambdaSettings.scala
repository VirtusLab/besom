import sbt._
import sbt.Keys._

object AwsLambdaSettings {
  val sdkSharedSettings = Seq(
    name := "yaga-aws-lambda-sdk"
  )

  val sdkJvmSettings = sdkSharedSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",
      // Adding a version of software.amazon.awssdk:lambda newer than 2.26.9 (at least until 2.28.26) to the classpath magically causes besom.internal.ResourceDecoder.resolve to crash at runtime for besom 0.4.0
      "software.amazon.awssdk" % "lambda" % "2.26.9",
    )
  )

  val sdkJsSettings = sdkSharedSettings ++ Seq(

  )

  val besomSettings = Seq(
    name := "yaga-aws-lambda-besom",
    libraryDependencies ++= Seq(
      "org.virtuslab" %% "besom-core" % "0.4.0-SNAPSHOT",
      "org.virtuslab" %% "besom-aws" % "6.70.0-mini-core.0.4-SNAPSHOT",
      classGraphDep,
    )
  )

  val codegenSettings = Seq(
    name := "yaga-aws-lambda-codegen",
    libraryDependencies ++= Seq(
      classGraphDep,
    )
  )

  val sbtPluginSettings = Seq(
    name := "sbt-yaga-aws-lambda",
    libraryDependencies ++= Seq(

    ),
    addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.0"),
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.18.2")
  )

  val classGraphDep = "io.github.classgraph" % "classgraph" % "4.8.179"
}