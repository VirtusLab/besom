package yaga.generated.lambdatest.parent

import besom.json.*

import yaga.generated.lambdatest.child.{Bar, Baz}
import yaga.extensions.aws.lambda.ShapedFunctionHandle

case class ParentLambdaConfig(
  childLambdaHandle: ShapedFunctionHandle[Bar, Baz]
) derives JsonFormat
