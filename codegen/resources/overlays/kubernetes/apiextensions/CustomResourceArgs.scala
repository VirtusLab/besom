package besom.api.kubernetes.apiextensions

/** The set of arguments for constructing a CustomResource resource.
  *
  * NOTE: This type is fairly loose, since other than `apiVersion` and `kind`, there are no fields required across all CRDs.
  */
trait CustomResourceArgsLike:
  /** APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest
    * internal value, and may reject unrecognized values.
    * @see
    *   [[https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources Kubernetes API Version]]
    * @return
    */
  def apiVersion: besom.types.Input[String]

  /** Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client
    * submits requests to. Cannot be updated. In CamelCase.
    *
    * @see
    *   [[https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds Kubernetes Kinds]]
    * @return
    */
  def kind: besom.types.Input[String]

  /** Standard Kubernetes object metadata
    * @see
    *   [[https://git.k8s.io/community/contributors/devel/api-conventions.md#metadata Kubernetes Metadata]]
    * @return
    *   the object metadata
    */
  def metadata: besom.types.Input[scala.Option[besom.api.kubernetes.meta.v1.inputs.ObjectMetaArgs]]
