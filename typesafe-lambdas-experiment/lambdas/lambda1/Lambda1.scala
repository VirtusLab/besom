//> using scala 3.5.0
//> using dep com.amazonaws:aws-lambda-java-core:1.2.3
//> using dep software.amazon.awssdk:lambda:2.28.16
//> using dep ch.qos.logback:logback-classic:1.5.8
//> using resourceDir resources

package lambdatest

import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.Context
import scala.beans.BeanProperty

import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.core.SdkBytes

class NoInput()

class Lambda1 extends RequestHandler[NoInput, Unit]:
  val lambdaClient = LambdaClient.builder().build()

  override def handleRequest(event: NoInput, context: Context): Unit =
    doStuff()

  def doStuff(): Unit =
    invokeLambda2()

  def invokeLambda2(): Unit =
    val lambdaName = "lambda2"
    val payload = """{"str": "abca"}"""
    val payloadBytes = SdkBytes.fromUtf8String(payload);
    val invokeRequest = InvokeRequest.builder()
      .functionName(lambdaName)
      .payload(payloadBytes)
      .build()

    println(s"Invoking lambda2 with payload: $payload")
    val invokeResult = lambdaClient.invoke(invokeRequest)
    val resultPayload = invokeResult.payload().asUtf8String()
    println(s"Invocation result: $resultPayload")

@main def runLambda(lambdaName: String) =
  Lambda1().doStuff()