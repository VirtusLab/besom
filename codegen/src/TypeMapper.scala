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
      case typeTokenPattern("pulumi", "providers", providerName) =>
        (providerName, Utils.IndexModuleName, Utils.ProviderTypeName)
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

  private def scalaTypeFromTypeUri(fileUri: String, typePath: String, asArgsType: Boolean): Option[Type.Ref] = {
    // Example URIs available in AsScalaTypeTest.scala
    val packageInfo = fileUri match {
      case "" =>
        defaultPackageInfo
      case s"/${providerName}/v${schemaVersion}/schema.json" =>
        schemaProvider.packageInfo(schemaName = providerName, schemaVersion = schemaVersion)
      case s"${protocol}://${host}/${providerName}/v${schemaVersion}/schema.json" =>
        // FIXME: Use the hostname to determine the download server
        logger.error(
          s"Skipping --server while downloading of external schema - missing implementation for '${protocol}://${host}'}"
        )
        schemaProvider.packageInfo(schemaName = providerName, schemaVersion = schemaVersion)
    }

    val (escapedTypeToken, isFromTypeUri, isFromResourceUri, isProvider) = typePath match {
      case s"/provider"           => (packageInfo.providerTypeToken, false, false, true)
      case s"/types/${token}"     => (token, true, false, false)
      case s"/resources/${token}" => (token, false, true, false)
      case s"/${rest}" =>
        throw TypeMapperError(
          s"Invalid named type reference, fileUri:' ${fileUri}', typePath: '${typePath}', rest: '${rest}''"
        )
      case token => (token, false, false, false)
    }

    val typeToken          = escapedTypeToken.replace("%2F", "/") // TODO: Proper URL un-escaping ?
    val uniformedTypeToken = typeToken.toLowerCase

    val typeCoordinates =
      parseTypeToken(typeToken, packageInfo.moduleToPackageParts, packageInfo.providerToPackageParts)

    lazy val hasProviderDefinition   = packageInfo.providerTypeToken == uniformedTypeToken
    lazy val hasResourceDefinition   = packageInfo.resourceTypeTokens.contains(uniformedTypeToken)
    lazy val hasObjectTypeDefinition = packageInfo.objectTypeTokens.contains(uniformedTypeToken)
    lazy val hasEnumTypeDefinition   = packageInfo.enumTypeTokens.contains(uniformedTypeToken)

    def providerClassCoordinates: Option[ClassCoordinates] = {
      if (hasProviderDefinition) {
        Some(typeCoordinates.asResourceClass(asArgsType = false))
      } else {
        None
      }
    }

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
      if (isProvider) {
        providerClassCoordinates
      } else if (isFromResourceUri) {
        resourceClassCoordinates
      } else if (isFromTypeUri) {
        objectClassCoordinates
      } else {
        (resourceClassCoordinates, objectClassCoordinates) match {
          case (Some(coordinates), None) =>
            logger.warn(
              s"Assuming a '/resources/` prefix for type URI, fileUri: '${fileUri}', typePath: '${typePath}''"
            )
            Some(coordinates)
          case (None, Some(coordinates)) =>
            logger.warn(s"Assuming a '/types/` prefix for type URI, fileUri: '${fileUri}', typePath: '${typePath}''")
            Some(coordinates)
          case (None, None) => None
          case _ =>
            throw TypeMapperError(
              s"Type URI can refer to both a resource or an object type, fileUri: '${fileUri}', typePath: '${typePath}''"
            )
        }
      }

    classCoordinates.map(_.fullyQualifiedTypeRef)
  }

  def asScalaType(typeRef: TypeReference, asArgsType: Boolean, fallbackType: Option[PrimitiveType] = None): Type =
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
            t"besom.types.Asset"
          case "pulumi.json#/Any" =>
            t"besom.types.PulumiAny"
          case "pulumi.json#/Json" =>
            t"besom.types.PulumiJson"

          case s"$fileUri#$typePath" =>
            scalaTypeFromTypeUri(fileUri, typePath, asArgsType) match {
              case Some(scalaType) => scalaType
              // we ignore namedType.`type` because it is deprecated according to metaschema
              case None =>
                throw TypeMapperError(s"NamedType with URI '${namedType.typeUri}' has no corresponding type definition")
            }
          case token =>
            scalaTypeFromTypeUri("", token, asArgsType) match {
              case Some(scalaType) => scalaType
              // we ignore namedType.`type` because it is deprecated according to metaschema
              case None =>
                throw TypeMapperError(s"NamedType with URI '${namedType.typeUri}' has no corresponding type definition")
            }
        }

      // try a fallback type if specified, used by UnionType
      case _ => fallbackType match {
        case Some(primitiveType) => asScalaType(primitiveType, asArgsType)
        case None =>
          throw TypeMapperError(
            s"Unsupported type: '${typeRef}', no corresponding type definition and no fallback type"
          )
      }
    }

  private def unescape(value: String) = {
    value.replace("%2F", "/") // TODO: Proper URL un-escaping
  }
}
