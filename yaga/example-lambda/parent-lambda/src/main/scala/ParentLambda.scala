package lambdatest.parent

import besom.json.*
import yaga.extensions.aws.lambda.{LambdaHandler, LambdaClient, LambdaHandle}
import yaga.generated.childlambda.lambdatest.child.{Foo, Bar, Baz}

case class Config(
  childLambdaHandle: LambdaHandle[Bar, Baz]
) derives JsonFormat

case class Qux(
  str: String = "abcb"
) derives JsonFormat

class ParentLambda extends LambdaHandler[Config, Qux, Unit]:
  val lambdaClient = LambdaClient()
  println("Parent lambda initialized")

  override def handleInput(input: Qux) =
    val childLambdaHandle = config.childLambdaHandle

    val childAsyncInput = Bar(Foo(str = input.str))
    println(s"Invoking child lambda asynchronously with input: $childAsyncInput")
    lambdaClient.invokeAsyncUnsafe(childLambdaHandle, childAsyncInput)
    println("Triggered child lambda")
    
    val childSyncInput = Bar(Foo(str = input.str.toUpperCase))
    println(s"Invoking child lambda synchronously with input: $childSyncInput")
    val output = lambdaClient.invokeSyncUnsafe(childLambdaHandle, childSyncInput)
    println(s"Child lambda output: $output")
