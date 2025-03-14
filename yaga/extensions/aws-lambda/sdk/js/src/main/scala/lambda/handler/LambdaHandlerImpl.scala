package yaga.extensions.aws.lambda

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSExport}
import scala.concurrent.*

private[lambda] abstract class LambdaHandlerImpl[C, I, O] extends LambdaHandlerBase[C, I, O]:
  override protected def environmentVariables: Map[String, String] =
    js.Dynamic.global.process.env.asInstanceOf[js.Dictionary[String]].toMap[String, String]

  final def handleRequest(event: js.Any, context: LambdaContext.UnderlyingContext): Future[Any] =
    import scala.concurrent.ExecutionContext.Implicits.global
    
    val lambdaContext = LambdaContext(context)
    
    val invocationResult = 
      for {
        lambdaInput <- Future { readInput(event) }
        lambdaOutput <- handleInputAsync(lambdaInput)(using lambdaContext)
        outputJsObject <- Future { writeOutput(lambdaOutput) }
      } yield outputJsObject

    invocationResult

  private def readInput(event: js.Any): Input =
    val jsonString = js.JSON.stringify(event)
    inputReader.read(jsonString)

  // Node.js runtime expects the output to be a js object to be serialized to JSON rather than JSON as a string.
  // To keep the semantics of the JVM runtime, we serialize the output to a JSON string and then parse it to a js object
  // even though it adds some runtime overhead.
  private def writeOutput(lambdaOutput: O): js.Any =
    val jsonString = outputWriter.write(lambdaOutput)
    if jsonString.isEmpty then
      null
    else
      js.JSON.parse(jsonString)
