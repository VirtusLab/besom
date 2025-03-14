package yaga.extensions.aws.lambda

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.Thenable.Implicits.thenable2future

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import yaga.json.{JsonReader, JsonWriter}

/*
Using AWS SDK v3 for JavaScript available by default in nodejs22.x runtime
https://sdk.amazonaws.com/js/api/latest/index.html
*/

import LambdaClient.*

class LambdaClient private(
  val underlyingClient: AwsSdkLambdaClient
) extends LambdaClientApi:

  override def invokeWithResponse[I, O](function: LambdaHandle[I, O], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputWriter: JsonWriter[I],
    outputReader: JsonReader[O]
  ): Future[O] =
    val inputJson = inputWriter.write(input)

    val invokeCommandInput = js.Dynamic.literal(
      FunctionName = function.functionName,
      InvocationType = "RequestResponse",
      Payload = inputJson
    )
    val invokeCommand = new AwsSdkInvokeCommand(invokeCommandInput)
    val invokeResult = underlyingClient.send(invokeCommand)
    
    invokeResult.map { result =>
      val responseStatus = result.StatusCode
      assert(
        responseStatus == 200, // Expected for synchronous invocation: https://sdk.amazonaws.com/java/api/2.29.9/software/amazon/awssdk/services/lambda/model/InvokeResponse.html#statusCode()
        s"Request-response lambda invocation failed (function name: ${function.functionName}, response status: ${responseStatus})"
      )
      val payloadJson = js.Dynamic.global.Buffer.from(result.Payload).toString
      outputReader.read(payloadJson)
    }

  def triggerEvent[I, O](function: LambdaHandle[I, ?], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputWriter: JsonWriter[I]
  ): Future[Unit] =
    val inputJson = inputWriter.write(input)
    val invokeCommandInput = js.Dynamic.literal(
      FunctionName = function.functionName,
      InvocationType = "Event",
      Payload = inputJson
    )
    val invokeCommand = new AwsSdkInvokeCommand(invokeCommandInput)
    val invokeResult = underlyingClient.send(invokeCommand)
    invokeResult.map { result =>
      val responseStatus = result.StatusCode
      assert(
        responseStatus == 202, // Expected for synchronous invocation: https://sdk.amazonaws.com/java/api/2.29.9/software/amazon/awssdk/services/lambda/model/InvokeResponse.html#statusCode()
        s"Event lambda invocation failed (function name: ${function.functionName}, response status: ${responseStatus})"
      )
    }

object LambdaClient extends LambdaClientCompanion:
  override type UnderlyingLambdaClient = AwsSdkLambdaClient

  override def apply(
    underlyingClient: UnderlyingLambdaClient = defaultUnderlyingClient
  ): LambdaClientApi =
    new LambdaClient(underlyingClient)

  override def defaultUnderlyingClient: UnderlyingLambdaClient =
    val clientConfig = js.Dynamic.literal()
    new AwsSdkLambdaClient(clientConfig)

  @js.native
  @JSImport("@aws-sdk/client-lambda", "LambdaClient")
  private [lambda] class AwsSdkLambdaClient(config: js.Any) extends js.Object:
    def send(command: AwsSdkInvokeCommand): js.Promise[AwsSdkInvokeCommandResult] = js.native

  @js.native
  @JSImport("@aws-sdk/client-lambda", "InvokeCommand")
  private [lambda] class AwsSdkInvokeCommand(commandInput: js.Any) extends js.Object

  @js.native
  private [lambda] trait AwsSdkInvokeCommandResult extends js.Object:
    def StatusCode: Int = js.native
    def Payload: js.Any = js.native
