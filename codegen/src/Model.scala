package besom.codegen

import scala.meta._

sealed trait Tpe {
  def asScalaType(basePackage: String, specialPackageMappings: Map[String, String]): Type = this match {
    case BooleanType => t"Boolean"
    case StringType => t"String"
    case IntegerType => t"Int"
    case NumberType => t"Double"
    case ArrayType(elemType) => t"List[${elemType.asScalaType(basePackage, specialPackageMappings)}]"
    case MapType(elemType) => t"Map[String, ${elemType.asScalaType(basePackage, specialPackageMappings)}]"
    case UnionType(choiceTypes) =>
      choiceTypes.map(_.asScalaType(basePackage, specialPackageMappings)).reduce{ (t1, t2) => t"$t1 | $t2"}
    case namedType: NamedType =>
      //TODO: encoding of spaces in URIs
      namedType.typeUri match {
        // TODO: handle pulumi types:
        case "pulumi.json#/Archive" =>
          t"besom.types.Archive"
        case "pulumi.json#/Asset" =>
          t"besom.types.Asset"
        case "pulumi.json#/Any" =>
          t"pulumi.types.Any"
        case "pulumi.json#/Json" =>
          t"pulumi.types.Json"

        case typeUri =>
          // Example URI:
          // #/types/kubernetes:rbac.authorization.k8s.io%2Fv1beta1:Subject

          val typeIdParts = typeUri.split("#")(1).split(":")

          val providerName = typeIdParts(0).split("/").last
          val versionedPackage = typeIdParts(1).replace("%2F", "/") // TODO: Proper URL unescaping
          val packageSuffix = specialPackageMappings.getOrElse(key = versionedPackage, default = versionedPackage)
          val typeName = typeIdParts(2)

          val packageParts = basePackage.split("\\.") ++ Seq(providerName) ++ packageSuffix.split("\\.")
          val packageRef = packageParts.toList.foldLeft[Term.Ref](q"__root__")((acc, name) => Term.Select(acc, Term.Name(name)))

          t"${packageRef}.${Type.Name(typeName)}"

      }
    }

  def defaultValue: Option[Term] = this match {
    case ArrayType(_) => Some(q"List.empty")
    case MapType(_) => Some(q"Map.empty")
    case tpe =>
      // TODO: default for options?
      None
  }

  def isMapType = this match {
    case _: MapType => true
    case _ => false
  }
}

// TODO: Add enums?

sealed trait PrimitiveType extends Tpe

object BooleanType extends PrimitiveType
object StringType extends PrimitiveType
object IntegerType extends PrimitiveType
object NumberType extends PrimitiveType
case class ArrayType(elemType: Tpe) extends Tpe
case class MapType(valueType: Tpe) extends Tpe
case class UnionType(choiceTypes: List[Tpe]) extends Tpe

case class NamedType(typeUri: String) extends Tpe


case class FieldSpec(name: String, tpe: Tpe, isRequired: Boolean)

case class TypeSpec(
  typeId: String,
  description: Option[String],
  properties: List[FieldSpec],
  inputProperties: Option[List[FieldSpec]]
)

case class ResourceClassSpec(
  name: String,
  packagePrefix: String,
  description: Option[String],
  fields: List[FieldSpec]
)

case class InputClassSpec(
  name: String,
  packagePrefix: String,
  description: Option[String],
  fields: List[FieldSpec],
)

case class OutputClassSpec(
  name: String,
  packagePrefix: String,
  description: Option[String],
  properties: List[FieldSpec]
)
