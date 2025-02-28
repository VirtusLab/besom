sbtPlugin := true

organization := "org.virtuslab"
name := "sbt-yaga-aws-lambda"
version := "0.4.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.get-coursier" %% "coursier" % "2.1.20"
)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.18.2")