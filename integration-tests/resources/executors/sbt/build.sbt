lazy val root = project
  .in(file("."))
  .settings(
    scalaVersion := "3.3.1",
    scalacOptions ++= Seq("-java-output-version", "11"),
    javacOptions in (Compile, compile) ++= Seq("-source", "11", "-target", "11"),
    libraryDependencies ++= Seq(
      "org.virtuslab" %% "besom-core" % "0.2.2",
      "org.virtuslab" %% "besom-fake-standard-resource" % "1.2.3-TEST",
      "org.virtuslab" %% "besom-fake-external-resource" % "2.3.4-TEST"
    )
  )
