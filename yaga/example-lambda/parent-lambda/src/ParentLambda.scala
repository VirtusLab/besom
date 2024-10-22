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

import yaga.extensions.aws.lambda.{EnvWriter, EnvReader, ShapedLambdaClient, ShapedRequestHandler, ShapedFunctionHandle}

import yaga.generated.lambdatest.child.{Foo, Bar, Baz}

case class ParentLambdaConfig(
  childLambdaHandle: ShapedFunctionHandle[Bar, Baz]
) derives JsonFormat

@yaga.extensions.aws.lambda.annotations.ConfigSchema("""Struct(List((childLambdaHandle,Struct(ArraySeq((functionName,String), (inputSchema,String), (outputSchema,String))))))""")
@yaga.extensions.aws.lambda.annotations.InputSchema("""Struct(ArraySeq())""")
@yaga.extensions.aws.lambda.annotations.OutputSchema("""Struct(ArraySeq())""")
class ParentLambda extends ShapedRequestHandler[ParentLambdaConfig, Unit, Unit]:
  val lambdaClient = LambdaClient.builder().build()
  val shapedLambdaClient = ShapedLambdaClient()
  println("Parent lambda initialized")

  override def handleInput(input: Unit, context: Context): Unit =
    invokeChildLambda()

  def invokeChildLambda() =
    val input = Bar(Foo(str = "abcb"))
    val lambdaHandle = config.childLambdaHandle
    println(s"Invoking child lambda with input: $input")
    val output = shapedLambdaClient.invokeSync(lambdaHandle, input)
    println(s"Child lambda output: $output")

@main def runLambda(lambdaName: String) =
  ParentLambda().invokeChildLambda()
