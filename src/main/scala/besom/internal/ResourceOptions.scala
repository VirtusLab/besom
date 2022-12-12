package besom.internal

import besom.util.*

trait CommonResourceOptions[F[+_]]:
  def id: Option[Output[F, NonEmptyString]]
  def parent: Option[Resource]
  def dependsOn: Output[F, List[Resource]]
  def protect: Boolean
  def ignoreChanges: List[String]
  def version: String // TODO?
  def customTimeouts: Option[String] // CustomTimeouts // TODO
  // def resourceTransformations: List[ResourceTransformation], // TODO
  // def aliases: List[Output[Alias]], // TODO
  def urn: Option[String] // TODO better type
  def replaceOnChanges: List[String] // TODO?
  def retainOnDelete: Boolean
  def pluginDownloadUrl: Option[String]

final case class CommonResourceOptionsImpl[F[+_]](
    id: Option[Output[F, NonEmptyString]],
    parent: Option[Resource],
    dependsOn: Output[F, List[Resource]],
    protect: Boolean,
    ignoreChanges: List[String],
    version: String, // TODO?
    customTimeouts: Option[String], // CustomTimeouts // TODO
    // resourceTransformations: List[ResourceTransformation], // TODO
    // aliases: List[Output[Alias]], // TODO
    urn: Option[String], // TODO better type
    replaceOnChanges: List[String], // TODO?
    retainOnDelete: Boolean,
    pluginDownloadUrl: Option[String]
) extends CommonResourceOptions[F]

sealed trait ResourceOptions[F[+_]]

final case class CustomResourceOptions[F[+_]] private[internal] (
    common: CommonResourceOptions[F],
    provider: Option[String], // ProviderResource // TODO
    deleteBeforeReplace: Boolean,
    additionalSecretOutputs: List[String],
    importId: Option[String]
) extends ResourceOptions[F],
      CommonResourceOptions[F]:
  export common.*

final case class ComponentResourceOptions[F[+_]] private[internal] (
    common: CommonResourceOptions[F],
    providers: List[String] // ProviderResource // TODO
) extends ResourceOptions[F],
      CommonResourceOptions[F]:
  export common.*

object CustomResourceOptions:
  def apply[F[+_]](using Monad[F], Context): CustomResourceOptionsPartiallyApplied[F] =
    new CustomResourceOptionsPartiallyApplied[F]

object ComponentResourceOptions:
  def apply[F[+_]](using Monad[F], Context): ComponentResourceOptionsPartiallyApplied[F] =
    new ComponentResourceOptionsPartiallyApplied[F]

class CustomResourceOptionsPartiallyApplied[F[+_]](using F: Monad[F], ctx: Context):
  def apply(
      id: Output[F, NonEmptyString] | NotProvided = NotProvided,
      parent: Resource | NotProvided = NotProvided,
      dependsOn: Output[F, List[Resource]] = Output(List.empty[Resource]),
      protect: Boolean = false,
      ignoreChanges: List[String] = List.empty,
      version: NonEmptyString | NotProvided = NotProvided, // TODO? UGLY AF
      provider: String | NotProvided = NotProvided, // ProviderResource // TODO
      customTimeouts: String | NotProvided = NotProvided, // CustomTimeouts // TODO
      // resourceTransformations: List[ResourceTransformation], // TODO
      // aliases: List[Output[Alias]], // TODO
      urn: String | NotProvided = NotProvided, // TODO better type
      replaceOnChanges: List[String] = List.empty, // TODO?
      retainOnDelete: Boolean = false,
      pluginDownloadUrl: String | NotProvided = NotProvided,
      deleteBeforeReplace: Boolean = false,
      additionalSecretOutputs: List[String] = List.empty,
      importId: String | NotProvided = NotProvided
  ): ResourceOptions[F] =
    val common = CommonResourceOptionsImpl[F](
      id = id.asOption,
      parent = parent.asOption,
      dependsOn = dependsOn,
      protect = protect,
      ignoreChanges = ignoreChanges,
      version = version.asOption.getOrElse(""), // grpc & go are "strongly" typed
      customTimeouts = customTimeouts.asOption,
      urn = urn.asOption,
      replaceOnChanges = replaceOnChanges,
      retainOnDelete = retainOnDelete,
      pluginDownloadUrl = pluginDownloadUrl.asOption
    )
    new CustomResourceOptions(
      common,
      provider = provider.asOption,
      deleteBeforeReplace = deleteBeforeReplace,
      additionalSecretOutputs = additionalSecretOutputs,
      importId = importId.asOption
    )

class ComponentResourceOptionsPartiallyApplied[F[+_]](using F: Monad[F], ctx: Context):
  def apply(
      providers: List[String] = List.empty, // ProviderResource // TODO
      id: Output[F, NonEmptyString] | NotProvided = NotProvided,
      parent: Resource | NotProvided = NotProvided,
      dependsOn: Output[F, List[Resource]] = Output(List.empty[Resource]),
      protect: Boolean = false,
      ignoreChanges: List[String] = List.empty,
      version: NonEmptyString | NotProvided = NotProvided, // TODO? UGLY AF
      customTimeouts: String | NotProvided = NotProvided, // CustomTimeouts // TODO
      // resourceTransformations: List[ResourceTransformation], // TODO
      // aliases: List[Output[Alias]], // TODO
      urn: String | NotProvided = NotProvided, // TODO better type
      replaceOnChanges: List[String] = List.empty, // TODO?
      retainOnDelete: Boolean = false,
      pluginDownloadUrl: String | NotProvided = NotProvided
  ): ResourceOptions[F] =
    val common = CommonResourceOptionsImpl[F](
      id = id.asOption,
      parent = parent.asOption,
      dependsOn = dependsOn,
      protect = protect,
      ignoreChanges = ignoreChanges,
      version = version.asOption.getOrElse(""), // grpc & go are "strongly" typed
      customTimeouts = customTimeouts.asOption,
      urn = urn.asOption,
      replaceOnChanges = replaceOnChanges,
      retainOnDelete = retainOnDelete,
      pluginDownloadUrl = pluginDownloadUrl.asOption
    )
    new ComponentResourceOptions(common, providers)

// def test[F[+_]: Monad: Context]: Unit =
// CustomResourceOptions(urn = "sdsds", version = "dfsd")
// ComponentResourceOptions(customTimeouts = "works")
