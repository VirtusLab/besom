package besom.internal

import besom.util.*, Types.*
import com.google.protobuf.struct.Struct
import pulumirpc.resource.SupportsFeatureRequest
import pulumirpc.resource.RegisterResourceRequest
import pulumirpc.resource.RegisterResourceRequest.PropertyDependencies
import besom.internal.logging.*

case class InvokeOptions()

type Providers = Map[String, ProviderResource]

class Context(
  private[besom] val runInfo: RunInfo,
  private[besom] val runOptions: RunOptions,
  private[besom] val featureSupport: FeatureSupport,
  val config: Config,
  val logger: BesomLogger,
  private[besom] val monitor: Monitor,
  private[besom] val engine: Engine,
  private[besom] val taskTracker: TaskTracker,
  private[besom] val stackPromise: Promise[Stack],
  private[besom] val resources: Resources
) extends TaskTracker:

  val projectName: NonEmptyString = runInfo.project
  val stackName: NonEmptyString   = runInfo.stack

  export taskTracker.{registerTask, waitForAllTasks}
  export resources.getStateFor

  private[besom] def isDryRun: Boolean = runInfo.dryRun

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

  private[besom] def close: Result[Unit] =
    for
      _ <- monitor.close()
      _ <- engine.close()
    yield ()

  private[besom] def registerResourceOutputsInternal(): Result[Unit] = Result.unit // TODO

  private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
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
    serializedArgs: Struct,
    parentUrn: Option[String],
    providerId: Option[String],
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

  private[besom] def invoke[A: ArgsEncoder, R: Decoder](typ: ResourceType, args: A, opts: InvokeOptions): Result[R] =
    Result(???)

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
      _            <- logger.trace(s"Preparing inputs for $label: gathering direct dependencies")
      directDeps   <- options.dependsOn.getValueOrElse(List.empty).map(_.toSet)
      _            <- logger.trace(s"Preparing inputs for $label: serializing resource properties")
      serResult    <- PropertiesSerializer.serializeResourceProperties(label, args)
      _            <- logger.trace(s"Preparing inputs for $label: serialized resource properties, getting parent URN")
      parentUrnOpt <- resolveParentUrn(state.typ, options)
      _            <- logger.trace(s"Preparing inputs for $label: got parent URN, getting provider reg ID")
      provIdOpt    <- resolveProviderRegistrationId(state)
      _            <- logger.trace(s"Preparing inputs for $label: got provider reg ID, resolving provider references")
      providerRefs <- resolveProviderReferences(state)
      _            <- logger.trace(s"Preparing inputs for $label: resolved provider refs, aggregating dep URNs")
      depUrns      <- aggregateDependencyUrns(directDeps, serResult.propertyToDependentResources)
      _            <- logger.trace(s"Preparing inputs for $label: aggregated dep URNs, resolving aliases")
      aliases      <- resolveAliases(resource)
      _            <- logger.trace(s"Preparing inputs for $label: resolved aliases, done")
    yield PreparedInputs(serResult.serialized, parentUrnOpt, provIdOpt, providerRefs, depUrns, aliases, options)

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
              parent = inputs.parentUrn.getOrElse(""), // protobuf expects empty string and not null
              custom = resource.isCustom,
              `object` = Some(inputs.serializedArgs), // TODO when could this be None?
              protect = inputs.options.protect, // TODO we don't do what pulumi-java does, we do what pulumi-go does
              dependencies = inputs.depUrns.allDeps.toList,
              provider = inputs.providerId.getOrElse(""), // protobuf expects empty string and not null
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
              acceptResources = runOptions.acceptResources,
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
            for
              _    <- logger.debug(s"Executing RegisterResourceRequest for ${state.asLabel}")
              _    <- logger.trace(s"RegisterResourceRequest for ${state.asLabel}: ${pprint(req)}")
              resp <- this.monitor.registerResource(req)
            yield resp
          }
          .flatMap { response =>
            given Context = this
            for
              _                 <- logger.debug(s"Received RegisterResourceResponse for ${state.asLabel}")
              _                 <- logger.trace(s"RegisterResourceResponse for ${state.asLabel}: ${pprint(response)}")
              rawResourceResult <- RawResourceResult.fromResponse(response)
            yield rawResourceResult
          }
          .either
          .flatMap { eitherErrorOrResult =>
            given Context = this
            for
              _ <- logger.debug(
                s"Resolving resource ${state.asLabel} with ${eitherErrorOrResult
                    .fold(t => s"an error: ${t.getMessage()}", _ => "a result")}"
              )
              _         <- logger.trace(s"Resolving resource ${state.asLabel} with: ${pprint(eitherErrorOrResult)}")
              errOrUnit <- resolver.resolve(eitherErrorOrResult).either
              _ <- logger.debug(
                s"Resolved resource ${state.asLabel} ${errOrUnit.fold(t => s"with an error: ${t.getMessage()}", _ => "successfully")}"
              ) *>
                errOrUnit.fold(
                  error => Result.fail(error),
                  _ => Result.unit
                )
            yield ()
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
    given Context = this
    val label     = Label.fromNameAndType(name, typ)
    summon[ResourceDecoder[R]].makeResolver(label).flatMap { (resource, resolver) =>
      logger.debug(s"registering resource $label, trying to add to cache...") *>
        resources.cacheResource(typ, name, args, options, resource).flatMap { addedToCache =>
          if addedToCache then
            for
              _      <- logger.debug(s"Registering resource $label, added to cache...")
              tuple  <- summon[ArgsEncoder[A]].encode(args, _ => false)
              _      <- logger.debug(s"Encoded args for $label")
              state  <- createResourceState(typ, name, resource, options)
              _      <- logger.debug(s"Created resource state for $label")
              _      <- resources.add(resource, state)
              _      <- logger.debug(s"Added resource $label to resources")
              inputs <- prepareResourceInputs(label, resource, state, args, options)
              _      <- logger.debug(s"Prepared inputs for resource $label")
              _      <- addChildToParentResource(resource, options.parent)
              _      <- logger.debug(s"Added child $label to parent (isDefined: ${options.parent.isDefined})")
              _      <- executeRegisterResourceRequest(resource, state, resolver, inputs)
              _      <- logger.debug(s"executed RegisterResourceRequest for $label in background")
            yield resource
          else resources.getCachedResource(typ, name, args, options).map(_.asInstanceOf[R])
        }
    }

  private[besom] def registerResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  ): Output[R] =
    println(s"registerResource $typ $name")
    given Context = this
    Output(registerResourceInternal[R, A](typ, name, args, options).map(OutputData(_)))

  private[besom] def readResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
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
        case Some(parent) =>
          logger.trace(s"resolveParent for $typ - parent found in ResourceOptions: $parent") *>
            Result.pure(Some(parent))
        case None =>
          logger.trace(s"resolveParent for $typ - parent not found in ResourceOptions, using Stack") *>
            getStack.map(Some(_))

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
      _                <- logger.trace(s"mergeProviders for $typ - initialProviders: $initialProviders")
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

  private[besom] def createResourceState(
    typ: ResourceType,
    name: NonEmptyString,
    resource: Resource,
    resourceOptions: ResourceOptions
  ): Result[ResourceState] =
    for
      _             <- logger.debug(s"createResourceState $typ $name")
      parent        <- resolveParent(typ, resourceOptions)
      opts          <- applyTransformations(resourceOptions, parent) // todo add logging
      aliases       <- collapseAliases(opts) // todo add logging
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

