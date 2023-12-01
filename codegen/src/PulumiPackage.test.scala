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
    expectedType: String,
    expectedArgsType: Option[String] = None,
    metadata: Option[PackageMetadata] = None,
    package_ : Option[PulumiPackage] = None,
    tags: Set[munit.Tag] = Set()
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
    ),
    Data(
      name = "TypeReference with union with a resource",
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
      expectedType = "String | besom.api.aws.apigateway.RestApi",
      metadata = Some(PackageMetadata("aws", "6.7.0")),
      tags = Set(munit.Ignore) // FIXME: https://github.com/VirtusLab/besom/issues/144
    ),
    Data(
      name = "TypeReference with external (downloaded) named type with duplicate object",
      json = """|{
              |    "type": "object",
              |    "$ref": "/kubernetes/v3.7.0/schema.json#/types/kubernetes:core%2Fv1:Pod"
              |}
              |""".stripMargin,
      expectedType = "besom.api.kubernetes.core.v1.outputs.Pod",
      expectedArgsType = Some("besom.api.kubernetes.core.v1.inputs.PodArgs"),
    )
  ).foreach(data =>
    test(data.name.withTags(data.tags)) {
      implicit val config: Config.CodegenConfig = CodegenConfig()
      implicit val logger: Logger               = new Logger(config.logLevel)

      implicit val schemaProvider: SchemaProvider =
        new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
      val (_, packageInfo) = (data.metadata, data.package_) match {
        case (Some(metadata), Some(package_)) =>  schemaProvider.packageInfo(metadata, package_)
        case (Some(metadata), None)           =>  schemaProvider.packageInfo(metadata)
        case (None, package_)           =>  schemaProvider.packageInfo(
          PackageMetadata(defaultTestSchemaName, "0.0.0"),
          package_.getOrElse(PulumiPackage(defaultTestSchemaName))
        )
      }
      implicit val tm: TypeMapper = new TypeMapper(packageInfo, schemaProvider)

      val propertyDefinition = UpickleApi.read[PropertyDefinition](data.json)

      assertEquals(propertyDefinition.typeReference.asScalaType().syntax, data.expectedType)
      assertEquals(propertyDefinition.typeReference.asScalaType(asArgsType = true).syntax, data.expectedArgsType.getOrElse(data.expectedType))
    }
  )
}
