package besom.internal

import besom.types.{ResourceId, URN}
import besom.util.*

import scala.collection.immutable.Iterable

sealed trait ResourceOptsVariant:
  type Constructor
  val constructor: Constructor
object ResourceOptsVariant:
  trait StackRef extends ResourceOptsVariant:
    type Constructor = StackReferenceResourceOptions.type
    val constructor = StackReferenceResourceOptions
  trait Component extends ResourceOptsVariant:
    type Constructor = ComponentResourceOptions.type
    val constructor = ComponentResourceOptions
  trait Custom extends ResourceOptsVariant:
    type Constructor = CustomResourceOptions.type
    val constructor = CustomResourceOptions

sealed trait ResolvedResourceOptions:
  def parent: Option[Resource]
  def dependsOn: Iterable[Resource]
  def protect: Boolean
  def ignoreChanges: Iterable[String]
  def version: Option[String]
  def customTimeouts: Option[CustomTimeouts]
  // def resourceTransformations: Iterable[ResourceTransformation], // TODO
  def aliases: Iterable[ResourceAlias]
  def urn: Option[URN]
  def replaceOnChanges: Iterable[String]
  def retainOnDelete: Boolean
  def pluginDownloadUrl: Option[String]
  def deletedWith: Option[Resource]

  private[besom] def getImportId(using Context): Option[ResourceId] = this match
    case cr: CustomResolvedResourceOptions         => cr.importId
    case sr: StackReferenceResolvedResourceOptions => sr.importId
    case _                                         => None

case class CommonResolvedResourceOptions(
  parent: Option[Resource],
  dependsOn: Iterable[Resource],
  protect: Boolean,
  ignoreChanges: Iterable[String],
  version: Option[String],
  customTimeouts: Option[CustomTimeouts],
  // resourceTransformations: Iterable[ResourceTransformation], // TODO
  aliases: Iterable[ResourceAlias],
  urn: Option[URN],
  replaceOnChanges: Iterable[String],
  retainOnDelete: Boolean,
  pluginDownloadUrl: Option[String],
  deletedWith: Option[Resource]
)

case class CustomResolvedResourceOptions(
  common: CommonResolvedResourceOptions,
  provider: Option[ProviderResource],
  deleteBeforeReplace: Boolean,
  additionalSecretOutputs: Iterable[String],
  importId: Option[ResourceId]
) extends ResolvedResourceOptions:
  export common.*

case class ComponentResolvedResourceOptions(
  common: CommonResolvedResourceOptions,
  providers: Iterable[ProviderResource]
) extends ResolvedResourceOptions:
  export common.*

case class StackReferenceResolvedResourceOptions(
  common: CommonResolvedResourceOptions,
  importId: Option[ResourceId]
) extends ResolvedResourceOptions:
  export common.*

trait CommonResourceOptions:
  def parent: Output[Option[Resource]]
  def dependsOn: Output[Iterable[Resource]]
  def protect: Output[Boolean]
  def ignoreChanges: Output[Iterable[String]]
  def version: Output[Option[String]]
  def customTimeouts: Output[Option[CustomTimeouts]]
  // def resourceTransformations: Iterable[ResourceTransformation], // TODO
  def aliases: Output[Iterable[ResourceAlias]]
  // TODO this is only necessary for Resource deserialization, dependency resources and multi-language remote components
  def urn: Output[Option[URN]]
  def replaceOnChanges: Output[Iterable[String]]
  def retainOnDelete: Output[Boolean]
  def pluginDownloadUrl: Output[Option[String]]
  // TODO: new resource option: https://github.com/pulumi/pulumi/pull/11883 this also needs a supported feature check!
  def deletedWith: Output[Option[Resource]]
end CommonResourceOptions

extension (cro: CommonResourceOptions)
  def resolve(implicitParent: Option[Resource])(using Context): Result[CommonResolvedResourceOptions] =
    for
      explicitParent    <- cro.parent.getData
      dependsOn         <- cro.dependsOn.getData
      protect           <- cro.protect.getData
      ignoreChanges     <- cro.ignoreChanges.getData
      version           <- cro.version.getData
      customTimeouts    <- cro.customTimeouts.getData
      aliases           <- cro.aliases.getData
      urn               <- cro.urn.getData
      replaceOnChanges  <- cro.replaceOnChanges.getData
      retainOnDelete    <- cro.retainOnDelete.getData
      pluginDownloadUrl <- cro.pluginDownloadUrl.getData
      deletedWith       <- cro.deletedWith.getData
    yield CommonResolvedResourceOptions(
      // if no parent is provided by the user explicitly, use the implicit parent from Context
      parent = explicitParent.getValueOrElse(None).orElse(implicitParent),
      dependsOn = dependsOn.getValueOrElse(Iterable.empty),
      protect = protect.getValueOrElse(false),
      ignoreChanges = ignoreChanges.getValueOrElse(Iterable.empty),
      version = version.getValueOrElse(None),
      customTimeouts = customTimeouts.getValueOrElse(None),
      aliases = aliases.getValueOrElse(Iterable.empty),
      urn = urn.getValueOrElse(None),
      replaceOnChanges = replaceOnChanges.getValueOrElse(Iterable.empty),
      retainOnDelete = retainOnDelete.getValueOrElse(false),
      pluginDownloadUrl = pluginDownloadUrl.getValueOrElse(None),
      deletedWith = deletedWith.getValueOrElse(None)
    )

