import java.nio.file.Path

ThisBuild / scalaVersion := "3.3.5"

lazy val childLambdaA = project.in(file("child-lambda-a"))
  .awsJsLambda(
    handlerClass = "com.virtuslab.child_lambda_a.ChildLambdaA"
  )

lazy val childLambdaB = project.in(file("child-lambda-b"))
  .awsJsLambda(
    handlerClass = "com.virtuslab.child_lambda_b.ChildLambdaB"
  )

lazy val parentLambda = project.in(file("parent-lambda"))
  .awsJsLambda(handlerClass = "com.virtuslab.parent_lambda.ParentLambda")
  .withYagaDependencies(
    childLambdaA.awsLambdaModel(),
    childLambdaB.awsLambdaModel()
  )

lazy val infra = project.in(file("infra"))
  .withYagaDependencies(
    childLambdaA.awsLambdaInfra(packagePrefix = "child_a"),
    childLambdaB.awsLambdaInfra(packagePrefix = "child_b"),
    parentLambda.awsLambdaInfra(packagePrefix = "parent")
  )
