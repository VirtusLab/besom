import sbt._
import sbt.Keys._

object CommonSettings {
  val scala3LTSVersion = "3.3.5"
  val scala3NextVersion = "3.6.4"

  val sdkModuleSettings = Seq(
    scalaVersion := scala3LTSVersion,
  )

  val besomModuleSettings = Seq(
    scalaVersion := scala3LTSVersion,
  )

  val compilerPluginModuleSettings = Seq(
    scalaVersion := scala3LTSVersion,
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scala3LTSVersion
    )
  )

  val codegenModuleSettings = Seq(
    scalaVersion := scala3NextVersion,
  )

  val sbtPluginModuleSettings = Seq(
    sbtPlugin := true
  )
}