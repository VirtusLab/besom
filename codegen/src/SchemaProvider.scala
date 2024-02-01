package besom.codegen

import besom.codegen.PackageVersion.PackageVersionOps
import besom.codegen.Utils.PulumiPackageOps
import besom.codegen.metaschema.*
import besom.codegen.{PackageVersion, SchemaFile, SchemaName}

import scala.collection.mutable.ListBuffer

type SchemaFile = os.Path

trait SchemaProvider {
  def packageInfo(metadata: PackageMetadata, schema: Option[SchemaFile] = None): (PulumiPackage, PulumiPackageInfo)
  def packageInfo(metadata: PackageMetadata, pulumiPackage: PulumiPackage): (PulumiPackage, PulumiPackageInfo)
  def dependencies(schemaName: SchemaName, packageVersion: PackageVersion): List[(SchemaName, PackageVersion)]
}

class DownloadingSchemaProvider(schemaCacheDirPath: os.Path)(implicit logger: Logger) extends SchemaProvider {

  private val schemaFileName = "schema.json"
  private val packageInfos: collection.mutable.Map[(SchemaName, PackageVersion), PulumiPackageInfo] =
    collection.mutable.Map.empty

  private def pulumiPackage(metadata: PackageMetadata): (PulumiPackage, PackageMetadata) = {
    val schemaFilePath = schemaCacheDirPath / metadata.name / metadata.version.orDefault.asString / schemaFileName

    if (!os.exists(schemaFilePath)) {
      logger.debug(
        s"Downloading schema for '${metadata.name}:${metadata.version.orDefault}' into '${schemaFilePath.relativeTo(os.pwd)}'"
      )

      val schemaSource =
        if (metadata.version.isDefault)
          metadata.name
        else
          s"${metadata.name}@${metadata.version.orDefault}"

      val installCmd: List[String] =
        List("pulumi", "--non-interactive", "--logtostderr", "plugin", "install", "resource", metadata.name) ++ {
          // use version only if it is not the default, otherwise try to install the latest
          if (metadata.version.isDefault)
            List.empty
          else
            List(metadata.version.get.asString)
        } ++ {
          // use server only if it is defined
          metadata.server.map(url => List("--server", url)).getOrElse(List.empty)
        }
      try {
        os.proc(installCmd).call()
      } catch {
        case e: os.SubprocessException =>
          val msg = s"Failed to install plugin '${e.result.command.mkString(" ")}' using Pulumi CLI"
          logger.error(msg)
          throw GeneralCodegenException(msg, e)
      }

      val schema =
        try {
          os.proc("pulumi", "--non-interactive", "--logtostderr", "package", "get-schema", schemaSource).call().out.text()
        } catch {
          case e: os.SubprocessException =>
            val msg =
              s"Failed to download schema '${e.result.command.mkString(" ")}' into '${schemaFilePath.relativeTo(os.pwd)}'"
            logger.error(msg)
            throw GeneralCodegenException(msg, e)
        }

      // parse and save the schema using path corrected for the actual name and version for the package
      val pulumiPackage         = PulumiPackage.fromString(schema)
      val reconciled            = pulumiPackage.toPackageMetadata(metadata)
      val correctSchemaFilePath = schemaCacheDirPath / reconciled.name / reconciled.version.orDefault.asString / schemaFileName
      os.write.over(correctSchemaFilePath, schema, createFolders = true)
      (pulumiPackage, reconciled)
    } else {
      logger.debug(
        s"Using cached schema for ${metadata.name}:${metadata.version.orDefault}' from '${schemaFilePath.relativeTo(os.pwd)}'"
      )
      (PulumiPackage.fromFile(schemaFilePath), metadata)
    }
  }

  private def pulumiPackage(metadata: PackageMetadata, schema: SchemaFile): (PulumiPackage, PackageMetadata) = {
    // parse and save the schema using path corrected for the actual name and version for the package
    val pulumiPackage         = PulumiPackage.fromFile(schema)
    val reconciled            = pulumiPackage.toPackageMetadata(metadata)
    val correctSchemaFilePath = schemaCacheDirPath / reconciled.name / reconciled.version.orDefault.asString / schemaFileName
    os.copy.over(schema, correctSchemaFilePath, replaceExisting = true, createFolders = true)
    (pulumiPackage, metadata)
  }

  def dependencies(schemaName: SchemaName, packageVersion: PackageVersion): List[(SchemaName, PackageVersion)] =
    packageInfos.keys.filterNot { case (name, _) => name == schemaName }.toList

