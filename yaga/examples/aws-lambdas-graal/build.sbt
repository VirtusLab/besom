import java.nio.file.Path

val scalaV = "3.3.4"

lazy val childLambdaA = project.in(file("child-lambda-a"))
  .awsGraalLambda()
  .settings(
    scalaVersion := scalaV
  )

lazy val childLambdaB = project.in(file("child-lambda-b"))
  .awsGraalLambda()
  .settings(
    scalaVersion := scalaV
  )

lazy val parentLambda = project.in(file("parent-lambda"))
  .awsGraalLambda()
  .withYagaDependencies(
    childLambdaA.awsLambdaModel(),
    childLambdaB.awsLambdaModel()
  )
  .settings(
    scalaVersion := scalaV
  )


lazy val infra = project.in(file("infra"))
  .withYagaDependencies(
    childLambdaA.awsLambdaInfra(packagePrefix = "child_a"),
    childLambdaB.awsLambdaInfra(packagePrefix = "child_b"),
    parentLambda.awsLambdaInfra(packagePrefix = "parent")
  )
  .settings(
    scalaVersion := scalaV
  )
