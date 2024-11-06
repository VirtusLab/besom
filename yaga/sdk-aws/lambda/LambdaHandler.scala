package yaga.extensions.aws.lambda

import scala.util.Try
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.lambda.runtime.Context
import yaga.extensions.aws.lambda.internal.{EnvReader, InputStreamReader, OutputStreamWriter}

abstract class LambdaHandler[C, I, O](using configReader: EnvReader[C], inputReader: InputStreamReader[I], outputWriter: OutputStreamWriter[O]) extends RequestStreamHandler:
  protected type RequestContext = Context

  protected lazy val config: C = configReader.read(sys.env) match
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
        in <- inputReader.read(input)
        result <- Try { handleInput(in)(using context) }.toEither
        _ <- outputWriter.write(output, result)
      } yield {}
    
    result match
      case Left(e) =>
        throw e
      case Right(_) =>
        {}
