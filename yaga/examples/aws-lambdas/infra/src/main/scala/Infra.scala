import besom.*
import besom.api.aws
import besom.api.aws.lambda.FunctionArgs
import besom.json.*

import child_a.com.virtuslab.child_lambda_a.ChildLambdaA
import child_b.com.virtuslab.child_lambda_b.ChildLambdaB
import parent.com.virtuslab.parent_lambda.{ParentLambda, Config as ParentLambdaConfig}

@main def main = Pulumi.run {
  val lambdaAssumeRole = aws.iam.Role(
    name = "lambdaAssumeRole",
    aws.iam.RoleArgs(
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

  val childLambdaA = ChildLambdaA(
    "childLambdaA",
    FunctionArgs(
      role = lambdaAssumeRole.arn
    )
  )

  val childLambdaB = ChildLambdaB(
    "childLambdaB",
    FunctionArgs(
      role = lambdaAssumeRole.arn
    )
  )

  val lambdaInvokePolicy = aws.iam.Policy("lambdaInvokePolicy", aws.iam.PolicyArgs(
    name = "lambdaInvokePolicy",
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
            "Resource": [
              ${childLambdaA.arn},
              ${childLambdaB.arn}
            ]
        }
      ]
    }""".map(_.prettyPrint)
  ))

  val parentLambdaRole = aws.iam.Role("parentLambdaRole", aws.iam.RoleArgs(
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
      lambdaInvokePolicy.arn
    )
  ))

  val parentLambda = ParentLambda(
    "parentLambda",
    FunctionArgs(
      role = parentLambdaRole.arn,
      timeout = 30
    ),
    config =
      for
        childA <- childLambdaA
        childB <- childLambdaB
      yield
        ParentLambdaConfig(
          childLambdaA = childA.lambdaHandle,
          childLambdaB = childB.lambdaHandle
        )
  )

  Stack()
    .exports(
      child_lambda_a_arn = childLambdaA.arn,
      child_lambda_b_arn = childLambdaB.arn,
      parent_lambda_arn = parentLambda.arn
    )
}
