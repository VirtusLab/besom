package besom.codegen

import scala.collection.mutable.ListBuffer
import besom.codegen.metaschema._
import besom.codegen.Utils.PulumiPackageOps

object SchemaProvider {
  type ProviderName = String
  type SchemaVersion = String
}

class SchemaProvider(schemaCacheDirPath: os.Path) {
  import SchemaProvider._

  private val packageInfos: collection.mutable.Map[(ProviderName, SchemaVersion), PulumiPackageInfo] = collection.mutable.Map.empty

  private def downloadedSchemaFilePath(providerName: ProviderName, schemaVersion: SchemaVersion): os.Path = {
    val schemaFilePath = schemaCacheDirPath / providerName / schemaVersion / "schema.json"
        
    if (!os.exists(schemaFilePath)) {
      val schemaSource = s"${providerName}@${schemaVersion}"
      os.makeDir.all(schemaFilePath / os.up)
      os.proc("pulumi", "plugin", "install", "resource", providerName, schemaVersion).call()
      os.proc("pulumi", "package", "get-schema", schemaSource).call(stdout = schemaFilePath)
    }

    schemaFilePath
  }

  def pulumiPackage(providerName: ProviderName, schemaVersion: SchemaVersion): PulumiPackage = {
    val schemaFilePath = downloadedSchemaFilePath(providerName, schemaVersion)
    PulumiPackage.fromFile(schemaFilePath)
  }

  private def loadPackageInfo(providerName: ProviderName, schemaVersion: SchemaVersion) = {
    val schemaFilePath = downloadedSchemaFilePath(providerName, schemaVersion)

    val pulumiPackage = PulumiPackage.fromFile(schemaFilePath)

    val enumTypeTokensBuffer = ListBuffer.empty[String]
    val objectTypeTokensBuffer = ListBuffer.empty[String]
  
    pulumiPackage.types.foreach {
      case (typeToken, _: EnumTypeDefinition) => enumTypeTokensBuffer += typeToken.toLowerCase // Unifying to lower case to circumvent inconsistencies in low quality schemas (e.g. aws)
      case (typeToken, _: ObjectTypeDefinition) => objectTypeTokensBuffer += typeToken.toLowerCase
    }

    PulumiPackageInfo(
      enumTypeTokens = enumTypeTokensBuffer.toSet,
      objectTypeTokens = objectTypeTokensBuffer.toSet,
      resourceTypeTokens = pulumiPackage.resources.keySet.map(_.toLowerCase),
      moduleToPackageParts = pulumiPackage.moduleToPackageParts,
      moduleFormat = pulumiPackage.meta.moduleFormat.r
    )
  }

  def packageInfo(providerName: ProviderName, schemaVersion: SchemaVersion): PulumiPackageInfo = {
    packageInfos.getOrElseUpdate(
      (providerName, schemaVersion),
      loadPackageInfo(providerName, schemaVersion)
    )
  }
}