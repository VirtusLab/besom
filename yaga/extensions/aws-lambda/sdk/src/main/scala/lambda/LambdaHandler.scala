package yaga.extensions.aws.lambda

import scala.util.Try
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.lambda.runtime.Context
import yaga.json.{JsonReader, JsonWriter}
import yaga.extensions.aws.lambda.internal.EnvReader

abstract class LambdaHandler[C, I, O](using configReader: JsonReader[C], inputReader: JsonReader[I], outputWriter: JsonWriter[O], lambdaShape: LambdaShape[C, I, O]) extends RequestStreamHandler:
  protected type RequestContext = Context

  protected lazy val config: C = EnvReader.read[C](sys.env) match
    case Right(conf) =>
      conf
    case Left(e) =>
      println("Could not read the configuration from environment variables")
      throw e

  def context(using ctx: RequestContext): RequestContext = ctx

  def handleInput(input: I): RequestContext ?=> O

  final override def handleRequest(input: java.io.InputStream, output: java.io.OutputStream, context: Context): Unit = 
    val result = 
      for {
        in <- Try { readInput(input) }.toEither
        result <- Try { handleInput(in)(using context) }.toEither
        _ <- Try { writeOutput(output, result) }.toEither
      } yield {}
    
    result match
      case Left(e) =>
        throw e
      case Right(_) =>
        {}

  private def readInput(inputStream: java.io.InputStream): I =
    val jsonString = scala.io.Source.fromInputStream(inputStream, "UTF-8").mkString
    inputReader.read(jsonString)

  private def writeOutput(outputStream: java.io.OutputStream, lambdaOutput: O): Unit =
    val jsonString = outputWriter.write(lambdaOutput)
    outputStream.write(jsonString.getBytes("UTF-8"))