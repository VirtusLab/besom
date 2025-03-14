import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object CoreSettings {
  private val modelSharedSettings = Seq(
    name := "yaga-core-model",
    libraryDependencies ++= Seq(
      // "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core" % "2.33.2",
      // "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % "2.33.2" % "compile-internal"
    )
  )

  val modelJvmSettings = modelSharedSettings ++ Seq(
    libraryDependencies ++= Seq(
      "org.virtuslab" %% "besom-json" % "0.4.0-SNAPSHOT",
    )
  )

  val modelJsSettings = modelSharedSettings ++ Seq(
    libraryDependencies ++= Seq(
      "org.virtuslab" %% "besom-json_sjs1" % "0.4.0-SNAPSHOT",
    )
  )

  val codegenSettings = Seq(
    name := "yaga-core-codegen",
    libraryDependencies ++= Seq(
      "ch.epfl.scala" %% "tasty-query" % "1.5.0",
      "org.scalameta" % "scalameta_2.13" % "4.12.7",
      "com.lihaoyi" %% "os-lib" % "0.11.3",
      ("io.get-coursier" % "coursier" % "2.1.24") cross CrossVersion.for3Use2_13
    )
  )

  val sbtPluginSettings = Seq(
    name := "sbt-yaga-core",
    libraryDependencies ++= Seq(
      "io.get-coursier" %% "coursier" % "2.1.24"
    )
  )
}