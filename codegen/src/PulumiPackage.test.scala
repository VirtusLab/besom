package besom.codegen.metaschema

import besom.codegen.PulumiTypeReference.TypeReferenceOps
import besom.codegen.{Config, DownloadingSchemaProvider, Logger, PackageMetadata, ThisPackageInfo, UpickleApi, PropertyInfo}
import scala.meta.*

//noinspection ScalaFileName,TypeAnnotation
class PulumiPackageTest extends munit.FunSuite {
  implicit val logger: Logger       = new Logger
  private val defaultTestSchemaName = "test-as-scala-type"

  case class Data(
    name: String,
    json: String,
    expectedType: String
  )

  List(
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
      val propertyDefinition = UpickleApi.read[PropertyDefinition](data.json)

      implicit val schemaProvider: DownloadingSchemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
      val (_, packageInfo) = schemaProvider.packageInfo(
        PackageMetadata(defaultTestSchemaName, "0.0.0"),
        PulumiPackage(defaultTestSchemaName)
      )
      implicit val thisPackageInfo: ThisPackageInfo = ThisPackageInfo(packageInfo)
      val typeReferenceScala                        = propertyDefinition.typeReference.asScalaType().toTry.get
      assertEquals(typeReferenceScala.syntax, data.expectedType)

      val info = PropertyInfo
        .from(
          propertyName = "test",
          propertyDefinition = propertyDefinition,
          isPropertyRequired = true
        )
        .toTry
        .get
      assertEquals(info.asParam.syntax, s"test: ${data.expectedType}")
      assertEquals(info.asOutputParam.syntax, s"test: besom.types.Output[${data.expectedType}]")
      assertEquals(info.asScalaGetter.syntax, s"def test: besom.types.Output[${data.expectedType}] = output.map(_.test)")
      assertEquals(info.asScalaOptionGetter.syntax, s"def test: besom.types.Output[scala.Option[${data.expectedType}]] = output.map(_.map(_.test))")
    }
  )
}
