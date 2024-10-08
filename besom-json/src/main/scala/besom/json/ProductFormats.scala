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

trait ProductFormats:
  self: StandardFormats with AdditionalFormats =>

  def writeNulls: Boolean             = false
  def requireNullsForOptions: Boolean = false

  inline def jsonFormatN[T <: Product]: RootJsonFormat[T] = ${ ProductFormatsMacro.jsonFormatImpl[T]('self) }
  inline def jsonReaderN[T <: Product]: RootJsonReader[T] = ${ ProductFormatsMacro.jsonReaderImpl[T]('self) }

object ProductFormatsMacro:
  import scala.deriving.*
  import scala.quoted.*

  private def findDefaultParams[T](using quotes: Quotes, tpe: Type[T]): Expr[Map[String, Any]] =
    import quotes.reflect.*

    TypeRepr.of[T].classSymbol match
      case None => '{ Map.empty[String, Any] }
      case Some(sym) =>
        val comp = sym.companionClass
        try
          val mod = Ref(sym.companionModule)
          val names =
            for p <- sym.caseFields if p.flags.is(Flags.HasDefault)
            yield p.name
          val namesExpr: Expr[List[String]] =
            Expr.ofList(names.map(Expr(_)))

          val body = comp.tree.asInstanceOf[ClassDef].body
          val idents: List[Ref] =
            for
              case deff @ DefDef(name, _, _, _) <- body
              if name.startsWith("$lessinit$greater$default")
            yield mod.select(deff.symbol)
          val typeArgs = TypeRepr.of[T].typeArgs
          val identsExpr: Expr[List[Any]] =
            if typeArgs.isEmpty then Expr.ofList(idents.map(_.asExpr))
            else Expr.ofList(idents.map(_.appliedToTypes(typeArgs).asExpr))

          '{ $namesExpr.zip($identsExpr).toMap }
        catch case cce: ClassCastException => '{ Map.empty[String, Any] } // TODO drop after https://github.com/lampepfl/dotty/issues/19732

  private def prepareFormatInstances(elemLabels: Type[?], elemTypes: Type[?])(using Quotes): List[Expr[(String, JsonFormat[?], Boolean)]] =
    (elemLabels, elemTypes) match
      case ('[EmptyTuple], '[EmptyTuple]) => Nil
      case ('[label *: labelsTail], '[tpe *: tpesTail]) =>
        val label = Type.valueOfConstant[label].get.asInstanceOf[String]
        val isOption = Type.of[tpe] match
          case '[Option[?]] => Expr(true)
          case _            => Expr(false)

        val fieldName = Expr(label)
        val fieldFormat = Expr.summon[JsonFormat[tpe]].getOrElse {
          quotes.reflect.report.errorAndAbort("Missing given instance of JsonFormat[" ++ Type.show[tpe] ++ "]")
        } // TODO: Handle missing instance
        val namedInstance = '{ (${ fieldName }, $fieldFormat, ${ isOption }) }
        namedInstance :: prepareFormatInstances(Type.of[labelsTail], Type.of[tpesTail])

  private def prepareReaderInstances(elemLabels: Type[?], elemTypes: Type[?])(using Quotes): List[Expr[(String, JsonReader[?], Boolean)]] =
    (elemLabels, elemTypes) match
      case ('[EmptyTuple], '[EmptyTuple]) => Nil
      case ('[label *: labelsTail], '[tpe *: tpesTail]) =>
        val label = Type.valueOfConstant[label].get.asInstanceOf[String]
        val isOption = Type.of[tpe] match
          case '[Option[?]] => Expr(true)
          case _            => Expr(false)

        val fieldName = Expr(label)
        val fieldFormat = Expr.summon[JsonReader[tpe]].getOrElse {
          quotes.reflect.report.errorAndAbort("Missing given instance of JsonFormat[" ++ Type.show[tpe] ++ "]")
        } // TODO: Handle missing instance
        val namedInstance = '{ (${ fieldName }, $fieldFormat, ${ isOption }) }
        namedInstance :: prepareReaderInstances(Type.of[labelsTail], Type.of[tpesTail])

  def jsonFormatImpl[T <: Product: Type](prodFormats: Expr[ProductFormats])(using Quotes): Expr[RootJsonFormat[T]] =
    Expr.summon[Mirror.Of[T]].get match
      case '{
            $m: Mirror.ProductOf[T] { type MirroredElemLabels = elementLabels; type MirroredElemTypes = elementTypes }
          } =>
        // instances are in correct order of fields of the product
        val allInstancesExpr = Expr.ofList(prepareFormatInstances(Type.of[elementLabels], Type.of[elementTypes]))
        val defaultArguments = findDefaultParams[T]

        '{
          new RootJsonFormat[T]:
            private val allInstances = ${ allInstancesExpr }
            private val fmts         = ${ prodFormats }
            private val defaultArgs  = ${ defaultArguments }

            def read(json: JsValue): T = json match
              case JsObject(fields) =>
                val values = allInstances.map { case (fieldName, fieldFormat, isOption) =>
                  try fieldFormat.read(fields(fieldName))
                  catch
                    case e: NoSuchElementException =>
                      // if field has a default value, use it, we didn't find anything in the JSON
                      if defaultArgs.contains(fieldName) then defaultArgs(fieldName)
                      // if field is optional and requireNullsForOptions is disabled, return None
                      // otherwise we require an explicit null value
                      else if isOption && !fmts.requireNullsForOptions then None
                      // it's missing so we throw an exception
                      else throw DeserializationException("Object is missing required member '" ++ fieldName ++ "'", null, fieldName :: Nil)
                    case DeserializationException(msg, cause, fieldNames) =>
                      throw DeserializationException(msg, cause, fieldName :: fieldNames)
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
                    case None        => if fmts.writeNulls then (fieldName, format.write(fieldValue)) :: acc else acc
                    case value       => (fieldName, format.write(value)) :: acc
              }

              JsObject(fields.toMap)
        }

  def jsonReaderImpl[T <: Product: Type](prodFormats: Expr[ProductFormats])(using Quotes): Expr[RootJsonReader[T]] =
    Expr.summon[Mirror.Of[T]].get match
      case '{
            $m: Mirror.ProductOf[T] { type MirroredElemLabels = elementLabels; type MirroredElemTypes = elementTypes }
          } =>
        // instances are in correct order of fields of the product
        val allInstancesExpr = Expr.ofList(prepareReaderInstances(Type.of[elementLabels], Type.of[elementTypes]))
        val defaultArguments = findDefaultParams[T]

        '{
          new RootJsonReader[T]:
            private val allInstances = ${ allInstancesExpr }
            private val fmts         = ${ prodFormats }
            private val defaultArgs  = ${ defaultArguments }

            def read(json: JsValue): T = json match
              case JsObject(fields) =>
                val values = allInstances.map { case (fieldName, fieldFormat, isOption) =>
                  try fieldFormat.read(fields(fieldName))
                  catch
                    case e: NoSuchElementException =>
                      // if field has a default value, use it, we didn't find anything in the JSON
                      if defaultArgs.contains(fieldName) then defaultArgs(fieldName)
                      // if field is optional and requireNullsForOptions is disabled, return None
                      // otherwise we require an explicit null value
                      else if isOption && !fmts.requireNullsForOptions then None
                      // it's missing so we throw an exception
                      else throw DeserializationException("Object is missing required member '" ++ fieldName ++ "'", null, fieldName :: Nil)
                    case DeserializationException(msg, cause, fieldNames) =>
                      throw DeserializationException(msg, cause, fieldName :: fieldNames)
                }
                $m.fromProduct(Tuple.fromArray(values.toArray))

              case _ => throw DeserializationException("Object expected", null, allInstances.map(_._1))

        }

end ProductFormatsMacro

/** This trait supplies an alternative rendering mode for optional case class members. Normally optional members that are undefined (`None`)
  * are not rendered at all. By mixing in this trait into your custom JsonProtocol you can enforce the rendering of undefined members as
  * `null`. (Note that this only affect JSON writing, besom-json will always read missing optional members as well as `null` optional
  * members as `None`.)
  */
trait NullOptions extends ProductFormats {
  this: StandardFormats with AdditionalFormats =>

  override def writeNulls: Boolean = true

}
