package yaga.extensions.aws.lambda

import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest

class ShapedLambdaClient(
  val unshapedClient: LambdaClient = LambdaClient.builder().build()
):
  def invokeSync[I, O](function: ShapedFunctionHandle[I, O], input: I)(using 
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