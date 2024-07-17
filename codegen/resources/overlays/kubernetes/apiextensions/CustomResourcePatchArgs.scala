package besom.api.kubernetes.apiextensions

final case class CustomResourcePatchArgs[A] private (
  apiVersion: String,
  kind: String,
  metadata: besom.types.Output[scala.Option[besom.api.kubernetes.meta.v1.inputs.ObjectMetaPatchArgs]],
  spec: besom.types.Output[scala.Option[A]]
)

object CustomResourcePatchArgs:
  def apply[A](
    apiVersion: String,
    kind: String,
    metadata: besom.types.Input.Optional[besom.api.kubernetes.meta.v1.inputs.ObjectMetaPatchArgs] = scala.None,
    spec: besom.types.Input.Optional[A] = scala.None
  )(using besom.types.Context): CustomResourcePatchArgs[A] =
    new CustomResourcePatchArgs(
      apiVersion = apiVersion,
      kind = kind,
      metadata = metadata.asOptionOutput(isSecret = false),
      spec = spec.asOptionOutput(isSecret = false)
    )

  extension [A](customResourcePatchArgs: CustomResourcePatchArgs[A])
    def withArgs(
      apiVersion: String = customResourcePatchArgs.apiVersion,
      kind: String = customResourcePatchArgs.kind,
      metadata: besom.types.Input.Optional[besom.api.kubernetes.meta.v1.inputs.ObjectMetaPatchArgs] = customResourcePatchArgs.metadata,
      spec: besom.types.Input.Optional[A] = customResourcePatchArgs.spec
    )(using besom.types.Context): CustomResourcePatchArgs[A] =
      new CustomResourcePatchArgs(
        apiVersion = apiVersion,
        kind = kind,
        metadata = metadata.asOptionOutput(isSecret = false),
        spec = spec.asOptionOutput(isSecret = false)
      )
