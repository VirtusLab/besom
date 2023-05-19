package besom.internal

import besom.util.*, Types.*
import com.google.protobuf.struct.{Struct, Value}
import pulumirpc.resource.SupportsFeatureRequest

case class RawResourceResult(urn: String, id: Option[String], data: Struct, dependencies: Map[String, Set[Resource]])

// needed for parent/child relationship tracking
class ResourceManager(private val resources: Ref[Map[Resource, ResourceState]]):
  def add(resource: ProviderResource, state: ProviderResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: CustomResource, state: CustomResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: ComponentResource, state: ComponentResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: Resource, state: ResourceState): Result[Unit] = (resource, state) match
    case (pr: ProviderResource, prs: ProviderResourceState) =>
      add(pr, prs)
    case (cr: CustomResource, crs: CustomResourceState) =>
      add(cr, crs)
    case (compr: ComponentResource, comprs: ComponentResourceState) =>
      add(compr, comprs)
    case _ => Result.fail(new Exception(s"resource ${resource} and state ${state} don't match"))

  def getStateFor(resource: ProviderResource): Result[ProviderResourceState] =
    resources.get.flatMap(_.get(resource) match
      case Some(state) =>
        state match
          case crs: CustomResourceState =>
            Result.fail(new Exception(s"state for ProviderResource ${resource} is a CustomResourceState!"))
          case prs: ProviderResourceState => Result.pure(prs)
          case comprs: ComponentResourceState =>
            Result.fail(new Exception(s"state for ProviderResource ${resource} is a ComponentResourceState!"))

      case None => Result.fail(new Exception(s"state for resource ${resource} not found"))
    )

  def getStateFor(resource: CustomResource): Result[CustomResourceState] =
    resources.get.flatMap(_.get(resource) match
      case Some(state) =>
        state match
          case crs: CustomResourceState => Result.pure(crs)
          case prs: ProviderResourceState =>
            Result.fail(new Exception(s"state for CustomResource ${resource} is a ProviderResourceState!"))
          case comprs: ComponentResourceState =>
            Result.fail(new Exception(s"state for CustomResource ${resource} is a ComponentResourceState!"))

      case None => Result.fail(new Exception(s"state for resource ${resource} not found"))
    )

  def getStateFor(resource: ComponentResource): Result[ComponentResourceState] =
    resources.get.flatMap(_.get(resource) match
      case Some(state) =>
        state match
          case crs: CustomResourceState =>
            Result.fail(new Exception(s"state for ComponentResource ${resource} is a CustomResourceState!"))
          case prs: ProviderResourceState =>
            Result.fail(new Exception(s"state for ComponentResource ${resource} is a ProviderResourceState!"))
          case comprs: ComponentResourceState => Result.pure(comprs)

      case None => Result.fail(new Exception(s"state for resource ${resource} not found"))
    )

  def getStateFor(resource: Resource): Result[ResourceState] =
    resources.get.flatMap(_.get(resource) match
      case Some(state) => Result.pure(state)
      case None        => Result.fail(new Exception(s"state for resource ${resource} not found"))
    )

object ResourceManager:
  def apply(): Result[ResourceManager] = Ref(Map.empty).map(new ResourceManager(_))

trait Context {

  def projectName: NonEmptyString
  def stackName: NonEmptyString
  def config: Config

  // just testing out possible shapes
  def component[Args: Encoder, Out](tpe: NonEmptyString, name: NonEmptyString, args: Args)(
    block: => Output[Out]
  ): Output[Out] = ???

  private[besom] val runInfo: RunInfo
  private[besom] val keepResources: Boolean
  private[besom] val keepOutputValues: Boolean
  private[besom] val monitor: Monitor
  private[besom] val engine: Engine
  private[besom] val workgroup: WorkGroup

  private[besom] def initializeStack: Result[Unit]

  private[besom] def getStack: Result[Stack]

  private[besom] def isDryRun: Boolean = runInfo.dryRun

  private[besom] def registerTask[A](fa: => Result[A]): Result[A]

  private[besom] def waitForAllTasks: Result[Unit]

  private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
    typ: ProviderType,
    name: NonEmptyString,
    args: A,
    options: CustomResourceOptions
  ): Output[R]

  private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder](
    typ: ResourceType,
    name: NonEmptyString
  ): Output[R]

  private[besom] def registerResource[R <: Resource: ResourceDecoder](
    typ: ResourceType,
    name: NonEmptyString
  ): Output[R]

  private[besom] def readResource[R <: Resource: ResourceDecoder](
    typ: ResourceType,
    name: NonEmptyString
  ): Output[R]

  private[besom] def createResourceState(
    typ: ResourceType,
    name: NonEmptyString,
    resource: Resource,
    resourceOptions: ResourceOptions
  ): Result[ResourceState]

  private[besom] def close: Result[Unit]
}

