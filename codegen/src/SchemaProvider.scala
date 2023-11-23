package besom.codegen

import scala.collection.mutable.ListBuffer
import besom.codegen.metaschema._
import besom.codegen.PackageMetadata.{SchemaFile, SchemaName}
import besom.codegen.PackageVersion.PackageVersion
import besom.codegen.Utils.PulumiPackageOps
import besom.codegen.PackageVersion.PackageVersionOps

trait SchemaProvider {

  def dependencies(schemaName: SchemaName, packageVersion: PackageVersion): List[(SchemaName, PackageVersion)]

  def packageInfo(schemaOrMetadata: Either[SchemaFile, PackageMetadata]): (PulumiPackage, PulumiPackageInfo)
  def packageInfo(metadata: PackageMetadata): (PulumiPackage, PulumiPackageInfo)
}

class DownloadingSchemaProvider(schemaCacheDirPath: os.Path)(implicit logger: Logger) extends SchemaProvider {

  private val schemaFileName = "schema.json"
  private val packageInfos: collection.mutable.Map[(SchemaName, PackageVersion), PulumiPackageInfo] =
    collection.mutable.Map.empty

  private def pulumiPackage(metadata: PackageMetadata): (PulumiPackage, PackageMetadata) = {
    val schemaFilePath = schemaCacheDirPath / metadata.name / metadata.version / schemaFileName

    if (!os.exists(schemaFilePath)) {
      logger.debug(
        s"Downloading schema for '${metadata.name}:${metadata.version}' into '${schemaFilePath.relativeTo(os.pwd)}'"
      )

      val schemaSource =
        if (metadata.version.isDefault)
          metadata.name
        else
          s"${metadata.name}@${metadata.version}"

      val installCmd: List[String] =
        List("pulumi", "plugin", "install", "resource", metadata.name) ++ {
          // use version only if it is not the default, otherwise try to install the latest
          if (!metadata.version.isDefault) List(metadata.version) else List.empty
        } ++ {
          // use server only if it is defined
          metadata.server.map(url => List("--server", url)).getOrElse(List.empty)
        }
      os.proc(installCmd).call()

      val schema =
        try {
          os.proc("pulumi", "package", "get-schema", schemaSource).call().out.text
        } catch {
          case e: os.SubprocessException =>
            val msg =
              s"Failed to download schema for ${metadata.name}:${metadata.version}' into '${schemaFilePath.relativeTo(os.pwd)}'"
            logger.error(msg)
            throw GeneralCodegenError(msg, e)
        }

      // parse and save the schema using path corrected for the actual name and version for the package
      val pulumiPackage         = PulumiPackage.fromString(schema)
      val reconciled            = pulumiPackage.toPackageMetadata(metadata.version)
      val correctSchemaFilePath = schemaCacheDirPath / reconciled.name / reconciled.version / schemaFileName
      os.write.over(correctSchemaFilePath, schema, createFolders = true)
      (pulumiPackage, reconciled)
    } else {
      logger.debug(
        s"Using cached schema for ${metadata.name}:${metadata.version}' from '${schemaFilePath.relativeTo(os.pwd)}'"
      )
      (PulumiPackage.fromFile(schemaFilePath), metadata)
    }
  }

  private def pulumiPackage(schema: SchemaFile): (PulumiPackage, PackageMetadata) = {
    // try to get fallback version information from the schema file path
    val pathVersion = {
      val directory = (schema / os.up).last
      PackageVersion.parse(directory).getOrElse(PackageVersion.default)
    }

    // parse and save the schema using path corrected for the actual name and version for the package
    val pulumiPackage         = PulumiPackage.fromFile(schema)
    val metadata              = pulumiPackage.toPackageMetadata(pathVersion)
    val correctSchemaFilePath = schemaCacheDirPath / metadata.name / metadata.version / schemaFileName
    os.copy.over(schema, correctSchemaFilePath, replaceExisting = true, createFolders = true)
    (pulumiPackage, metadata)
  }

  def dependencies(schemaName: SchemaName, packageVersion: PackageVersion): List[(SchemaName, PackageVersion)] =
    packageInfos.keys.filterNot { case (name, version) =>
      name == schemaName && version == packageVersion
    }.toList

  def packageInfo(metadata: PackageMetadata): (PulumiPackage, PulumiPackageInfo) = {
    val (p, m) = pulumiPackage(metadata)
    packageInfo(p, m)
  }

  def packageInfo(schemaOrMetadata: Either[SchemaFile, PackageMetadata]): (PulumiPackage, PulumiPackageInfo) = {
    val (pulumiPackage, packageMetadata) = schemaOrMetadata match {
      case Left(schema)              => this.pulumiPackage(schema)
      case Right(m: PackageMetadata) => this.pulumiPackage(m)
    }
    packageInfo(pulumiPackage, packageMetadata)
  }

  def packageInfo(
    pulumiPackage: PulumiPackage,
    packageMetadata: PackageMetadata
  ): (PulumiPackage, PulumiPackageInfo) = {
    val info = packageInfos.getOrElseUpdate(
      (packageMetadata.name, packageMetadata.version),
      reconcilePackageInfo(pulumiPackage, packageMetadata)
    )
    (pulumiPackage, info)
  }

  private def reconcilePackageInfo(
    pulumiPackage: PulumiPackage,
    packageMetadata: PackageMetadata
  ): PulumiPackageInfo = {
    require(pulumiPackage.name == packageMetadata.name, "Package name mismatch")

    // pre-process the package to gather information about types, that are used later during various parts of codegen
    // most notable place is TypeMapper.scalaTypeFromTypeUri
    val enumTypeTokensBuffer             = ListBuffer.empty[String]
    val objectTypeTokensBuffer           = ListBuffer.empty[String]

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

    PulumiPackageInfo(
      name = pulumiPackage.name,
      version = pulumiPackage.reconcileVersion(packageMetadata.version),
      enumTypeTokens = enumTypeTokensBuffer.toSet,
      objectTypeTokens = objectTypeTokensBuffer.toSet,
      providerTypeToken = pulumiPackage.providerTypeToken,
      resourceTypeTokens = pulumiPackage.resources.keySet.map(_.toLowerCase),
      moduleToPackageParts = pulumiPackage.moduleToPackageParts,
      providerToPackageParts = pulumiPackage.providerToPackageParts
    )
  }
}
