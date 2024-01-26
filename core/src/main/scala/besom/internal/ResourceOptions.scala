package besom.internal

import besom.types.{ResourceId, URN}
import besom.util.*

enum CustomTimeout:
  case Create, Update, Delete, Read

sealed trait ResourceOptsVariant:
  type Constructor
  val constructor: Constructor
class StackRef extends ResourceOptsVariant:
  type Constructor = StackReferenceResourceOptions.type
  val constructor = StackReferenceResourceOptions
class Component extends ResourceOptsVariant:
  type Constructor = ComponentResourceOptions.type
  val constructor = ComponentResourceOptions
class Custom extends ResourceOptsVariant:
  type Constructor = CustomResourceOptions.type
  val constructor = CustomResourceOptions

sealed trait ResolvedResourceOptions:
  def parent: Option[Resource]
  def dependsOn: Vector[Resource]
  def protect: Boolean
  def ignoreChanges: Vector[String]
  def version: Option[String]
  def customTimeouts: Option[CustomTimeout]
  // def resourceTransformations: Vector[ResourceTransformation], // TODO
  // def aliases: Vector[Output[Alias]], // TODO
  def urn: Option[URN]
  def replaceOnChanges: Vector[String]
  def retainOnDelete: Boolean
  def pluginDownloadUrl: Option[String]
  def deletedWith: Option[Resource]

  private[besom] def getImportId(using Context): Option[ResourceId] = this match
    case cr: CustomResolvedResourceOptions         => cr.importId
    case sr: StackReferenceResolvedResourceOptions => sr.importId
    case _                                         => None

case class CommonResolvedResourceOptions(
  parent: Option[Resource],
  dependsOn: Vector[Resource],
  protect: Boolean,
  ignoreChanges: Vector[String],
  version: Option[String],
  customTimeouts: Option[CustomTimeout],
  // resourceTransformations: Vector[ResourceTransformation], // TODO
  // aliases: Vector[Output[Alias]], // TODO
  urn: Option[URN],
  replaceOnChanges: Vector[String],
  retainOnDelete: Boolean,
  pluginDownloadUrl: Option[String],
  deletedWith: Option[Resource]
)

case class CustomResolvedResourceOptions(
  common: CommonResolvedResourceOptions,
  provider: Option[ProviderResource],
  deleteBeforeReplace: Boolean,
  additionalSecretOutputs: Vector[String],
  importId: Option[ResourceId]
) extends ResolvedResourceOptions:
  export common.*

case class ComponentResolvedResourceOptions(
  common: CommonResolvedResourceOptions,
  providers: Vector[ProviderResource]
) extends ResolvedResourceOptions:
  export common.*

case class StackReferenceResolvedResourceOptions(
  common: CommonResolvedResourceOptions,
  importId: Option[ResourceId]
) extends ResolvedResourceOptions:
  export common.*

trait CommonResourceOptions:
  def parent: Output[Option[Resource]]
  def dependsOn: Output[Vector[Resource]]
  def protect: Output[Boolean]
  def ignoreChanges: Output[Vector[String]]
  def version: Output[Option[String]] // TODO?
  def customTimeouts: Output[Option[CustomTimeout]] // CustomTimeouts // TODO
  // def resourceTransformations: Vector[ResourceTransformation], // TODO
  // def aliases: Vector[Output[Alias]], // TODO
  // TODO this is only necessary for Resource deserialization, dependency resources and multi-language remote components
  def urn: Output[Option[URN]]
  def replaceOnChanges: Output[Vector[String]] // TODO?
  def retainOnDelete: Output[Boolean]
  def pluginDownloadUrl: Output[Option[String]]
  // TODO: new resource option: https://github.com/pulumi/pulumi/pull/11883 this also needs a supported feature check!
  def deletedWith: Output[Option[Resource]]
end CommonResourceOptions

