package yaga.extensions.aws.lambda

import software.amazon.awssdk.services.lambda.{LambdaClient as UnshapedLambdaClient}
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import yaga.extensions.aws.lambda.internal.{LambdaInputSerializer, LambdaOutputDeserializer}

class LambdaClient(
  val unshapedClient: UnshapedLambdaClient = UnshapedLambdaClient.builder().build()
):
  def invokeSync[I, O](function: LambdaHandle[I, O], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputSerializer: LambdaInputSerializer[I],
    outputDeserializer: LambdaOutputDeserializer[O]
  ): O = // TODO Wrap O in some error monad?; Expose other elements of InvokeResponse
    val inputBytes = inputSerializer.serialize(input)

    val invokeRequest = InvokeRequest.builder()
      .functionName(function.functionName)
      .payload(inputBytes)
      .build()

    val invokeResult = unshapedClient.invoke(invokeRequest)
    val outputBytes = invokeResult.payload()
    val result = outputDeserializer.deserialize(outputBytes)
    result
