package yaga.generated.parentlambda.lambdatest.parent

import besom.json.defaultProtocol
import besom.json.defaultProtocol.given

case class Config(
  childLambdaHandle: yaga.extensions.aws.lambda.LambdaHandle[
    yaga.generated.lambdatest.child.Bar,
    yaga.generated.lambdatest.child.Baz
  ]
) derives besom.json.JsonFormat
