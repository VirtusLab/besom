package besom.codegen.metaschema

import besom.codegen.PulumiTypeReference.TypeReferenceOps
import besom.codegen.{Config, DownloadingSchemaProvider, Logger, PackageMetadata, ThisPackageInfo, UpickleApi}

//noinspection ScalaFileName,TypeAnnotation
class PulumiPackageTest extends munit.FunSuite {
  implicit val logger: Logger = new Logger(Logger.Level.Debug)
  private val defaultTestSchemaName = "test-as-scala-type"

  test("TypeReference with nested union and array with missing name type should use fallback") {
    val json =
      """|{
         |  "type": "string",
         |  "oneOf": [
         |    {
         |      "type": "string"
         |    },
         |    {
         |      "type": "array",
         |      "items": {
         |        "type": "string",
         |        "$ref": "#/types/aws:s3%2FroutingRules:RoutingRule"
         |      }
         |    }
         |  ]
         |}
         |""".stripMargin

    val propertyDefinition      = UpickleApi.read[PropertyDefinition](json)

    implicit val schemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
    val (_, packageInfo) = schemaProvider.packageInfo(
      PulumiPackage(defaultTestSchemaName),
      PackageMetadata(defaultTestSchemaName, "0.0.0")
    )
    implicit val thisPackageInfo: ThisPackageInfo = ThisPackageInfo(packageInfo)

    val typeReferenceScala = propertyDefinition.typeReference.asScalaType().toTry.get
    assertEquals(typeReferenceScala.toString(), "String | scala.collection.immutable.List[String]")
  }
}
