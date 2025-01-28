package com.virtuslab.parent_lambda

import besom.json.*
import yaga.extensions.aws.lambda.{LambdaClient, LambdaHandle, LambdaHandler}
import com.virtuslab.child_lambda_a
import com.virtuslab.child_lambda_b

case class Config(
  childLambdaA: LambdaHandle[child_lambda_a.Bar, Unit],
  childLambdaB: LambdaHandle[child_lambda_b.Bar, child_lambda_b.Baz]
) derives JsonFormat

case class Qux(
  str: String = "abcb"
) derives JsonFormat

class ParentLambda extends LambdaHandler[Config, Qux, String]:
  val lambdaClient = LambdaClient()
  println("Parent lambda initialized")

  override def handleInput(input: Qux) =
    // Async call to child-lambda-a
    val childLambdaAInput = child_lambda_a.Bar(child_lambda_a.Foo(str = input.str))
    lambdaClient.invokeAsyncUnsafe(config.childLambdaA, childLambdaAInput)

    // Sync call to child-lambda-b
    val childLambdaBInput = child_lambda_b.Bar(child_lambda_b.Foo(str = input.str))
    val baz = lambdaClient.invokeSyncUnsafe(config.childLambdaB, childLambdaBInput)
    println(s"Response from child-lambda-b: ${baz}")

    "Processing completed"
