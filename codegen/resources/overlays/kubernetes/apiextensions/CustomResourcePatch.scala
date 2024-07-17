package besom.api.kubernetes.apiextensions

final case class CustomResourcePatch[A] private (
  urn: besom.types.Output[besom.types.URN],
  id: besom.types.Output[besom.types.ResourceId],
  apiVersion: besom.types.Output[String],
  kind: besom.types.Output[String],
  metadata: besom.types.Output[scala.Option[besom.api.kubernetes.meta.v1.outputs.ObjectMetaPatch]],
  spec: besom.types.Output[scala.Option[A]]
) extends besom.CustomResource

object CustomResourcePatch:
  def apply[A: besom.types.Encoder: besom.types.Decoder](using ctx: besom.types.Context)(
    name: besom.util.NonEmptyString,
    args: CustomResourcePatchArgs[A],
    opts: besom.ResourceOptsVariant.Custom ?=> besom.CustomResourceOptions = besom.CustomResourceOptions()
  ): besom.types.Output[CustomResourcePatch[A]] = {
    val resourceName = besom.types.ResourceType.unsafeOf(s"kubernetes:${args.apiVersion}:${args.kind}")
    given besom.types.ResourceDecoder[CustomResourcePatch[A]] = besom.internal.ResourceDecoder.derived[CustomResourcePatch[A]]
    given besom.types.Decoder[CustomResourcePatch[A]]         = besom.internal.Decoder.customResourceDecoder[CustomResourcePatch[A]]
    given besom.types.Encoder[CustomResourcePatchArgs[A]]     = besom.internal.Encoder.derived[CustomResourcePatchArgs[A]]
    given besom.types.ArgsEncoder[CustomResourcePatchArgs[A]] = besom.internal.ArgsEncoder.derived[CustomResourcePatchArgs[A]]
    ctx.readOrRegisterResource[CustomResourcePatch[A], CustomResourcePatchArgs[A]](
      resourceName,
      name,
      args,
      opts(using besom.ResourceOptsVariant.Custom)
    )
  }

  given outputOps: {} with
    extension [A](output: besom.types.Output[CustomResourcePatch[A]])
      def urn: besom.types.Output[besom.types.URN]                                                         = output.flatMap(_.urn)
      def id: besom.types.Output[besom.types.ResourceId]                                                   = output.flatMap(_.id)
      def apiVersion: besom.types.Output[String]                                                           = output.flatMap(_.apiVersion)
      def kind: besom.types.Output[String]                                                                 = output.flatMap(_.kind)
      def metadata: besom.types.Output[scala.Option[besom.api.kubernetes.meta.v1.outputs.ObjectMetaPatch]] = output.flatMap(_.metadata)
      def spec: besom.types.Output[scala.Option[A]]                                                        = output.flatMap(_.spec)
