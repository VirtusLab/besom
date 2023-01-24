package besom.internal

import besom.util.*

trait CommonResourceOptions:
  def id: Option[Output[NonEmptyString]]
  def parent: Option[Resource]
  def dependsOn: Output[List[Resource]]
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

final case class CommonResourceOptionsImpl(
  id: Option[Output[NonEmptyString]],
  parent: Option[Resource],
  dependsOn: Output[List[Resource]],
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
) extends CommonResourceOptions

sealed trait ResourceOptions

final case class CustomResourceOptions private[internal] (
  common: CommonResourceOptions,
  provider: Option[String], // ProviderResource // TODO
  deleteBeforeReplace: Boolean,
  additionalSecretOutputs: List[String],
  importId: Option[String]
) extends ResourceOptions,
      CommonResourceOptions:
  export common.*

final case class ComponentResourceOptions private[internal] (
  common: CommonResourceOptions,
  providers: List[String] // ProviderResource // TODO
) extends ResourceOptions,
      CommonResourceOptions:
  export common.*

object CustomResourceOptions:
  def apply(using Context)(
    id: Output[NonEmptyString] | NotProvided = NotProvided,
    parent: Resource | NotProvided = NotProvided,
    dependsOn: Output[List[Resource]] = Output(List.empty[Resource]),
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
  ): CustomResourceOptions =
    val common = CommonResourceOptionsImpl(
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

object ComponentResourceOptions:
  def apply(using Context)(
    providers: List[String] = List.empty, // ProviderResource // TODO
    id: Output[NonEmptyString] | NotProvided = NotProvided,
    parent: Resource | NotProvided = NotProvided,
    dependsOn: Output[List[Resource]] = Output(List.empty[Resource]),
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
  ): ComponentResourceOptions =
    val common = CommonResourceOptionsImpl(
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
