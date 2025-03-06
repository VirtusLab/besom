package yaga.extensions.aws.lambda

object Codecs:
  private case class LambdaHandleSerdeModel(
    functionName: String
    // TODO include stringified schema here?
  )

  given lambdaHandleEncoder[I, O]: besom.types.Encoder[LambdaHandle[I, O]] =
    besom.internal.Encoder.derived[LambdaHandleSerdeModel].contramap: handle =>
      LambdaHandleSerdeModel(
        functionName = handle.functionName
      )
