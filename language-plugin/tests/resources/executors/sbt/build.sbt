lazy val root = project
  .in(file("."))
  .settings(
    scalaVersion := "3.3.0",
    libraryDependencies ++= Seq(
      "org.virtuslab" %% "besom-fake-standard-resource" % "1.2.3-TEST",
      "org.virtuslab" %% "besom-fake-external-resource" % "2.3.4-TEST"
    )
  )
