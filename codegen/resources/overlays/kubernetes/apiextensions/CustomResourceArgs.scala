package besom.api.kubernetes.apiextensions

final case class CustomResourceArgs[A] private (
  apiVersion: String,
  kind: String,
  metadata: besom.types.Output[scala.Option[besom.api.kubernetes.meta.v1.inputs.ObjectMetaArgs]],
  spec: besom.types.Output[A]
)

object CustomResourceArgs:
  def apply[A](
    apiVersion: String,
    kind: String,
    metadata: besom.types.Input.Optional[besom.api.kubernetes.meta.v1.inputs.ObjectMetaArgs] = scala.None,
    spec: besom.types.Input[A]
  )(using besom.types.Context): CustomResourceArgs[A] =
    new CustomResourceArgs(
      apiVersion = apiVersion,
      kind = kind,
      metadata = metadata.asOptionOutput(isSecret = false),
      spec = spec.asOutput(isSecret = false)
    )

  extension [A](customResourceArgs: CustomResourceArgs[A])
    def withArgs(
      apiVersion: String = customResourceArgs.apiVersion,
      kind: String = customResourceArgs.kind,
      metadata: besom.types.Input.Optional[besom.api.kubernetes.meta.v1.inputs.ObjectMetaArgs] = customResourceArgs.metadata,
      spec: besom.types.Input[A] = customResourceArgs.spec
    )(using besom.types.Context): CustomResourceArgs[A] =
      new CustomResourceArgs(
        apiVersion = apiVersion,
        kind = kind,
        metadata = metadata.asOptionOutput(isSecret = false),
        spec = spec.asOutput(isSecret = false)
      )
