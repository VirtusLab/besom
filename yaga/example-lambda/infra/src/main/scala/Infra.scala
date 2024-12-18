import besom.*
import besom.api.aws
import besom.api.aws.lambda.FunctionArgs
import besom.json.*

import childlambda.{Lambda as ChildLambda} // TODO use packages with version?
import parentlambda.{Lambda as ParentLambda}
import parentlambda.lambdatest.parent.{Config as ParentLambdaConfig}

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

  val childLambda = ChildLambda(
    "childLambda",
    FunctionArgs(
      role = basicFunctionRole.arn
    )
  )

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
            "Resource": ${childLambda.arn}
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

  val parentLambda = ParentLambda(
    "parentLambda",
    FunctionArgs(
      role = invokeOtherLambdaRole.arn,
      timeout = 30
    ),
    config = childLambda.map { child =>
      ParentLambdaConfig(
        childLambdaHandle = child.lambdaHandle
      )
    }
  )

  Stack()
    .exports(
      parentLambdaName = parentLambda.arn,
      childLambdaName = childLambda.arn
    )
}
