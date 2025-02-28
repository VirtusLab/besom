//> using target.platform jvm

package yaga.extensions.aws.lambda

import com.amazonaws.services.lambda.runtime.Context

class LambdaContext private[lambda](underlying: LambdaHandler.Context) extends LambdaContextApi:
  override def functionName: String = underlying.getFunctionName()
