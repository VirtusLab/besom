import besom.*
import besom.api.aws
import besom.api.aws.lambda.{Function, FunctionArgs}
import besom.json.*

import yaga.extensions.aws.lambda.ShapedFunction
import yaga.generated.lambdatest.child.{Bar, Baz} // TODO use packages with version?
import yaga.generated.lambdatest.parent.{ParentLambdaConfig, Qux}

@main def main = Pulumi.run {
  val basicFunctionRole = aws.iam.Role(
    name = "basicFunctionRole",
    aws.iam.RoleArgs(
      // TODO Add some helpers so that users don't have to use copy-pasted code
      assumeRolePolicy = json"""{
          "Version": "2012-10-17",
          "Statement": [{
              "Effect": "Allow",
              "Principal": {
                  "Service": "lambda.amazonaws.com"
              },
              "Action": "sts:AssumeRole"
          }]
      }""".map(_.prettyPrint),
      managedPolicyArns = List(aws.iam.enums.ManagedPolicy.AWSLambdaBasicExecutionRole.value)
    )
  )

  val childHandlerMeta = ShapedFunction.lambdaHandlerMetadataFromLocalJar[Unit, Bar, Baz](
    // TODO Path relative to the directory containing this file while a FileArchive(...) would treat this as relative to the directory containing Pulumi.yaml. Which semantics should we use?
    jarPath = "../../.out/lambdas/child-lambda.jar",
    handlerClassName = "lambdatest.child.ChildLambda"
  )

  val childLambdaArgs = FunctionArgs(
    name = "childLambda",
    runtime = "java21",
    role = basicFunctionRole.arn // TODO Could we somehow check IAM permissions at compile time? 
  )

  val childLambda = ShapedFunction(
    "childLambda",
    childHandlerMeta,
    config = (), // TODO don't require config if it's empty
    childLambdaArgs
  )

  val childLambdaArn = childLambda.unshapedFunction.arn

  val invokeOtherLambdaPolicy = aws.iam.Policy("invokeOtherLambdaPolicy", aws.iam.PolicyArgs(
    name = "invokeOtherLambdaPolicy",
    policy = json"""{
      "Version": "2012-10-17",
      "Statement": [
        {
            "Sid": "Statement1",
            "Effect": "Allow",
            "Action": [
                "lambda:InvokeFunction",
                "lambda:InvokeAsync"
            ],
            "Resource": ${childLambdaArn}
        }
      ]
    }""".map(_.prettyPrint)
  ))

  val invokeOtherLambdaRole = aws.iam.Role("invokeOtherLambdaRole", aws.iam.RoleArgs(
    assumeRolePolicy = json"""{
      "Version": "2012-10-17",
      "Statement": [{
          "Effect": "Allow",
          "Principal": {
              "Service": "lambda.amazonaws.com"
          },
          "Action": "sts:AssumeRole"
      }]
    }""".map(_.prettyPrint),
    managedPolicyArns = List(
      aws.iam.enums.ManagedPolicy.AWSLambdaBasicExecutionRole.value,
      invokeOtherLambdaPolicy.arn
    )
  ))

  val parentHandlerMeta = ShapedFunction.lambdaHandlerMetadataFromLocalJar[ParentLambdaConfig, Qux, Unit](
    jarPath = "../../.out/lambdas/parent-lambda.jar",
    handlerClassName = "lambdatest.parent.ParentLambda"
  )

  val parentLambdaArgs = FunctionArgs(
    name = "parentLambda",
    runtime = "java21",
    role = invokeOtherLambdaRole.arn,
    timeout = 30
  )

  val parentLambda = ShapedFunction(
    "parentLambda",
    parentHandlerMeta,
    config = childLambda.map { cl =>
      ParentLambdaConfig(
        childLambdaHandle = cl.functionHandle
      )
    },
    parentLambdaArgs
  )

  val parentLambdaArn = parentLambda.arn

  Stack()
    .exports(
      parentLambdaName = parentLambdaArn,
      childLambdaName = childLambdaArn
    )
}
