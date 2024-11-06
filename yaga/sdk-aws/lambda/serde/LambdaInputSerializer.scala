package yaga.extensions.aws.lambda.internal

import software.amazon.awssdk.core.SdkBytes

import besom.json.JsonWriter

trait LambdaInputSerializer[I]:
  // TODO Handle serde failures with some monad
  def serialize(input: I): SdkBytes

object LambdaInputSerializer:
  given fromBesomJsonWriter[A](using jsonWriter: JsonWriter[A]): LambdaInputSerializer[A] with
    def serialize(input: A): SdkBytes =
      val str = jsonWriter.write(input).toString
      SdkBytes.fromUtf8String(str)
