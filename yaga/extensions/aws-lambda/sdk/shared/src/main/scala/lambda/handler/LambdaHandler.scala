package yaga.extensions.aws.lambda

import yaga.json.{JsonReader, JsonWriter}

abstract class LambdaHandler[C, I, O](using val configReader: JsonReader[C], val inputReader: JsonReader[I], val outputWriter: JsonWriter[O], val lambdaShape: LambdaShape[C, I, O])
  extends LambdaHandlerImpl[C, I, O], LambdaHandlerSyncApi

abstract class LambdaAsyncHandler[C, I, O](using val configReader: JsonReader[C], val inputReader: JsonReader[I], val outputWriter: JsonWriter[O], val lambdaShape: LambdaShape[C, I, O])
  extends LambdaHandlerImpl[C, I, O], LambdaHandlerAsyncApi