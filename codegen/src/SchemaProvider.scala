package besom.codegen

import scala.collection.mutable.ListBuffer
import besom.codegen.metaschema._
import besom.codegen.Utils.PulumiPackageOps

object SchemaProvider {
  type ProviderName  = String
  type SchemaVersion = String
}

trait SchemaProvider {
  import SchemaProvider._

  def pulumiPackage(providerName: ProviderName, schemaVersion: SchemaVersion): PulumiPackage
  def packageInfo(providerName: ProviderName, schemaVersion: SchemaVersion): PulumiPackageInfo

  def pulumiPackage(schemaFilePath: os.Path): PulumiPackage = {
    PulumiPackage.fromFile(schemaFilePath)
  }

  def loadPackageInfo(pulumiPackage: PulumiPackage): PulumiPackageInfo = {
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
      enumTypeTokens = enumTypeTokensBuffer.toSet,
      objectTypeTokens = objectTypeTokensBuffer.toSet,
      resourceTypeTokens = pulumiPackage.resources.keySet.map(_.toLowerCase),
      moduleToPackageParts = pulumiPackage.moduleToPackageParts,
      moduleFormat = pulumiPackage.meta.moduleFormat.r
    )
  }
}

class DownloadingSchemaProvider(schemaCacheDirPath: os.Path) extends SchemaProvider {

  import SchemaProvider._

  private val packageInfos: collection.mutable.Map[(ProviderName, SchemaVersion), PulumiPackageInfo] =
    collection.mutable.Map.empty

  private def downloadedSchemaFilePath(providerName: ProviderName, schemaVersion: SchemaVersion): os.Path = {
    val schemaFilePath = schemaCacheDirPath / providerName / schemaVersion / "schema.json"

    if (!os.exists(schemaFilePath)) {
      val schemaSource = s"$providerName@$schemaVersion"
      os.makeDir.all(schemaFilePath / os.up)
      os.proc("pulumi", "package", "get-schema", schemaSource).call(stdout = schemaFilePath)
    }

    schemaFilePath
  }

  def addSchemaFile(providerName: ProviderName, schemaVersion: SchemaVersion, content: os.Path): os.Path = {
    val schemaFilePath = schemaCacheDirPath / providerName / schemaVersion / "schema.json"
    os.copy.over(content, schemaFilePath, replaceExisting = true, createFolders = true)
    schemaFilePath
  }

  def addSchemaString(providerName: ProviderName, schemaVersion: SchemaVersion, content: String): os.Path = {
    val schemaFilePath = schemaCacheDirPath / providerName / schemaVersion / "schema.json"
    os.write.over(schemaFilePath, content, createFolders = true)
    schemaFilePath
  }

  def pulumiPackage(providerName: ProviderName, schemaVersion: SchemaVersion): PulumiPackage =
    pulumiPackage(downloadedSchemaFilePath(providerName, schemaVersion))

  def packageInfo(providerName: ProviderName, schemaVersion: SchemaVersion): PulumiPackageInfo = {
    packageInfos.getOrElseUpdate(
      (providerName, schemaVersion),
      loadPackageInfo(providerName, schemaVersion)
    )
  }

  private def loadPackageInfo(providerName: ProviderName, schemaVersion: SchemaVersion): PulumiPackageInfo =
    loadPackageInfo(pulumiPackage(providerName, schemaVersion))
}
