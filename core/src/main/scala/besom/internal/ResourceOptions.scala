package besom.internal

import besom.util.*
import besom.types.{URN, ResourceId}

trait CommonResourceOptions:
  def parent: Option[Resource]
  def dependsOn: Output[List[Resource]]
  def protect: Boolean
  def ignoreChanges: List[String]
  def version: String // TODO?
  def customTimeouts: Option[String] // CustomTimeouts // TODO
  // def resourceTransformations: List[ResourceTransformation], // TODO
  // def aliases: List[Output[Alias]], // TODO
  // TODO this is only necessary for Resource deserialization, dependency resources and multi-language remote components
  def urn: Option[URN]
  def replaceOnChanges: List[String] // TODO?
  def retainOnDelete: Boolean
  def pluginDownloadUrl: String
  // TODO: new resource option: https://github.com/pulumi/pulumi/pull/11883 this also needs a supported feature check!
  def deletedWith: Option[Resource]

final case class CommonResourceOptionsImpl(
  parent: Option[Resource],
  dependsOn: Output[List[Resource]],
  protect: Boolean,
  ignoreChanges: List[String],
  version: String, // should be blank string when not provided TODO?
  customTimeouts: Option[String], // CustomTimeouts // TODO
  // resourceTransformations: List[ResourceTransformation], // TODO
  // aliases: List[Output[Alias]], // TODO
  urn: Option[URN],
  replaceOnChanges: List[String], // TODO?
  retainOnDelete: Boolean,
  pluginDownloadUrl: String, // should be blank string when not provided
  // TODO: new resource option: https://github.com/pulumi/pulumi/pull/11883 this also needs a supported feature check!
  deletedWith: Option[Resource]
) extends CommonResourceOptions

sealed trait ResourceOptions:
  def parent: Option[Resource]
  def version: String
  def pluginDownloadUrl: String
  def dependsOn: Output[List[Resource]]
  def protect: Boolean
  def ignoreChanges: List[String]
  def replaceOnChanges: List[String]
  def retainOnDelete: Boolean
  def urn: Option[URN]

  private[besom] def hasURN: Boolean = urn.isDefined
  private[besom] def hasImportId(using Context): Boolean = this match
    case cr: CustomResourceOptions         => cr.importId.isDefined
    case sr: StackReferenceResourceOptions => sr.importId.isDefined
    case _                                 => false

  private[besom] def getImportId(using Context): Option[ResourceId] = this match
    case cr: CustomResourceOptions         => cr.importId
    case sr: StackReferenceResourceOptions => sr.importId
    case _                                 => None

final case class CustomResourceOptions private[internal] (
  common: CommonResourceOptions,
  provider: Option[ProviderResource],
  deleteBeforeReplace: Boolean,
  additionalSecretOutputs: List[String],
  importId: Option[ResourceId] // TODO should this be Id?
) extends ResourceOptions,
      CommonResourceOptions:
  export common.*

final case class ComponentResourceOptions private[internal] (
  common: CommonResourceOptions,
  providers: List[ProviderResource]
) extends ResourceOptions,
      CommonResourceOptions:
  export common.*

final case class StackReferenceResourceOptions private[internal] (
  common: CommonResourceOptions,
  importId: Option[ResourceId]
) extends ResourceOptions,
      CommonResourceOptions:
  export common.*

trait CustomResourceOptionsFactory:
  def apply(using Context)(
    parent: Resource | NotProvided = NotProvided,
    dependsOn: Output[List[Resource]] = Output(List.empty[Resource]),
    deletedWith: Resource | NotProvided = NotProvided,
    protect: Boolean = false,
    ignoreChanges: List[String] = List.empty,
    version: NonEmptyString | NotProvided = NotProvided, // TODO? UGLY AF
    provider: ProviderResource | NotProvided = NotProvided,
    customTimeouts: String | NotProvided = NotProvided, // CustomTimeouts // TODO
    // resourceTransformations: List[ResourceTransformation], // TODO
    // aliases: List[Output[Alias]], // TODO
    urn: URN | NotProvided = NotProvided, // TODO better type
    replaceOnChanges: List[String] = List.empty, // TODO?
    retainOnDelete: Boolean = false,
    pluginDownloadUrl: String | NotProvided = NotProvided,
    deleteBeforeReplace: Boolean = false,
    additionalSecretOutputs: List[String] = List.empty,
    importId: ResourceId | NotProvided = NotProvided
  ): CustomResourceOptions = CustomResourceOptions.apply(
    parent = parent,
    dependsOn = dependsOn,
    deletedWith = deletedWith,
    protect = protect,
    ignoreChanges = ignoreChanges,
    version = version,
    provider = provider,
    customTimeouts = customTimeouts,
    urn = urn,
    replaceOnChanges = replaceOnChanges,
    retainOnDelete = retainOnDelete,
    pluginDownloadUrl = pluginDownloadUrl,
    deleteBeforeReplace = deleteBeforeReplace,
    additionalSecretOutputs = additionalSecretOutputs,
    importId = importId
  )

