package besom.codegen

import java.io.File
import ujson._

case class Schemas()

object SchemaReader {
  private def extractDataType(obj: Obj): Tpe = {
    // TODO
    // handle default values
    // handle secrets
    val objMap = obj.obj
    objMap.get("oneOf") match {
      case Some(Arr(values)) =>
        val typeChoices = values.map(value => extractDataType(value.obj)).toList
        UnionType(typeChoices)
      case None =>
        objMap.get("$ref") match {
          case Some(strValue) =>
            NamedType(typeUri = strValue.str)
          case None =>
            objMap.get("type") match {
              case Some(strValue) => strValue.str match {
                case "string" => StringType
                case "integer" => IntegerType
                case "number" => NumberType
                case "boolean" => BooleanType
                case "array" =>
                  val elemType = extractDataType(obj("items").obj)
                  ArrayType(elemType)
                case "object" => 
                  val valueType = objMap.get("additionalProperties").map(value => extractDataType(value.obj)).getOrElse(StringType)
                  MapType(valueType)
                  // TODO objects with fixed schema (with `properties` defined)
              }
              case _ =>
                throw new Exception(s"Cannot extract type from $obj")
            }
        }
      case _ =>
        throw new Exception(s"Cannot extract type from $obj")
    }
  }

  def readPackageMappings(path: String): Map[String, String] = {
    val input = ujson.Readable.fromFile(new File(path))
    val json = ujson.read(input)
    json("language")("java")("packages").obj.map{ case (key, value) => key -> value.str }.toMap
  }

  def readResources(path: String): Seq[TypeSpec] = {
    val input = ujson.Readable.fromFile(new File(path))
    val json = ujson.read(input)

    json("resources").obj.map { case (resourceId, resourceSpecObj) =>
      readTypeSpec(resourceId, resourceSpecObj)
    }.toSeq
  }

  def readTypeSpec(typeId: String, specObj: ujson.Value) = {
    val requiredProperties = specObj.obj.get("required").map(_.arr.map(_.str)).getOrElse(Nil)

    def parseProperty(name: String, prop: ujson.Value) = {
      val tpe = extractDataType(prop.obj)
      val isRequired = requiredProperties.contains(name)
      FieldSpec(name, tpe, isRequired = isRequired)
    }

    TypeSpec(
      typeId = typeId,
      description = specObj.obj.get("description").map(_.str),
      properties = specObj("properties").obj.map((parseProperty _).tupled).toList,
      inputProperties = specObj.obj.get("inputProperties").map(_.obj.map((parseProperty _).tupled).toList)
    )
  }

  def readOutputs(path: String): Seq[TypeSpec] = {
    val input = ujson.Readable.fromFile(new File(path))
    val json = ujson.read(input)

    json("types").obj.map { case (outputId, outputSpecObj) =>
      readTypeSpec(outputId, outputSpecObj)
    }.toSeq
  }
}