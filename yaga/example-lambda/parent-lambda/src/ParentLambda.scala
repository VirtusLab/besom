//> using scala 3.3.3
//> using dep ch.qos.logback:logback-classic:1.5.11
//> using dep org.virtuslab::yaga-aws:0.4.0-SNAPSHOT
//> using resourceDir ../resources

package lambdatest.parent

import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.Context

import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.core.SdkBytes

import besom.json.*

import yaga.extensions.aws.lambda.{LambdaHandler, LambdaShape, ShapedLambdaClient, ShapedFunctionHandle}

import yaga.generated.lambdatest.child.{Foo, Bar, Baz}

case class ParentLambdaConfig(
  childLambdaHandle: ShapedFunctionHandle[Bar, Baz]
) derives JsonFormat

case class Qux(
  str: String = "abcb"
) derives JsonFormat

class ParentLambda extends LambdaHandler[ParentLambdaConfig, Qux, Unit] derives LambdaShape:
  val shapedLambdaClient = ShapedLambdaClient()
  println("Parent lambda initialized")

  override def handleInput(input: Qux) =
    val childInput = Bar(Foo(str = input.str))
    val lambdaHandle = config.childLambdaHandle
    println(s"Invoking child lambda with input: $childInput")
    val output = shapedLambdaClient.invokeSync(lambdaHandle, childInput)
    println(s"Child lambda output: $output")
