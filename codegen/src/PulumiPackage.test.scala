package besom.codegen.metaschema

import besom.codegen.Utils.TypeReferenceOps
import besom.codegen.{Config, DownloadingSchemaProvider, Logger, PackageMetadata, TypeMapper, UpickleApi}

//noinspection ScalaFileName,TypeAnnotation
class PulumiPackageTest extends munit.FunSuite {
  implicit val logger: Logger = new Logger
  private val defaultTestSchemaName = "test-as-scala-type"

  test("TypeReference with nested union and named types") {
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

    val schemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
    val (_, packageInfo) = schemaProvider.packageInfo(
      PackageMetadata(defaultTestSchemaName, "0.0.0"),
      PulumiPackage(defaultTestSchemaName)
    )
    implicit val tm: TypeMapper = new TypeMapper(packageInfo, schemaProvider)
    val typeReferenceScala = propertyDefinition.typeReference.asScalaType()
    assertEquals(typeReferenceScala.toString(), "String | scala.collection.immutable.List[String]")
  }
}
