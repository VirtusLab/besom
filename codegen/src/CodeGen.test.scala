package besom.codegen.metaschema

import besom.codegen.Config.{CodegenConfig, ProviderConfig}
import besom.codegen.{CodeGen, Config, DownloadingSchemaProvider, FilePath, Logger, PackageMetadata, SchemaProvider, SourceFile, ThisPackageInfo, scalameta}

//noinspection ScalaFileName
class CodeGenTest extends munit.FunSuite {
  val defaultTestSchemaName = "test-codegen"

  case class Data(
    name: String,
    json: String,
    expected: Map[String, String]
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
              |) extends besom.ProviderResource derives besom.ResourceDecoder
              |
              |object Provider:
              |  def apply(using ctx: besom.types.Context)(
              |    name: besom.util.NonEmptyString,
              |    args: ProviderArgs = ProviderArgs(),
              |    opts: besom.CustomResourceOptions = besom.CustomResourceOptions()
              |  ): besom.types.Output[Provider] =
              |    ctx.registerResource[Provider, ProviderArgs]("pulumi:providers:example", name, args, opts)
              |
              |  given outputOps: {} with
              |    extension(output: besom.types.Output[Provider])
              |      def urn: besom.types.Output[besom.types.URN] = output.flatMap(_.urn)
              |      def id: besom.types.Output[besom.types.ResourceId] = output.flatMap(_.id)
              |""".stripMargin,
        "src/index/ProviderArgs.scala" ->
          s"""|package besom.api.example
              |
              |final case class ProviderArgs(
              |  helmReleaseSettings: besom.types.Output[scala.Option[besom.api.example.inputs.HelmReleaseSettings]]
              |) derives besom.ProviderArgsEncoder
              |
              |object ProviderArgs:
              |  def apply(
              |    helmReleaseSettings: besom.types.Input.Optional[besom.api.example.inputs.HelmReleaseSettingsArgs] = scala.None
              |  )(using besom.types.Context): ProviderArgs =
              |    new ProviderArgs(
              |      helmReleaseSettings = helmReleaseSettings.asOptionOutput(isSecret = false)
              |    )
              |""".stripMargin // TODO: this input should be marked as json=true equivalent
      )
    )
  ).foreach(data =>
    test(data.name) {
      implicit val config: Config.CodegenConfig = CodegenConfig()
      implicit val logger: Logger               = new Logger(config.logLevel)

      implicit val schemaProvider: SchemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
      val (pulumiPackage, packageInfo) = schemaProvider.packageInfo(
        PackageMetadata(defaultTestSchemaName, "0.0.0"),
        PulumiPackage.fromString(data.json)
      )
      implicit val thisPackageInfo: ThisPackageInfo = ThisPackageInfo(packageInfo)
      implicit val providerConfig: ProviderConfig   = Config.providersConfigs(packageInfo.name)

      val codegen = new CodeGen
      codegen.sourceFilesForProviderResource(pulumiPackage).foreach {
        case SourceFile(FilePath(f: String), code: String) if data.expected.contains(f) =>
          val _ = scalameta.parseSource(code)
          assertNoDiff(code, data.expected(f))
        case SourceFile(filename, _) =>
          fail(s"Unexpected file: ${filename.osSubPath}")
      }
    }
  )
}
