package yaga.extensions.aws.lambda

import yaga.json.{JsonReader, JsonWriter}

trait LambdaClientApi:
  def invokeSyncUnsafe[I, O](function: LambdaHandle[I, O], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputWriter: JsonWriter[I],
    outputReader: JsonReader[O]
  ): O

  def invokeAsyncUnsafe[I, O](function: LambdaHandle[I, ?], input: I)(using
    // TODO Remove serde typeclasses from the signature by bundling them in the function handle?
    inputWriter: JsonWriter[I]
  ): Unit