final case class CommonResourceOptionsImpl(
  parent: Output[Option[Resource]],
  dependsOn: Output[Iterable[Resource]],
  protect: Output[Boolean],
  ignoreChanges: Output[Iterable[String]],
  version: Output[Option[String]],
  customTimeouts: Output[Option[CustomTimeouts]],
  // resourceTransformations: Iterable[ResourceTransformation], // TODO
  aliases: Output[Iterable[ResourceAlias]],
  urn: Output[Option[URN]],
  replaceOnChanges: Output[Iterable[String]],
  retainOnDelete: Output[Boolean],
  pluginDownloadUrl: Output[Option[String]],
  deletedWith: Output[Option[Resource]]
) extends CommonResourceOptions

sealed trait ResourceOptions:
  def parent: Output[Option[Resource]]
  def version: Output[Option[String]]
  def pluginDownloadUrl: Output[Option[String]]
  def dependsOn: Output[Iterable[Resource]]
  def protect: Output[Boolean]
  def ignoreChanges: Output[Iterable[String]]
  def replaceOnChanges: Output[Iterable[String]]
  def retainOnDelete: Output[Boolean]
  def urn: Output[Option[URN]]

  private[besom] def resolve(using ctx: Context): Result[ResolvedResourceOptions] =
    val maybeComponentParent = ctx.getParent

    this match
      case cr: CustomResourceOptions =>
        cr.common.resolve(maybeComponentParent).flatMap { common =>
          for
            provider                <- cr.provider.getValueOrElse(None)
            importId                <- cr.importId.getValueOrElse(None)
            deleteBeforeReplace     <- cr.deleteBeforeReplace.getValueOrElse(false)
            additionalSecretOutputs <- cr.additionalSecretOutputs.getValueOrElse(Iterable.empty)
          yield CustomResolvedResourceOptions(
            common,
            provider = provider,
            deleteBeforeReplace = deleteBeforeReplace,
            additionalSecretOutputs = additionalSecretOutputs,
            importId = importId
          )
        }
      case sr: StackReferenceResourceOptions =>
        sr.common.resolve(maybeComponentParent).flatMap { common =>
          for importId <- sr.importId.getValueOrElse(None)
          yield StackReferenceResolvedResourceOptions(
            common,
            importId = importId
          )
        }
      case co: ComponentResourceOptions =>
        co.common.resolve(maybeComponentParent).flatMap { common =>
          for providers <- co.providers.getValueOrElse(Iterable.empty)
          yield ComponentResolvedResourceOptions(
            common,
            providers = providers
          )
        }
  end resolve

  private[besom] def hasURN: Result[Boolean] = urn.map(_.isDefined).getValueOrElse(false)

  private[besom] def getURN: Result[URN] = urn.getValueOrElse(None).flatMap {
    case Some(urn) => Result.pure(urn)
    case None      => Result.fail(Exception("URN is not defined"))
  }

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
  additionalSecretOutputs: Output[Iterable[String]],
  importId: Output[Option[ResourceId]]
) extends ResourceOptions,
      CommonResourceOptions:
  export common.*

