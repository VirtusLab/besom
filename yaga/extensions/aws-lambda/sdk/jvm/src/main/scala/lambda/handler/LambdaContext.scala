package yaga.extensions.aws.lambda

import com.amazonaws.services.lambda.runtime.Context

class LambdaContext private[lambda](underlying: LambdaContext.UnderlyingContext) extends LambdaContextApi:
  override def functionName: String = underlying.getFunctionName()

object LambdaContext:
  type UnderlyingContext = com.amazonaws.services.lambda.runtime.Context