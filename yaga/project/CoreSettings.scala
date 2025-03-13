import sbt._
import sbt.Keys._

object CoreSettings {
  val modelSettings = Seq(
    name := "yaga-core-model",
    libraryDependencies ++= Seq(
      "org.virtuslab" %% "besom-json" % "0.4.0-SNAPSHOT",

      // "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.33.2",
      // "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.33.2" % "compile-internal"
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