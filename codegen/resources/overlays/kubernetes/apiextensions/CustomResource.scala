package besom.api.kubernetes.apiextensions

import besom.util.interpolator.{*, given}
import besom.types.ResourceType

/** CustomResource represents a resource definition we'd use to create an instance of a Kubernetes CustomResourceDefinition (CRD).
  *
  * For example, the CoreOS Prometheus operator exposes a CRD `monitoring.coreos.com/ServiceMonitor` to create a `ServiceMonitor`, we'd pass
  * a `CustomResourceArgs` containing the `ServiceMonitor` definition to `apiextensions.CustomResource`.
  */
trait CustomResourceLike extends besom.CustomResource:
  /** APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest
    * internal value, and may reject unrecognized values.
    *
    * @see
    *   [[https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources Kubernetes API Version]]
    * @return
    */
  def apiVersion: besom.types.Output[String]

  /** Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client
    * submits requests to. Cannot be updated. In CamelCase.
    *
    * @see
    *   [[https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds Kubernetes Kinds]]
    * @return
    */
  def kind: besom.types.Output[String]

  /** Standard Kubernetes object metadata
    *
    * @see
    *   [[https://git.k8s.io/community/contributors/devel/api-conventions.md#metadata Kubernetes Metadata]]
    * @return
    *   the object metadata
    */
  def metadata: besom.types.Output[besom.api.kubernetes.meta.v1.outputs.ObjectMeta]
end CustomResourceLike

trait CustomResourceFactory[
  R <: besom.api.kubernetes.apiextensions.CustomResourceLike,
  A <: besom.api.kubernetes.apiextensions.CustomResourceArgsLike
]:

  /** Create a CustomResource resource with the given unique name, arguments, and options.
    *
    * @param name
    *   The unique name of the resource.
    * @param args
    *   The arguments to use to populate this resource's properties.
    * @param opts
    *   A bag of options that control this resource's behavior.
    */
  def apply(using ctx: besom.types.Context, dd: besom.types.ResourceDecoder[R], ae: besom.types.ArgsEncoder[A])(
    name: besom.util.NonEmptyString,
    args: A,
    opts: besom.ResourceOptsVariant.Custom ?=> besom.CustomResourceOptions = besom.CustomResourceOptions()
  ): besom.types.Output[R] =
    ctx.readOrRegisterResource[R, A](
      p"kubernetes:${args.apiVersion}:${args.kind}".flatMap(ResourceType.parseOutput(_)),
      name,
      args,
      opts(using besom.ResourceOptsVariant.Custom)
    )

  given factoryOutputOps: {} with
    extension (output: besom.types.Output[R])
      def urn: besom.types.Output[besom.types.URN]                                      = output.flatMap(_.urn)
      def id: besom.types.Output[besom.types.ResourceId]                                = output.flatMap(_.id)
      def apiVersion: besom.types.Output[String]                                        = output.flatMap(_.apiVersion)
      def kind: besom.types.Output[String]                                              = output.flatMap(_.kind)
      def metadata: besom.types.Output[besom.api.kubernetes.meta.v1.outputs.ObjectMeta] = output.flatMap(_.metadata)