  def packageInfo(metadata: PackageMetadata, schema: Option[SchemaFile] = None): (PulumiPackage, PulumiPackageInfo) = {
    val (pulumiPackage, packageMetadata) = (metadata, schema) match {
      case (m, Some(schema))          => this.pulumiPackage(m, schema)
      case (m: PackageMetadata, None) => this.pulumiPackage(m)
    }
    packageInfo(packageMetadata, pulumiPackage)
  }

  def packageInfo(
    metadata: PackageMetadata,
    pulumiPackage: PulumiPackage
  ): (PulumiPackage, PulumiPackageInfo) = {
    val info = packageInfos.getOrElseUpdate(
      (metadata.name, metadata.version.orDefault),
      reconcilePackageInfo(pulumiPackage, metadata)
    )
    (pulumiPackage, info)
  }

  private def reconcilePackageInfo(
    pulumiPackage: PulumiPackage,
    packageMetadata: PackageMetadata
  ): PulumiPackageInfo = {
    if (pulumiPackage.name != packageMetadata.name) {
      logger.warn(
        s"Package name mismatch for '${packageMetadata.name}' != '${pulumiPackage.name}', " +
          s"will be reconciled - this is fine in tests"
      )
    }

    // pre-process the package to gather information about types, that are used later during various parts of codegen
    // most notable place is TypeMapper.scalaTypeFromTypeUri
    val enumTypeTokensBuffer     = ListBuffer.empty[String]
    val objectTypeTokensBuffer   = ListBuffer.empty[String]
    val resourceTypeTokensBuffer = ListBuffer.empty[String]

    val enumValueToInstancesBuffer = ListBuffer.empty[(PulumiToken, Map[ConstValue, EnumInstanceName])]

    def valueToInstances(enumDefinition: EnumTypeDefinition): Map[ConstValue, EnumInstanceName] =
      enumDefinition.`enum`.map { (valueDefinition: EnumValueDefinition) =>
        val caseRawName: EnumInstanceName = valueDefinition.name.getOrElse {
          valueDefinition.value match {
            case StringConstValue(value) => value
            case const                   => throw GeneralCodegenException(s"The name of enum cannot be derived from value ${const}")
          }
        }
        valueDefinition.value -> caseRawName
      }.toMap

    // Post-process the tokens to unify them to lower case to circumvent inconsistencies in low quality schemas (e.g. aws)
    // This allows us to use case-insensitive matching when looking up tokens
    pulumiPackage.parsedTypes.foreach {
      case (coordinates, definition: EnumTypeDefinition) =>
        enumValueToInstancesBuffer += coordinates.token -> valueToInstances(definition)

        if (enumTypeTokensBuffer.contains(coordinates.token.asLookupKey))
          logger.warn(s"Duplicate enum type token '${coordinates.token.asLookupKey}' in package '${packageMetadata.name}'")
        enumTypeTokensBuffer += coordinates.token.asLookupKey
      case (coordinates, _: ObjectTypeDefinition) =>
        if (objectTypeTokensBuffer.contains(coordinates.token.asLookupKey))
          logger.warn(s"Duplicate object type token '${coordinates.token.asLookupKey}' in package '${packageMetadata.name}'")
        objectTypeTokensBuffer += coordinates.token.asLookupKey
    }
    pulumiPackage.parsedResources.foreach { case (coordinates, _: ResourceDefinition) =>
      if (resourceTypeTokensBuffer.contains(coordinates.token.asLookupKey))
        logger.warn(s"Duplicate resource type token '${coordinates.token.asLookupKey}' in package '${packageMetadata.name}'")
      resourceTypeTokensBuffer += coordinates.token.asLookupKey
    }

    val reconciledMetadata = pulumiPackage.toPackageMetadata(packageMetadata)
    PulumiPackageInfo(
      name = reconciledMetadata.name,
      version = reconciledMetadata.version.orDefault,
      enumTypeTokens = enumTypeTokensBuffer.toSet,
      objectTypeTokens = objectTypeTokensBuffer.toSet,
      providerTypeToken = pulumiPackage.providerTypeToken,
      resourceTypeTokens = resourceTypeTokensBuffer.toSet,
      moduleToPackageParts = pulumiPackage.moduleToPackageParts,
      providerToPackageParts = pulumiPackage.providerToPackageParts,
      enumValueToInstances = enumValueToInstancesBuffer.toMap
    )
  }
}
