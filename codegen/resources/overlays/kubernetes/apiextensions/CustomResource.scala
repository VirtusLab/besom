package besom.api.kubernetes.apiextensions

final case class CustomResource[A] private (
  urn: besom.types.Output[besom.types.URN],
  id: besom.types.Output[besom.types.ResourceId],
  apiVersion: besom.types.Output[String],
  kind: besom.types.Output[String],
  metadata: besom.types.Output[besom.api.kubernetes.meta.v1.outputs.ObjectMeta],
  spec: besom.types.Output[A]
) extends besom.CustomResource

object CustomResource:
  def apply[A: besom.types.Encoder: besom.types.Decoder](
    name: besom.util.NonEmptyString,
    args: CustomResourceArgs[A],
    opts: besom.ResourceOptsVariant.Component ?=> besom.ComponentResourceOptions = besom.ComponentResourceOptions()
  ): besom.types.Output[CustomResource[A]] = {
    val resourceName                                     = besom.types.ResourceType.unsafeOf(s"kubernetes:${args.apiVersion}:${args.kind}")
    given besom.types.ResourceDecoder[CustomResource[A]] = besom.internal.ResourceDecoder.derived[CustomResource[A]]
    given besom.types.Decoder[CustomResource[A]]         = besom.internal.Decoder.customResourceDecoder[CustomResource[A]]
    given besom.types.Encoder[CustomResourceArgs[A]]     = besom.internal.Encoder.derived[CustomResourceArgs[A]]
    given besom.types.ArgsEncoder[CustomResourceArgs[A]] = besom.internal.ArgsEncoder.derived[CustomResourceArgs[A]]
    ctx.readOrRegisterResource[CustomResource[A], CustomResourceArgs[A]](
      resourceName,
      name,
      args,
      opts(using besom.ResourceOptsVariant.Component)
    )
  }

  given outputOps: {} with
    extension [A](output: besom.types.Output[CustomResource[A]])
      def urn: besom.types.Output[besom.types.URN]                                      = output.flatMap(_.urn)
      def id: besom.types.Output[besom.types.ResourceId]                                = output.flatMap(_.id)
      def apiVersion: besom.types.Output[String]                                        = output.flatMap(_.apiVersion)
      def kind: besom.types.Output[String]                                              = output.flatMap(_.kind)
      def metadata: besom.types.Output[besom.api.kubernetes.meta.v1.outputs.ObjectMeta] = output.flatMap(_.metadata)
      def spec: besom.types.Output[A]                                                   = output.flatMap(_.spec)
