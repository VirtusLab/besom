package besom.codegen

import besom.codegen.metaschema.*
import besom.codegen.scalameta.interpolator.*

import scala.meta.*
import scala.meta.dialects.Scala33

class TypeMapper(
  val defaultPackageInfo: PulumiPackageInfo,
  schemaProvider: SchemaProvider
)(implicit logger: Logger) {
  private def scalaTypeFromTypeUri(
    typeUri: String,
    asArgsType: Boolean
  ): Option[Type.Ref] = {
    scalaCoordinatesFromTypeUri(typeUri, asArgsType).map(_.typeRef)
  }

  private def preParseFromTypeUri(
    typeUri: String
  ): (PulumiToken, PulumiPackageInfo, Boolean, Boolean) = {
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

    (PulumiToken(escapedTypeToken), packageInfo, isFromTypeUri, isFromResourceUri)
  }

  private def scalaCoordinatesFromTypeUri(
    typeUri: String,
    asArgsType: Boolean
  ): Option[ScalaDefinitionCoordinates] = {
    val (typeToken, packageInfo, isFromTypeUri, isFromResourceUri) = preParseFromTypeUri(typeUri)
    val typeCoordinates = PulumiDefinitionCoordinates.fromToken(
      typeToken,
      packageInfo.moduleToPackageParts,
      packageInfo.providerToPackageParts
    )

    val uniformedTypeToken           = typeCoordinates.token.asLookupKey
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

    def objectOrEnumClassCoordinates: Option[ScalaDefinitionCoordinates] = {
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
      (resourceClassCoordinates, objectOrEnumClassCoordinates) match {
        case (Some(_), _) if isFromResourceUri =>
          resourceClassCoordinates
        case (_, Some(_)) if isFromTypeUri =>
          objectOrEnumClassCoordinates
        case (Some(_), None) =>
          logger.warn(s"Assuming a '/resources/` prefix for type URI, typeUri: '${typeUri}'")
          resourceClassCoordinates
        case (None, Some(_)) =>
          logger.warn(s"Assuming a '/types/` prefix for type URI, typeUri: '${typeUri}'")
          objectOrEnumClassCoordinates
        case (None, None) =>
          logger.warn(s"Found no type definition for type URI, typeUri: '${typeUri}'")
          None
        case _ =>
          throw TypeMapperError(
            s"Type URI can refer to both a resource or an object type, typeUri: '${typeUri}'"
          )
      }

    classCoordinates
  }

  def asScalaType(typeRef: TypeReference, asArgsType: Boolean, fallbackType: Option[AnonymousType] = None): Type =
    typeRef match {
      case BooleanType         => scalameta.types.Boolean
      case StringType          => scalameta.types.String
      case IntegerType         => scalameta.types.Int
      case NumberType          => scalameta.types.Double
      case UrnType             => scalameta.types.besom.types.URN
      case ResourceIdType      => scalameta.types.besom.types.ResourceId
      case ArrayType(elemType) => scalameta.types.List(asScalaType(elemType, asArgsType))
      case MapType(elemType)   => scalameta.types.Map(scalameta.types.String, asScalaType(elemType, asArgsType))
      case unionType: UnionType =>
        unionType.oneOf.map(asScalaType(_, asArgsType, unionType.`type`)).reduce { (t1, t2) =>
          if t1.syntax == t2.syntax then t1 else Type.ApplyInfix(t1, Type.Name("|"), t2)
        }
      case namedType: NamedType =>
        unescape(namedType.typeUri) match {
          case "pulumi.json#/Archive" =>
            scalameta.types.besom.types.Archive
          case "pulumi.json#/Asset" =>
            scalameta.types.besom.types.AssetOrArchive
          case "pulumi.json#/Any" =>
            scalameta.types.besom.types.PulumiAny
          case "pulumi.json#/Json" =>
            scalameta.types.besom.types.PulumiJson

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
    }

  // TODO: This is a temporary solution, we should use a proper type mapping using ADTs
  def enumValue(typeRef: TypeReference, value: ConstValue): Option[Term.Ref] =
    typeRef match {
      case namedType: NamedType =>
        unescape(namedType.typeUri) match {
          case "pulumi.json#/Archive" | "pulumi.json#/Asset" | "pulumi.json#/Any" | "pulumi.json#/Json" =>
            None
          case typeUri =>
            preParseFromTypeUri(typeUri) match
              case (token: PulumiToken, packageInfo, _, _) =>
                for
                  instances <- packageInfo.enumValueToInstances.get(token)
                  instance  <- instances.get(value)
                  enumType  <- scalaCoordinatesFromTypeUri(typeUri, asArgsType = false)
                yield enumType.withSelectionName(Some(instance)).termRef
        }
      case _ => None
    }

  // TODO: This is a temporary solution, we should use a proper type mapping using ADTs
  def findTokenAndDependencies(typeRef: TypeReference): Vector[(Option[PulumiToken], Option[PackageMetadata])] =
    typeRef match {
      case ArrayType(elemType) => findTokenAndDependencies(elemType)
      case MapType(elemType)   => findTokenAndDependencies(elemType)
      case unionType: UnionType =>
        unionType.oneOf.map(findTokenAndDependencies(_)).reduce(_ ++ _)
      case namedType: NamedType =>
        unescape(namedType.typeUri) match {
          case "pulumi.json#/Archive" | "pulumi.json#/Asset" | "pulumi.json#/Any" | "pulumi.json#/Json" =>
            Vector((None, None))
          case typeUri =>
            preParseFromTypeUri(typeUri) match
              case (token: PulumiToken, packageInfo, _, _) =>
                Vector((Some(token), Some(packageInfo.asPackageMetadata)))
        }
      case _ => Vector((None, None))
    }

  private def unescape(value: String) = {
    value.replace("%2F", "/") // TODO: Proper URL un-escaping
  }
}
