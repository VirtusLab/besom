/*
 * Copyright (C) 2009-2011 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package besom.json

import scala.language.implicitConversions

def deserializationError(msg: String, cause: Throwable = null, fieldNames: List[String] = Nil) =
  throw new DeserializationException(msg, cause, fieldNames)

def serializationError(msg: String) = throw new SerializationException(msg)

case class DeserializationException(msg: String, cause: Throwable = null, fieldNames: List[String] = Nil)
    extends RuntimeException(msg, cause)
class SerializationException(msg: String) extends RuntimeException(msg)

private[json] class RichAny[T](any: T) {
  def toJson(implicit writer: JsonWriter[T]): JsValue = writer.write(any)
}

private[json] class RichString(string: String) {
  def parseJson: JsValue                               = JsonParser(string)
  def parseJson(settings: JsonParserSettings): JsValue = JsonParser(string, settings)
}

private[json] trait DefaultExports:
  type JsField = (String, JsValue)

  def jsonReader[T](implicit reader: JsonReader[T]) = reader
  def jsonWriter[T](implicit writer: JsonWriter[T]) = writer

  implicit def enrichAny[T](any: T): RichAny[T]         = new RichAny(any)
  implicit def enrichString(string: String): RichString = new RichString(string)

private[json] trait DefaultProtocol:
  implicit val defaultProtocol: JsonProtocol = DefaultJsonProtocol

/** This allows to perform a single import: `import besom.json.*` to get basic JSON behaviour. If you need to extend JSON handling in any
  * way, please use `import besom.json.custom.*`, then extend `DefaultJsonProtocol`:
  *
  * ```
  *   object MyCustomJsonProtocol extends DefaultJsonProtocol:
  *     given someCustomTypeFormat: JsonFormat[A] = ...
  * ```
  * build your customized protocol that way and set it up for your `derives` clauses using:
  * ```
  *   given JsonProtocol = MyCustomJsonProtocol
  *
  *   case class MyCaseClass(a: String, b: Int) derives JsonFormat
  * ```
  */
object custom extends DefaultExports:
  export besom.json.{JsonProtocol, DefaultJsonProtocol}
  export besom.json.{JsonFormat, JsonReader, JsonWriter}
  export besom.json.{RootJsonFormat, RootJsonReader, RootJsonWriter}
  export besom.json.{DeserializationException, SerializationException}
  export besom.json.{JsValue, JsObject, JsArray, JsString, JsNumber, JsBoolean, JsNull}

object DefaultJsonExports extends DefaultExports with DefaultProtocol

export DefaultJsonExports.*
export DefaultJsonProtocol.*
