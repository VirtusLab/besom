package besom.codegen

import scala.util.matching.Regex
import scala.meta._
import scala.meta.dialects.Scala33
import besom.codegen.metaschema._

class TypeMapper(
  val defaultPackageInfo: PulumiPackageInfo,
  schemaProvider: SchemaProvider
)(implicit logger: Logger) {
  private val typeTokenFmt: String    = "(.*):(.*)?:(.*)" // provider:module:type
  private val typeTokenPattern: Regex = ("^" + typeTokenFmt + "$").r

  def parseTypeToken(
    typeToken: String,
    moduleToPackageParts: String => Seq[String],
    providerToPackageParts: String => Seq[String]
  ): PulumiTypeCoordinates = {
    val (providerName, modulePortion, typeName) = typeToken match {
      case typeTokenPattern(providerName, modulePortion, typeName) =>
        (providerName, modulePortion, typeName)
      case _ =>
        throw TypeMapperError(
          s"Cannot parse type token: $typeToken, " +
            s"typeTokenPattern: $typeTokenPattern"
        )
    }
    PulumiTypeCoordinates(
      providerPackageParts = providerToPackageParts(providerName),
      modulePackageParts = moduleToPackageParts(modulePortion),
      typeName = typeName
    )
  }

  private def scalaTypeFromTypeUri(
    typeUri: String,
    asArgsType: Boolean
  ): Option[Type.Ref] = {
    // Example URIs available in AsScalaTypeTest.scala
    val (fileUri, typePath) = typeUri.replace("%2F", "/").split("#") match {
      case Array(typePath)          => ("", typePath) // JSON Schema Pointer like reference
      case Array(fileUri, typePath) => (fileUri, typePath) // Reference to external schema
      case _                        => throw new Exception(s"Unexpected type URI format: ${typeUri}")
    }

    val packageInfo = fileUri match {
      case "" =>
        defaultPackageInfo
      case s"/${providerName}/v${schemaVersion}/schema.json" =>
        schemaProvider.packageInfo(schemaName = providerName, schemaVersion = schemaVersion)
    }

    val (escapedTypeToken, isFromTypeUri, isFromResourceUri) = typePath match {
      case s"/types/${token}"     => (token, true, false)
      case s"/resources/${token}" => (token, false, true)
      case s"/${rest}"            => throw new Exception(s"Invalid named type reference: ${typeUri}")
      case token                  => (token, false, false)
    }

    val typeToken          = escapedTypeToken.replace("%2F", "/") // TODO: Proper URL unescaping ?
    val uniformedTypeToken = typeToken.toLowerCase

    val typeCoordinates =
      parseTypeToken(typeToken, packageInfo.moduleToPackageParts, packageInfo.providerToPackageParts)

    lazy val hasResourceDefinition   = packageInfo.resourceTypeTokens.contains(uniformedTypeToken)
    lazy val hasObjectTypeDefinition = packageInfo.objectTypeTokens.contains(uniformedTypeToken)
    lazy val hasEnumTypeDefinition   = packageInfo.enumTypeTokens.contains(uniformedTypeToken)

    def resourceClassCoordinates: Option[ClassCoordinates] = {
      if (hasResourceDefinition) {
        Some(typeCoordinates.asResourceClass(asArgsType = asArgsType))
      } else {
        None
      }
    }

    def objectClassCoordinates: Option[ClassCoordinates] = {
      if (hasObjectTypeDefinition) {
        Some(typeCoordinates.asObjectClass(asArgsType = asArgsType))
      } else if (hasEnumTypeDefinition) {
        Some(typeCoordinates.asEnumClass)
      } else {
        None
      }
    }

    val classCoordinates: Option[ClassCoordinates] =
      if (isFromResourceUri) {
        resourceClassCoordinates
      } else if (isFromTypeUri) {
        objectClassCoordinates
      } else {
        (resourceClassCoordinates, objectClassCoordinates) match {
          case (Some(coordinates), None) =>
            logger.warn(s"Assuming a '/resources/` prefix for type URI ${typeUri}")
            Some(coordinates)
          case (None, Some(coordinates)) =>
            logger.warn(s"Assuming a '/types/` prefix for type URI ${typeUri}")
            Some(coordinates)
          case (None, None) => None
          case _ => throw new Exception(s"Type URI ${typeUri} can refer to both a resource or an object type")
        }
      }

    classCoordinates.map(_.fullyQualifiedTypeRef)
  }

  def asScalaType(typeRef: TypeReference, asArgsType: Boolean): Type = typeRef match {
    case BooleanType         => t"Boolean"
    case StringType          => t"String"
    case IntegerType         => t"Int"
    case NumberType          => t"Double"
    case UrnType             => t"besom.types.URN"
    case ResourceIdType      => t"besom.types.ResourceId"
    case ArrayType(elemType) => t"scala.collection.immutable.List[${asScalaType(elemType, asArgsType)}]"
    case MapType(elemType)   => t"scala.Predef.Map[String, ${asScalaType(elemType, asArgsType)}]"
    case unionType: UnionType =>
      unionType.oneOf.map(asScalaType(_, asArgsType)).reduce { (t1, t2) => t"$t1 | $t2" }
    case namedType: NamedType =>
      namedType.typeUri match {
        case "pulumi.json#/Archive" =>
          t"besom.types.Archive"
        case "pulumi.json#/Asset" =>
          t"besom.types.Asset"
        case "pulumi.json#/Any" =>
          t"besom.types.PulumiAny"
        case "pulumi.json#/Json" =>
          t"besom.types.PulumiJson"

        case typeUri =>
          scalaTypeFromTypeUri(typeUri, asArgsType = asArgsType)
            .getOrElse {
              // we ignore namedType.`type` because it is deprecated according to metaschema
              throw new Exception(s"Type with URI ${typeUri} has no corresponding type definition")
            }
      }
  }
}
