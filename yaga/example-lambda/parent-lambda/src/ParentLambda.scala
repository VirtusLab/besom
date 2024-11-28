//> using scala 3.3.3
//> using dep ch.qos.logback:logback-classic:1.5.12
//> using dep org.virtuslab::yaga-aws:0.4.0-SNAPSHOT
//> using resourceDir ../resources

package lambdatest.parent

import besom.json.*
import yaga.extensions.aws.lambda.{LambdaHandler, LambdaShape, LambdaClient, LambdaHandle}
import yaga.generated.childlambda.lambdatest.child.{Foo, Bar, Baz}

case class Config(
  childLambdaHandle: LambdaHandle[Bar, Baz]
) derives JsonFormat

case class Qux(
  str: String = "abcb"
) derives JsonFormat

class ParentLambda extends LambdaHandler[Config, Qux, Unit] derives LambdaShape:
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
