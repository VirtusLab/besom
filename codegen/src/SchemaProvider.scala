package besom.codegen

import besom.codegen.PackageVersion.PackageVersionOps
import besom.codegen.Utils.PulumiPackageOps
import besom.codegen.metaschema.*
import besom.codegen.{PackageVersion, SchemaFile, SchemaName}
import besom.model.SemanticVersion

type SchemaFile = os.Path

trait SchemaProvider {
  def packageInfo(metadata: PackageMetadata, schema: Option[SchemaFile] = None): PulumiPackageInfo
  def packageInfo(metadata: PackageMetadata, pulumiPackage: PulumiPackage): PulumiPackageInfo
  def dependencies(schemaName: SchemaName, packageVersion: PackageVersion): List[(SchemaName, PackageVersion)]
}

class DownloadingSchemaProvider(using logger: Logger, config: Config) extends SchemaProvider {
  private val schemaFileName = "schema.json"
  private val packageInfos: collection.mutable.Map[(SchemaName, PackageVersion), PulumiPackageInfo] =
    collection.mutable.Map.empty

  private def pulumiPackage(metadata: PackageMetadata): (PulumiPackage, PackageMetadata) = {
    val schemaFilePath = config.schemasDir / metadata.name / metadata.version.orDefault.asString / schemaFileName

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
      val correctSchemaFilePath = config.schemasDir / reconciled.name / reconciled.version.orDefault.asString / schemaFileName
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
    val correctSchemaFilePath = config.schemasDir / reconciled.name / reconciled.version.orDefault.asString / schemaFileName
    os.copy.over(schema, correctSchemaFilePath, replaceExisting = true, createFolders = true)
    (pulumiPackage, metadata)
  }

  def dependencies(schemaName: SchemaName, packageVersion: PackageVersion): List[(SchemaName, PackageVersion)] =
    packageInfos.keys.filterNot { case (name, _) => name == schemaName }.toList

  def packageInfo(metadata: PackageMetadata, schema: Option[SchemaFile] = None): PulumiPackageInfo = {
    val (initialPackage, packageMetadata) = (metadata, schema) match {
      case (m, Some(schema))          => this.pulumiPackage(m, schema)
      case (m: PackageMetadata, None) => this.pulumiPackage(m)
    }
    
    // Apply hotfixes to the package
    val pulumiPackage = Hotfix.applyToPackage(
      initialPackage,
      packageMetadata.name,
      SemanticVersion.parseTolerant(packageMetadata.version.orDefault.asString).fold(
        e => throw GeneralCodegenException(s"Invalid version: ${packageMetadata.version.orDefault.asString}", e),
        identity
      )
    )
    
    packageInfos.getOrElseUpdate(
      (packageMetadata.name, packageMetadata.version.orDefault),
      reconcilePackageInfo(pulumiPackage, packageMetadata)
    )
  }

  def packageInfo(
    metadata: PackageMetadata,
    pulumiPackage: PulumiPackage
  ): PulumiPackageInfo = {
    packageInfos.getOrElseUpdate(
      (metadata.name, metadata.version.orDefault),
      reconcilePackageInfo(pulumiPackage, metadata)
    )
  }

  private def reconcilePackageInfo(
    pulumiPackage: PulumiPackage,
    packageMetadata: PackageMetadata
  ): PulumiPackageInfo = PulumiPackageInfo.from(pulumiPackage, packageMetadata)
}
