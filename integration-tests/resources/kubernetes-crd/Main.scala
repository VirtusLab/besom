import besom.*
import besom.api.kubernetes as k8s

@main def main = Pulumi.run {
  val provider = k8s.Provider(
    "k8s",
    k8s.ProviderArgs(
      renderYamlToDirectory = "output",
      cluster = Output.secret[Option[String]](None)
    )
  )

  // Create a CustomResourceDefinition and a CustomResource.

  val foobarGroup      = "stable.example.com"
  val foobarKind       = "FooBar"
  val foobarVersion    = "v1"
  val foobarApiVersion = s"${foobarGroup}/${foobarVersion}"

  val crd = k8s.apiextensions.v1.CustomResourceDefinition(
    "foobar",
    k8s.apiextensions.v1.CustomResourceDefinitionArgs(
      metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
        name = "foobars.stable.example.com"
      ),
      spec = k8s.apiextensions.v1.inputs.CustomResourceDefinitionSpecArgs(
        group = foobarGroup,
        names = k8s.apiextensions.v1.inputs.CustomResourceDefinitionNamesArgs(
          kind = foobarKind,
          plural = "foobars",
          singular = "foobar",
          shortNames = List("fb")
        ),
        scope = "Namespaced",
        versions = List(
          k8s.apiextensions.v1.inputs.CustomResourceDefinitionVersionArgs(
            name = foobarVersion,
            served = true,
            storage = true,
            schema = k8s.apiextensions.v1.inputs.CustomResourceValidationArgs(
              openAPIV3Schema = k8s.apiextensions.v1.inputs.JsonSchemaPropsArgs(
                `type` = "object",
                properties = Map(
                  "spec" -> k8s.apiextensions.v1.inputs.JsonSchemaPropsArgs(
                    `type` = "object",
                    properties = Map(
                      "foo" -> k8s.apiextensions.v1.inputs.JsonSchemaPropsArgs(
                        `type` = "string"
                      ),
                      "bar" -> k8s.apiextensions.v1.inputs.JsonSchemaPropsArgs(
                        `type` = "integer"
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    ),
    opts = opts(provider = provider)
  )

  case class FoobarSpec private (
    foo: Option[String],
    bar: Option[Int]
  )
  object FoobarSpec:
    given decoder(using besom.types.Context): besom.types.Decoder[FoobarSpec] =
      besom.internal.Decoder.derived[FoobarSpec]

  case class FoobarSpecArgs(
    foo: besom.types.Output[Option[String]],
    bar: besom.types.Output[Option[Int]]
  )
  object FoobarSpecArgs:
    def apply(
      foo: besom.types.Input.Optional[String] = None,
      bar: besom.types.Input.Optional[Int] = None
    )(using besom.types.Context): FoobarSpecArgs =
      new FoobarSpecArgs(
        foo = foo.asOptionOutput(isSecret = false),
        bar = bar.asOptionOutput(isSecret = false)
      )
    given encoder(using besom.types.Context): besom.types.Encoder[FoobarSpecArgs] =
      besom.internal.Encoder.derived[FoobarSpecArgs]
    given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[FoobarSpecArgs] =
      besom.internal.ArgsEncoder.derived[FoobarSpecArgs]

  case class FoobarArgs(
    apiVersion: besom.types.Output[String],
    kind: besom.types.Output[String],
    metadata: besom.types.Output[scala.Option[besom.api.kubernetes.meta.v1.inputs.ObjectMetaArgs]],
    spec: besom.types.Output[FoobarSpecArgs]
  ) extends k8s.apiextensions.CustomResourceArgsLike
  object FoobarArgs:
    def apply(
      apiVersion: besom.types.Input[String],
      kind: besom.types.Input[String],
      metadata: besom.types.Input.Optional[besom.api.kubernetes.meta.v1.inputs.ObjectMetaArgs] = scala.None,
      spec: besom.types.Input[FoobarSpecArgs] = FoobarSpecArgs()
    )(using besom.types.Context): FoobarArgs =
      new FoobarArgs(
        apiVersion = apiVersion.asOutput(isSecret = false),
        kind = kind.asOutput(isSecret = false),
        metadata = metadata.asOptionOutput(isSecret = false),
        spec = spec.asOutput(isSecret = false)
      )
    given encoder(using besom.types.Context): besom.types.Encoder[FoobarArgs] =
      besom.internal.Encoder.derived[FoobarArgs]
    given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[FoobarArgs] =
      besom.internal.ArgsEncoder.derived[FoobarArgs]
  end FoobarArgs

  case class Foobar(
    urn: besom.types.Output[besom.types.URN],
    id: besom.types.Output[besom.types.ResourceId],
    apiVersion: besom.types.Output[String],
    kind: besom.types.Output[String],
    metadata: besom.types.Output[besom.api.kubernetes.meta.v1.outputs.ObjectMeta],
    spec: besom.types.Output[FoobarSpec]
  ) extends k8s.apiextensions.CustomResourceLike
  object Foobar extends k8s.apiextensions.CustomResourceFactory[Foobar, FoobarArgs]:
    given resourceDecoder(using besom.types.Context): besom.types.ResourceDecoder[Foobar] =
      besom.internal.ResourceDecoder.derived[Foobar]
    given decoder(using besom.types.Context): besom.types.Decoder[Foobar] =
      besom.internal.Decoder.customResourceDecoder[Foobar]
    given outputOps: {} with
      extension (output: besom.types.Output[Foobar]) def spec: besom.types.Output[FoobarSpec] = output.flatMap(_.spec)
  end Foobar

  val cr = Foobar(
    "foobar",
    FoobarArgs(
      apiVersion = foobarApiVersion,
      kind = foobarKind,
      metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
        name = "example"
      ),
      spec = FoobarSpecArgs(
        foo = "Hello, World!",
        bar = 42
      )
    ),
    opts = opts(provider = provider)
  )

  Stack(crd, cr)
}
