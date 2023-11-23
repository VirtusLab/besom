package besom.codegen

import scala.meta._
import scala.meta.dialects.Scala33
import besom.codegen.metaschema._

import scala.annotation.unused

/** Pulumi Type Ref is a reference to a type in this or another schema document. NOT to be confused with Scala Type Ref.
  *
  * A type from this document is referenced as "#/types/pulumi:type:token". A type from another document is referenced as
  * "path#/types/pulumi:type:token", where path is of the form: "/provider/vX.Y.Z/schema.json" or "pulumi.json" or "http[s]://example.com
  * /provider/vX.Y.Z/schema.json".
  *
  * A resource from this document is referenced as “#/resources/pulumi:type:token”. A resource from another document is referenced as
  * "path#/resources/pulumi:type:token", where path is of the form: "/provider/vX.Y.Z/schema.json" or "pulumi.json" or
  * "http[s]://example.com /provider/vX.Y.Z/schema.json".
  *
  * Example URIs available in TypeMapper.test.scala
  */
sealed trait PulumiTypeReference {
  def asScalaType(asArgsType: Boolean): Either[Exception, Type] = asScalaType(asArgsType)
  def asScalaType: Either[Exception, Type]                      = asScalaType(asArgsType = false)
}
object PulumiTypeReference {
  def from(
    typeRef: TypeReference
  )(implicit thisPackageInfo: ThisPackageInfo, schemaProvider: SchemaProvider, logger: Logger): PulumiTypeReference = {
    typeRef match {
      case anonymousType: AnonymousType => anonymousType.asPulumiTypeReference
      case namedType: NamedType         => namedType.asPulumiTypeReference
    }
  }

  private def fromNamedType(
    namedType: NamedType
  )(implicit thisPackageInfo: ThisPackageInfo, schemaProvider: SchemaProvider, logger: Logger): PulumiTypeReference = {
    val fallback = namedType.`type`.map(_.asPulumiTypeReference)
    unescape(namedType.typeUri) match {
      case s"pulumi.json#/${name}" => BuiltinTypeReference(name)
      case typeRefUri =>
        typeRefUri.split("#").filterNot(_.isBlank) match {
          case Array(typePath) => // JSON Schema Pointer like reference
            InternalTypeReference.from(typePath, fallback)
          case Array(fileUri, typePath) => // Reference to external schema
            ExternalTypeReference.from(fileUri, typePath, fallback)
        }
    }
  }

  private def unescape(value: String) = {
    value.replace("%2F", "/") // TODO: Proper URL un-escaping
  }

  implicit class AnonymousTypeOps(anonymousType: AnonymousType) {
    def asPulumiTypeReference(implicit
      thisPackageInfo: ThisPackageInfo,
      schemaProvider: SchemaProvider,
      logger: Logger
    ): PulumiTypeReference =
      AnonymousTypeReference.from(anonymousType)

    def asScalaType(asArgsType: Boolean = false)(implicit
      thisPackageInfo: ThisPackageInfo,
      schemaProvider: SchemaProvider,
      logger: Logger
    ): Either[Exception, Type] =
      asPulumiTypeReference.asScalaType(asArgsType)
  }

  implicit class NamedTypeOps(namedType: NamedType) {
    def asPulumiTypeReference(implicit
      thisPackageInfo: ThisPackageInfo,
      schemaProvider: SchemaProvider,
      logger: Logger
    ): PulumiTypeReference =
      PulumiTypeReference.fromNamedType(namedType)

    def asScalaType(
      asArgsType: Boolean = false
    )(implicit
      thisPackageInfo: ThisPackageInfo,
      schemaProvider: SchemaProvider,
      logger: Logger
    ): Either[Exception, Type] =
      asPulumiTypeReference.asScalaType(asArgsType)
  }

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asPulumiTypeReference(implicit
      thisPackageInfo: ThisPackageInfo,
      schemaProvider: SchemaProvider,
      logger: Logger
    ): PulumiTypeReference =
      PulumiTypeReference.from(typeRef)

