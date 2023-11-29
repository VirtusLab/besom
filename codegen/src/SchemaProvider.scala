package besom.codegen

import scala.collection.mutable.ListBuffer
import besom.codegen.metaschema.*
import besom.codegen.PackageMetadata.{SchemaFile, SchemaName}
import besom.codegen.PackageVersion
import besom.codegen.Utils.PulumiPackageOps
import besom.codegen.PackageVersion.PackageVersionOps

trait SchemaProvider {
  def packageInfo(metadata: PackageMetadata, schema: Option[SchemaFile] = None): (PulumiPackage, PulumiPackageInfo)
  def packageInfo(packageMetadata: PackageMetadata, pulumiPackage: PulumiPackage): (PulumiPackage, PulumiPackageInfo)
  def dependencies(schemaName: SchemaName, packageVersion: PackageVersion): List[(SchemaName, PackageVersion)]
}

class DownloadingSchemaProvider(schemaCacheDirPath: os.Path)(implicit logger: Logger) extends SchemaProvider {

  private val schemaFileName = "schema.json"
  private val packageInfos: collection.mutable.Map[(SchemaName, PackageVersion), PulumiPackageInfo] =
    collection.mutable.Map.empty

  private def pulumiPackage(metadata: PackageMetadata): (PackageMetadata, PulumiPackage) = {
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
        List("pulumi", "plugin", "install", "resource", metadata.name) ++ {
          // use version only if it is not the default, otherwise try to install the latest
          metadata.version.map(_.asString).map(List(_)).getOrElse(List.empty)
        } ++ {
          // use server only if it is defined
          metadata.server.map(url => List("--server", url)).getOrElse(List.empty)
        }
      try {
        os.proc(installCmd).call()
      } catch {
        case e: os.SubprocessException =>
          val msg = s"Failed to install plugin '${metadata.name}:${metadata.version.getOrElse("latest")}' using Pulumi CLI"
          logger.error(msg)
          throw GeneralCodegenError(msg, e)
      }

      val schema =
        try {
          os.proc("pulumi", "package", "get-schema", schemaSource).call().out.text()
        } catch {
          case e: os.SubprocessException =>
            val msg = s"Failed to download schema for ${metadata.name}:${metadata.version.getOrElse("latest")}' using Pulumi CLI"
            logger.error(msg)
            throw GeneralCodegenError(msg, e)
        }

      // parse and save the schema using path corrected for the actual name and version for the package
      val pulumiPackage      = PulumiPackage.fromString(schema)
      val reconciledMetadata = pulumiPackage.toPackageMetadata(metadata)
      val reconciledSchemaFilePath =
        schemaCacheDirPath / reconciledMetadata.name / reconciledMetadata.version.orDefault.asString / schemaFileName
      os.write.over(reconciledSchemaFilePath, schema, createFolders = true)
      (reconciledMetadata, pulumiPackage)
    } else {
      logger.debug(
        s"Using cached schema for ${metadata.name}:${metadata.version.orDefault}' from '${schemaFilePath.relativeTo(os.pwd)}'"
      )
      (metadata, PulumiPackage.fromFile(schemaFilePath))
    }
  }

  private def pulumiPackage(metadata: PackageMetadata, schema: SchemaFile): (PackageMetadata, PulumiPackage) = {
    // parse and save the schema using path corrected for the actual name and version for the package
    val pulumiPackage      = PulumiPackage.fromFile(schema)
    val reconciledMetadata = pulumiPackage.toPackageMetadata(metadata)
    val reconciledSchemaFilePath =
      schemaCacheDirPath / reconciledMetadata.name / reconciledMetadata.version.orDefault.asString / schemaFileName
    os.copy.over(schema, reconciledSchemaFilePath, replaceExisting = true, createFolders = true)
    (reconciledMetadata, pulumiPackage)
  }

  def dependencies(schemaName: SchemaName, packageVersion: PackageVersion): List[(SchemaName, PackageVersion)] =
    packageInfos.keys.filterNot { case (name, version) =>
      name == schemaName && version == packageVersion
    }.toList

  def packageInfo(metadata: PackageMetadata, schema: Option[SchemaFile] = None): (PulumiPackage, PulumiPackageInfo) = {
    val (packageMetadata, pulumiPackage) = (metadata, schema) match {
      case (m, Some(schema))          => this.pulumiPackage(m, schema)
      case (m: PackageMetadata, None) => this.pulumiPackage(m)
    }
    packageInfo(packageMetadata, pulumiPackage)
  }

  def packageInfo(
    packageMetadata: PackageMetadata,
    pulumiPackage: PulumiPackage
  ): (PulumiPackage, PulumiPackageInfo) = {
    val info = packageInfos.getOrElseUpdate(
      (packageMetadata.name, packageMetadata.version.orDefault),
      reconcilePackageInfo(packageMetadata, pulumiPackage)
    )
    (pulumiPackage, info)
  }

  private def reconcilePackageInfo(
    packageMetadata: PackageMetadata,
    pulumiPackage: PulumiPackage
  ): PulumiPackageInfo = {
    if (pulumiPackage.name != packageMetadata.name) {
      logger.warn(
        s"Package name mismatch for '${packageMetadata.name}' != '${pulumiPackage.name}', " +
          s"will be reconciled - this is fine in tests"
      )
    }

    // pre-process the package to gather information about types, that are used later during various parts of codegen
    // most notable place is TypeMapper.scalaTypeFromTypeUri
    val enumTypeTokensBuffer   = ListBuffer.empty[String]
    val objectTypeTokensBuffer = ListBuffer.empty[String]

    // Post-process the tokens to unify them to lower case to circumvent inconsistencies in low quality schemas (e.g. aws)
    // This allows us to use case-insensitive matching when looking up tokens
    pulumiPackage.types.foreach {
      case (typeToken, _: EnumTypeDefinition) =>
        if (enumTypeTokensBuffer.contains(typeToken.toLowerCase))
          logger.warn(s"Duplicate enum type token '${typeToken}' in package '${packageMetadata.name}'")
        enumTypeTokensBuffer += typeToken.toLowerCase
      case (typeToken, _: ObjectTypeDefinition) =>
        if (objectTypeTokensBuffer.contains(typeToken.toLowerCase))
          logger.warn(s"Duplicate object type token '${typeToken}' in package '${packageMetadata.name}'")
        objectTypeTokensBuffer += typeToken.toLowerCase
    }

    val reconciledMetadata = pulumiPackage.toPackageMetadata(packageMetadata)
    PulumiPackageInfo(
      name = reconciledMetadata.name,
      version = reconciledMetadata.version.orDefault,
      enumTypeTokens = enumTypeTokensBuffer.toSet,
      objectTypeTokens = objectTypeTokensBuffer.toSet,
      providerTypeToken = pulumiPackage.providerTypeToken,
      resourceTypeTokens = pulumiPackage.resources.keySet.map(_.toLowerCase),
      moduleToPackageParts = pulumiPackage.moduleToPackageParts,
      providerToPackageParts = pulumiPackage.providerToPackageParts
    )
  }
}