extension (cro: CommonResourceOptions)
  def resolve(using Context): Result[CommonResolvedResourceOptions] =
    for
      parent            <- cro.parent.getData
      dependsOn         <- cro.dependsOn.getData
      protect           <- cro.protect.getData
      ignoreChanges     <- cro.ignoreChanges.getData
      version           <- cro.version.getData
      customTimeouts    <- cro.customTimeouts.getData
      urn               <- cro.urn.getData
      replaceOnChanges  <- cro.replaceOnChanges.getData
      retainOnDelete    <- cro.retainOnDelete.getData
      pluginDownloadUrl <- cro.pluginDownloadUrl.getData
      deletedWith       <- cro.deletedWith.getData
    yield CommonResolvedResourceOptions(
      parent = parent.getValueOrElse(None),
      dependsOn = dependsOn.getValueOrElse(Vector.empty),
      protect = protect.getValueOrElse(false),
      ignoreChanges = ignoreChanges.getValueOrElse(Vector.empty),
      version = version.getValueOrElse(None),
      customTimeouts = customTimeouts.getValueOrElse(None),
      urn = urn.getValueOrElse(None),
      replaceOnChanges = replaceOnChanges.getValueOrElse(Vector.empty),
      retainOnDelete = retainOnDelete.getValueOrElse(false),
      pluginDownloadUrl = pluginDownloadUrl.getValueOrElse(None),
      deletedWith = deletedWith.getValueOrElse(None)
    )

final case class CommonResourceOptionsImpl(
  parent: Output[Option[Resource]],
  dependsOn: Output[Vector[Resource]],
  protect: Output[Boolean],
  ignoreChanges: Output[Vector[String]],
  version: Output[Option[String]], // should be blank string when not provided TODO?
  customTimeouts: Output[Option[CustomTimeout]], // CustomTimeouts // TODO
  // resourceTransformations: Vector[ResourceTransformation], // TODO
  // aliases: Vector[Output[Alias]], // TODO
  urn: Output[Option[URN]],
  replaceOnChanges: Output[Vector[String]], // TODO?
  retainOnDelete: Output[Boolean],
  pluginDownloadUrl: Output[Option[String]], // should be blank string when not provided
  // TODO: new resource option: https://github.com/pulumi/pulumi/pull/11883 this also needs a supported feature check!
  deletedWith: Output[Option[Resource]]
) extends CommonResourceOptions

sealed trait ResourceOptions:
  def parent: Output[Option[Resource]]
  def version: Output[Option[String]]
  def pluginDownloadUrl: Output[Option[String]]
  def dependsOn: Output[Vector[Resource]]
  def protect: Output[Boolean]
  def ignoreChanges: Output[Vector[String]]
  def replaceOnChanges: Output[Vector[String]]
  def retainOnDelete: Output[Boolean]
  def urn: Output[Option[URN]]

  private[besom] def resolve(using Context): Result[ResolvedResourceOptions] =
    this match
      case cr: CustomResourceOptions =>
        cr.common.resolve.flatMap { common =>
          for
            provider                <- cr.provider.getValueOrElse(None)
            importId                <- cr.importId.getValueOrElse(None)
            deleteBeforeReplace     <- cr.deleteBeforeReplace.getValueOrElse(false)
            additionalSecretOutputs <- cr.additionalSecretOutputs.getValueOrElse(Vector.empty)
          yield CustomResolvedResourceOptions(
            common,
            provider = provider,
            deleteBeforeReplace = deleteBeforeReplace,
            additionalSecretOutputs = additionalSecretOutputs,
            importId = importId
          )
        }
      case sr: StackReferenceResourceOptions =>
        sr.common.resolve.flatMap { common =>
          for importId <- sr.importId.getValueOrElse(None)
          yield StackReferenceResolvedResourceOptions(
            common,
            importId = importId
          )
        }
      case co: ComponentResourceOptions =>
        co.common.resolve.flatMap { common =>
          for providers <- co.providers.getValueOrElse(Vector.empty)
          yield ComponentResolvedResourceOptions(
            common,
            providers = providers
          )
        }

  private[besom] def hasURN: Result[Boolean] = urn.map(_.isDefined).getValueOrElse(false)

  private[besom] def hasImportId(using Context): Result[Boolean] = this match
    case cr: CustomResourceOptions         => cr.importId.map(_.isDefined).getValueOrElse(false)
    case sr: StackReferenceResourceOptions => sr.importId.map(_.isDefined).getValueOrElse(false)
    case _                                 => Result.pure(false)

  private[besom] def getImportId(using Context): Result[Option[ResourceId]] = this match
    case cr: CustomResourceOptions         => cr.importId.getValueOrElse(None)
    case sr: StackReferenceResourceOptions => sr.importId.getValueOrElse(None)
    case _                                 => Result.pure(None)
end ResourceOptions

