import java.nio.file.Path

val scalaV = "3.3.4"

lazy val childLambdaA = project.in(file("child-lambda-a"))
  .awsJsLambda
  .settings(
    scalaVersion := scalaV,
    yagaAwsLambdaHandlerClassName := "com.virtuslab.child_lambda_a.ChildLambdaA"
  )

lazy val childLambdaB = project.in(file("child-lambda-b"))
  .awsJsLambda
  .settings(
    scalaVersion := scalaV,
    yagaAwsLambdaHandlerClassName := "com.virtuslab.child_lambda_b.ChildLambdaB"
  )

lazy val parentLambda = project.in(file("parent-lambda"))
  // .awsJsLambda
  .awsJvmLambda
  .withYagaDependencies(
    childLambdaA.awsLambdaModel(),
    childLambdaB.awsLambdaModel()
  )
  .settings(
    scalaVersion := scalaV,
    yagaAwsLambdaHandlerClassName := "com.virtuslab.parent_lambda.ParentLambda"
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
  .settings(
    libraryDependencies += yagaBesomAwsSdkDep
  )
