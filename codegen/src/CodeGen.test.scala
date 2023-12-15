package besom.codegen.metaschema

import besom.codegen.*
import besom.codegen.Config.{CodegenConfig, ProviderConfig}

import scala.meta.*
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
              |      def urn : besom.types.Output[besom.types.URN] = output.flatMap(_.urn)
              |      def id : besom.types.Output[besom.types.ResourceId] = output.flatMap(_.id)
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
      name = "Resource with method and a function",
      json = """|{
                |  "name": "google-native",
                |  "resources": {
                |    "google-native:container/v1:Cluster": {
                |      "type": "object",
                |      "properties": {
                |        "name": {
                |          "type": "string"
                |        }
                |      },
                |      "required": [
                |        "name"
                |      ],
                |      "methods": {
                |        "getKubeconfig": "google-native:container/v1:Cluster/getKubeconfig"
                |      }
                |    }
                |  },
                |  "functions": {
                |    "google-native:container/v1:Cluster/getKubeconfig": {
                |      "inputs": {
                |        "properties": {
                |          "__self__": {
                |            "$ref": "#/resources/google-native:container%2Fv1:Cluster"
                |          }
                |        },
                |        "type": "object",
                |        "required": [
                |          "__self__"
                |        ]
                |      },
                |      "outputs": {
                |        "properties": {
                |          "kubeconfig": {
                |            "type": "string"
                |          }
                |        },
                |        "type": "object",
                |        "required": [
                |          "kubeconfig"
                |        ]
                |      }
                |    },
                |    "google-native:container/v1:getCluster": {
                |      "description": "Gets the details of a specific cluster.",
                |      "inputs": {
                |        "properties": {
                |          "clusterId": {
                |            "type": "string"
                |          },
                |          "location": {
                |            "type": "string"
                |          }
                |        },
                |        "type": "object",
                |        "required": [
                |          "clusterId"
                |        ]
                |      }
                |    }
                |  }
                |}
                |""".stripMargin,
      expected = Map(
        "src/container/v1/Cluster.scala" ->
          """|package besom.api.googlenative.container.v1
             |
             |final case class Cluster private(
             |  urn: besom.types.Output[besom.types.URN],
             |  id: besom.types.Output[besom.types.ResourceId],
             |  name: besom.types.Output[String]
             |) extends besom.CustomResource:
             |  def getKubeconfig(using ctx: besom.types.Context)(
             |    args: besom.api.googlenative.container.v1.ClusterGetKubeconfigArgs = besom.api.googlenative.container.v1.ClusterGetKubeconfigArgs(),
             |    opts: besom.InvokeOptions = besom.InvokeOptions()
             |  ): besom.types.Output[besom.api.googlenative.container.v1.ClusterGetKubeconfigResult] =
             |     ctx.call[besom.api.googlenative.container.v1.ClusterGetKubeconfigArgs, besom.api.googlenative.container.v1.ClusterGetKubeconfigResult, besom.api.googlenative.container.v1.Cluster]("google-native:container/v1:Cluster/getKubeconfig", args, this, opts)
             |
             |object Cluster:
             |  def apply(using ctx: besom.types.Context)(
             |    name: besom.util.NonEmptyString,
             |    args: ClusterArgs = ClusterArgs(),
             |    opts: besom.CustomResourceOptions = besom.CustomResourceOptions()
             |  ): besom.types.Output[Cluster] =
             |    ctx.readOrRegisterResource[Cluster, ClusterArgs]("google-native:container/v1:Cluster", name, args, opts)
             |
             |  given resourceDecoder(using besom.types.Context): besom.types.ResourceDecoder[Cluster] =
             |    besom.internal.ResourceDecoder.derived[Cluster]
             |
             |  given decoder(using besom.types.Context): besom.types.Decoder[Cluster] =
             |    besom.internal.Decoder.customResourceDecoder[Cluster]
             |
             |  given outputOps: {} with
             |    extension(output: besom.types.Output[Cluster])
             |      def urn : besom.types.Output[besom.types.URN] = output.flatMap(_.urn)
             |      def id : besom.types.Output[besom.types.ResourceId] = output.flatMap(_.id)
             |      def name : besom.types.Output[String] = output.flatMap(_.name)
             |""".stripMargin,
        "src/container/v1/ClusterArgs.scala" ->
          """|package besom.api.googlenative.container.v1
             |
             |final case class ClusterArgs private(
             |
             |)
             |
             |object ClusterArgs:
             |  def apply(
             |
             |  )(using besom.types.Context): ClusterArgs =
             |    new ClusterArgs(
             |
             |    )
             |
             |  given encoder(using besom.types.Context): besom.types.Encoder[ClusterArgs] =
             |    besom.internal.Encoder.derived[ClusterArgs]
             |  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[ClusterArgs] =
             |    besom.internal.ArgsEncoder.derived[ClusterArgs]
             |""".stripMargin,
        "src/container/v1/ClusterGetKubeconfigArgs.scala" ->
          """|package besom.api.googlenative.container.v1
             |
             |final case class ClusterGetKubeconfigArgs private(
             |
             |)
             |
             |object ClusterGetKubeconfigArgs:
             |  def apply(
             |
             |  )(using besom.types.Context): ClusterGetKubeconfigArgs =
             |    new ClusterGetKubeconfigArgs(
             |
             |    )
             |
             |  given encoder(using besom.types.Context): besom.types.Encoder[ClusterGetKubeconfigArgs] =
             |    besom.internal.Encoder.derived[ClusterGetKubeconfigArgs]
             |  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[ClusterGetKubeconfigArgs] =
             |    besom.internal.ArgsEncoder.derived[ClusterGetKubeconfigArgs]
             |""".stripMargin,
        "src/container/v1/ClusterGetKubeconfigResult.scala" ->
          """|package besom.api.googlenative.container.v1
             |
             |
             |final case class ClusterGetKubeconfigResult private(
             |  kubeconfig: String
             |)
             |object ClusterGetKubeconfigResult :
             |
             |
             |  given outputOps: {} with
             |    extension(output: besom.types.Output[ClusterGetKubeconfigResult])
             |      def kubeconfig : besom.types.Output[String] = output.map(_.kubeconfig)
             |
             |  given decoder(using besom.types.Context): besom.types.Decoder[ClusterGetKubeconfigResult] =
             |    besom.internal.Decoder.derived[ClusterGetKubeconfigResult]
             |
             |  given optionOutputOps: {} with
             |    extension(output: besom.types.Output[scala.Option[ClusterGetKubeconfigResult]])
             |      def kubeconfig : besom.types.Output[scala.Option[String]] = output.map(_.map(_.kubeconfig))
             |""".stripMargin,
        "src/container/v1/getCluster.scala" ->
          """|package besom.api.googlenative.container.v1
             |
             |def getCluster(using ctx: besom.types.Context)(
             |  args: besom.api.googlenative.container.v1.GetClusterArgs,
             |  opts: besom.InvokeOptions = besom.InvokeOptions()
             |): besom.types.Output[scala.Unit] =
             |   ctx.invoke[besom.api.googlenative.container.v1.GetClusterArgs, scala.Unit]("google-native:container/v1:getCluster", args, opts)
             |""".stripMargin,
        "src/container/v1/GetClusterArgs.scala" ->
          """|package besom.api.googlenative.container.v1
             |
             |final case class GetClusterArgs private(
             |  clusterId: besom.types.Output[String],
             |  location: besom.types.Output[scala.Option[String]]
             |)
             |
             |object GetClusterArgs:
             |  def apply(
             |    clusterId: besom.types.Input[String],
             |    location: besom.types.Input.Optional[String] = scala.None
             |  )(using besom.types.Context): GetClusterArgs =
             |    new GetClusterArgs(
             |      clusterId = clusterId.asOutput(isSecret = false),
             |      location = location.asOptionOutput(isSecret = false)
             |    )
             |
             |  given encoder(using besom.types.Context): besom.types.Encoder[GetClusterArgs] =
             |    besom.internal.Encoder.derived[GetClusterArgs]
             |  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[GetClusterArgs] =
             |    besom.internal.ArgsEncoder.derived[GetClusterArgs]
             |""".stripMargin
      ),
      ignored = List(
        "src/index/Provider.scala",
        "src/index/ProviderArgs.scala"
      )
    ),
    Data(
      name = "Enum property with default string union value",
      json = """|{
                |  "name": "azure-native",
                |  "resources": {
                |    "azure-native:windowsesu:MultipleActivationKey": {
                |      "properties": {
                |        "supportType": {
                |          "type": "string",
                |          "description": "Type of support",
                |          "default": "SupplementalServicing"
                |        }
                |      },
                |      "type": "object",
                |      "inputProperties": {
                |        "supportType": {
                |          "oneOf": [
                |            {
                |              "type": "string"
                |            },
                |            {
                |              "$ref": "#/types/azure-native:windowsesu:SupportType"
                |            }
                |          ],
                |          "default": "SupplementalServicing"
                |        }
                |      }
                |    }
                |  },
                |  "types": {
                |    "azure-native:windowsesu:SupportType": {
                |      "type": "string",
                |      "enum": [
                |        {
                |          "value": "SupplementalServicing"
                |        },
                |        {
                |          "value": "PremiumAssurance"
                |        }
                |      ]
                |    }
                |  }
                |}
                |""".stripMargin,
      ignored = List(
        "src/index/Provider.scala",
        "src/index/ProviderArgs.scala",
        "src/windowsesu/MultipleActivationKey.scala"
      ),
      expected = Map(
        "src/windowsesu/enums/SupportType.scala" ->
          """|package besom.api.azurenative.windowsesu.enums
             |
             |sealed abstract class SupportType(val name: String, val value: String) extends besom.types.StringEnum
             |
             |object SupportType extends besom.types.EnumCompanion[String, SupportType]("SupportType"):
             |  object SupplementalServicing extends SupportType("SupplementalServicing", "SupplementalServicing")
             |  object PremiumAssurance extends SupportType("PremiumAssurance", "PremiumAssurance")
             |
             |  override val allInstances: Seq[SupportType] = Seq(
             |    SupplementalServicing,
             |    PremiumAssurance
             |  )
             |  given besom.types.EnumCompanion[String, SupportType] = this
             |""".stripMargin,
        "src/windowsesu/MultipleActivationKeyArgs.scala" ->
          """|package besom.api.azurenative.windowsesu
             |
             |final case class MultipleActivationKeyArgs private(
             |  supportType: besom.types.Output[String | besom.api.azurenative.windowsesu.enums.SupportType]
             |)
             |
             |object MultipleActivationKeyArgs:
             |  def apply(
             |    supportType: besom.types.Input[String | besom.api.azurenative.windowsesu.enums.SupportType] = "SupplementalServicing"
             |  )(using besom.types.Context): MultipleActivationKeyArgs =
             |    new MultipleActivationKeyArgs(
             |      supportType = supportType.asOutput(isSecret = false)
             |    )
             |
             |  given encoder(using besom.types.Context): besom.types.Encoder[MultipleActivationKeyArgs] =
             |    besom.internal.Encoder.derived[MultipleActivationKeyArgs]
             |  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[MultipleActivationKeyArgs] =
             |    besom.internal.ArgsEncoder.derived[MultipleActivationKeyArgs]
             |""".stripMargin
      )
    ),
    Data(
      name = "Enum property with default no string union value",
      json = """|{
                |  "name": "azure-native",
                |  "resources": {
                |    "azure-native:hybriddata:JobDefinition": {
                |      "properties": {
                |        "userConfirmation": {
                |          "type": "string",
                |          "description": "Enum to detect if user confirmation is required. If not passed will default to NotRequired.",
                |          "default": "NotRequired"
                |        }
                |      },
                |      "type": "object",
                |      "inputProperties": {
                |        "userConfirmation": {
                |          "$ref": "#/types/azure-native:hybriddata:UserConfirmation",
                |          "default": "NotRequired"
                |        }
                |      }
                |    }
                |  },
                |  "types": {
                |    "azure-native:hybriddata:UserConfirmation": {
                |      "description": "Enum to detect if user confirmation is required. If not passed will default to NotRequired.",
                |      "type": "string",
                |      "enum": [
                |        {
                |          "value": "NotRequired"
                |        },
                |        {
                |          "value": "Required"
                |        }
                |      ]
                |    }
                |  }
                |}
                |""".stripMargin,
      ignored = List(
        "src/index/Provider.scala",
        "src/index/ProviderArgs.scala",
        "src/hybriddata/JobDefinition.scala"
      ),
      expected = Map(
        "src/hybriddata/enums/UserConfirmation.scala" ->
          """|package besom.api.azurenative.hybriddata.enums
             |
             |sealed abstract class UserConfirmation(val name: String, val value: String) extends besom.types.StringEnum
             |
             |object UserConfirmation extends besom.types.EnumCompanion[String, UserConfirmation]("UserConfirmation"):
             |  object NotRequired extends UserConfirmation("NotRequired", "NotRequired")
             |  object Required extends UserConfirmation("Required", "Required")
             |
             |  override val allInstances: Seq[UserConfirmation] = Seq(
             |    NotRequired,
             |    Required
             |  )
             |  given besom.types.EnumCompanion[String, UserConfirmation] = this
             |""".stripMargin,
        "src/hybriddata/JobDefinitionArgs.scala" ->
          """|package besom.api.azurenative.hybriddata
             |
             |final case class JobDefinitionArgs private(
             |  userConfirmation: besom.types.Output[besom.api.azurenative.hybriddata.enums.UserConfirmation]
             |)
             |
             |object JobDefinitionArgs:
             |  def apply(
             |    userConfirmation: besom.types.Input[besom.api.azurenative.hybriddata.enums.UserConfirmation] = besom.api.azurenative.hybriddata.enums.UserConfirmation.NotRequired
             |  )(using besom.types.Context): JobDefinitionArgs =
             |    new JobDefinitionArgs(
             |      userConfirmation = userConfirmation.asOutput(isSecret = false)
             |    )
             |
             |  given encoder(using besom.types.Context): besom.types.Encoder[JobDefinitionArgs] =
             |    besom.internal.Encoder.derived[JobDefinitionArgs]
             |  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[JobDefinitionArgs] =
             |    besom.internal.ArgsEncoder.derived[JobDefinitionArgs]
             |""".stripMargin
      )
    ),
    Data(
      name = "Enum properties with default integer and double values",
      json = """|{
                |  "name": "plant",
                |  "resources": {
                |    "plant:tree/v1:RubberTree": {
                |      "inputProperties": {
                |        "container": {
                |          "$ref": "#/types/plant::Container"
                |        }
                |      },
                |      "properties": {
                |        "container": {
                |          "$ref": "#/types/plant::Container"
                |        }
                |      }
                |    }
                |  },
                |  "types": {
                |    "plant::Container": {
                |      "type": "object",
                |      "properties": {
                |        "size": {
                |          "$ref": "#/types/plant::ContainerSize",
                |          "default": 4
                |        },
                |        "brightness": {
                |          "$ref": "#/types/plant::ContainerBrightness",
                |          "default": 1.0
                |        }
                |      }
                |    },
                |    "plant::ContainerSize": {
                |      "type": "integer",
                |      "description": "plant container sizes",
                |      "enum": [
                |        {
                |          "value": 4,
                |          "name": "FourInch"
                |        },
                |        {
                |          "value": 6,
                |          "name": "SixInch"
                |        },
                |        {
                |          "value": 8,
                |          "name": "EightInch"
                |        }
                |      ]
                |    },
                |    "plant::ContainerBrightness": {
                |      "type": "number",
                |      "enum": [
                |        {
                |          "name": "ZeroPointOne",
                |          "value": 0.1
                |        },
                |        {
                |          "name": "One",
                |          "value": 1.0
                |        }
                |      ]
                |    }
                |  }
                |}
                |""".stripMargin,
      ignored = List(
        "src/index/Provider.scala",
        "src/index/ProviderArgs.scala",
        "src/index/outputs/Container.scala",
        "src/index/enums/ContainerSize.scala",
        "src/index/enums/ContainerBrightness.scala",
        "src/tree/v1/RubberTree.scala",
        "src/tree/v1/RubberTreeArgs.scala"
      ),
      expected = Map(
        "src/index/inputs/ContainerArgs.scala" ->
          """|package besom.api.plant.inputs
             |
             |final case class ContainerArgs private(
             |  brightness: besom.types.Output[besom.api.plant.enums.ContainerBrightness],
             |  size: besom.types.Output[besom.api.plant.enums.ContainerSize]
             |)
             |
             |object ContainerArgs:
             |  def apply(
             |    brightness: besom.types.Input[besom.api.plant.enums.ContainerBrightness] = besom.api.plant.enums.ContainerBrightness.One,
             |    size: besom.types.Input[besom.api.plant.enums.ContainerSize] = besom.api.plant.enums.ContainerSize.FourInch
             |  )(using besom.types.Context): ContainerArgs =
             |    new ContainerArgs(
             |      brightness = brightness.asOutput(isSecret = false),
             |      size = size.asOutput(isSecret = false)
             |    )
             |
             |  given encoder(using besom.types.Context): besom.types.Encoder[ContainerArgs] =
             |    besom.internal.Encoder.derived[ContainerArgs]
             |  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[ContainerArgs] =
             |    besom.internal.ArgsEncoder.derived[ContainerArgs]
             |""".stripMargin
      )
    ),
    Data(
      name = "Type with dots in package segment",
      json = """|{
                |  "name": "embedded-crd",
                |  "types": {
                |    "kubernetes:crd.k8s.amazonaws.com/v1alpha1:ENIConfig": {
                |      "properties": {
                |        "apiVersion": {
                |          "type": "string",
                |          "const": "crd.k8s.amazonaws.com/v1alpha1"
                |        },
                |        "kind": {
                |          "type": "string",
                |          "const": "ENIConfig"
                |        },
                |        "spec": {
                |          "type": "object",
                |          "$ref": "#/types/kubernetes:crd.k8s.amazonaws.com/v1alpha1:ENIConfigSpec"
                |        }
                |      },
                |      "type": "object"
                |    },
                |    "kubernetes:crd.k8s.amazonaws.com/v1alpha1:ENIConfigSpec": {
                |      "properties": {
                |        "securityGroups": {
                |          "type": "array",
                |          "items": {
                |            "type": "string"
                |          }
                |        },
                |        "subnet": {
                |          "type": "string"
                |        }
                |      },
                |      "type": "object"
                |    }
                |  }
                |}""".stripMargin,
      ignored = List(
        "src/index/Provider.scala",
        "src/index/ProviderArgs.scala"
      ),
      expected = Map(
        "src/crdk8samazonawscom/v1alpha1/outputs/EniConfig.scala" ->
          """|package besom.api.kubernetes.crdk8samazonawscom.v1alpha1.outputs
             |
             |
             |final case class EniConfig private(
             |  apiVersion: String,
             |  kind: String,
             |  spec: scala.Option[besom.api.kubernetes.crdk8samazonawscom.v1alpha1.outputs.EniConfigSpec]
             |)
             |object EniConfig :
             |
             |
             |  given outputOps: {} with
             |    extension(output: besom.types.Output[EniConfig])
             |      def apiVersion : besom.types.Output[String] = output.map(_.apiVersion)
             |      def kind : besom.types.Output[String] = output.map(_.kind)
             |      def spec : besom.types.Output[scala.Option[besom.api.kubernetes.crdk8samazonawscom.v1alpha1.outputs.EniConfigSpec]] = output.map(_.spec)
             |
             |  given decoder(using besom.types.Context): besom.types.Decoder[EniConfig] =
             |    besom.internal.Decoder.derived[EniConfig]
             |
             |  given optionOutputOps: {} with
             |    extension(output: besom.types.Output[scala.Option[EniConfig]])
             |      def apiVersion : besom.types.Output[scala.Option[String]] = output.map(_.map(_.apiVersion))
             |      def kind : besom.types.Output[scala.Option[String]] = output.map(_.map(_.kind))
             |      def spec : besom.types.Output[scala.Option[besom.api.kubernetes.crdk8samazonawscom.v1alpha1.outputs.EniConfigSpec]] = output.map(_.flatMap(_.spec))
             |
             |
             |
             |""".stripMargin,
        "src/crdk8samazonawscom/v1alpha1/inputs/EniConfigArgs.scala" ->
          """|package besom.api.kubernetes.crdk8samazonawscom.v1alpha1.inputs
             |
             |final case class EniConfigArgs private(
             |  apiVersion: besom.types.Output[String],
             |  kind: besom.types.Output[String],
             |  spec: besom.types.Output[scala.Option[besom.api.kubernetes.crdk8samazonawscom.v1alpha1.inputs.EniConfigSpecArgs]]
             |)
             |
             |object EniConfigArgs:
             |  def apply(
             |    spec: besom.types.Input.Optional[besom.api.kubernetes.crdk8samazonawscom.v1alpha1.inputs.EniConfigSpecArgs] = scala.None
             |  )(using besom.types.Context): EniConfigArgs =
             |    new EniConfigArgs(
             |      apiVersion = besom.types.Output("crd.k8s.amazonaws.com/v1alpha1"),
             |      kind = besom.types.Output("ENIConfig"),
             |      spec = spec.asOptionOutput(isSecret = false)
             |    )
             |
             |  given encoder(using besom.types.Context): besom.types.Encoder[EniConfigArgs] =
             |    besom.internal.Encoder.derived[EniConfigArgs]
             |  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[EniConfigArgs] =
             |    besom.internal.ArgsEncoder.derived[EniConfigArgs]
             |
             |
             |""".stripMargin,
        "src/crdk8samazonawscom/v1alpha1/outputs/EniConfigSpec.scala" ->
          """|package besom.api.kubernetes.crdk8samazonawscom.v1alpha1.outputs
             |
             |
             |final case class EniConfigSpec private(
             |  securityGroups: scala.Option[scala.collection.immutable.List[String]],
             |  subnet: scala.Option[String]
             |)
             |object EniConfigSpec :
             |
             |
             |  given outputOps: {} with
             |    extension(output: besom.types.Output[EniConfigSpec])
             |      def securityGroups : besom.types.Output[scala.Option[scala.collection.immutable.List[String]]] = output.map(_.securityGroups)
             |      def subnet : besom.types.Output[scala.Option[String]] = output.map(_.subnet)
             |
             |  given decoder(using besom.types.Context): besom.types.Decoder[EniConfigSpec] =
             |    besom.internal.Decoder.derived[EniConfigSpec]
             |
             |  given optionOutputOps: {} with
             |    extension(output: besom.types.Output[scala.Option[EniConfigSpec]])
             |      def securityGroups : besom.types.Output[scala.Option[scala.collection.immutable.List[String]]] = output.map(_.flatMap(_.securityGroups))
             |      def subnet : besom.types.Output[scala.Option[String]] = output.map(_.flatMap(_.subnet))
             |
             |
             |
             |""".stripMargin,
        "src/crdk8samazonawscom/v1alpha1/inputs/EniConfigSpecArgs.scala" ->
          """|package besom.api.kubernetes.crdk8samazonawscom.v1alpha1.inputs
             |
             |final case class EniConfigSpecArgs private(
             |  securityGroups: besom.types.Output[scala.Option[scala.collection.immutable.List[String]]],
             |  subnet: besom.types.Output[scala.Option[String]]
             |)
             |
             |object EniConfigSpecArgs:
             |  def apply(
             |    securityGroups: besom.types.Input.Optional[scala.collection.immutable.List[besom.types.Input[String]]] = scala.None,
             |    subnet: besom.types.Input.Optional[String] = scala.None
             |  )(using besom.types.Context): EniConfigSpecArgs =
             |    new EniConfigSpecArgs(
             |      securityGroups = securityGroups.asOptionOutput(isSecret = false),
             |      subnet = subnet.asOptionOutput(isSecret = false)
             |    )
             |
             |  given encoder(using besom.types.Context): besom.types.Encoder[EniConfigSpecArgs] =
             |    besom.internal.Encoder.derived[EniConfigSpecArgs]
             |  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[EniConfigSpecArgs] =
             |    besom.internal.ArgsEncoder.derived[EniConfigSpecArgs]
             |""".stripMargin
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
        interceptMessage[Exception](data.expectedError.get)(codegen.scalaFiles(pulumiPackage, packageInfo))
      else
        codegen.scalaFiles(pulumiPackage, packageInfo).foreach {
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
