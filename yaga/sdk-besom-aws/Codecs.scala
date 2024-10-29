package yaga.extensions.aws.lambda

object Codecs:
  private case class ShapedFunctionHandleSerdeModel(
    functionName: String
    // TODO include stringified schema here?
  )

  given shapedFunctionHandleEncoder[I, O]: besom.types.Encoder[ShapedFunctionHandle[I, O]] =
    besom.internal.Encoder.derived[ShapedFunctionHandleSerdeModel].contramap: handle =>
      ShapedFunctionHandleSerdeModel(
        functionName = handle.functionName
      )
