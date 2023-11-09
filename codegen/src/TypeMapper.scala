package besom.codegen

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

  private def scalaTypeFromTypeUri(
    typeUri: String,
    asArgsType: Boolean,
    underlyingType: Option[AnonymousType]
  ): Option[Type.Ref] = {
    // Example URIs:
    // "/provider/vX.Y.Z/schema.json#/types/pulumi:type:token"
    // "/kubernetes/v3.7.0/schema.json#/provider"
    // "/kubernetes/v3.7.0/schema.json#/resources/kubernetes:storage.k8s.io%2Fv1:StorageClass"
    // "#/types/kubernetes:rbac.authorization.k8s.io%2Fv1beta1:Subject"
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
      PulumiDefinitionCoordinates.fromRawToken(
        typeToken,
        packageInfo.moduleToPackageParts,
        packageInfo.providerToPackageParts
      )

    lazy val hasResourceDefinition  = packageInfo.resourceTypeTokens.contains(uniformedTypeToken)
    lazy val hasObjectTypeDefintion = packageInfo.objectTypeTokens.contains(uniformedTypeToken)
    lazy val hasEnumTypeDefintion   = packageInfo.enumTypeTokens.contains(uniformedTypeToken)

    def resourceClassCoordinates: Option[ScalaDefinitionCoordinates] = {
      if (hasResourceDefinition) {
        Some(typeCoordinates.asResourceClass(asArgsType = asArgsType))
      } else {
        None
      }
    }

    def objectClassCoordinates: Option[ScalaDefinitionCoordinates] = {
      if (hasObjectTypeDefintion) {
        Some(typeCoordinates.asObjectClass(asArgsType = asArgsType))
      } else if (hasEnumTypeDefintion) {
        Some(typeCoordinates.asEnumClass)
      } else {
        None
      }
    }

    val classCoordinates: Option[ScalaDefinitionCoordinates] =
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
