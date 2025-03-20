package yaga.extensions.aws.lambda

import scala.util.control.NonFatal
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSExport}
import scala.scalajs.js.JSConverters._

private[lambda] abstract class LambdaHandlerImpl[C, I, O] extends LambdaHandlerBase[C, I, O]:
  def handleRequest(event: js.Any, context: LambdaContext.UnderlyingContext): js.Promise[Any]

  override protected def environmentVariables: Map[String, String] =
    js.Dynamic.global.process.env.asInstanceOf[js.Dictionary[String]].toMap[String, String]

  protected[lambda] def readInput(event: js.Any): Input =
    try
      val jsonString = js.JSON.stringify(event)
      inputReader.read(jsonString)
    catch
      case NonFatal(e) =>
        println(s"Exception while reading lambda's input: ${e.getMessage}")
        throw e

  // Node.js runtime expects the output to be a js object to be serialized to JSON rather than JSON as a string.
  // To keep the semantics of the JVM runtime, we serialize the output to a JSON string and then parse it to a js object
  // even though it adds some runtime overhead.
  protected[lambda] def writeOutput(lambdaOutput: O): js.Any =
    try
      val jsonString = outputWriter.write(lambdaOutput)
      if jsonString.isEmpty then
        null
      else
        js.JSON.parse(jsonString)
    catch
      case NonFatal(e) =>
        println(s"Exception while writing lambda's output: ${e.getMessage}")
        throw e


private[lambda] abstract class LambdaHandlerSyncImpl[C, I, O] extends LambdaHandlerImpl[C, I, O], LambdaHandlerSyncApi:
  override def handleRequest(event: js.Any, context: LambdaContext.UnderlyingContext): js.Promise[Any] =
    new js.Promise[Any]({ (resolve, reject) =>
      try
        val result = handleRequestSync(event, context)
        resolve(result)
      catch
        case NonFatal(e) =>
          reject(e)
    })

  def handleRequestSync(event: js.Any, context: LambdaContext.UnderlyingContext): js.Any =
    val lambdaContext = LambdaContext(context)

    val lambdaInput = readInput(event)
    val lambdaOutput = handleInput(lambdaInput)(using lambdaContext)
    writeOutput(lambdaOutput)


private[lambda] abstract class LambdaHandlerAsyncImpl[C, I, O] extends LambdaHandlerImpl[C, I, O], LambdaHandlerAsyncApi:
  override def handleRequest(event: js.Any, context: LambdaContext.UnderlyingContext): js.Promise[Any] =
    val lambdaContext = LambdaContext(context)

    import scala.concurrent.*
    import scala.concurrent.ExecutionContext.Implicits.global

    val invocationResult = 
      for {
        lambdaInput <- Future { readInput(event) }
        lambdaOutput <- handleInput(lambdaInput)(using lambdaContext)
        outputJsObject <- Future { writeOutput(lambdaOutput) }
      } yield outputJsObject

    invocationResult.toJSPromise
