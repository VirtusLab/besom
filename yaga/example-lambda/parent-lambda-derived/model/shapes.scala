package yaga.generated.lambdatest.parent

import besom.json.defaultProtocol
import besom.json.defaultProtocol.given

case class Config(
  childLambdaHandle: yaga.extensions.aws.lambda.LambdaHandle[
    yaga.generated.lambdatest.child.Bar,
    yaga.generated.lambdatest.child.Baz
  ]
) derives besom.json.JsonFormat

final case class ConfigArgs private(
  childLambdaHandle: besom.types.Output[
    yaga.extensions.aws.lambda.LambdaHandle[
      yaga.generated.lambdatest.child.Bar,
      yaga.generated.lambdatest.child.Baz
    ]
  ]
)

object ConfigArgs:
  def apply(
    childLambdaHandle: besom.types.Input[
      yaga.extensions.aws.lambda.LambdaHandle[
        yaga.generated.lambdatest.child.Bar,
        yaga.generated.lambdatest.child.Baz
      ]
    ]
  ): ConfigArgs =
    new ConfigArgs(
      childLambdaHandle = childLambdaHandle.asOutput(isSecret = false)
    )

  extension (parentLambdaConfigArgs: ConfigArgs) def withArgs(
    childLambdaHandle: besom.types.Input[
      yaga.extensions.aws.lambda.LambdaHandle[
        yaga.generated.lambdatest.child.Bar,
        yaga.generated.lambdatest.child.Baz
      ]
    ] = parentLambdaConfigArgs.childLambdaHandle
  ): ConfigArgs =
    new ConfigArgs(
      childLambdaHandle = childLambdaHandle.asOutput(isSecret = false)
    )

  import yaga.extensions.aws.lambda.Codecs.given

  given encoder: besom.types.Encoder[ConfigArgs] =
    besom.internal.Encoder.derived[ConfigArgs]
  given argsEncoder: besom.types.ArgsEncoder[ConfigArgs] =
    besom.internal.ArgsEncoder.derived[ConfigArgs]


case class Qux(
  str: scala.Predef.String = "abcb"
) derives besom.json.JsonFormat
