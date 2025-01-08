import java.nio.file.Path

val scalaV = "3.3.3"

lazy val child = project.in(file("child-lambda"))
  .awsLambda
  .settings(
    scalaVersion := scalaV
  )

lazy val parent = project.in(file("parent-lambda"))
  .awsLambda
  .withYagaDependencies(
    child.awsLambdaModel()
  )
  .settings(
    scalaVersion := scalaV
  )


lazy val infra = project.in(file("infra"))
  .withYagaDependencies(
    child.awsLambdaInfra(packagePrefix = Some("childlambda")),
    parent.awsLambdaInfra(packagePrefix = Some("parentlambda"))
  )
  .settings(
    scalaVersion := scalaV
  )
