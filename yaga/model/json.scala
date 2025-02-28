package yaga.json

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromString, writeToString}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

trait JsonReader[A]:
  def read(json: String): A

object JsonReader extends JsonReaderLowPriority:
  given unitReader: JsonReader[Unit] = new JsonReader[Unit]:
    def read(json: String): Unit = ()

trait JsonReaderLowPriority:
  inline given fromJsoniterCodec[A]: JsonReader[A] =
    new JsonReader[A]:
      given codec: JsonValueCodec[A] = JsonCodecMaker.make[A]
      def read(json: String): A = readFromString(json)

trait JsonWriter[A]:
  def write(obj: A): String

object JsonWriter extends JsonWriterLowPriority:
  given unitWriter: JsonWriter[Unit] = new JsonWriter[Unit]:
    def write(obj: Unit): String = ""

trait JsonWriterLowPriority:

  inline given fromJsoniterCodec[A]: JsonWriter[A] =
    new JsonWriter[A]:
      given codec: JsonValueCodec[A] = JsonCodecMaker.make[A]
      def write(obj: A): String = writeToString(obj)