object Context:

  def apply(
    runInfo: RunInfo,
    runOptions: RunOptions,
    featureSupport: FeatureSupport,
    config: Config,
    logger: BesomLogger,
    monitor: Monitor,
    engine: Engine,
    taskTracker: TaskTracker,
    stackPromise: Promise[Stack],
    resources: Resources
  ): Context = new Context(
    runInfo,
    runOptions,
    featureSupport,
    config,
    logger,
    monitor,
    engine,
    taskTracker,
    stackPromise,
    resources
  )

  def apply(
    runInfo: RunInfo,
    runOptions: RunOptions,
    featureSupport: FeatureSupport,
    config: Config,
    logger: BesomLogger,
    monitor: Monitor,
    engine: Engine,
    taskTracker: TaskTracker
  ): Result[Context] =
    for
      stack     <- Promise[Stack]()
      resources <- Resources()
    yield apply(
      runInfo,
      runOptions,
      featureSupport,
      config,
      logger,
      monitor,
      engine,
      taskTracker,
      stack,
      resources
    )

  def apply(runInfo: RunInfo, runOptions: RunOptions): Result[Context] =
    for
      taskTracker    <- TaskTracker()
      monitor        <- Monitor(runInfo.monitorAddress)
      engine         <- Engine(runInfo.engineAddress)
      logger         <- BesomLogger(engine)(using taskTracker)
      config         <- Config(runInfo.project, logger)
      featureSupport <- FeatureSupport(monitor)
      ctx            <- apply(runInfo, runOptions, featureSupport, config, logger, monitor, engine, taskTracker)
      _              <- ctx.initializeStack
    yield ctx
