//> using target.platform js

package yaga.extensions.aws.lambda

import scala.scalajs.js

class LambdaContext private[lambda](underlying: LambdaHandler.Context) extends LambdaContextApi:
  override def functionName: String = underlying.functionName
