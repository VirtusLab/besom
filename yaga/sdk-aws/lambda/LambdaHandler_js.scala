//> using target.platform js

package yaga.extensions.aws.lambda

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSExport}
import scala.util.Try
import yaga.json.{JsonReader, JsonWriter}

// import com.github.plokhotnyuk.jsoniter_scala.macros._
// import com.github.plokhotnyuk.jsoniter_scala.core._

import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromString, writeToString}
// import com.github.plokhotnyuk.jsoniter_scala.core.

abstract class LambdaHandler[C, I, O](using configReader: JsonReader[C], inputReader: JsonReader[I], outputWriter: JsonWriter[O], lambdaShape: LambdaShape[C, I, O]) extends LambdaHandlerBase[C, I, O]:
  // private inline given configCodec: JsonValueCodec[C] = JsonCodecMaker.make
  // private inline given inputCodec: JsonValueCodec[I] = JsonCodecMaker.make
  // private inline given outputCodec: JsonValueCodec[O] = JsonCodecMaker.make

  // protected inline given inputReader: JsonReader[I] = JsonReader.fromJsoniterCodec[I]
  // protected inline given outputWriter: JsonWriter[O] = JsonWriter.fromJsoniterCodec[O]

  // inline def readConfigJson(json: String): C = readFromString(json)
  // inline def readInputJson(json: String): I = readFromString(json)
  // inline def writeOutputJson(obj: O): String = writeToString(obj)

  override protected def environmentVariables: Map[String, String] =
    js.Dynamic.global.process.env.asInstanceOf[js.Dictionary[String]].toMap[String, String]

  final def handleRequest(event: js.Any, context: LambdaHandler.Context, callback: LambdaHandler.Callback): Unit =
    val invocationResult = 
      for {
        lambdaInput <- readInput(event)
        lambdaContext = LambdaContext(context)
        lambdaOutput <- Try { handleInput(lambdaInput)(using lambdaContext) }.toEither
        outputJsObject <- writeOutput(lambdaOutput)
      } yield outputJsObject
    
    invocationResult match
      case Left(e) =>
        callback(LambdaHandler.Error(e.getMessage), null)
      case Right(outputJsObject) =>
        callback(null, outputJsObject)

  private def readInput(event: js.Any): Either[Throwable, I] =
    Try {
      val jsonString = js.JSON.stringify(event)
      // inputReader.read(JsonParser(jsonString))
      inputReader.read(jsonString)
    }.toEither

  // Node.js runtime expects the output to be a js object to be serialized to JSON rather than JSON as a string.
  // To keep the semantics of the JVM runtime, we serialize the output to a JSON string and then parse it to a js object
  // even though it adds some runtime overhead.
  private inline def writeOutput(lambdaOutput: O): Either[Throwable, js.Any] =
    Try {
      val jsonString = outputWriter.write(lambdaOutput)
      if jsonString.isEmpty then
        null
      else
        js.JSON.parse(jsonString)
    }.toEither


object LambdaHandler:
  @js.native
  /* private[lambda]  */trait Context extends js.Object:
    def functionName: String = js.native

  @js.native
  /* private [lambda]  */trait Callback extends js.Object:
    def apply(err: Error, data: js.Any): Unit = js.native

  @js.native
  @JSGlobal
  /* private [lambda]  */class Error(message: String) extends js.Object