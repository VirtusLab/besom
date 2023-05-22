package besom.internal

import besom.util.*, Types.*
import com.google.protobuf.struct.{Struct, Value}
import pulumirpc.resource.SupportsFeatureRequest
import org.checkerframework.checker.units.qual.A
import pulumirpc.resource.RegisterResourceRequest
import pulumirpc.resource.RegisterResourceRequest.PropertyDependencies

case class RawResourceResult(urn: String, id: Option[String], data: Struct, dependencies: Map[String, Set[Resource]])

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

  private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  ): Output[R]

  private[besom] def registerResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  ): Output[R]

  private[besom] def readResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
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
    private[besom] val resources: Resources
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
    ): Output[R] =
      given Context = this
      Output(registerResourceInternal[R, A](typ, name, args, options).map(OutputData(_)))

    override private[besom] def registerTask[A](fa: => Result[A]): Result[A] = workgroup.runInWorkGroup(fa)

    override private[besom] def waitForAllTasks: Result[Unit] = workgroup.waitForAll *> workgroup.reset

    override private[besom] def close: Result[Unit] =
      for
        _ <- monitor.close()
        _ <- engine.close()
      yield ()

    private[besom] def registerResourceOutputsInternal(): Result[Unit] = Result.unit // TODO

    override private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
      typ: ResourceType,
      name: NonEmptyString,
      args: A,
      options: ResourceOptions
    ): Output[R] =
      // val effect: Output[R] = ???
      // registerResourceCreation(typ, name, effect) // put into ConcurrentHashMap eagerly!
      // effect
      ???

    private[besom] def resolveProviderReferences(state: ResourceState): Result[Map[String, String]] =
      Result
        .sequence(
          state.providers.map { case (pkg, provider) =>
            provider.registrationId.map(pkg -> _)
          }
        )
        .map(_.toMap)

    private[besom] def resolveTransitivelyReferencedComponentResourceUrns(
      resources: Set[Resource]
    ): Result[Set[String]] =
      def findAllReachableResources(left: Set[Resource], acc: Set[Resource]): Result[Set[Resource]] =
        if left.isEmpty then Result.pure(acc)
        else
          val current = left.head
          val childrenResult = current match
            case cr: ComponentResource => this.resources.getStateFor(cr).map(_.children)
            case _                     => Result.pure(Set.empty)

          childrenResult.flatMap { children =>
            findAllReachableResources((left - current) ++ children, acc + current)
          }

      findAllReachableResources(resources, Set.empty).flatMap { allReachableResources =>
        Result
          .sequence {
            allReachableResources
              .filter {
                case _: ComponentResource => false // TODO remote components - if remote it's reachable
                case _: CustomResource    => true
                case _                    => false
              }
              .map(_.urn.getValue)
          }
          .map(_.flatten) // this drops all URNs that are not present (given getValue returns Option[URN])
      }

    case class AggregatedDependencyUrns(
      allDeps: Set[String],
      allDepsByProperty: Map[String, Set[String]]
    )

    case class PreparedInputs(
      serializedArgs: Value,
      parentUrn: String,
      providerId: String,
      providerRefs: Map[String, String],
      depUrns: AggregatedDependencyUrns,
      aliases: List[String],
      options: ResourceOptions
    )

    private[besom] def aggregateDependencyUrns(
      directDeps: Set[Resource],
      propertyDeps: Map[String, Set[Resource]]
    ): Result[AggregatedDependencyUrns] =
      resolveTransitivelyReferencedComponentResourceUrns(directDeps).flatMap { transiviteDepUrns =>
        val x = propertyDeps.map { case (propertyName, resources) =>
          resolveTransitivelyReferencedComponentResourceUrns(resources).map(propertyName -> _)
        }.toVector

        Result.sequence(x).map { propertyDeps =>
          val allDeps           = transiviteDepUrns ++ propertyDeps.flatMap(_._2)
          val allDepsByProperty = propertyDeps.toMap

          AggregatedDependencyUrns(allDeps, allDepsByProperty)
        }
      }

    private[besom] def resolveAliases(resource: Resource): Result[List[String]] =
      Result.pure(List.empty) // TODO aliases

    private[besom] def resolveProviderRegistrationId(state: ResourceState): Result[Option[String]] =
      state match
        case prs: ProviderResourceState =>
          prs.custom.common.provider match
            case Some(provider) => provider.registrationId.map(Some(_))
            case None           => Result.pure(None)

        case crs: CustomResourceState =>
          crs.common.provider match
            case Some(provider) => provider.registrationId.map(Some(_))
            case None           => Result.pure(None)

        case _ => Result.pure(None)

    private[besom] def prepareResourceInputs[A: ArgsEncoder](
      label: String,
      resource: Resource,
      state: ResourceState,
      args: A,
      options: ResourceOptions
    ): Result[PreparedInputs] =
      for
        directDeps      <- options.dependsOn.getValueOrElse(List.empty).map(_.toSet)
        serResult       <- PropertiesSerializer.serializeResourceProperties(label, args)
        maybeParentUrn  <- resolveParentUrn(state.typ, options)
        maybeProviderId <- resolveProviderRegistrationId(state)
        providerRefs    <- resolveProviderReferences(state)
        depUrns         <- aggregateDependencyUrns(directDeps, serResult.propertyToDependentResources)
        aliases         <- resolveAliases(resource)
      yield PreparedInputs(
        serResult.serialized,
        maybeParentUrn.getOrElse(""),
        maybeProviderId.getOrElse(""),
        providerRefs,
        depUrns,
        aliases,
        options
      )

    private[besom] def executeRegisterResourceRequest[R <: Resource](
      resource: Resource,
      state: ResourceState,
      resolver: ResourceResolver[R],
      inputs: PreparedInputs
    ): Result[Unit] =
      this
        .registerTask {
          // X `type`: _root_.scala.Predef.String = "",
          // X name: _root_.scala.Predef.String = "",
          // X parent: _root_.scala.Predef.String = "",
          // X custom: _root_.scala.Boolean = false,
          // X `object`: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
          // X protect: _root_.scala.Boolean = false,
          // X dependencies: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
          // X provider: _root_.scala.Predef.String = "",
          // X propertyDependencies: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.resource.RegisterResourceRequest.PropertyDependencies] = _root_.scala.collection.immutable.Map.empty,
          // X deleteBeforeReplace: _root_.scala.Boolean = false,
          // X version: _root_.scala.Predef.String = "",
          // X ignoreChanges: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
          // X acceptSecrets: _root_.scala.Boolean = false,
          // X additionalSecretOutputs: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
          // N @scala.deprecated(message="Marked as deprecated in proto file", "") urnAliases: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
          // X importId: _root_.scala.Predef.String = "",
          // X customTimeouts: _root_.scala.Option[pulumirpc.resource.RegisterResourceRequest.CustomTimeouts] = _root_.scala.None,
          // X deleteBeforeReplaceDefined: _root_.scala.Boolean = false,
          // X supportsPartialValues: _root_.scala.Boolean = false,
          // X remote: _root_.scala.Boolean = false,
          // X acceptResources: _root_.scala.Boolean = false,
          // X providers: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String] = _root_.scala.collection.immutable.Map.empty,
          // X replaceOnChanges: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
          // X pluginDownloadURL: _root_.scala.Predef.String = "",
          // X retainOnDelete: _root_.scala.Boolean = false,
          // X aliases: _root_.scala.Seq[pulumirpc.resource.Alias] = _root_.scala.Seq.empty,
          // unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
          Result
            .defer {
              RegisterResourceRequest(
                `type` = state.typ,
                name = state.name,
                parent = inputs.parentUrn,
                custom = resource.isCustom,
                `object` =
                  inputs.serializedArgs.kind.structValue, // TODO this is most certainly wrong, ArgsEncoder should return Struct
                protect = inputs.options.protect, // TODO we don't do what pulumi-java does, we do what pulumi-go does
                dependencies = inputs.depUrns.allDeps.toList,
                provider = inputs.providerId,
                providers = inputs.providerRefs,
                propertyDependencies = inputs.depUrns.allDepsByProperty.map { case (key, value) =>
                  key -> PropertyDependencies(value.toList)
                }.toMap,
                deleteBeforeReplaceDefined = true,
                deleteBeforeReplace = inputs.options match
                  case CustomResourceOptions(_, _, deleteBeforeReplace, _, _) => deleteBeforeReplace
                  case _                                                      => false
                ,
                version = inputs.options.version,
                ignoreChanges = inputs.options.ignoreChanges,
                acceptSecrets = true, // TODO doing like java does it
                acceptResources = true, // TODO read this from PULUMI_DISABLE_RESOURCE_REFERENCES env var
                additionalSecretOutputs = inputs.options match
                  case CustomResourceOptions(_, _, _, additionalSecretOutputs, _) => additionalSecretOutputs
                  case _                                                          => List.empty
                ,
                replaceOnChanges = inputs.options.replaceOnChanges,
                importId = inputs.options match
                  case CustomResourceOptions(_, _, _, _, importId) => importId.getOrElse("")
                  case _                                           => ""
                ,
                aliases = inputs.aliases.map { alias =>
                  pulumirpc.resource.Alias(pulumirpc.resource.Alias.Alias.Urn(alias))
                }.toList,
                remote = false, // TODO remote components
                customTimeouts = None, // TODO custom timeouts
                pluginDownloadURL = inputs.options.pluginDownloadUrl,
                retainOnDelete = inputs.options.retainOnDelete,
                supportsPartialValues = false // TODO partial values
              )
            }
            .flatMap { req =>
              pprint.pprintln(req)
              this.monitor.registerResource(req)
            }
            .map { resp =>
              given Context = this
              RawResourceResult(
                urn = resp.urn,
                id = if resp.id.isEmpty then None else Some(resp.id),
                data = resp.`object`.getOrElse {
                  throw new Exception("ONIXPECTED: no object in response") // TODO is this correct?
                },
                dependencies = resp.propertyDependencies.map { case (propertyName, propertyDeps) =>
                  val deps: Set[Resource] = propertyDeps.urns.toSet
                    .map(Output(_))
                    .map(DependencyResource(_)) // we do not register DependencyResources!

                  propertyName -> deps
                }
              )
            }
            .either
            .flatMap { eitherErrorOrResult =>
              resolver.resolve(eitherErrorOrResult)(using this)
            }
        }
        .fork
        .void

    private[besom] def addChildToParentResource(resource: Resource, maybeParent: Option[Resource]): Result[Unit] =
      maybeParent match
        case None         => Result.unit
        case Some(parent) => resources.updateStateFor(parent)(_.addChild(resource))

    private[besom] def registerResourceInternal[R <: Resource: ResourceDecoder, A: ArgsEncoder](
      typ: ResourceType,
      name: NonEmptyString,
      args: A,
      options: ResourceOptions
    ): Result[R] =
      val label = s"resource:$name[$typ]#..." // todo saner label or use data from resource state?
      summon[ResourceDecoder[R]].makeResolver(using this).flatMap { (resource, resolver) =>
        for
          tuple <- summon[ArgsEncoder[A]].encode(args, _ => false)
          _ = println(s"encoded for $label:")
          _ = pprint.pprintln(tuple._1)
          _ = pprint.pprintln(tuple._2)
          state  <- createResourceState(typ, name, resource, options)
          _      <- resources.add(resource, state)
          inputs <- prepareResourceInputs(label, resource, state, args, options)
          _      <- addChildToParentResource(resource, options.parent)
          _      <- executeRegisterResourceRequest(resource, state, resolver, inputs)
        yield resource
      }

    override private[besom] def registerResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
      typ: ResourceType,
      name: NonEmptyString,
      args: A,
      options: ResourceOptions
    ): Output[R] =
      given Context = this
      Output(registerResourceInternal[R, A](typ, name, args, options).map(OutputData(_)))

    override private[besom] def readResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
      typ: ResourceType,
      name: NonEmptyString,
      args: A,
      options: ResourceOptions
    ): Output[R] = ???
    // summon[ResourceDecoder[R]].makeFulfillable(using this) match
    //  case (r, fulfillable) =>

    // This method returns an Option of Resource because for Stack there is no parent resource,
    // for any other resource the parent is either explicitly set in ResourceOptions or the stack is the parent.
    private def resolveParent(typ: ResourceType, resourceOptions: ResourceOptions): Result[Option[Resource]] =
      if typ == Stack.RootPulumiStackTypeName then Result.pure(None)
      else
        resourceOptions.parent match
          case Some(parent) => Result.pure(Some(parent))
          case None         => getStack.map(Some(_))

    private def resolveParentUrn(typ: ResourceType, resourceOptions: ResourceOptions): Result[Option[String]] =
      resolveParent(typ, resourceOptions).flatMap {
        case None =>
          Result.pure(None)
        case Some(parent) =>
          parent.urn.getData.map(_.getValue)
      }

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
        case Some(parent) => resources.getStateFor(parent).map(_.providers)

      def overwriteWithProvidersFromOptions(initialProviders: Providers): Result[Providers] =
        opts match
          case CustomResourceOptions(common, provider, _, _, _) =>
            provider match
              case None => Result.pure(initialProviders)
              case Some(provider) =>
                resources.getStateFor(provider).map { prs =>
                  initialProviders + (prs.pkg -> provider)
                }

          case ComponentResourceOptions(_, providers) =>
            Result
              .sequence(
                providers.map(provider => resources.getStateFor(provider).map(rs => rs.pkg -> provider))
              )
              .map(_.toMap)
              // overwrite overlapping initialProviders with providers from ComponentResourceOptions
              .map(initialProviders ++ _)

          case StackReferenceResourceOptions(_, _) => Result.pure(initialProviders)

      for
        initialProviders <- getParentProviders
        providers        <- overwriteWithProvidersFromOptions(initialProviders)
      yield providers

    private def getProvider(
      typ: ResourceType,
      providers: Providers,
      opts: ResourceOptions
    ): Result[Option[ProviderResource]] =
      val pkg = typ.getPackage
      opts match
        case CustomResourceOptions(_, providerOpt, _, _, _) =>
          providerOpt match
            case None =>
              providers.get(pkg) match
                case None           => Result.pure(None)
                case Some(provider) => Result.pure(Some(provider))

            case Some(providerFromOpts) =>
              resources.getStateFor(providerFromOpts).flatMap { prs =>
                if prs.pkg != pkg then
                  providers.get(pkg) match
                    case None           => Result.pure(None)
                    case Some(provider) => Result.pure(Some(provider))
                else Result.pure(Some(providerFromOpts))
              }

        case _ =>
          providers.get(pkg) match
            case None           => Result.pure(None)
            case Some(provider) => Result.pure(Some(provider))

    override private[besom] def createResourceState(
      typ: ResourceType,
      name: NonEmptyString,
      resource: Resource,
      resourceOptions: ResourceOptions
    ): Result[ResourceState] =
      for
        parent        <- resolveParent(typ, resourceOptions)
        opts          <- applyTransformations(resourceOptions, parent)
        aliases       <- collapseAliases(opts)
        providers     <- mergeProviders(typ, opts)
        maybeProvider <- getProvider(typ, providers, opts)
      yield {
        val commonRS = CommonResourceState(
          children = Set.empty,
          provider = maybeProvider,
          providers = providers,
          version = resourceOptions.version,
          pluginDownloadUrl = resourceOptions.pluginDownloadUrl,
          name = name,
          typ = typ,
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
          case DependencyResource(urn) => throw new Exception("DependencyResource should not be registered")
      }

  def apply(
    runInfo: RunInfo,
    keepResources: Boolean,
    keepOutputValues: Boolean,
    monitor: Monitor,
    engine: Engine,
    workgroup: WorkGroup,
    stackPromise: Promise[Stack],
    resources: Resources
  ): Context = new ContextImpl(
    runInfo,
    keepResources,
    keepOutputValues,
    monitor,
    engine,
    workgroup,
    stackPromise,
    resources
  )

  def apply(
    runInfo: RunInfo,
    monitor: Monitor,
    engine: Engine,
    keepResources: Boolean,
    keepOutputValues: Boolean
  ): Result[Context] =
    for
      wg        <- WorkGroup()
      stack     <- Promise[Stack]
      resources <- Resources()
    yield apply(runInfo, keepResources, keepOutputValues, monitor, engine, wg, stack, resources)

  def apply(runInfo: RunInfo): Result[Context] =
    for
      monitor          <- Monitor(runInfo.monitorAddress)
      engine           <- Engine(runInfo.engineAddress)
      keepResources    <- monitor.supportsFeature(SupportsFeatureRequest("resourceReferences")).map(_.hasSupport)
      keepOutputValues <- monitor.supportsFeature(SupportsFeatureRequest("outputValues")).map(_.hasSupport)
      ctx              <- apply(runInfo, monitor, engine, keepResources, keepOutputValues)
      _                <- ctx.initializeStack
    yield ctx
