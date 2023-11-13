package besom.codegen.metaschema

import besom.codegen.Utils.TypeReferenceOps
import besom.codegen.{Config, DownloadingSchemaProvider, Logger, PackageMetadata, TypeMapper, UpickleApi}

//noinspection ScalaFileName,TypeAnnotation
class PulumiPackageTest extends munit.FunSuite {
  implicit val logger: Logger        = new Logger(Logger.Level.Debug)
  private val defaultTestSchemaName  = "test-as-scala-type"
  private val defaultPackageMetadata = PackageMetadata(defaultTestSchemaName, "0.0.0")
  private val defaultPulumiPackage   = PulumiPackage(defaultTestSchemaName)

  def testPropertyDefinition(testName: String)(
    json: String,
    pack: Either[PackageMetadata, (PackageMetadata, PulumiPackage)] =
      Right(defaultPackageMetadata, defaultPulumiPackage)
  )(expectations: scala.meta.Type => Any)(implicit loc: munit.Location): Unit = test(testName) {
    val propertyDefinition = UpickleApi.read[PropertyDefinition](json)

    val schemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
    val (_, packageInfo) = pack match {
      case Left(m)       => schemaProvider.packageInfo(m)
      case Right((m, p)) => schemaProvider.packageInfo(p, m)
    }

    implicit val tm: TypeMapper = new TypeMapper(packageInfo, schemaProvider)
    val typeReferenceScala      = propertyDefinition.typeReference.asScalaType()

    expectations(typeReferenceScala)
  }

  testPropertyDefinition("TypeReference with nested union and named types")(
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
              |""".stripMargin
  ) { typeReferenceScala =>
    assertEquals(typeReferenceScala.toString(), "String | scala.collection.immutable.List[String]")
  }

  testPropertyDefinition("TypeReference with union with a resource")(
    json = """|{
              |  "type": "string",
              |  "oneOf": [
              |    {
              |      "type": "string"
              |    },
              |    {
              |      "type": "string",
              |      "$ref": "#/types/aws:apigateway%2FrestApi:RestApi"
              |    }
              |  ],
              |  "willReplaceOnChanges": true
              |}
              |""".stripMargin,
    pack = Left(PackageMetadata("aws", "6.7.0"))
  ) { typeReferenceScala =>
    assertEquals(typeReferenceScala.toString(), "String | besom.api.aws.apigateway.RestApi")
  }

  testPropertyDefinition("TypeReference with external named type with duplicate object")(
    json = """|{
              |    "type": "object",
              |    "$ref": "/kubernetes/v3.7.0/schema.json#/types/kubernetes:core%2Fv1:Pod"
              |}
              |""".stripMargin
  ) { typeReferenceScala =>
    assertEquals(typeReferenceScala.toString(), "besom.api.kubernetes.core.v1.outputs.Pod")
  }
}
