package yaga.extensions.aws.lambda

import scala.util.control.NonFatal
import com.amazonaws.services.lambda.runtime.RequestStreamHandler

private[lambda] abstract class LambdaHandlerImpl[C, I, O] extends RequestStreamHandler, LambdaHandlerBase[C, I, O]:
  override protected def environmentVariables: Map[String, String] =
    sys.env

  protected[lambda] def readInput(inputStream: java.io.InputStream): I =
    try
      val jsonString = scala.io.Source.fromInputStream(inputStream, "UTF-8").mkString
      inputReader.read(jsonString)
    catch
      case e: Exception =>
        println(s"Exception while reading lambda's input: ${e.getMessage}")
        throw e

  protected[lambda] def writeOutput(outputStream: java.io.OutputStream, lambdaOutput: O): Unit =
    try
      val jsonString = outputWriter.write(lambdaOutput).toString
      outputStream.write(jsonString.getBytes("UTF-8"))
    catch
      case e: Exception =>
        println(s"Exception while writing lambda's output: ${e.getMessage}")
        throw e


private[lambda] abstract class LambdaHandlerSyncImpl[C, I, O] extends LambdaHandlerImpl[C, I, O], LambdaHandlerSyncApi:
  override def handleRequest(input: java.io.InputStream, output: java.io.OutputStream, context: LambdaContext.UnderlyingContext): Unit =
    val lambdaContext = LambdaContext(context)

    val lambdaInput = readInput(input)
    val lambdaOutput = handleInput(lambdaInput)(using lambdaContext)
    writeOutput(output, lambdaOutput)


private[lambda] abstract class LambdaHandlerAsyncImpl[C, I, O] extends LambdaHandlerImpl[C, I, O], LambdaHandlerAsyncApi:
  override def handleRequest(input: java.io.InputStream, output: java.io.OutputStream, context: LambdaContext.UnderlyingContext): Unit =
    val lambdaContext = LambdaContext(context)

    import scala.concurrent.*
    import scala.concurrent.duration.*
    import scala.concurrent.ExecutionContext.Implicits.global

    val invocationResult = 
      for {
        lambdaInput <- Future { readInput(input) }
        lambdaOutput <- handleInput(lambdaInput)(using lambdaContext)
        _ <- Future { writeOutput(output, lambdaOutput) }
      } yield {}

    Await.result(invocationResult, Duration.Inf)