final case class CustomResourceOptions private[internal] (
  common: CommonResourceOptions,
  provider: Output[Option[ProviderResource]],
  deleteBeforeReplace: Output[Boolean],
  additionalSecretOutputs: Output[Vector[String]],
  importId: Output[Option[ResourceId]] // TODO should this be Id?
) extends ResourceOptions,
      CommonResourceOptions:
  export common.*

final case class ComponentResourceOptions private[internal] (
  common: CommonResourceOptions,
  providers: Output[Vector[ProviderResource]]
) extends ResourceOptions,
      CommonResourceOptions:
  export common.*

final case class StackReferenceResourceOptions private[internal] (
  common: CommonResourceOptions,
  importId: Output[Option[ResourceId]]
) extends ResourceOptions,
      CommonResourceOptions:
  export common.*

trait CustomResourceOptionsFactory:
  def apply(using Context)(
    parent: Input.Optional[Resource] = None,
    dependsOn: Input[Vector[Resource]] = Vector.empty,
    deletedWith: Input.Optional[Resource] = None,
    protect: Input[Boolean] = false,
    ignoreChanges: Input[Vector[String]] = Vector.empty,
    version: Input.Optional[NonEmptyString] = None, // TODO? UGLY AF
    provider: Input.Optional[ProviderResource] = None,
    customTimeouts: Input.Optional[CustomTimeout] = None, // CustomTimeouts // TODO
    // resourceTransformations: Vector[ResourceTransformation], // TODO
    // aliases: Vector[Output[Alias]], // TODO
    urn: Input.Optional[URN] = None, // TODO better type
    replaceOnChanges: Input[Vector[String]] = Vector.empty, // TODO?
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deleteBeforeReplace: Input[Boolean] = false,
    additionalSecretOutputs: Input[Vector[String]] = Vector.empty,
    importId: Input.Optional[ResourceId] = None
  ): CustomResourceOptions = CustomResourceOptions.apply(
    parent = parent.asOptionOutput(),
    dependsOn = dependsOn.asOutput(),
    deletedWith = deletedWith.asOptionOutput(),
    protect = protect.asOutput(),
    ignoreChanges = ignoreChanges.asOutput(),
    version = version.asOptionOutput(),
    provider = provider.asOptionOutput(),
    customTimeouts = customTimeouts.asOptionOutput(),
    urn = urn.asOptionOutput(),
    replaceOnChanges = replaceOnChanges.asOutput(),
    retainOnDelete = retainOnDelete.asOutput(),
    pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
    deleteBeforeReplace = deleteBeforeReplace.asOutput(),
    additionalSecretOutputs = additionalSecretOutputs.asOutput(),
    importId = importId.asOptionOutput()
  )

object CustomResourceOptions:
  def apply(using Context)(
    parent: Input.Optional[Resource] = None,
    dependsOn: Input[Vector[Resource]] = Vector.empty,
    deletedWith: Input.Optional[Resource] = None,
    protect: Input[Boolean] = false,
    ignoreChanges: Input[Vector[String]] = Vector.empty,
    version: Input.Optional[NonEmptyString] = None, // TODO? UGLY AF
    provider: Input.Optional[ProviderResource] = None,
    customTimeouts: Input.Optional[CustomTimeout] = None, // CustomTimeouts // TODO
    // resourceTransformations: Vector[ResourceTransformation], // TODO
    // aliases: Vector[Output[Alias]], // TODO
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input[Vector[String]] = Vector.empty, // TODO?
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deleteBeforeReplace: Input[Boolean] = false,
    additionalSecretOutputs: Input[Vector[String]] = Vector.empty,
    importId: Input.Optional[ResourceId] = None
  ): CustomResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOptionOutput(),
      dependsOn = dependsOn.asOutput(),
      protect = protect.asOutput(),
      ignoreChanges = ignoreChanges.asOutput(),
      version = version.asOptionOutput(),
      customTimeouts = customTimeouts.asOptionOutput(),
      urn = urn.asOptionOutput(),
      replaceOnChanges = replaceOnChanges.asOutput(),
      retainOnDelete = retainOnDelete.asOutput(),
      pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
      deletedWith = deletedWith.asOptionOutput()
    )
    new CustomResourceOptions(
      common,
      provider = provider.asOptionOutput(),
      deleteBeforeReplace = deleteBeforeReplace.asOutput(),
      additionalSecretOutputs = additionalSecretOutputs.asOutput(),
      importId = importId.asOptionOutput()
    )
