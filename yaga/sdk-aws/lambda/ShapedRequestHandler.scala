package yaga.extensions.aws.lambda

import scala.util.Try
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.lambda.runtime.Context

abstract class ShapedRequestHandler[C, I, O](using configReader: EnvReader[C], inputReader: InputStreamReader[I], outputWriter: OutputStreamWriter[O]) extends RequestStreamHandler:
  protected val config: C = configReader.read(sys.env) match
    case Right(conf) =>
      conf
    case Left(e) =>
      println("Could not read the configuration from environment variables")
      throw e

  def handleInput(input: I, context: Context): O

  final override def handleRequest(input: java.io.InputStream, output: java.io.OutputStream, context: Context): Unit = 
    val result = 
      for {
        in <- inputReader.read(input)
        result <- Try { handleInput(in, context) }.toEither
        _ <- outputWriter.write(output, result)
      } yield {}
    
    result match
      case Left(e) =>
        throw e
      case Right(_) =>
        {}
