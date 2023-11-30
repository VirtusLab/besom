package besom.codegen.metaschema

import besom.codegen.Config.CodegenConfig
import besom.codegen.Utils.TypeReferenceOps
import besom.codegen._

import scala.meta._
import scala.meta.dialects.Scala33

//noinspection ScalaFileName,TypeAnnotation
class PulumiPackageTest extends munit.FunSuite {
  private val defaultTestSchemaName = "test-as-scala-type"

  case class Data(
    name: String,
    json: String,
    expectedType: String
  )

  Vector(
    Data(
      name = "PropertyDefinition with nested union and array with missing name type should use fallback",
      json = """|{
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
                |""".stripMargin,
      expectedType = "String | scala.collection.immutable.List[String]"
    ),
    Data(
      name = "PropertyDefinition with underlying object",
      json = """|{
                |  "type": "object",
                |  "$ref": "#/types/kubernetes:crd.k8s.amazonaws.com/v1alpha1:ENIConfigSpec"
                |}
                |""".stripMargin,
      expectedType = "scala.Predef.Map[String, String]"
    )
  ).foreach(data =>
    test(data.name) {
      implicit val config: Config.CodegenConfig = CodegenConfig()
      implicit val logger: Logger               = new Logger(config.logLevel)

      implicit val schemaProvider: SchemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
      val (_, packageInfo) = schemaProvider.packageInfo(
        PackageMetadata(defaultTestSchemaName, "0.0.0"),
        PulumiPackage(defaultTestSchemaName)
      )
      implicit val tm: TypeMapper = new TypeMapper(packageInfo, schemaProvider)

      val propertyDefinition = UpickleApi.read[PropertyDefinition](data.json)

      assertEquals(propertyDefinition.typeReference.asScalaType().syntax, data.expectedType)
      assertEquals(propertyDefinition.typeReference.asScalaType(asArgsType = true).syntax, data.expectedType)
    }
  )
}