    def asScalaType(
      asArgsType: Boolean = false
    )(implicit thisPackageInfo: ThisPackageInfo, schemaProvider: SchemaProvider, logger: Logger): Either[Exception, Type] =
      asPulumiTypeReference.asScalaType(asArgsType)

    def asString: String = {
      typeRef match {
        case named @ NamedType(_, _)  => s"Named['${named.typeUri}' | ${named.`type`.map(_.asString)}]"
        case union: UnionType         => s"Union[${union.oneOf.map(_.asString).mkString(", ")} | ${union.`type`.map(_.asString)}]"
        case array: ArrayType         => s"Array[${array.items.asString}]"
        case map: MapType             => s"Map[String, ${map.additionalProperties.asString}]"
        case anonymous: AnonymousType => anonymous.getClass.getSimpleName.stripSuffix("$")
      }
    }
  }
}

sealed trait PulumiTypeTokenReference extends PulumiTypeReference {

  import PulumiTypeTokenReference._

  def token: PulumiToken
  def path: Path
  def fallback: Option[PulumiTypeReference]

  protected def packageInfo: PulumiPackageInfo

  protected implicit val thisPackageInfo: ThisPackageInfo
  protected implicit val schemaProvider: SchemaProvider
  protected implicit val logger: Logger

  override def asScalaType(asArgsType: Boolean = false): Either[Exception, Type] = {
    val typeCoordinates = PulumiDefinitionCoordinates.fromToken(
      token,
      packageInfo.moduleToPackageParts,
      packageInfo.providerToPackageParts
    )

    // this has to be original not normalized token from schema otherwise it will break the codegen
    // it is not ideal, and we might look into normalizing the token in the future, but for not it is what it is
    val uniformedTypeToken           = token.asLookupKey
    lazy val hasProviderDefinition   = packageInfo.providerTypeToken == uniformedTypeToken
    lazy val hasResourceDefinition   = packageInfo.resourceTypeTokens.contains(uniformedTypeToken)
    lazy val hasObjectTypeDefinition = packageInfo.objectTypeTokens.contains(uniformedTypeToken)
    lazy val hasEnumTypeDefinition   = packageInfo.enumTypeTokens.contains(uniformedTypeToken)
    if (logger.printLevel == Logger.Level.Debug) {
      logger.debug(
        s"asScalaType: token: '${token.asString}', path: '${path}', asArgsType: '${asArgsType}', fallbackType: '${fallback}', " +
          s"uniformedTypeToken: '${uniformedTypeToken}', hasProviderDefinition: '${hasProviderDefinition}', " +
          s"hasResourceDefinition: '${hasResourceDefinition}', hasObjectTypeDefinition: '${hasObjectTypeDefinition}', " +
          s"hasEnumTypeDefinition: '${hasEnumTypeDefinition}', package: '${packageInfo.asPackageMetadata}'"
      )
    }

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

    val classCoordinates: Option[ScalaDefinitionCoordinates] =
      path match {
        case Path.Resource => resourceClassCoordinates
        case Path.Provider => resourceClassCoordinates
        case Path.Type     => objectClassCoordinates
        case Path.Unspecified =>
          (resourceClassCoordinates, objectClassCoordinates) match {
            case (Some(coordinates), None) =>
              logger.warn(
                s"Assuming a '/resources/` prefix for type, token: '${token}', path: '${path}'"
              )
              Some(coordinates)
            case (None, Some(coordinates)) =>
              logger.warn(s"Assuming a '/types/` prefix for type, token: '${token}', path: '${path}'")
              Some(coordinates)
            case (None, None) =>
              logger.debug(s"Found no type definition for named type, token: '${token}', path: '${path}'")
              None
            case _ =>
              throw TypeError(
                s"Type URI can refer to both a resource or an object type, token: '${token}', path: '${path}'"
              )
          }
      }

    classCoordinates.map(_.fullyQualifiedTypeRef) match {
      case Some(ref) => Right(ref)
      case None =>
        fallback.map(_.asScalaType(asArgsType)) match {
          case Some(ref) => ref
          case None      => Left(TypeError(s"Could not find a type definition for '${this}', token: '${token.asString}', path: '${path}'"))
        }
    }
  }
}
object PulumiTypeTokenReference {
  sealed trait Path
  object Path {
    case object Type extends Path
    case object Resource extends Path
    case object Provider extends Path
    case object Unspecified extends Path
  }
}