final case class ComponentResourceOptions private[internal] (
  common: CommonResourceOptions,
  providers: Output[Iterable[ProviderResource]]
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
    dependsOn: Input.OneOrIterable[Resource] = Iterable.empty,
    deletedWith: Input.Optional[Resource] = None,
    protect: Input[Boolean] = false,
    ignoreChanges: Input.OneOrIterable[String] = Iterable.empty,
    version: Input.Optional[NonEmptyString] = None,
    provider: Input.Optional[ProviderResource] = None,
    customTimeouts: Input.Optional[CustomTimeouts] = None,
    // resourceTransformations: Iterable[ResourceTransformation], // TODO
    aliases: Input.OneOrIterable[ResourceAlias] = Iterable.empty,
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input.OneOrIterable[String] = Iterable.empty,
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deleteBeforeReplace: Input[Boolean] = false,
    additionalSecretOutputs: Input.OneOrIterable[String] = Iterable.empty,
    importId: Input.Optional[ResourceId] = None
  ): CustomResourceOptions = CustomResourceOptions.apply(
    parent = parent.asOptionOutput(),
    dependsOn = dependsOn.asManyOutput(),
    deletedWith = deletedWith.asOptionOutput(),
    protect = protect.asOutput(),
    ignoreChanges = ignoreChanges.asManyOutput(),
    version = version.asOptionOutput(),
    provider = provider.asOptionOutput(),
    customTimeouts = customTimeouts.asOptionOutput(),
    aliases = aliases.asManyOutput(),
    urn = urn.asOptionOutput(),
    replaceOnChanges = replaceOnChanges.asManyOutput(),
    retainOnDelete = retainOnDelete.asOutput(),
    pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
    deleteBeforeReplace = deleteBeforeReplace.asOutput(),
    additionalSecretOutputs = additionalSecretOutputs.asManyOutput(),
    importId = importId.asOptionOutput()
  )

object CustomResourceOptions:
  def apply(using Context)(
    parent: Input.Optional[Resource] = None,
    dependsOn: Input.OneOrIterable[Resource] = Iterable.empty,
    deletedWith: Input.Optional[Resource] = None,
    protect: Input[Boolean] = false,
    ignoreChanges: Input.OneOrIterable[String] = Iterable.empty,
    version: Input.Optional[NonEmptyString] = None,
    provider: Input.Optional[ProviderResource] = None,
    customTimeouts: Input.Optional[CustomTimeouts] = None,
    // resourceTransformations: Iterable[ResourceTransformation], // TODO
    aliases: Input.OneOrIterable[ResourceAlias] = Iterable.empty,
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input.OneOrIterable[String] = Iterable.empty,
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deleteBeforeReplace: Input[Boolean] = false,
    additionalSecretOutputs: Input.OneOrIterable[String] = Iterable.empty,
    importId: Input.Optional[ResourceId] = None
  ): CustomResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOptionOutput(),
      dependsOn = dependsOn.asManyOutput(),
      protect = protect.asOutput(),
      ignoreChanges = ignoreChanges.asManyOutput(),
      version = version.asOptionOutput(),
      customTimeouts = customTimeouts.asOptionOutput(),
      aliases = aliases.asManyOutput(),
      urn = urn.asOptionOutput(),
      replaceOnChanges = replaceOnChanges.asManyOutput(),
      retainOnDelete = retainOnDelete.asOutput(),
      pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
      deletedWith = deletedWith.asOptionOutput()
    )
    new CustomResourceOptions(
      common,
      provider = provider.asOptionOutput(),
      deleteBeforeReplace = deleteBeforeReplace.asOutput(),
      additionalSecretOutputs = additionalSecretOutputs.asManyOutput(),
      importId = importId.asOptionOutput()
    )
  end apply
end CustomResourceOptions

trait ComponentResourceOptionsFactory:
  def apply(using Context)(
    providers: Input.OneOrIterable[ProviderResource] = Iterable.empty,
    parent: Input.Optional[Resource] = None,
    dependsOn: Input.OneOrIterable[Resource] = Iterable.empty,
    protect: Input[Boolean] = false,
    ignoreChanges: Input.OneOrIterable[String] = Iterable.empty,
    version: Input.Optional[NonEmptyString] = None,
    customTimeouts: Input.Optional[CustomTimeouts] = None,
    // resourceTransformations: Iterable[ResourceTransformation], // TODO
    aliases: Input.OneOrIterable[ResourceAlias] = Iterable.empty,
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input.OneOrIterable[String] = Iterable.empty,
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deletedWith: Input.Optional[Resource] = None
  ): ComponentResourceOptions = ComponentResourceOptions.apply(
    providers = providers.asManyOutput(),
    parent = parent.asOptionOutput(),
    dependsOn = dependsOn.asManyOutput(),
    protect = protect.asOutput(),
    ignoreChanges = ignoreChanges.asManyOutput(),
    version = version.asOptionOutput(),
    customTimeouts = customTimeouts.asOptionOutput(),
    aliases = aliases.asManyOutput(),
    urn = urn.asOptionOutput(),
    replaceOnChanges = replaceOnChanges.asManyOutput(),
    retainOnDelete = retainOnDelete.asOutput(),
    pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
    deletedWith = deletedWith.asOptionOutput()
  )

