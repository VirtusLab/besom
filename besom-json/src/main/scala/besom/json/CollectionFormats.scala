/*
 * Original implementation (C) 2009-2011 Debasish Ghosh
 * Adapted and extended in 2011 by Mathias Doenitz
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

import scala.reflect.ClassTag

trait CollectionFormats {

  /**
    * Supplies the JsonFormat for Lists.
   */
  implicit def listFormat[T :JsonFormat]: RootJsonFormat[List[T]] = new RootJsonFormat[List[T]] {
    def write(list: List[T]) = JsArray(list.map(_.toJson).toVector)
    def read(value: JsValue): List[T] = value match {
      case JsArray(elements) => elements.iterator.map(_.convertTo[T]).toList
      case x => deserializationError("Expected List as JsArray, but got " + x)
    }
  }
  
  /**
    * Supplies the JsonFormat for Arrays.
   */
  implicit def arrayFormat[T :JsonFormat :ClassTag]: RootJsonFormat[Array[T]] = new RootJsonFormat[Array[T]] {
    def write(array: Array[T]) = JsArray(array.map(_.toJson).toVector)
    def read(value: JsValue) = value match {
      case JsArray(elements) => elements.map(_.convertTo[T]).toArray[T]
      case x => deserializationError("Expected Array as JsArray, but got " + x)
    }
  }
  
  /**
    * Supplies the JsonFormat for Maps. The implicitly available JsonFormat for the key type K must
    * always write JsStrings, otherwise a [[besom.json.SerializationException]] will be thrown.
   */
  implicit def mapFormat[K :JsonFormat, V :JsonFormat]: RootJsonFormat[Map[K, V]] = new RootJsonFormat[Map[K, V]] {
    def write(m: Map[K, V]) = JsObject {
      m.map { field =>
        field._1.toJson match {
          case JsString(x) => x -> field._2.toJson
          case x => throw new SerializationException("Map key must be formatted as JsString, not '" + x + "'")
        }
      }
    }
    def read(value: JsValue) = value match {
      case x: JsObject => x.fields.map { field =>
        (JsString(field._1).convertTo[K], field._2.convertTo[V])
      }
      case x => deserializationError("Expected Map as JsObject, but got " + x)
    }
  }

  import collection.immutable as imm

  implicit def immIterableFormat[T :JsonFormat]: RootJsonFormat[imm.Iterable[T]]     = viaSeq[imm.Iterable[T], T](seq => imm.Iterable(seq :_*))
  implicit def immSeqFormat[T :JsonFormat]: RootJsonFormat[imm.Seq[T]]               = viaSeq[imm.Seq[T], T](seq => imm.Seq(seq :_*))
  implicit def immIndexedSeqFormat[T :JsonFormat]: RootJsonFormat[imm.IndexedSeq[T]] = viaSeq[imm.IndexedSeq[T], T](seq => imm.IndexedSeq(seq :_*))
  implicit def immLinearSeqFormat[T :JsonFormat]: RootJsonFormat[imm.LinearSeq[T]]   = viaSeq[imm.LinearSeq[T], T](seq => imm.LinearSeq(seq :_*))
  implicit def immSetFormat[T :JsonFormat]: RootJsonFormat[imm.Set[T]]               = viaSeq[imm.Set[T], T](seq => imm.Set(seq :_*))
  implicit def vectorFormat[T :JsonFormat]: RootJsonFormat[Vector[T]]                = viaSeq[Vector[T], T](seq => Vector(seq :_*))

  import collection.*

  implicit def iterableFormat[T :JsonFormat]: RootJsonFormat[Iterable[T]]     = viaSeq[Iterable[T], T](seq => Iterable(seq :_*))
  implicit def seqFormat[T :JsonFormat]: RootJsonFormat[Seq[T]]               = viaSeq[Seq[T], T](seq => Seq(seq :_*))
  implicit def indexedSeqFormat[T :JsonFormat]: RootJsonFormat[IndexedSeq[T]] = viaSeq[IndexedSeq[T], T](seq => IndexedSeq(seq :_*))
  implicit def linearSeqFormat[T :JsonFormat]: RootJsonFormat[LinearSeq[T]]   = viaSeq[LinearSeq[T], T](seq => LinearSeq(seq :_*))
  implicit def setFormat[T :JsonFormat]: RootJsonFormat[Set[T]]               = viaSeq[Set[T], T](seq => Set(seq :_*))

  /**
    * A JsonFormat construction helper that creates a JsonFormat for an Iterable type I from a builder function
    * List => I.
   */
  def viaSeq[I <: Iterable[T], T :JsonFormat](f: imm.Seq[T] => I): RootJsonFormat[I] = new RootJsonFormat[I] {
    def write(iterable: I) = JsArray(iterable.iterator.map(_.toJson).toVector)
    def read(value: JsValue) = value match {
      case JsArray(elements) => f(elements.map(_.convertTo[T]))
      case x => deserializationError("Expected Collection as JsArray, but got " + x)
    }
  }
}