trait PulumiTypeTokenReferenceParser {
  import besom.codegen.PulumiTypeTokenReference.Path

  protected def parseTypePath(
    typePath: String,
    packageInfo: PulumiPackageInfo
  ): (PulumiToken, Path) = {
    typePath match {
      case s"/provider"           => (PulumiToken(packageInfo.providerTypeToken), Path.Provider)
      case s"/types/${token}"     => (PulumiToken(token), Path.Type)
      case s"/resources/${token}" => (PulumiToken(token), Path.Resource)
      case s"/${rest}"            => throw TypeError(s"Invalid named type reference, typePath: '${typePath}', rest: '${rest}''")
      case token                  => (PulumiToken(token), Path.Unspecified)
    }
  }
}

case class ExternalTypeReference(
  token: PulumiToken,
  path: PulumiTypeTokenReference.Path,
  fallback: Option[PulumiTypeReference],
  dependency: PackageMetadata
)(
  protected val packageInfo: PulumiPackageInfo
)(implicit
  protected val thisPackageInfo: ThisPackageInfo,
  protected val schemaProvider: SchemaProvider,
  protected val logger: Logger
) extends PulumiTypeTokenReference
object ExternalTypeReference extends PulumiTypeTokenReferenceParser {
  type SchemaPath = String
  object SchemaPath {
    def apply(path: String): SchemaPath = path.trim match {
      case ""   => throw TypeError(s"Unexpected empty SchemaPath")
      case path => path
    }
  }
  implicit class SchemaPathOps(schemaPath: SchemaPath) {
    private[codegen] def toMetadata: PackageMetadata = {
      schemaPath.trim match {
        case s"/${providerName}/v${schemaVersion}/schema.json" =>
          PackageMetadata(providerName, schemaVersion)
        case s"${protocol}://${host}/${providerName}/v${schemaVersion}/schema.json" =>
          PackageMetadata(providerName, schemaVersion).withUrl(s"${protocol}://${host}/${providerName}")
        case _ => throw TypeError(s"Unexpected external type schema URI format: '${schemaPath}'")
      }
    }
  }

  private[codegen] def from(fileUri: String, typePath: String, fallback: Option[PulumiTypeReference])(implicit
    thisPackageInfo: ThisPackageInfo,
    schemaProvider: SchemaProvider,
    logger: Logger
  ): ExternalTypeReference = {
    val packageMetadata = SchemaPath(fileUri).toMetadata
    val packageInfo     = schemaProvider.packageInfo(packageMetadata)._2
    val (token, path)   = parseTypePath(typePath, packageInfo)
    ExternalTypeReference(token, path, fallback, packageMetadata)(packageInfo)
  }
}

case class InternalTypeReference(
  token: PulumiToken,
  path: PulumiTypeTokenReference.Path,
  fallback: Option[PulumiTypeReference]
)(
  protected val packageInfo: PulumiPackageInfo
)(implicit
  protected val thisPackageInfo: ThisPackageInfo,
  protected val schemaProvider: SchemaProvider,
  protected val logger: Logger
) extends PulumiTypeTokenReference
object InternalTypeReference extends PulumiTypeTokenReferenceParser {
  private[codegen] def from(
    typePath: String, fallback: Option[PulumiTypeReference]
  )(implicit thisPackageInfo: ThisPackageInfo, schemaProvider: SchemaProvider, logger: Logger): InternalTypeReference = {
    val (token, path) = parseTypePath(typePath, thisPackageInfo.instance)
    InternalTypeReference(token, path, fallback)(thisPackageInfo.instance)
  }
}

