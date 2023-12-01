package besom.codegen

import scala.meta._
import scala.meta.dialects.Scala33
import besom.codegen.metaschema._

class TypeMapper(
  val defaultPackageInfo: PulumiPackageInfo,
  schemaProvider: SchemaProvider
)(implicit logger: Logger) {
  private def scalaTypeFromTypeUri(
    typeUri: String,
    asArgsType: Boolean
  ): Option[Type.Ref] = {
    // Example URIs available in TypeMapper.test.scala

    val (fileUri, typePath) = typeUri.split("#") match {
      case Array(typePath)          => ("", typePath) // JSON Schema Pointer like reference
      case Array(fileUri, typePath) => (fileUri, typePath) // Reference to external schema
      case _                        => throw TypeMapperError(s"Unexpected type URI format: ${typeUri}")
    }

    val packageInfo = fileUri match {
      case "" =>
        defaultPackageInfo
      case s"/${providerName}/v${schemaVersion}/schema.json" =>
        schemaProvider.packageInfo(PackageMetadata(providerName, schemaVersion))._2
      case s"${protocol}://${host}/${providerName}/v${schemaVersion}/schema.json" =>
        schemaProvider
          .packageInfo(
            PackageMetadata(providerName, schemaVersion)
              .withUrl(s"${protocol}://${host}/${providerName}")
          )
          ._2
      case _ =>
        throw TypeMapperError(s"Unexpected file URI format: ${fileUri}")
    }

    val (escapedTypeToken, isFromTypeUri, isFromResourceUri) = typePath match {
      case s"/provider"           => (packageInfo.providerTypeToken, false, true)
      case s"/types/${token}"     => (token, true, false)
      case s"/resources/${token}" => (token, false, true)
      case s"/${rest}" =>
        throw TypeMapperError(
          s"Invalid named type reference, fileUri:' ${fileUri}', typePath: '${typePath}', rest: '${rest}''"
        )
      case token => (token, false, false)
    }

    val uniformedTypeToken = escapedTypeToken.toLowerCase

    val typeCoordinates =
      PulumiDefinitionCoordinates.fromRawToken(
        escapedTypeToken,
        packageInfo.moduleToPackageParts,
        packageInfo.providerToPackageParts
      )

    lazy val hasProviderDefinition   = packageInfo.providerTypeToken == uniformedTypeToken
    lazy val hasResourceDefinition   = packageInfo.resourceTypeTokens.contains(uniformedTypeToken)
    lazy val hasObjectTypeDefinition = packageInfo.objectTypeTokens.contains(uniformedTypeToken)
    lazy val hasEnumTypeDefinition   = packageInfo.enumTypeTokens.contains(uniformedTypeToken)

    def resourceClassCoordinates: Option[ScalaDefinitionCoordinates] = {
      if (hasResourceDefinition || hasProviderDefinition) {
        Some(typeCoordinates.asResourceClass(asArgsType = asArgsType))
      } else {
        None
      }
    }

    def objectClassCoordinates: Option[ScalaDefinitionCoordinates] = {
      if (hasObjectTypeDefinition) {
        Some(typeCoordinates.asObjectClass(asArgsType = asArgsType))
      } else if (hasEnumTypeDefinition) {
        Some(typeCoordinates.asEnumClass)
      } else {
        None
      }
    }

    // We need to be careful here because a type URI can refer to both a resource and an object type
    // and we need to be flexible, because schemas are not always consistent
    val classCoordinates: Option[ScalaDefinitionCoordinates] =
      (resourceClassCoordinates, objectClassCoordinates) match {
        case (Some(_), _) if isFromResourceUri =>
          resourceClassCoordinates
        case (_, Some(_)) if isFromTypeUri =>
          objectClassCoordinates
        case (Some(_), None) =>
          logger.warn(s"Assuming '/resources/` prefix for type URI, fileUri: '${fileUri}', typePath: '${typePath}'")
          resourceClassCoordinates
        case (None, Some(_)) =>
          logger.warn(s"Assuming '/types/` prefix for type URI, fileUri: '${fileUri}', typePath: '${typePath}'")
          objectClassCoordinates
        case (None, None) =>
          logger.warn(s"Found no type definition for type URI, fileUri: '${fileUri}', typePath: '${typePath}'")
          None
        case _ =>
          throw TypeMapperError(
            s"Type URI can refer to both a resource or an object type, fileUri: '${fileUri}', typePath: '${typePath}'"
          )
      }

    classCoordinates.map(_.fullyQualifiedTypeRef)
  }

  def asScalaType(typeRef: TypeReference, asArgsType: Boolean, fallbackType: Option[AnonymousType] = None): Type =
    typeRef match {
      case BooleanType         => t"Boolean"
      case StringType          => t"String"
      case IntegerType         => t"Int"
      case NumberType          => t"Double"
      case UrnType             => t"besom.types.URN"
      case ResourceIdType      => t"besom.types.ResourceId"
      case ArrayType(elemType) => t"scala.collection.immutable.List[${asScalaType(elemType, asArgsType)}]"
      case MapType(elemType)   => t"scala.Predef.Map[String, ${asScalaType(elemType, asArgsType)}]"
      case unionType: UnionType =>
        unionType.oneOf.map(asScalaType(_, asArgsType, unionType.`type`)).reduce { (t1, t2) =>
          if (t1.syntax == t2.syntax) t"$t1" else t"$t1 | $t2"
        }
      case namedType: NamedType =>
        unescape(namedType.typeUri) match {
          case "pulumi.json#/Archive" =>
            t"besom.types.Archive"
          case "pulumi.json#/Asset" =>
            t"besom.types.AssetOrArchive"
          case "pulumi.json#/Any" =>
            t"besom.types.PulumiAny"
          case "pulumi.json#/Json" =>
            t"besom.types.PulumiJson"

          case typeUri =>
            scalaTypeFromTypeUri(typeUri, asArgsType) match {
              case Some(scalaType) => scalaType
              case None            =>
                // try a fallback type if specified, used by UnionType
                fallbackType match {
                  case Some(primitiveType) => asScalaType(primitiveType, asArgsType)
                  case None                =>
                    // we should ignore namedType.`type` because it is deprecated according to metaschema
                    // but at least aws provider uses it in one weird place
                    namedType.`type` match {
                      case Some(deprecatedFallbackType) => asScalaType(deprecatedFallbackType, asArgsType)
                      case None =>
                        throw TypeMapperError(
                          s"Unsupported type: '${typeRef}', no corresponding type definition and no fallback type"
                        )
                    }
                }
            }
        }
      case _ =>
        throw TypeMapperError(s"Unsupported type: '${typeRef}'")
    }

  private def unescape(value: String) = {
    value.replace("%2F", "/") // TODO: Proper URL un-escaping
  }
}