object Context:

  type Providers = Map[String, ProviderResource]

  private[besom] class ContextImpl(
    private[besom] val runInfo: RunInfo,
    private[besom] val keepResources: Boolean,
    private[besom] val keepOutputValues: Boolean,
    private[besom] val monitor: Monitor,
    private[besom] val engine: Engine,
    private[besom] val workgroup: WorkGroup,
    private[besom] val stackPromise: Promise[Stack],
    private[besom] val resourceManager: ResourceManager
  ) extends Context:

    val projectName: NonEmptyString = runInfo.project
    val stackName: NonEmptyString   = runInfo.stack
    val config: Config              = runInfo.config

    private[besom] def getStack: Result[Stack] = stackPromise.get

    private[besom] def initializeStack: Result[Unit] =
      val rootPulumiStackName = projectName +++ "-" +++ stackName
      for
        stack <- registerResourceInternal[Stack, EmptyArgs](
          Stack.RootPulumiStackTypeName,
          rootPulumiStackName,
          EmptyArgs(),
          ComponentResourceOptions(using this)() // TODO pass initial ResourceTransformations here
        )
        _ <- registerResourceOutputsInternal()
        _ <- stackPromise.fulfill(stack)
      yield ()

    private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
      typ: ProviderType,
      name: NonEmptyString,
      args: A,
      options: CustomResourceOptions
    ): Output[R] = ???

    override private[besom] def registerTask[A](fa: => Result[A]): Result[A] = workgroup.runInWorkGroup(fa)

    override private[besom] def waitForAllTasks: Result[Unit] = workgroup.waitForAll

    override private[besom] def close: Result[Unit] =
      for
        _ <- monitor.close()
        _ <- engine.close()
      yield ()

    private[besom] def registerResourceOutputsInternal(): Result[Unit] = ???

    override private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder](
      typ: ResourceType,
      name: NonEmptyString
    ): Output[R] =
      // val effect: Output[R] = ???
      // registerResourceCreation(typ, name, effect) // put into ConcurrentHashMap eagerly!
      // effect
      ???

    private[besom] def prepareResourceInputs[A: ArgsEncoder](
      resource: Resource,
      args: A,
      options: ResourceOptions,
      state: ResourceState
    ): Result[Struct] = ??? // use PropertiesSerializer.serializeResourceProperties
    // for inputs <- summon[ArgsEncoder[A]].encode(args) yield inputs

    private[besom] def executeRegisterResourceRequest[R <: Resource](
      resource: Resource,
      state: ResourceState,
      resolver: ResourceResolver[R],
      inputs: Struct
    ): Result[Unit] =
      this
        .registerTask {
          Result.defer {
            ??? // call grpc here
          }
        }
        .fork
        .void

    private[besom] def registerResourceInternal[R <: Resource: ResourceDecoder, A: ArgsEncoder](
      typ: ResourceType,
      name: NonEmptyString,
      args: A,
      options: ResourceOptions
    ): Result[R] =
      summon[ResourceDecoder[R]].makeResolver(using this).flatMap { (resource, resolver) =>
        for
          state  <- createResourceState(typ, name, resource, options)
          _      <- resourceManager.add(resource, state)
          inputs <- prepareResourceInputs(resource, args, options, state)
          _      <- executeRegisterResourceRequest(resource, state, resolver, inputs)
        yield resource
      }

    override private[besom] def registerResource[R <: Resource: ResourceDecoder](
      typ: ResourceType,
      name: NonEmptyString
    ): Output[R] = ???
    override private[besom] def readResource[R <: Resource: ResourceDecoder](
      typ: ResourceType,
      name: NonEmptyString
    ): Output[R] = ???
    // summon[ResourceDecoder[R]].makeFulfillable(using this) match
    //  case (r, fulfillable) =>

    private def resolveParent(typ: ResourceType, resourceOptions: ResourceOptions): Result[Option[Resource]] =
      if typ == Stack.RootPulumiStackTypeName then Result.pure(None)
      else
        resourceOptions.parent match
          case Some(parent) => Result.pure(Some(parent))
          case None         => getStack.map(Some(_))

    private def applyTransformations(
      resourceOptions: ResourceOptions,
      parent: Option[Resource]
    ): Result[ResourceOptions] =
      Result.pure(resourceOptions) // TODO resource transformations

    private def collapseAliases(opts: ResourceOptions): Result[List[Output[String]]] =
      Result.pure(List.empty) // TODO aliases

    private def mergeProviders(typ: String, opts: ResourceOptions): Result[Providers] =
      def getParentProviders = opts.parent match
        case None         => Result.pure(Map.empty)
        case Some(parent) => resourceManager.getStateFor(parent).map(_.providers)

      def overwriteWithProvidersFromOptions(initialProviders: Providers): Result[Providers] =
        opts match
          case CustomResourceOptions(common, provider, _, _, _) =>
            provider match
              case None => Result.pure(initialProviders)
              case Some(provider) =>
                resourceManager.getStateFor(provider).map { prs =>
                  initialProviders + (prs.pkg -> provider)
                }

          case ComponentResourceOptions(_, providers) =>
            Result
              .sequence(
                providers.map(provider => resourceManager.getStateFor(provider).map(rs => rs.pkg -> provider))
              )
              .map(_.toMap)
              // overwrite overlapping initialProviders with providers from ComponentResourceOptions
              .map(initialProviders ++ _)

          case StackReferenceResourceOptions(_, _) => Result.pure(initialProviders)

      for
        initialProviders <- getParentProviders
        providers        <- overwriteWithProvidersFromOptions(initialProviders)
      yield providers

    private def getProvider(typ: ResourceType, providers: Providers, opts: ResourceOptions): Result[ProviderResource] =
      val pkg = typ.getPackage
      opts match
        case CustomResourceOptions(_, providerOpt, _, _, _) =>
          providerOpt match
            case None =>
              providers.get(pkg) match
                case None           => Result.fail(new Exception(s"no provider found for package ${pkg}"))
                case Some(provider) => Result.pure(provider)

            case Some(providerFromOpts) =>
              resourceManager.getStateFor(providerFromOpts).flatMap { prs =>
                if prs.pkg != pkg then
                  providers.get(pkg) match
                    case None           => Result.fail(new Exception(s"no provider found for package ${pkg}"))
                    case Some(provider) => Result.pure(provider)
                else Result.pure(providerFromOpts)
              }

        case _ =>
          providers.get(pkg) match
            case None           => Result.fail(new Exception(s"no provider found for package ${pkg}"))
            case Some(provider) => Result.pure(provider)

    override private[besom] def createResourceState(
      typ: ResourceType,
      name: NonEmptyString,
      resource: Resource,
      resourceOptions: ResourceOptions
    ): Result[ResourceState] =
      for
        parent    <- resolveParent(typ, resourceOptions)
        opts      <- applyTransformations(resourceOptions, parent)
        aliases   <- collapseAliases(opts)
        providers <- mergeProviders(typ, opts)
        provider  <- getProvider(typ, providers, opts)
      yield {
        val commonRS = CommonResourceState(
          children = Set.empty,
          provider = provider,
          providers = providers,
          version = resourceOptions.version,
          pluginDownloadUrl = resourceOptions.pluginDownloadUrl,
          name = name,
          remoteComponent = false // TODO remote components pulumi-go: context.go:819-822
        )

        resource match
          case pr: ProviderResource =>
            ProviderResourceState(
              custom = CustomResourceState(
                id = pr.id,
                common = commonRS
              ),
              pkg = typ.getPackage
            )
          case compr: ComponentResource =>
            ComponentResourceState(common = commonRS)
          case cr: CustomResource =>
            CustomResourceState(
              id = cr.id,
              common = commonRS
            )
      }

  def apply(
    runInfo: RunInfo,
    keepResources: Boolean,
    keepOutputValues: Boolean,
    monitor: Monitor,
    engine: Engine,
    workgroup: WorkGroup,
    stackPromise: Promise[Stack],
    resourceManager: ResourceManager
  ): Context = new ContextImpl(
    runInfo,
    keepResources,
    keepOutputValues,
    monitor,
    engine,
    workgroup,
    stackPromise,
    resourceManager
  )

  def apply(
    runInfo: RunInfo,
    monitor: Monitor,
    engine: Engine,
    keepResources: Boolean,
    keepOutputValues: Boolean
  ): Result[Context] =
    for
      wg    <- WorkGroup()
      stack <- Promise[Stack]
      rm    <- ResourceManager()
    yield apply(runInfo, keepResources, keepOutputValues, monitor, engine, wg, stack, rm)

  def apply(runInfo: RunInfo): Result[Context] =
    for
      monitor          <- Monitor(runInfo.monitorAddress)
      engine           <- Engine(runInfo.engineAddress)
      keepResources    <- monitor.supportsFeature(SupportsFeatureRequest("resourceReferences")).map(_.hasSupport)
      keepOutputValues <- monitor.supportsFeature(SupportsFeatureRequest("outputValues")).map(_.hasSupport)
      ctx              <- apply(runInfo, monitor, engine, keepResources, keepOutputValues)
      _                <- ctx.initializeStack
    yield ctx

object Providers:
  val ProviderResourceTypePrefix: NonEmptyString = "pulumi:providers:"
  def providerResourceType(`package`: NonEmptyString): NonEmptyString =
    ProviderResourceTypePrefix +++ `package`