object ComponentResourceOptions:
  def apply(using Context)(
    providers: Input.OneOrIterable[ProviderResource] = Iterable.empty,
    parent: Input.Optional[Resource] = None,
    dependsOn: Input.OneOrIterable[Resource] = Iterable.empty,
    protect: Input[Boolean] = false,
    ignoreChanges: Input.OneOrIterable[String] = Iterable.empty,
    version: Input.Optional[NonEmptyString] = None,
    customTimeouts: Input.Optional[CustomTimeouts] = None,
    // resourceTransformations: Iterable[ResourceTransformation], // TODO
    aliases: Input.OneOrIterable[ResourceAlias] = Iterable.empty,
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input.OneOrIterable[String] = Iterable.empty,
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deletedWith: Input.Optional[Resource] = None
  ): ComponentResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOptionOutput(),
      dependsOn = dependsOn.asManyOutput(),
      protect = protect.asOutput(),
      ignoreChanges = ignoreChanges.asManyOutput(),
      version = version.asOptionOutput(),
      customTimeouts = customTimeouts.asOptionOutput(),
      aliases = aliases.asManyOutput(),
      urn = urn.asOptionOutput(),
      replaceOnChanges = replaceOnChanges.asManyOutput(),
      retainOnDelete = retainOnDelete.asOutput(),
      pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
      deletedWith = deletedWith.asOptionOutput()
    )
    new ComponentResourceOptions(common, providers.asManyOutput())

object StackReferenceResourceOptions:
  def apply(using Context)(
    parent: Input.Optional[Resource] = None,
    dependsOn: Input.OneOrIterable[Resource] = Iterable.empty,
    protect: Input[Boolean] = false,
    ignoreChanges: Input.OneOrIterable[String] = Iterable.empty,
    version: Input.Optional[NonEmptyString] = None,
    customTimeouts: Input.Optional[CustomTimeouts] = None,
    // resourceTransformations: Iterable[ResourceTransformation], // TODO
    aliases: Input.OneOrIterable[ResourceAlias] = Iterable.empty,
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input.OneOrIterable[String] = Iterable.empty,
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deletedWith: Input.Optional[Resource] = None,
    importId: Input.Optional[ResourceId] = None
  ): StackReferenceResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOptionOutput(),
      dependsOn = dependsOn.asManyOutput(),
      protect = protect.asOutput(),
      ignoreChanges = ignoreChanges.asManyOutput(),
      version = version.asOptionOutput(),
      customTimeouts = customTimeouts.asOptionOutput(),
      aliases = aliases.asManyOutput(),
      urn = urn.asOptionOutput(),
      replaceOnChanges = replaceOnChanges.asManyOutput(),
      retainOnDelete = retainOnDelete.asOutput(),
      pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
      deletedWith = deletedWith.asOptionOutput()
    )
    new StackReferenceResourceOptions(common, importId.asOptionOutput())

trait StackReferenceResourceOptionsFactory:
  def apply(using Context)(
    parent: Input.Optional[Resource] = None,
    dependsOn: Input.OneOrIterable[Resource] = Iterable.empty,
    protect: Input[Boolean] = false,
    ignoreChanges: Input.OneOrIterable[String] = Iterable.empty,
    version: Input.Optional[NonEmptyString] = None,
    customTimeouts: Input.Optional[CustomTimeouts] = None,
    // resourceTransformations: Iterable[ResourceTransformation], // TODO
    aliases: Input.OneOrIterable[ResourceAlias] = Iterable.empty,
    urn: Input.Optional[URN] = None,
    replaceOnChanges: Input.OneOrIterable[String] = Iterable.empty,
    retainOnDelete: Input[Boolean] = false,
    pluginDownloadUrl: Input.Optional[String] = None,
    deletedWith: Input.Optional[Resource] = None,
    importId: Input.Optional[ResourceId] = None
  ): StackReferenceResourceOptions =
    val common = CommonResourceOptionsImpl(
      parent = parent.asOptionOutput(),
      dependsOn = dependsOn.asManyOutput(),
      protect = protect.asOutput(),
      ignoreChanges = ignoreChanges.asManyOutput(),
      version = version.asOptionOutput(),
      customTimeouts = customTimeouts.asOptionOutput(),
      aliases = aliases.asManyOutput(),
      urn = urn.asOptionOutput(),
      replaceOnChanges = replaceOnChanges.asManyOutput(),
      retainOnDelete = retainOnDelete.asOutput(),
      pluginDownloadUrl = pluginDownloadUrl.asOptionOutput(),
      deletedWith = deletedWith.asOptionOutput()
    )
    new StackReferenceResourceOptions(common, importId.asOptionOutput())
