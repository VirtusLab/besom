//> using scala 3.5.0
//> using dep com.amazonaws:aws-lambda-java-core:1.2.3

package lambdatest

import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.Context
import scala.beans.BeanProperty

class Lambda2Input(@BeanProperty var str: String):
  def this() = this("")

class Lambda2 extends RequestHandler[Lambda2Input, String]:
  override def handleRequest(event: Lambda2Input, context: Context): String =
    event.str + event.str