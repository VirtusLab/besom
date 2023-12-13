package besom.codegen.metaschema

import besom.codegen.Config.{CodegenConfig, ProviderConfig}
import besom.codegen._

import scala.meta._
import scala.meta.dialects.Scala33

//noinspection ScalaFileName,TypeAnnotation
class CodeGenTest extends munit.FunSuite {
  val defaultTestSchemaName = "test-codegen"

  case class Data(
    name: String,
    json: String,
    ignored: List[String] = List.empty,
    expected: Map[String, String] = Map.empty,
    expectedError: Option[String] = None,
    tags: Set[munit.Tag] = Set()
  )

  Vector(
    Data(
      name = "Provider with plain objects",
      json = s"""|{
                 |  "name": "example",
                 |  "provider": {
                 |    "description": "The provider type for the kubernetes package.",
                 |    "type": "object",
                 |    "inputProperties": {
                 |      "helmReleaseSettings": {
                 |        "$$ref": "#/types/example:index:HelmReleaseSettings",
                 |        "description": "BETA FEATURE - Options to configure the Helm Release resource."
                 |      }
                 |    }
                 |  },
                 |  "types": {
                 |    "example:index:HelmReleaseSettings": {
                 |      "description": "BETA FEATURE - Options to configure the Helm Release resource.",
                 |      "properties": {
                 |        "driver": {
                 |          "type": "string",
                 |          "description": "The backend storage driver for Helm. Values are: configmap, secret, memory, sql.",
                 |          "default": "secret",
                 |          "defaultInfo": {
                 |            "environment": ["PULUMI_K8S_HELM_DRIVER"]
                 |          }
                 |        },
                 |        "pluginsPath": {
                 |          "type": "string",
                 |          "description": "The path to the helm plugins directory.",
                 |          "defaultInfo": {
                 |            "environment": ["PULUMI_K8S_HELM_PLUGINS_PATH"]
                 |          }
                 |        },
                 |        "requiredArg": {
                 |          "type": "string",
                 |          "description": "to test required args"
                 |        }
                 |      },
                 |      "required": ["requiredArg"],
                 |      "type": "object"
                 |    }
                 |  }
                 |}
                 |""".stripMargin,
      expected = Map(
        "src/index/Provider.scala" ->
          s"""|package besom.api.example
              |
              |final case class Provider private(
              |  urn: besom.types.Output[besom.types.URN],
              |  id: besom.types.Output[besom.types.ResourceId]
              |) extends besom.ProviderResource
              |
              |object Provider:
              |  def apply(using ctx: besom.types.Context)(
              |    name: besom.util.NonEmptyString,
              |    args: ProviderArgs = ProviderArgs(),
              |    opts: besom.CustomResourceOptions = besom.CustomResourceOptions()
              |  ): besom.types.Output[Provider] =
              |    ctx.readOrRegisterResource[Provider, ProviderArgs]("pulumi:providers:example", name, args, opts)
              |
              |  given resourceDecoder(using besom.types.Context): besom.types.ResourceDecoder[Provider] = 
              |    besom.internal.ResourceDecoder.derived[Provider]
              |
              |  given decoder(using besom.types.Context): besom.types.Decoder[Provider] = 
              |    besom.internal.Decoder.customResourceDecoder[Provider]
              |
              |  given outputOps: {} with
              |    extension(output: besom.types.Output[Provider])
              |      def urn: besom.types.Output[besom.types.URN] = output.flatMap(_.urn)
              |      def id: besom.types.Output[besom.types.ResourceId] = output.flatMap(_.id)
              |""".stripMargin,
        "src/index/ProviderArgs.scala" ->
          s"""|package besom.api.example
              |
              |final case class ProviderArgs private(
              |  helmReleaseSettings: besom.types.Output[scala.Option[besom.api.example.inputs.HelmReleaseSettingsArgs]]
              |)
              |
              |object ProviderArgs:
              |  def apply(
              |    helmReleaseSettings: besom.types.Input.Optional[besom.api.example.inputs.HelmReleaseSettingsArgs] = scala.None
              |  )(using besom.types.Context): ProviderArgs =
              |    new ProviderArgs(
              |      helmReleaseSettings = helmReleaseSettings.asOptionOutput(isSecret = false)
              |    )
              |
              |  given encoder(using besom.types.Context): besom.types.ProviderArgsEncoder[ProviderArgs] = 
              |    besom.internal.ProviderArgsEncoder.derived[ProviderArgs]
              |""".stripMargin
      ),
      ignored = List(
        "src/index/outputs/HelmReleaseSettings.scala",
        "src/index/outputs/HelmReleaseSettingsArgs.scala",
        "src/index/inputs/HelmReleaseSettingsArgs.scala"
      )
    ),
    Data(
      name = "Error on id property",
      json = """|{
                |  "name": "fake-provider",
                |  "version": "0.0.1",
                |  "resources": {
                |    "fake-provider:index:typ": {
                |      "properties": {
                |        "id": {
                |          "type": "boolean"
                |        }
                |      },
                |      "type": "object"
                |    }
                |  }
                |}
                |""".stripMargin,
      ignored = List(
        "src/index/Provider.scala",
        "src/index/ProviderArgs.scala",
        "src/index/Typ.scala",
        "src/index/TypArgs.scala"
      ),
      expectedError = Some(
        "invalid property for 'fake-provider:index:typ': property name 'id' is reserved"
      ),
      tags = Set(munit.Ignore) // FIXME: un-ignore when this is fixed: https://github.com/pulumi/pulumi-kubernetes/issues/2683
    ),
    Data(
      name = "Error on urn property",
      json = """|{
                |  "name": "fake-provider",
                |  "version": "0.0.1",
                |  "resources": {
                |    "fake-provider:index:typ": {
                |      "properties": {
                |        "urn": {
                |          "type": "boolean"
                |        }
                |      },
                |      "type": "object"
                |    }
                |  }
                |}
                |""".stripMargin,
      ignored = List(
        "src/index/Provider.scala",
        "src/index/ProviderArgs.scala",
        "src/index/Typ.scala",
        "src/index/TypArgs.scala"
      ),
      expectedError = Some(
        "invalid property for 'fake-provider:index:typ': property name 'urn' is reserved"
      ),
      tags = Set(munit.Ignore) // FIXME: un-ignore when this is fixed: https://github.com/pulumi/pulumi-kubernetes/issues/2683
    )
  ).foreach(data =>
    test(data.name.withTags(data.tags)) {
      implicit val config: Config.CodegenConfig = CodegenConfig()
      implicit val logger: Logger               = new Logger(config.logLevel)

      implicit val schemaProvider: SchemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
      val (pulumiPackage, packageInfo) = schemaProvider.packageInfo(
        PackageMetadata(defaultTestSchemaName, "0.0.0"),
        PulumiPackage.fromString(data.json)
      )
      implicit val providerConfig: ProviderConfig = Config.providersConfigs(packageInfo.name)
      implicit val tm: TypeMapper                 = new TypeMapper(packageInfo, schemaProvider)

      val codegen = new CodeGen
      if (data.expectedError.isDefined)
        interceptMessage[Exception](data.expectedError.get)(codegen.scalaFiles(pulumiPackage))
      else
        codegen.scalaFiles(pulumiPackage).foreach {
          case SourceFile(FilePath(f: String), code: String) if data.expected.contains(f) =>
            assertNoDiff(code, data.expected(f))
            code.parse[Source].get
          case SourceFile(FilePath(f: String), _: String) if data.ignored.contains(f) =>
            logger.debug(s"Ignoring file: $f")
          case SourceFile(filename, _) =>
            fail(s"Unexpected file: ${filename.osSubPath}")
        }
    }
  )
}
