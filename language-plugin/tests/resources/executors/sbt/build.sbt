lazy val root = project.in(file(".")).settings(
  scalaVersion := "3.2.2",
  libraryDependencies += "org.virtuslab" %% "besom-custom-resource-plugin" % "0.0.1-SNAPSHOT"
)