object CustomResourceOptions:
  def apply(using Context)(
    parent: Resource | NotProvided = NotProvided,
    dependsOn: Output[List[Resource]] = Output(List.empty[Resource]),
    deletedWith: Resource | NotProvided = NotProvided,
    protect: Boolean = false,
    ignoreChanges: List[String] = List.empty,
    version: NonEmptyString | NotProvided = NotProvided, // TODO? UGLY AF
    provider: ProviderResource | NotProvided = NotProvided,
    customTimeouts: String | NotProvided = NotProvided, // CustomTimeouts // TODO
    // resourceTransformations: List[ResourceTransformation], // TODO
    // aliases: List[Output[Alias]], // TODO
    urn: URN | NotProvided = NotProvided,
    replaceOnChanges: List[String] = List.empty, // TODO?
    retainOnDelete: Boolean = false,
    pluginDownloadUrl: String | NotProvided = NotProvided,
    deleteBeforeReplace: Boolean = false,
    additionalSecretOutputs: List[String] = List.empty,
    importId: ResourceId | NotProvided = NotProvided
  ): CustomResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOption,
      dependsOn = dependsOn,
      protect = protect,
      ignoreChanges = ignoreChanges,
      version = version.asOption.getOrElse(""), // grpc & go are "strongly" typed
      customTimeouts = customTimeouts.asOption,
      urn = urn.asOption,
      replaceOnChanges = replaceOnChanges,
      retainOnDelete = retainOnDelete,
      pluginDownloadUrl = pluginDownloadUrl.asOption.getOrElse(""),
      deletedWith = deletedWith.asOption
    )
    new CustomResourceOptions(
      common,
      provider = provider.asOption,
      deleteBeforeReplace = deleteBeforeReplace,
      additionalSecretOutputs = additionalSecretOutputs,
      importId = importId.asOption
    )
end CustomResourceOptions

object ComponentResourceOptions:
  def apply(using Context)(
    providers: List[ProviderResource] = List.empty,
    id: Output[NonEmptyString] | NotProvided = NotProvided,
    parent: Resource | NotProvided = NotProvided,
    dependsOn: Output[List[Resource]] = Output(List.empty[Resource]),
    protect: Boolean = false,
    ignoreChanges: List[String] = List.empty,
    version: NonEmptyString | NotProvided = NotProvided, // TODO? UGLY AF
    customTimeouts: String | NotProvided = NotProvided, // CustomTimeouts // TODO
    // resourceTransformations: List[ResourceTransformation], // TODO
    // aliases: List[Output[Alias]], // TODO
    urn: URN | NotProvided = NotProvided,
    replaceOnChanges: List[String] = List.empty, // TODO?
    retainOnDelete: Boolean = false,
    pluginDownloadUrl: String | NotProvided = NotProvided,
    deletedWith: Resource | NotProvided = NotProvided
  ): ComponentResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOption,
      dependsOn = dependsOn,
      protect = protect,
      ignoreChanges = ignoreChanges,
      version = version.asOption.getOrElse(""), // TODO grpc & go are "strongly" typed
      customTimeouts = customTimeouts.asOption,
      urn = urn.asOption,
      replaceOnChanges = replaceOnChanges,
      retainOnDelete = retainOnDelete,
      pluginDownloadUrl = pluginDownloadUrl.asOption.getOrElse(""),
      deletedWith = deletedWith.asOption
    )
    new ComponentResourceOptions(common, providers)
