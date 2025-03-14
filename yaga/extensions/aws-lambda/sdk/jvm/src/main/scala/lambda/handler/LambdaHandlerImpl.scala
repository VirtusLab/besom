package yaga.extensions.aws.lambda

import scala.concurrent.*
import scala.concurrent.duration.*
import com.amazonaws.services.lambda.runtime.RequestStreamHandler

private[lambda] abstract class LambdaHandlerImpl[C, I, O] extends RequestStreamHandler, LambdaHandlerBase[C, I, O]:
  override protected def environmentVariables: Map[String, String] =
    sys.env

  final override def handleRequest(input: java.io.InputStream, output: java.io.OutputStream, context: LambdaContext.UnderlyingContext): Unit =
    import scala.concurrent.ExecutionContext.Implicits.global

    val lambdaContext = LambdaContext(context)

    val invocationResult = 
      for {
        lambdaInput <- Future { readInput(input) }
        lambdaOutput <- handleInputAsync(lambdaInput)(using lambdaContext)
        _ <- Future { writeOutput(output, lambdaOutput) }
      } yield {}

    Await.result(invocationResult, Duration.Inf)

  private def readInput(inputStream: java.io.InputStream): I =
    val jsonString = scala.io.Source.fromInputStream(inputStream, "UTF-8").mkString
    inputReader.read(jsonString)

  private def writeOutput(outputStream: java.io.OutputStream, lambdaOutput: O): Unit =
    val jsonString = outputWriter.write(lambdaOutput).toString
    outputStream.write(jsonString.getBytes("UTF-8"))
