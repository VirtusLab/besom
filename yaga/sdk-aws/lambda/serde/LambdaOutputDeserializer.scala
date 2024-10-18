package yaga.extensions.aws.lambda

import software.amazon.awssdk.core.SdkBytes

import besom.json.{JsonParser, JsonReader}

trait LambdaOutputDeserializer[O]:
  // TODO Handle serde failures with some monad
  def deserialize(bytes: SdkBytes): O

object LambdaOutputDeserializer:
  given fromBesomJsonReader[A](using jsonReader: JsonReader[A]): LambdaOutputDeserializer[A] with
    def deserialize(bytes: SdkBytes): A =
      val str = bytes.asUtf8String()
      val json = JsonParser(str)
      jsonReader.read(json)