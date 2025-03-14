package yaga.extensions.aws.lambda

import scala.scalajs.js

class LambdaContext private[lambda](underlying: LambdaContext.UnderlyingContext) extends LambdaContextApi:
  override def functionName: String = underlying.functionName

object LambdaContext:
  @js.native
  trait UnderlyingContext extends js.Object:
    def functionName: String = js.native