case class BuiltinTypeReference(`type`: BuiltinTypeReference.Builtin) extends PulumiTypeReference {
  override def asScalaType(@unused unused: Boolean = false): Either[Exception, Type] =
    `type` match {
      case "Archive" => Right(t"besom.types.Archive")
      case "Asset"   => Right(t"besom.types.AssetOrArchive")
      case "Any"     => Right(t"besom.types.PulumiAny")
      case "Json"    => Right(t"besom.types.PulumiJson")
      case _         => Left(TypeError(s"Unexpected builtin type: '${`type`}'"))
    }
}
object BuiltinTypeReference {
  type Builtin = String
}

case class AnonymousTypeReference(
  typeRef: AnonymousType
)(implicit
  thisPackageInfo: ThisPackageInfo,
  schemaProvider: SchemaProvider,
  logger: Logger
) extends PulumiTypeReference {
  import PulumiTypeReference.AnonymousTypeOps
  import PulumiTypeReference.NamedTypeOps
  import PulumiTypeReference.TypeReferenceOps

  private def fallbackType: Option[PulumiTypeReference] = typeRef match {
    case UnionType(_, fallback, _) => fallback.map(_.asPulumiTypeReference)
    case _                         => None
  }

  override def asScalaType(asArgsType: Boolean = false): Either[Exception, Type] = {
    typeRef match {
      case BooleanType         => Right(t"Boolean")
      case StringType          => Right(t"String")
      case IntegerType         => Right(t"Int")
      case NumberType          => Right(t"Double")
      case UrnType             => Right(t"besom.types.URN")
      case ResourceIdType      => Right(t"besom.types.ResourceId")
      case ArrayType(elemType) => elemType.asScalaType(asArgsType).map(t => t"scala.collection.immutable.List[${t}]")
      case MapType(elemType)   => elemType.asScalaType(asArgsType).map(t => t"scala.Predef.Map[String, ${t}]")
      case unionType: UnionType => {
        val (errs, refs) = unionType.oneOf
          .map {
            case anonymous: AnonymousType => anonymous.asScalaType(asArgsType)
            case named @ NamedType(_, _)  => named.asScalaType(asArgsType)
          }
          .partitionMap(identity)

        def reduceTypes(t1: Type, t2: Type): Type = if (t1.syntax == t2.syntax) t1 else Type.ApplyInfix(t1, Type.Name("|"), t2)

        val maybeUnion = (errs, refs) match {
          case (_, refs) if refs.nonEmpty => Right(refs.reduce(reduceTypes))
          case (err :: Nil, _)            => Left(err)
          case (errs, _)                  => Left(AggregateCodegenError(errs: _*))
        }

        maybeUnion match {
          case Right(union) => Right(union)
          case Left(e1) =>
            fallbackType match {
              case None => Left(e1)
              case Some(ref) =>
                ref.asScalaType(asArgsType) match {
                  case Left(e)      => Left(AggregateCodegenError(e1, e))
                  case Right(value) => Right(value)
                }
            }
        }
      }
      case _ =>
        fallbackType match {
          case Some(ref) => ref.asScalaType(asArgsType)
          case None      => Left(TypeError(s"Unexpected anonymous type and no fallback: '${typeRef}'"))
        }
    }
  }
}
object AnonymousTypeReference {
  def from(
    typeRef: AnonymousType
  )(implicit thisPackageInfo: ThisPackageInfo, schemaProvider: SchemaProvider, logger: Logger): AnonymousTypeReference =
    AnonymousTypeReference(typeRef)
}
