package yaga.extensions.aws.lambda

import scala.concurrent.Future
import yaga.json.{JsonReader, JsonWriter}

trait LambdaClientApi:
  def invokeWithResponse[I, O](function: LambdaHandle[I, O], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputWriter: JsonWriter[I],
    outputReader: JsonReader[O]
  ): Future[O]

  def triggerEvent[I, O](function: LambdaHandle[I, ?], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputWriter: JsonWriter[I]
  ): Future[Unit]