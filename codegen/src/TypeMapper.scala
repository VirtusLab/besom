package besom.codegen

import scala.util.matching.Regex
import scala.meta._
import scala.meta.dialects.Scala33
import besom.codegen.metaschema._

class TypeMapper(
  defaultProviderName: SchemaProvider.ProviderName,
  defaultSchemaVersion: SchemaProvider.SchemaVersion,
  schemaProvider: SchemaProvider
)(implicit logger: Logger) {
  def defaultPackageInfo: PulumiPackageInfo =
    schemaProvider.packageInfo(providerName = defaultProviderName, schemaVersion = defaultSchemaVersion)

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
    asArgsType: Boolean,
    underlyingType: Option[PrimitiveType]
  ): Option[Type.Ref] = {
    // Example URIs:
    // "/provider/vX.Y.Z/schema.json#/types/pulumi:type:token"
    // #/types/kubernetes:rbac.authorization.k8s.io%2Fv1beta1:Subject
    // "aws:iam/documents:PolicyDocument"

    val (fileUri, typePath) = typeUri.replace("%2F", "/").split("#") match {
      case Array(typePath)          => ("", typePath) // JSON Schema Pointer like reference
      case Array(fileUri, typePath) => (fileUri, typePath) // Reference to external schema
      case _                        => throw new Exception(s"Unexpected type URI format: ${typeUri}")
    }

    val packageInfo = fileUri match {
      case "" =>
        defaultPackageInfo
      case s"/${providerName}/v${schemaVersion}/schema.json" =>
        schemaProvider.packageInfo(providerName = providerName, schemaVersion = schemaVersion)
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

    lazy val hasResourceDefinition  = packageInfo.resourceTypeTokens.contains(uniformedTypeToken)
    lazy val hasObjectTypeDefintion = packageInfo.objectTypeTokens.contains(uniformedTypeToken)
    lazy val hasEnumTypeDefintion   = packageInfo.enumTypeTokens.contains(uniformedTypeToken)

    def resourceClassCoordinates: Option[ClassCoordinates] = {
      if (hasResourceDefinition) {
        Some(typeCoordinates.asResourceClass(asArgsType = asArgsType))
      } else {
        None
      }
    }

    def objectClassCoordinates: Option[ClassCoordinates] = {
      if (hasObjectTypeDefintion) {
        Some(typeCoordinates.asObjectClass(asArgsType = asArgsType))
      } else if (hasEnumTypeDefintion) {
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
          scalaTypeFromTypeUri(typeUri, asArgsType = asArgsType, underlyingType = namedType.`type`)
            .getOrElse {
              logger.warn(
                s"Type URI ${typeUri} has no corresponding type definition - using its underlying type as fallback"
              )
              val underlyingType = namedType.`type`.getOrElse(
                throw new Exception(s"Type with URI ${typeUri} has no underlying primitive type to be used as fallback")
              )
              asScalaType(underlyingType, asArgsType = asArgsType)
            }
      }
  }
}
