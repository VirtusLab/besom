package besom.codegen.metaschema

import besom.codegen.Config.CodegenConfig
import besom.codegen._

import scala.meta._
import scala.meta.dialects.Scala33

//noinspection ScalaFileName,TypeAnnotation
class PropertyInfoTest extends munit.FunSuite {
  object TestPackageMetadata extends PackageMetadata("test-as-scala-type", Some(PackageVersion.default))

  case class Data(
    name: String,
    json: String,
    expectedName: String,
    expectedType: String,
    expectedArgsType: Option[String] = None,
    expectedInputArgsType: Option[String] = None,
    metadata: PackageMetadata = TestPackageMetadata,
    tags: Set[munit.Tag] = Set()
  )

  Vector(
    Data(
      name = "property with nested union and array with missing name type should use fallback",
      json = """|{
                |  "routingRules": {
                |    "type": "string",
                |    "oneOf": [
                |      {
                |        "type": "string"
                |      },
                |      {
                |        "type": "array",
                |        "items": {
                |          "type": "string",
                |          "$ref": "#/types/aws:s3%2FroutingRules:RoutingRule"
                |        }
                |      }
                |    ]
                |  }
                |}
                |""".stripMargin,
      expectedName = "routingRules",
      expectedType = "String | scala.collection.immutable.List[String]"
    ),
    Data(
      name = "property with underlying object",
      json = """|{
                |  "spec": {
                |    "type": "object",
                |    "$ref": "#/types/kubernetes:crd.k8s.amazonaws.com/v1alpha1:ENIConfigSpec"
                |  }
                |}
                |""".stripMargin,
      expectedName = "spec",
      expectedType = "scala.Predef.Map[String, String]"
    ),
    Data(
      name = "property with a union with a resource",
      json = """|{
                |  "restApi": {
                |    "type": "string",
                |    "oneOf": [
                |      {
                |        "type": "string"
                |      },
                |      {
                |        "type": "string",
                |        "$ref": "#/types/aws:apigateway%2FrestApi:RestApi"
                |      }
                |    ],
                |    "willReplaceOnChanges": true
                |  }
                |}
                |""".stripMargin,
      expectedName = "restApi",
      expectedType = "String | besom.api.aws.apigateway.RestApi",
      metadata = PackageMetadata("aws", "6.7.0")
    ),
    Data(
      name = "property with external (downloaded) named type with duplicate object",
      json = """|{
                |  "pod":{
                |      "type": "object",
                |      "$ref": "/kubernetes/v3.7.0/schema.json#/types/kubernetes:core%2Fv1:Pod"
                |  }
                |}
                |""".stripMargin,
      expectedName = "pod",
      expectedType = "besom.api.kubernetes.core.v1.outputs.Pod",
      expectedArgsType = Some("besom.api.kubernetes.core.v1.inputs.PodArgs"),
      expectedInputArgsType = Some("besom.api.kubernetes.core.v1.inputs.PodArgs")
    ),
    Data(
      name = "property named enum",
      json = """|{
                |  "enum": {
                |    "type": "array",
                |    "items": {
                |      "$ref": "pulumi.json#/Json"
                |    }
                |  }
                |}
                |""".stripMargin,
      expectedName = "`enum`",
      expectedType = "scala.collection.immutable.List[besom.types.PulumiJson]",
      expectedInputArgsType = Some("scala.collection.immutable.List[besom.types.Input[besom.types.PulumiJson]]")
    )
  ).foreach(data =>
    test(data.name.withTags(data.tags)) {
      implicit val config: Config.CodegenConfig = CodegenConfig()
      implicit val logger: Logger               = new Logger(config.logLevel)

      implicit val schemaProvider: SchemaProvider =
        new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
      val (_, packageInfo) = data.metadata match {
        case m @ TestPackageMetadata => schemaProvider.packageInfo(m, PulumiPackage(name = m.name))
        case _                       => schemaProvider.packageInfo(data.metadata)
      }
      implicit val tm: TypeMapper = new TypeMapper(packageInfo, schemaProvider)

      val (name, definition) = UpickleApi.read[Map[String, PropertyDefinition]](data.json).head
      val property           = PropertyInfo.from(name, definition, isPropertyRequired = false)

      assertEquals(property.name.syntax, data.expectedName)
      assertEquals(property.baseType.syntax, data.expectedType)
      assertEquals(property.argType.syntax, data.expectedArgsType.getOrElse(data.expectedType))
      assertEquals(property.inputArgType.syntax, data.expectedInputArgsType.getOrElse(data.expectedType))
    }
  )
}
