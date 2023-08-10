lazy val root = project
  .in(file("."))
  .settings(
    scalaVersion := "3.3.0",
    libraryDependencies += "org.virtuslab" %% "besom-custom-resource-plugin" % "0.0.1-SNAPSHOT"
  )
