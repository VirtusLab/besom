package besom.codegen

import scala.util.matching.Regex
import scala.meta._
import scala.meta.dialects.Scala33
import besom.codegen.metaschema._

class TypeMapper(
  defaultProviderName: SchemaProvider.ProviderName,
  defaultSchemaVersion: SchemaProvider.SchemaVersion,
  schemaProvider: SchemaProvider,
  // val moduleToPackageParts: String => Seq[String],
  // enumTypeTokens: Set[String],
  // objectTypeTokens: Set[String],
  // resourceTypeTokens: Set[String],
  moduleFormat: Regex
)(implicit logger: Logger) {
  val defaultPackageInfo = schemaProvider.packageInfo(providerName = defaultProviderName, schemaVersion = defaultSchemaVersion)

  def parseTypeToken(typeToken: String, moduleToPackageParts: String => Seq[String]): PulumiTypeCoordinates = {
    val Array(providerName, modulePortion, typeName) = typeToken.split(":")
    val moduleName = modulePortion match {
      case moduleFormat(name) => name
    }
    PulumiTypeCoordinates(
      providerPackageParts = moduleToPackageParts(providerName),
      modulePackageParts = moduleToPackageParts(moduleName),
      typeName = typeName
    )
  }

  // private def scalaTypeFromNamedPulumiType(typeToken: String, typeUri: String, packageInfo: PulumiPackageInfo, isFromTypeUri: Boolean, isFromResourceUri: Boolean, asArgsType: Boolean) = {
  //   val typeCoordinates = parseTypeToken(typeToken, packageInfo.moduleToPackageParts)
  //   val lowercaseTypeToken = typeToken.toLowerCase

  //   lazy val hasResourceDefinition = packageInfo.resourceTypeTokens.contains(typeToken)
  //   lazy val hasObjectTypeDefintion = packageInfo.objectTypeTokens.contains(typeToken)
  //   lazy val hasEnumTypeDefintion = packageInfo.enumTypeTokens.contains(typeToken)

  //   val classCoordinates = (isFromResourceUri, isFromTypeUri) match { // TODO: fix isFromResourceUri
  //     case (true, false) =>
  //       if (hasResourceDefinition) {
  //         typeCoordinates.asResourceClass(asArgsType = asArgsType)
  //       } else {
  //         throw new Exception(s"Nonexistent resource token: ${typeToken}")
  //       }
  //     case (false, true) =>
  //       if (hasEnumTypeDefintion) {
  //         typeCoordinates.asObjectClass(asArgsType = asArgsType)
  //       } else if (hasEnumTypeDefintion) {
  //         typeCoordinates.asEnumClass
  //       } else {
  //         throw new Exception(s"Nonexistent type token: ${typeToken}")
  //       }
  //     case (false, false) =>
  //       (hasResourceDefinition, hasObjectTypeDefintion, hasEnumTypeDefintion) match {
  //         case (true, false, false) =>
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be a resource type")
  //           typeCoordinates.asResourceClass(asArgsType = asArgsType)
  //         case (false, true, false) =>
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be an object type")
  //           typeCoordinates.asObjectClass(asArgsType = asArgsType)
  //         case (false, false, true) =>
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be an enum type")
  //           typeCoordinates.asEnumClass
  //         case (false, false, false) =>
  //           logger.warn(s"Named type reference: ${typeUri} has an invalid format and is not defined in the current schema - using underlying primitive type as fallback")
  //           asScalaType(namedType.`type`.get, asArgsType = asArgsType)
  //         case _ =>

  //           throw new Exception(s"Named type reference: ${typeToken} has an invalid format but no heristic for it was found")
  //       }
  //     // case (true, false, true) => typeCoordinates.asResourceArgsClass
  //     // case (false, false, false) => typeCoordinates.asObjectClass
  //     // case (false, false, true) => typeCoordinates.asObjectArgsClass
  //     // case (false, true, _) => typeCoordinates.asEnumClass
  //   }


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
      case Array(typePath) => ("", typePath)
      case Array(fileUri, typePath) => (fileUri, typePath)
      case _ => throw new Exception(s"Unexpected type URI format: ${typeUri}") 
    }

    val packageInfo = fileUri match {
      case "" =>
        defaultPackageInfo
      case s"/${providerName}/v${schemaVersion}/schema.json" =>
        schemaProvider.packageInfo(providerName = providerName, schemaVersion = schemaVersion)
    }

    val (escapedTypeToken, isFromTypeUri, isFromResourceUri) = typePath match {
      case s"/types/${token}" => (token, true, false)
      case s"/resources/${token}" => (token, false, true)
      case s"/${rest}" => throw new Exception(s"Invalid named type reference: ${typeUri}")
      case token => (token, false, false)
    }

    val typeToken = escapedTypeToken.replace("%2F", "/") // TODO: Proper URL unescaping ?
    val uniformedTypeToken = typeToken.toLowerCase
    
    val typeCoordinates = parseTypeToken(typeToken, packageInfo.moduleToPackageParts)

    lazy val hasResourceDefinition = packageInfo.resourceTypeTokens.contains(uniformedTypeToken)
    lazy val hasObjectTypeDefintion = packageInfo.objectTypeTokens.contains(uniformedTypeToken)
    lazy val hasEnumTypeDefintion = packageInfo.enumTypeTokens.contains(uniformedTypeToken)

    // def resourceClassCoordinates(rawTypeToken: String): Option[ClassCoordinates] = {
    //       val typeToken = escapedTypeToken.replace("%2F", "/") // TODO: Proper URL unescaping ?
    //   val uniformedTypeToken = typeToken.toLowerCase

    //   val typeCoordinates = parseTypeToken(typeToken, packageInfo.moduleToPackageParts)
    // }

    def resourceClassCoordinates = {
      if (hasResourceDefinition) {
        Some(typeCoordinates.asResourceClass(asArgsType = asArgsType))
      } else {
        None
      }
    }

    def objectClassCoordinates = {
      if (hasObjectTypeDefintion) {
        Some(typeCoordinates.asObjectClass(asArgsType = asArgsType))
      } else if (hasEnumTypeDefintion) {
        Some(typeCoordinates.asEnumClass)
      } else {
        None
      }
    }

    val classCoordinates =
      if (isFromResourceUri) {
        resourceClassCoordinates
      } else if (isFromTypeUri) {
        objectClassCoordinates
      } else { //resourceClassCoordinates.orElse(objectClassCoordinates)
        (resourceClassCoordinates, objectClassCoordinates) match {
          case (Some(coordinates), None) => Some(coordinates)
          case (None, Some(coordinates)) => Some(coordinates)
          case (None, None) => None
          case _ => throw new Exception(s"Type URI ${typeUri} can refer to both a resource or an object type")
        }
      }
    

    // val classCoordinates = ((isFromResourceUri, isFromTypeUri) : @unchecked) match {
    //   case (true, false) =>
    //     if (hasResourceDefinition) {
    //       Some(typeCoordinates.asResourceClass(asArgsType = asArgsType))
    //     } else {
    //       None
    //       // throw new Exception(s"Nonexistent type token for a resource reference: ${typeToken}")
    //     }
    //   case (false, true) =>
    //     if (hasObjectTypeDefintion) {
    //       Some(typeCoordinates.asObjectClass(asArgsType = asArgsType))
    //     } else if (hasEnumTypeDefintion) {
    //       Some(typeCoordinates.asEnumClass)
    //     } else {
    //       throw new Exception(s"Nonexistent type token for a type reference: ${typeToken}")
    //     }
    //   case (false, false) =>
    //     (hasResourceDefinition, hasObjectTypeDefintion, hasEnumTypeDefintion) match {
    //       case (true, false, false) =>
    //         logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be a resource type")
    //         typeCoordinates.asResourceClass(asArgsType = asArgsType)
    //       case (false, true, false) =>
    //         logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be an object type")
    //         typeCoordinates.asObjectClass(asArgsType = asArgsType)
    //       case (false, false, true) =>
    //         logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be an enum type")
    //         typeCoordinates.asEnumClass
    //       case (false, false, false) =>
    //         logger.warn(s"Named type reference: ${typeUri} has an invalid format and is not defined in the current schema - using underlying primitive type as fallback")
    //         asScalaType(underlyingType.get, asArgsType = asArgsType)
    //       case _ =>
    //         throw new Exception(s"Named type reference: ${typeToken} has an invalid format but no heristic for it was found")
    //     }
    // }

    classCoordinates.map(_.fullyQualifiedTypeRef)
  }

  // private def getType(
  //   typePath: String,
  //   packageInfo: PulumiPackageInfo,
  //   namedTypeDefinition: NamedType,
  //   asArgsType: Boolean
  // ) = {
  //   val (escapedTypeToken, isFromTypeUri, isFromResourceUri) = typePath match {
  //     case s"/types/${token}" => (token, true, false)
  //     case s"/resources/${token}" => (token, false, true)
  //     case s"/${rest}" => throw new Exception(s"Invalid named type path: ${typePath}")
  //     case token => (token, false, false)
  //   }

  //   val typeToken = escapedTypeToken.replace("%2F", "/").toLowerCase // TODO: Proper URL unescaping ?

  //   val typeCoordinates = parseTypeToken(typeToken, packageInfo.moduleToPackageParts)

  //   lazy val hasResourceDefinition = packageInfo.resourceTypeTokens.contains(typeToken)
  //   lazy val hasObjectTypeDefintion = packageInfo.objectTypeTokens.contains(typeToken)
  //   lazy val hasEnumTypeDefintion = packageInfo.enumTypeTokens.contains(typeToken)

  //   val classCoordinates: ClassCoordinates = (isFromResourceUri, isFromTypeUri) match {
  //     case (true, false) =>
  //       if (hasResourceDefinition) {
  //         typeCoordinates.asResourceClass(asArgsType = asArgsType)
  //       } else {
  //         throw new Exception(s"Nonexistent resource token: ${typeToken}")
  //       }
  //     case (false, true) =>
  //       if (hasEnumTypeDefintion) {
  //         typeCoordinates.asObjectClass(asArgsType = asArgsType)
  //       } else if (hasEnumTypeDefintion) {
  //         typeCoordinates.asEnumClass
  //       } else {
  //         throw new Exception(s"Nonexistent type token: ${typeToken}")
  //       }
  //     case (false, false) =>
  //       (hasResourceDefinition, hasObjectTypeDefintion, hasEnumTypeDefintion) match {
  //         case (true, false, false) =>
  //           // (Some(typeCoordinates.asResourceClass(asArgsType = asArgsType)), Some("invalid format - assuming it to be a resource type"))
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be a resource type")
  //           typeCoordinates.asResourceClass(asArgsType = asArgsType)
  //         case (false, true, false) =>
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be an object type")
  //           typeCoordinates.asObjectClass(asArgsType = asArgsType)
  //         case (false, false, true) =>
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be an enum type")
  //           typeCoordinates.asEnumClass
  //         case (false, false, false) =>
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format and is not defined in the current schema - using underlying primitive type as fallback")
  //           asScalaType(namedTypeDefinition.`type`.get, asArgsType = asArgsType)
  //         case _ =>

  //           throw new Exception(s"Named type reference: ${typeToken} has an invalid format but no heristic for it was found")
  //       }
  //   }


  //   classCoordinates.fullyQualifiedTypeRef
  // }

  // private def scalaTypeFromTypeToken(
  //   typeToken: String,
  //   packageInfo: PulumiPackageInfo,
  //   namedTypeDefinition: NamedType,
  //   isFromTypeUri: Boolean,
  //   isFromResourceUri: Boolean,
  //   asArgsType: Boolean
  // ): (Option[Type], Option[String]) = {
  //   val typeCoordinates = parseTypeToken(typeToken, packageInfo.moduleToPackageParts)

  //   lazy val hasResourceDefinition = packageInfo.resourceTypeTokens.contains(typeToken)
  //   lazy val hasObjectTypeDefintion = packageInfo.objectTypeTokens.contains(typeToken)
  //   lazy val hasEnumTypeDefintion = packageInfo.enumTypeTokens.contains(typeToken)

  //   val classCoordinates = (isFromResourceUri, isFromTypeUri) match {
  //     case (true, false) =>
  //       if (hasResourceDefinition) {
  //         typeCoordinates.asResourceClass(asArgsType = asArgsType)
  //       } else {
  //         throw new Exception(s"Nonexistent resource token: ${typeToken}")
  //       }
  //     case (false, true) =>
  //       if (hasEnumTypeDefintion) {
  //         typeCoordinates.asObjectClass(asArgsType = asArgsType)
  //       } else if (hasEnumTypeDefintion) {
  //         typeCoordinates.asEnumClass
  //       } else {
  //         throw new Exception(s"Nonexistent type token: ${typeToken}")
  //       }
  //     case (false, false) =>
  //       (hasResourceDefinition, hasObjectTypeDefintion, hasEnumTypeDefintion) match {
  //         case (true, false, false) =>
  //           (Some(typeCoordinates.asResourceClass(asArgsType = asArgsType)), Some("invalid format - assuming it to be a resource type"))
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be a resource type")
  //           typeCoordinates.asResourceClass(asArgsType = asArgsType)
  //         case (false, true, false) =>
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be an object type")
  //           typeCoordinates.asObjectClass(asArgsType = asArgsType)
  //         case (false, false, true) =>
  //           logger.warn(s"Named type reference: ${typeToken} has an invalid format - assuming it to be an enum type")
  //           typeCoordinates.asEnumClass
  //         case (false, false, false) =>
  //           logger.warn(s"Named type reference: ${typeUri} has an invalid format and is not defined in the current schema - using underlying primitive type as fallback")
  //           asScalaType(namedTypeDefinition.`type`.get, asArgsType = asArgsType)
  //         case _ =>

  //           throw new Exception(s"Named type reference: ${typeToken} has an invalid format but no heristic for it was found")
  //       }
  //   }


  //   classCoordinates.fullyQualifiedTypeRef
  // }

  def asScalaType(typeRef: TypeReference, asArgsType: Boolean): Type = typeRef match {
    case BooleanType => t"Boolean"
    case StringType => t"String"
    case IntegerType => t"Int"
    case NumberType => t"Double"
    case UrnType => t"besom.util.Types.URN"
    case ResourceIdType => t"besom.util.Types.ResourceId"
    case ArrayType(elemType) => t"scala.collection.immutable.List[${asScalaType(elemType, asArgsType)}]"
    case MapType(elemType) => t"scala.Predef.Map[String, ${asScalaType(elemType, asArgsType)}]"
    case unionType: UnionType =>
      unionType.oneOf.map(asScalaType(_, asArgsType)).reduce{ (t1, t2) => t"$t1 | $t2"}
    case namedType: NamedType =>
      namedType.typeUri match {
        case "pulumi.json#/Archive" =>
          t"besom.internal.Archive"
        case "pulumi.json#/Asset" =>
          t"besom.internal.Asset"
        case "pulumi.json#/Any" =>
          t"besom.types.PulumiAny"
        case "pulumi.json#/Json" =>
          t"besom.types.PulumiJson"

        case typeUri =>
          scalaTypeFromTypeUri(typeUri, asArgsType = asArgsType, underlyingType = namedType.`type`)
            .getOrElse {
              logger.warn(s"Type URI ${typeUri} has no corresponding type definition - using its underlying type as fallback")
              val underlyingType = namedType.`type`.getOrElse(
                throw new Exception(s"Type with URI ${typeUri} has no underlying primitive type to be used as fallback")
              )
              asScalaType(underlyingType, asArgsType = asArgsType)
            }

          // // Example URIs:
          // // "/provider/vX.Y.Z/schema.json#/types/pulumi:type:token"
          // // #/types/kubernetes:rbac.authorization.k8s.io%2Fv1beta1:Subject
          // // "aws:iam/documents:PolicyDocument"

          // val (fileUri, typePath) = typeUri.replace("%2F", "/").split("#") match {
          //   case Array(typePath) => ("", typePath)
          //   case Array(fileUri, typePath) => (fileUri, typePath)
          //   case _ => throw new Exception(s"Unexpected type URI format: ${typeUri}") 
          // }

          // //assert(fileUri == "", s"Invalid type URI: $typeUri - referencing other schemas is not supported")

          // val packageInfo = fileUri match {
          //   case "" =>
          //     defaultPackageInfo
          //   case s"/${providerName}/v${schemaVersion}/schema.json" =>
          //     schemaProvider.packageInfo(providerName = providerName, schemaVersion = schemaVersion)
          // }

          // val (escapedTypeToken, isFromTypeUri, isFromResourceUri) = typePath match {
          //   case s"/types/${token}" => (token, true, false)
          //   case s"/resources/${token}" => (token, false, true)
          //   case s"/${rest}" => throw new Exception(s"Invalid named type reference: ${typeUri}")
          //   case token => (token, false, false)
          // }

          // val typeToken = escapedTypeToken.replace("%2F", "/").toLowerCase // TODO: Proper URL unescaping ?



          // // scalaTypeFromTypeToken(token, packageInfo, isFromTypeUri = isFromTypeUri, isFromResourceUri = isFromResourceUri, asArgsType = asArgsType)


          // // typePath match {
          // //   case s"/types/${token}" =>
          // //     scalaTypeFromTypeToken(token, packageInfo, isFromTypeUri = true, isFromResourceUri = false, asArgsType = asArgsType)
          // //   case s"/resources/${token}" =>
          // //     scalaTypeFromTypeToken(token, packageInfo, isFromTypeUri = false, isFromResourceUri = true, asArgsType = asArgsType)
          // //   case s"/${rest}" =>
          // //     throw new Exception(s"Invalid named type reference: ${typeUri}")
          // //   case token =>
          // //     scalaTypeFromTypeToken(token, packageInfo, isFromTypeUri = false, isFromResourceUri = false, asArgsType = asArgsType)

          // //     // if (objectTypeTokens.contains(token) || enumTypeTokens.contains(token)) {
          // //     //   logger.warn(s"Named type reference: ${typeUri} has an invalid format - assuming '#/types/' prefix")
          // //     //   scalaTypeFromTypeToken(token, isFromTypeUri = false, isFromResourceUri = false, asArgsType = asArgsType)
          // //     // } else {
          // //     //   logger.warn(s"Named type reference: ${typeUri} has an invalid format and is not defined in the current schema - using underlying primitive type as fallback")
          // //     //   asScalaType(namedType.`type`.get, asArgsType = asArgsType)
          // //     // }
          // // }
      }
  }
}