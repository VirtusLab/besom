sbtPlugin := true

organization := "org.virtuslab"
name := "yaga-sbt-core"
version := "0.4.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.get-coursier" %% "coursier" % "2.1.20"
)
