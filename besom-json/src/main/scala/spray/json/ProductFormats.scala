/*
 * Copyright (C) 2011 Mathias Doenitz
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

import scala.annotation.tailrec
import scala.util.control.NonFatal

trait ProductFormats:
  self: StandardFormats with AdditionalFormats =>

  def writeNulls: Boolean = false

  inline def jsonFormatN[T <: Product]: RootJsonFormat[T] = ${ ProductFormatsMacro.jsonFormatImpl[T]('self) }

object ProductFormatsMacro:
  import scala.deriving.*
  import scala.quoted.*

  def jsonFormatImpl[T <: Product: Type](prodFormats: Expr[ProductFormats])(using Quotes): Expr[RootJsonFormat[T]] = 
    Expr.summon[Mirror.Of[T]].get match
      case '{
            $m: Mirror.ProductOf[T] { type MirroredElemLabels = elementLabels; type MirroredElemTypes = elementTypes }
          } => 
            def prepareInstances(elemLabels: Type[?], elemTypes: Type[?]): List[Expr[(String, JsonFormat[?], Boolean)]] =
              (elemLabels, elemTypes) match
                case ('[EmptyTuple], '[EmptyTuple]) => Nil
                case ('[label *: labelsTail], '[tpe *: tpesTail]) =>
                  val label          = Type.valueOfConstant[label].get.asInstanceOf[String]
                  val isOption = Type.of[tpe] match
                    case '[Option[?]] => Expr(true)
                    case _ => Expr(false)
                  
                  val fieldName = Expr(label)
                  val fieldFormat = Expr.summon[JsonFormat[tpe]].getOrElse {
                    quotes.reflect.report.errorAndAbort("Missing given instance of JsonFormat[" ++ Type.show[tpe] ++ "]")
                  } // TODO: Handle missing instance
                  val namedInstance = '{ (${fieldName}, $fieldFormat, ${isOption}) }
                  namedInstance :: prepareInstances(Type.of[labelsTail], Type.of[tpesTail])

            // instances are in correct order of fields of the product
            val allInstancesExpr = Expr.ofList(prepareInstances(Type.of[elementLabels], Type.of[elementTypes]))

            '{ 
              new RootJsonFormat[T]:
                private val allInstances = ${allInstancesExpr}
                private val fmts = ${prodFormats}
                private val keys = allInstances.map(_._1)
                def read(json: JsValue): T = json match
                  case JsObject(fields) => 
                    val values = allInstances.map { case (fieldName, fieldFormat, isOption) =>
                      val fieldValue = 
                        try fieldFormat.read(fields(fieldName))
                        catch 
                          case e: NoSuchElementException =>
                            if isOption then None 
                            else throw DeserializationException("Object is missing required member '" ++ fieldName ++ "'", null, fieldName :: Nil)
                          case DeserializationException(msg, cause, fieldNames) =>
                            throw DeserializationException(msg, cause, fieldName :: fieldNames)

                      fieldValue
                    }
                    $m.fromProduct(Tuple.fromArray(values.toArray))

                  case _ => throw DeserializationException("Object expected", null, allInstances.map(_._1))
                
                def write(obj: T): JsValue = 
                  val fieldValues = obj.productIterator.toList
                  val fields = allInstances.zip(fieldValues).foldLeft(List.empty[(String, JsValue)]) { 
                    case (acc, ((fieldName, fieldFormat, isOption), fieldValue)) =>
                      val format = fieldFormat.asInstanceOf[JsonFormat[Any]]
                      fieldValue match
                        case Some(value) => (fieldName, format.write(fieldValue)) :: acc
                        case None => if fmts.writeNulls then (fieldName, format.write(fieldValue)) :: acc else acc
                        case value => (fieldName, format.write(value)) :: acc
                  }

                  JsObject(fields.toMap)
            }

/**
 * This trait supplies an alternative rendering mode for optional case class members.
 * Normally optional members that are undefined (`None`) are not rendered at all.
 * By mixing in this trait into your custom JsonProtocol you can enforce the rendering of undefined members as `null`.
 * (Note that this only affect JSON writing, besom-json will always read missing optional members as well as `null`
 * optional members as `None`.)
 */
trait NullOptions extends ProductFormats {
  this: StandardFormats with AdditionalFormats =>

  override def writeNulls: Boolean = true

}
