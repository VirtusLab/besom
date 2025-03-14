package yaga.extensions.aws.lambda

import software.amazon.awssdk.services.lambda.{LambdaClient as UnshapedLambdaClient} // TODO replace with LambdaAsyncClient?
import software.amazon.awssdk.services.lambda.model.{InvokeRequest, InvocationType}
import software.amazon.awssdk.core.SdkBytes
import yaga.json.{JsonReader, JsonWriter}

import scala.jdk.FutureConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LambdaClient private(
  val underlyingClient: UnshapedLambdaClient
) extends LambdaClientApi:

  override def invokeWithResponse[I, O](function: LambdaHandle[I, O], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputWriter: JsonWriter[I],
    outputReader: JsonReader[O]
  ): Future[O] =
    val inputJson = inputWriter.write(input)

    val invokeRequest = InvokeRequest.builder()
      .functionName(function.functionName)
      .payload(SdkBytes.fromUtf8String(inputJson))
      .invocationType(InvocationType.REQUEST_RESPONSE)
      .build()

    val invokeResponse = Future { underlyingClient.invoke(invokeRequest) }

    invokeResponse.map { response =>
      val responseStatus = response.statusCode()
      assert(
        responseStatus == 200, // Expected for synchronous invocation: https://sdk.amazonaws.com/java/api/2.29.9/software/amazon/awssdk/services/lambda/model/InvokeResponse.html#statusCode()
        s"Synchronous lambda invocation failed (function name: ${function.functionName}, response status: ${responseStatus})"
      )
      val outputBytes = response.payload()
      outputReader.read(outputBytes.asUtf8String())
    }

  def triggerEvent[I, O](function: LambdaHandle[I, ?], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputWriter: JsonWriter[I]
  ): Future[Unit] =
    val inputJson = inputWriter.write(input)

    val invokeRequest = InvokeRequest.builder()
      .functionName(function.functionName)
      .payload(SdkBytes.fromUtf8String(inputJson))
      .invocationType(InvocationType.EVENT)
      .build()

    val invokeResponse = Future { underlyingClient.invoke(invokeRequest) }

    invokeResponse.map { response =>
      val responseStatus = response.statusCode()
      assert(
        responseStatus == 202, // Expected for asynchronous invocation: https://sdk.amazonaws.com/java/api/2.29.9/software/amazon/awssdk/services/lambda/model/InvokeResponse.html#statusCode()
        s"Asynchronous lambda invocation failed (function name: ${function.functionName}, response status: ${responseStatus})"
      )
    }

object LambdaClient extends LambdaClientCompanion:
  override type UnderlyingLambdaClient = UnshapedLambdaClient

  override def apply(
    underlyingClient: UnderlyingLambdaClient
  ): LambdaClientApi =
    new LambdaClient(underlyingClient)

  override def defaultUnderlyingClient: UnderlyingLambdaClient =
    UnshapedLambdaClient.builder().build()
