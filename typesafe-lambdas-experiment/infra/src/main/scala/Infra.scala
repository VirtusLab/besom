import besom.*
import besom.api.aws
import besom.api.aws.lambda.{Function, FunctionArgs}
import besom.json.*


@main def main = Pulumi.run {
  val basicFunctionRole = aws.iam.Role(
    name = "basicFunctionRole",
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

  val lambda1Code = Archive.FileArchive("../.out/lambdas/lambda1.jar")
  val lambda2Code = Archive.FileArchive("../.out/lambdas/lambda2.jar")

  val lambda2 = Function("lambda2", FunctionArgs(
    name = "lambda2",
    code = lambda2Code,
    handler = "lambdatest.Lambda2",
    runtime = "java21",
    role = basicFunctionRole.arn
  ))

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
            "Resource": ${lambda2.arn}
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

  val lambda1 = Function("lambda1", FunctionArgs(
    name = "lambda1",
    code = lambda1Code,
    handler = "lambdatest.Lambda1",
    runtime = "java21",
    role = invokeOtherLambdaRole.arn,
    timeout = 30
  ))


  Stack()
    .exports(
      lambda1Name = lambda1.arn,
      lambda2Name = lambda2.arn
    )
}
