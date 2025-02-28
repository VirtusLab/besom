//> using target.platform jvm

package yaga.extensions.aws.lambda

import scala.util.Try
import yaga.json.{JsonReader, JsonWriter}
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.lambda.runtime.Context

abstract class LambdaHandler[C, I, O](using configReader: JsonReader[C], inputReader: JsonReader[I], outputWriter: JsonWriter[O], lambdaShape: LambdaShape[C, I, O]) extends RequestStreamHandler, LambdaHandlerBase[C, I, O]:
  override protected def environmentVariables: Map[String, String] =
    sys.env

  final override def handleRequest(input: java.io.InputStream, output: java.io.OutputStream, context: Context): Unit = 
    val invocationResult = 
      for {
        lambdaInput <- readInput(input)
        lambdaContext = LambdaContext(context)
        lambdaOutput <- Try { handleInput(lambdaInput)(using lambdaContext) }.toEither
        _ <- writeOutput(output, lambdaOutput)
      } yield {}
    
    invocationResult match
      case Left(e) =>
        throw e
      case Right(_) =>
        {}

  private def readInput(inputStream: java.io.InputStream): Either[Throwable, I] =
    Try {
      val jsonString = scala.io.Source.fromInputStream(inputStream, "UTF-8").mkString
      inputReader.read(jsonString)
    }.toEither

  private def writeOutput(outputStream: java.io.OutputStream, lambdaOutput: O): Either[Throwable, Unit] =
    Try {
      val jsonString = outputWriter.write(lambdaOutput).toString
      outputStream.write(jsonString.getBytes("UTF-8"))
    }.toEither

object LambdaHandler:
  type Context = com.amazonaws.services.lambda.runtime.Context