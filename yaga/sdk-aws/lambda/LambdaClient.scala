package yaga.extensions.aws.lambda

import software.amazon.awssdk.services.lambda.{LambdaClient as UnshapedLambdaClient}
import software.amazon.awssdk.services.lambda.model.{InvokeRequest, InvocationType}
import yaga.extensions.aws.lambda.internal.{LambdaInputSerializer, LambdaOutputDeserializer}

class LambdaClient(
  val unshapedClient: UnshapedLambdaClient = UnshapedLambdaClient.builder().build()
):
  // TODO Wrap the result type O in some error monad?; Expose other elements of InvokeResponse?
  def invokeSyncUnsafe[I, O](function: LambdaHandle[I, O], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputSerializer: LambdaInputSerializer[I],
    outputDeserializer: LambdaOutputDeserializer[O]
  ): O =
    val inputBytes = inputSerializer.serialize(input)

    val invokeRequest = InvokeRequest.builder()
      .functionName(function.functionName)
      .payload(inputBytes)
      .invocationType(InvocationType.REQUEST_RESPONSE)
      .build()

    val invokeResponse = unshapedClient.invoke(invokeRequest)
    val responseStatus = invokeResponse.statusCode()
    assert(
      responseStatus == 200, // Expected for synchronous invocation: https://sdk.amazonaws.com/java/api/2.29.9/software/amazon/awssdk/services/lambda/model/InvokeResponse.html#statusCode()
      s"Synchronous lambda invocation failed (function name: ${function.functionName}, response status: ${responseStatus})"
    )

    val outputBytes = invokeResponse.payload()
    val result = outputDeserializer.deserialize(outputBytes)
    result

  def invokeAsyncUnsafe[I, O](function: LambdaHandle[I, ?], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputSerializer: LambdaInputSerializer[I]
  ): Unit =
    val inputBytes = inputSerializer.serialize(input)

    val invokeRequest = InvokeRequest.builder()
      .functionName(function.functionName)
      .payload(inputBytes)
      .invocationType(InvocationType.EVENT)
      .build()

    val invokeResponse = unshapedClient.invoke(invokeRequest)
    val responseStatus = invokeResponse.statusCode()
    assert(
      responseStatus == 202, // Expected for asynchronous invocation: https://sdk.amazonaws.com/java/api/2.29.9/software/amazon/awssdk/services/lambda/model/InvokeResponse.html#statusCode()
      s"Asynchronous lambda invocation failed (function name: ${function.functionName}, response status: ${responseStatus})"
    )