end CustomResourceOptions

object ComponentResourceOptions:
  def apply(using Context)(
    providers: Input[Vector[ProviderResource]] = Vector.empty,
    id: Input.Optional[NonEmptyString] = None,
    parent: Input.Optional[Resource] = None,
    dependsOn: Input[Vector[Resource]] = Vector.empty,
    protect: Input[Boolean] = false,
    ignoreChanges: Input[Vector[String]] = Vector.empty,
    version: Input.Optional[NonEmptyString] = None, // TODO? UGLY AF
    customTimeouts: Input.Optional[CustomTimeout] = None, // CustomTimeouts // TODO
    // resourceTransformations: Vector[ResourceTransformation], // TODO
    // aliases: Vector[Output[Alias]], // TODO
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input[Vector[String]] = Vector.empty, // TODO?
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deletedWith: Input.Optional[Resource] = None
  ): ComponentResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOptionOutput(),
      dependsOn = dependsOn.asOutput(),
      protect = protect.asOutput(),
      ignoreChanges = ignoreChanges.asOutput(),
      version = version.asOptionOutput(),
      customTimeouts = customTimeouts.asOptionOutput(),
      urn = urn.asOptionOutput(),
      replaceOnChanges = replaceOnChanges.asOutput(),
      retainOnDelete = retainOnDelete.asOutput(),
      pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
      deletedWith = deletedWith.asOptionOutput()
    )
    new ComponentResourceOptions(common, providers.asOutput())

object StackReferenceResourceOptions:
  def apply(using Context)(
    parent: Input.Optional[Resource] = None,
    dependsOn: Input[Vector[Resource]] = Vector.empty,
    protect: Input[Boolean] = false,
    ignoreChanges: Input[Vector[String]] = Vector.empty,
    version: Input.Optional[NonEmptyString] = None, // TODO? UGLY AF
    customTimeouts: Input.Optional[CustomTimeout] = None, // CustomTimeouts // TODO
    // resourceTransformations: Vector[ResourceTransformation], // TODO
    // aliases: Vector[Output[Alias]], // TODO
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input[Vector[String]] = Vector.empty, // TODO?
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deletedWith: Input.Optional[Resource] = None,
    importId: Input.Optional[ResourceId] = None
  ): StackReferenceResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOptionOutput(),
      dependsOn = dependsOn.asOutput(),
      protect = protect.asOutput(),
      ignoreChanges = ignoreChanges.asOutput(),
      version = version.asOptionOutput(),
      customTimeouts = customTimeouts.asOptionOutput(),
      urn = urn.asOptionOutput(),
      replaceOnChanges = replaceOnChanges.asOutput(),
      retainOnDelete = retainOnDelete.asOutput(),
      pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
      deletedWith = deletedWith.asOptionOutput()
    )
    new StackReferenceResourceOptions(common, importId.asOptionOutput())

trait StackReferenceResourceOptionsFactory:
  def apply(using Context)(
    parent: Input.Optional[Resource] = None,
    dependsOn: Input[Vector[Resource]] = Vector.empty,
    protect: Input[Boolean] = false,
    ignoreChanges: Input[Vector[String]] = Vector.empty,
    version: Input.Optional[NonEmptyString] = None, // TODO? UGLY AF
    customTimeouts: Input.Optional[CustomTimeout] = None, // CustomTimeouts // TODO
    // resourceTransformations: Vector[ResourceTransformation], // TODO
    // aliases: Vector[Output[Alias]], // TODO
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input[Vector[String]] = Vector.empty, // TODO?
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deletedWith: Input.Optional[Resource] = None,
    importId: Input.Optional[ResourceId] = None
  ): StackReferenceResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOptionOutput(),
      dependsOn = dependsOn.asOutput(),
      protect = protect.asOutput(),
      ignoreChanges = ignoreChanges.asOutput(),
      version = version.asOptionOutput(),
      customTimeouts = customTimeouts.asOptionOutput(),
      urn = urn.asOptionOutput(),
      replaceOnChanges = replaceOnChanges.asOutput(),
      retainOnDelete = retainOnDelete.asOutput(),
      pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
      deletedWith = deletedWith.asOptionOutput()
    )
    new StackReferenceResourceOptions(common, importId.asOptionOutput())
