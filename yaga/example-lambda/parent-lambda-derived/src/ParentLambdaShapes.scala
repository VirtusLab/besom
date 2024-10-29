package yaga.generated.lambdatest.parent

import besom.json.*

import yaga.generated.lambdatest.child.{Bar, Baz}
import yaga.extensions.aws.lambda.ShapedFunctionHandle

case class ParentLambdaConfig(
  childLambdaHandle: ShapedFunctionHandle[Bar, Baz]
) derives JsonFormat

final case class ParentLambdaConfigArgs private(
  childLambdaHandle: besom.types.Output[ShapedFunctionHandle[Bar, Baz]]
)

object ParentLambdaConfigArgs:
  def apply(
    childLambdaHandle: besom.types.Input[ShapedFunctionHandle[Bar, Baz]]
  ): ParentLambdaConfigArgs =
    new ParentLambdaConfigArgs(
      childLambdaHandle = childLambdaHandle.asOutput(isSecret = false)
    )

  extension (parentLambdaConfigArgs: ParentLambdaConfigArgs) def withArgs(
    childLambdaHandle: besom.types.Input[ShapedFunctionHandle[Bar, Baz]] = parentLambdaConfigArgs.childLambdaHandle
  ): ParentLambdaConfigArgs =
    new ParentLambdaConfigArgs(
      childLambdaHandle = childLambdaHandle.asOutput(isSecret = false)
    )

  import yaga.extensions.aws.lambda.Codecs.given

  given encoder: besom.types.Encoder[ParentLambdaConfigArgs] =
    besom.internal.Encoder.derived[ParentLambdaConfigArgs]
  given argsEncoder: besom.types.ArgsEncoder[ParentLambdaConfigArgs] =
    besom.internal.ArgsEncoder.derived[ParentLambdaConfigArgs]


case class Qux(
  str: String = "abcb"
) derives JsonFormat
