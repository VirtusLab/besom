sbtPlugin := true

organization := "org.virtuslab"
name := "sbt-yaga-aws-lambda"
version := "0.4.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.get-coursier" %% "coursier" % "2.1.20"
)

addSbtPlugin(
  "com.eed3si9n" % "sbt-assembly" % "2.3.0"
)