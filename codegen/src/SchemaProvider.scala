package besom.codegen

import scala.collection.mutable.ListBuffer
import besom.codegen.metaschema._
import besom.codegen.Utils.PulumiPackageOps

object SchemaProvider {
  type SchemaName    = String
  type SchemaVersion = String
  type SchemaFile    = os.Path

  val DefaultSchemaVersion: SchemaVersion = "0.0.0"
}

trait SchemaProvider {
  import SchemaProvider._
  import Utils.PulumiPackageOps

  def dependencies(schemaName: SchemaName, schemaVersion: SchemaVersion): List[(SchemaName, SchemaVersion)]

  def pulumiPackage(schemaName: SchemaName, schemaVersion: SchemaVersion): PulumiPackage
  def pulumiPackage(schema: SchemaFile): PulumiPackage

  def packageInfo(schemaName: SchemaName, schemaVersion: SchemaVersion): PulumiPackageInfo
  def packageInfo(schema: SchemaFile): PulumiPackageInfo

  def packageInfo(pulumiPackage: PulumiPackage): PulumiPackageInfo = {
    val enumTypeTokensBuffer   = ListBuffer.empty[String]
    val objectTypeTokensBuffer = ListBuffer.empty[String]

    // post-process the package to improve its quality
    pulumiPackage.types.foreach {
      case (typeToken, _: EnumTypeDefinition) =>
        enumTypeTokensBuffer += typeToken.toLowerCase // Unifying to lower case to circumvent inconsistencies in low quality schemas (e.g. aws)
      case (typeToken, _: ObjectTypeDefinition) =>
        objectTypeTokensBuffer += typeToken.toLowerCase
    }

    PulumiPackageInfo(
      schemaName = pulumiPackage.name,
      schemaVersion = pulumiPackage.version.getOrElse(SchemaProvider.DefaultSchemaVersion),
      enumTypeTokens = enumTypeTokensBuffer.toSet,
      objectTypeTokens = objectTypeTokensBuffer.toSet,
      providerTypeToken = pulumiPackage.providerTypeToken,
      resourceTypeTokens = pulumiPackage.resources.keySet.map(_.toLowerCase),
      moduleToPackageParts = pulumiPackage.moduleToPackageParts,
      providerToPackageParts = pulumiPackage.providerToPackageParts
    )
  }
}

class DownloadingSchemaProvider(schemaCacheDirPath: os.Path)(implicit logger: Logger) extends SchemaProvider {

  import SchemaProvider._

  private val packageInfos: collection.mutable.Map[(SchemaName, SchemaVersion), PulumiPackageInfo] =
    collection.mutable.Map.empty

  private def downloadPulumiPackage(providerName: SchemaName, schemaVersion: SchemaVersion): PulumiPackage =
    PulumiPackage.fromFile(downloadedSchemaFilePath(providerName, schemaVersion))

  private def downloadedSchemaFilePath(providerName: SchemaName, schemaVersion: SchemaVersion): os.Path = {
    val schemaFilePath = schemaCacheDirPath / providerName / schemaVersion / "schema.json"

    if (!os.exists(schemaFilePath)) {
      logger.debug(s"Downloading schema for $providerName:$schemaVersion' into '${schemaFilePath.relativeTo(os.pwd)}'")
      val schemaSource = s"$providerName@$schemaVersion"
      os.makeDir.all(schemaFilePath / os.up)
      try {
        os.proc("pulumi", "package", "get-schema", schemaSource).call(stdout = schemaFilePath)
      } catch {
        case e: os.SubprocessException =>
          os.remove.all(schemaFilePath) // cleanup the partially downloaded file
          val msg =
            s"Failed to download schema for $providerName:$schemaVersion' into '${schemaFilePath.relativeTo(os.pwd)}'"
          logger.error(msg)
          throw GeneralCodegenError(msg, e)
      }
    } else {
      logger.debug(s"Using cached schema for $providerName:$schemaVersion' from '${schemaFilePath.relativeTo(os.pwd)}'")
    }

    schemaFilePath
  }

  private def saveSchemaFilePath(providerName: SchemaName, schemaVersion: SchemaVersion, schema: os.Path): Unit = {
    val schemaFilePath = schemaCacheDirPath / providerName / schemaVersion / "schema.json"
    os.copy.over(schema, schemaFilePath, replaceExisting = true, createFolders = true)
  }

  override def packageInfo(pulumiPackage: PulumiPackage): PulumiPackageInfo = {
    packageInfos.getOrElseUpdate(
      (pulumiPackage.name, pulumiPackage.version.getOrElse(DefaultSchemaVersion)),
      super.packageInfo(pulumiPackage)
    )
  }

  def packageInfo(schemaName: SchemaName, schemaVersion: SchemaVersion): PulumiPackageInfo = {
    packageInfos.getOrElseUpdate(
      (schemaName, schemaVersion),
      packageInfo(downloadPulumiPackage(schemaName, schemaVersion))
    )
  }

  def pulumiPackage(schemaName: SchemaName, schemaVersion: SchemaVersion): PulumiPackage = {
    downloadPulumiPackage(schemaName, schemaVersion)
  }

  def packageInfo(schema: SchemaFile): PulumiPackageInfo = {
    val pack    = PulumiPackage.fromFile(schema)
    val name    = pack.name
    val version = pack.version.getOrElse(DefaultSchemaVersion)
    packageInfos.getOrElseUpdate(
      (name, version), {
        saveSchemaFilePath(name, version, schema)
        packageInfo(pack)
      }
    )
  }

  def pulumiPackage(schema: SchemaFile): PulumiPackage = {
    val pack    = PulumiPackage.fromFile(schema)
    val name    = pack.name
    val version = pack.version.getOrElse(DefaultSchemaVersion)
    saveSchemaFilePath(name, version, schema)
    pack
  }

  def dependencies(schemaName: SchemaName, schemaVersion: SchemaVersion): List[(SchemaName, SchemaVersion)] =
    packageInfos.keys.filterNot { case (name, version) =>
      name == schemaName && version == schemaVersion
    }.toList
}
