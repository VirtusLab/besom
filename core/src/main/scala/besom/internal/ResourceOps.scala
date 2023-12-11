package besom.internal

import com.google.protobuf.struct.*
import pulumirpc.resource.*
import pulumirpc.resource.RegisterResourceRequest.PropertyDependencies
import besom.util.*
import besom.types.*
import besom.internal.logging.*
import pulumirpc.provider.InvokeResponse

// TODO remove
import scala.annotation.unused

type Providers = Map[String, ProviderResource]

class ResourceOps(using ctx: Context, mdc: BesomMDC[Label]):

  private[besom] def registerResourceOutputsInternal(
    urnResult: Result[URN],
    outputs: Result[Struct]
  ): Result[Unit] =
    urnResult.flatMap { urn =>
      outputs.flatMap { struct =>
        val request = RegisterResourceOutputsRequest(
          urn = urn.asString,
          outputs = if struct.fields.isEmpty then None else Some(struct)
        )

        ctx.monitor.registerResourceOutputs(request)
      }
    }

  private[besom] def readOrRegisterResourceInternal[R <: Resource: ResourceDecoder, A: ArgsEncoder](
    typ: ResourceType,
    name: NonEmptyString,
    args: A,
    options: ResourceOptions
  ): Result[R] =
    summon[ResourceDecoder[R]].makeResolver.flatMap { (resource, resolver) =>
      // this order is then repeated in registerReadOrGetResource
      val mode = if options.hasURN then "Getting" else if options.hasImportId then "Reading" else "Registering"

      log.debug(s"$mode resource, trying to add to cache...") *>
        ctx.resources.cacheResource(typ, name, args, options, resource).flatMap { addedToCache =>
          if addedToCache then
            for
              _      <- log.debug(s"$mode resource, added to cache...")
              state  <- createResourceState(typ, name, resource, options)
              _      <- log.debug(s"Created resource state")
              _      <- ctx.resources.add(resource, state)
              _      <- log.debug(s"Added resource to resources")
              inputs <- prepareResourceInputs(resource, state, args, options)
              _      <- log.debug(s"Prepared inputs for resource")
              _      <- addChildToParentResource(resource, options.parent)
              _      <- log.debug(s"Added child to parent (isDefined: ${options.parent.isDefined})")
              _      <- registerReadOrGetResource(resource, state, resolver, inputs, options)
            yield resource
          else ctx.resources.getCachedResource(typ, name, args, options).map(_.asInstanceOf[R])
        }
    }

  private[besom] def invokeInternal[A: ArgsEncoder, R: Decoder](tok: FunctionToken, args: A, opts: InvokeOptions): Output[R] =
    def decodeResponse(resultAsValue: Value, props: Map[String, Set[Resource]]): Result[OutputData[R]] =
      val resourceLabel = mdc.get(Key.LabelKey)
      summon[Decoder[R]].decode(resultAsValue, resourceLabel).asResult.flatMap {
        case Validated.Invalid(errs) =>
          Result.fail(new AggregatedDecodingError(errs))
        case Validated.Valid(value) =>
          Result.pure(value.withDependencies(props.values.flatten.toSet))
      }

    val outputDataOfR =
      PropertiesSerializer.serializeFilteredProperties(args, _ => false).flatMap { invokeArgs =>
        if invokeArgs.containsUnknowns then Result.pure(OutputData.unknown())
        else
          for
            resultAsValue <- executeInvoke(tok, invokeArgs, opts)
            outpudDataOfR <- decodeResponse(resultAsValue, invokeArgs.propertyToDependentResources)
          yield outpudDataOfR
      }

    besom.internal.Output.ofData(outputDataOfR) // TODO why the hell compiler assumes it's besom.aliases.Output?
  end invokeInternal

  private[besom] def callInternal[A: ArgsEncoder, R: Decoder](
    tok: FunctionToken,
    args: A,
    resource: Resource,
    opts: InvokeOptions
  ): Output[R] =
    ???

  private def executeInvoke(tok: FunctionToken, invokeArgs: SerializationResult, opts: InvokeOptions): Result[Value] =
    def buildInvokeRequest(args: Struct, provider: Option[String], version: Option[String]): Result[ResourceInvokeRequest] =
      Result {
        ResourceInvokeRequest(
          tok = tok,
          args = Some(args),
          provider = provider.getOrElse(""),
          version = version.getOrElse(""),
          acceptResources = ctx.runInfo.acceptResources
        )
      }

    def parseInvokeResponse(tok: FunctionToken, response: InvokeResponse): Result[Value] =
      if response.failures.nonEmpty then
        val failures = response.failures
          .map { failure =>
            s"${failure.reason} (${failure.property})"
          }
          .mkString(", ")

        Result.fail(new Exception(s"Invoke of $tok failed: $failures"))
      else if response.`return`.isEmpty then Result.fail(new Exception(s"Invoke of $tok returned empty result"))
      else
        val result = response.`return`.get
        Result.pure(Value(Value.Kind.StructValue(result)))

    val maybeProviderResult = opts.getNestedProvider(tok)
    val maybeProviderIdResult = maybeProviderResult.flatMap {
      case Some(value) => value.registrationId.map(Some(_))
      case None        => Result(None)
    }
    val version = opts.version

    for
      maybeProviderId <- maybeProviderIdResult
      req             <- buildInvokeRequest(invokeArgs.serialized, maybeProviderId, version)
      _               <- log.debug(s"Invoke RPC prepared, req=${pprint(req)}")
      res             <- ctx.monitor.invoke(req)
      _               <- log.debug(s"Invoke RPC executed, res=${pprint(res)}")
      parsed          <- parseInvokeResponse(tok, res)
    yield parsed
  end executeInvoke

  private def registerReadOrGetResource[R <: Resource](
    resource: Resource,
    state: ResourceState,
    resolver: ResourceResolver[R],
    inputs: PreparedInputs,
    options: ResourceOptions
  ): Result[Unit] =
    options.urn match
      case Some(urn) =>
        val execGetResourceAndComplete =
          for
            eitherErrorOrResult <- getResourceByUrn(urn)
            _                   <- completeResource(eitherErrorOrResult, resolver, state)
          yield ()

        execGetResourceAndComplete.fork.void *> log.debug(s"executed GetResource in background")

      case None =>
        options.getImportId match
          case Some(id) if id.isBlank => Result.fail(Exception("importId can't be empty here :|")) // sanity check
          case Some(id) =>
            val execReadResourceAndComplete =
              for
                eitherErrorOrResult <- executeReadResourceRequest(state, inputs, id)
                _                   <- completeResource(eitherErrorOrResult, resolver, state)
              yield ()

            execReadResourceAndComplete.fork.void *> log.debug(s"executed ReadResourceRequest in background")

          case None =>
            val execRegisterResourceRequestAndComplete =
              for
                eitherErrorOrResult <- executeRegisterResourceRequest(resource, state, inputs)
                _                   <- completeResource(eitherErrorOrResult, resolver, state)
              yield ()

            execRegisterResourceRequestAndComplete.fork.void *> log.debug(s"executed RegisterResourceRequest in background")

  case class GetResourceArgs(urn: URN) derives ArgsEncoder

  private def getResourceByUrn(urn: URN): Result[Either[Throwable, RawResourceResult]] =
    val req                = GetResourceArgs(urn)
    val tok: FunctionToken = "pulumi:pulumi:getResource"

    val rawResourceResult =
      for
        invokeArgs <- PropertiesSerializer.serializeFilteredProperties(req, _ => false)
        value      <- executeInvoke(tok, invokeArgs, InvokeOptions())
        rrr        <- RawResourceResult.fromValue(tok, value)
      yield rrr

    rawResourceResult.either

  private def completeResource[R <: Resource](
    eitherErrorOrResult: Either[Throwable, RawResourceResult],
    resolver: ResourceResolver[R],
    state: ResourceState
  ): Result[Unit] =
    val debugMessageSuffix = eitherErrorOrResult.fold(t => s"an error: ${t.getMessage()}", _ => "a result")

    for
      _         <- log.debug(s"Resolving resource ${state.asLabel} with $debugMessageSuffix")
      _         <- log.trace(s"Resolving resource ${state.asLabel} with: ${pprint(eitherErrorOrResult)}")
      errOrUnit <- resolver.resolve(eitherErrorOrResult).either
      _         <- errOrUnit.fold(ctx.fail, _ => Result.unit) // fail context if resource resolution fails
      errOrUnitMsg = errOrUnit.fold(t => s"with an error: ${t.getMessage()}", _ => "successfully")
      failResult   = errOrUnit.fold(t => Result.fail(t), _ => Result.unit)
      _ <- log.debug(s"Resolved resource ${state.asLabel} $errOrUnitMsg") *> failResult
    yield ()

  private def resolveProviderReferences(state: ResourceState): Result[Map[String, String]] =
    Result
      .sequence(
        state.providers.map { case (pkg, provider) =>
          provider.registrationId.map(pkg -> _)
        }
      )
      .map(_.toMap)

  private def resolveTransitivelyReferencedComponentResourceUrns(
    resources: Set[Resource]
  ): Result[Set[URN]] =
    def findAllReachableResources(left: Set[Resource], acc: Set[Resource]): Result[Set[Resource]] =
      if left.isEmpty then Result.pure(acc)
      else
        val current = left.head
        val childrenResult = current match
          case cr: ComponentResource => ctx.resources.getStateFor(cr).map(_.children)
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
    allDeps: Set[URN],
    allDepsByProperty: Map[String, Set[URN]]
  )

  case class PreparedInputs(
    serializedArgs: Struct,
    parentUrn: Option[URN],
    providerId: Option[String],
    providerRefs: Map[String, String],
    depUrns: AggregatedDependencyUrns,
    aliases: List[String],
    options: ResourceOptions
  )

  private def aggregateDependencyUrns(
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

  private def resolveAliases(@unused resource: Resource): Result[List[String]] =
    Result.pure(List.empty) // TODO aliases

  private def resolveProviderRegistrationId(state: ResourceState): Result[Option[String]] =
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

  private def prepareResourceInputs[A: ArgsEncoder](
    resource: Resource,
    state: ResourceState,
    args: A,
    options: ResourceOptions
  ): Result[PreparedInputs] =
    for
      _            <- log.trace(s"Preparing inputs: gathering direct dependencies")
      directDeps   <- options.dependsOn.getValueOrElse(List.empty).map(_.toSet)
      _            <- log.trace(s"Preparing inputs: serializing resource properties")
      serResult    <- PropertiesSerializer.serializeResourceProperties(args)
      _            <- log.trace(s"Preparing inputs: serialized resource properties, getting parent URN")
      parentUrnOpt <- resolveParentUrn(state.typ, options)
      _            <- log.trace(s"Preparing inputs: got parent URN, getting provider reg ID")
      provIdOpt    <- resolveProviderRegistrationId(state)
      _            <- log.trace(s"Preparing inputs: got provider reg ID, resolving provider references")
      providerRefs <- resolveProviderReferences(state)
      _            <- log.trace(s"Preparing inputs: resolved provider refs, aggregating dep URNs")
      depUrns      <- aggregateDependencyUrns(directDeps, serResult.propertyToDependentResources)
      _            <- log.trace(s"Preparing inputs: aggregated dep URNs, resolving aliases")
      aliases      <- resolveAliases(resource)
      _            <- log.trace(s"Preparing inputs: resolved aliases, done")
    yield PreparedInputs(serResult.serialized, parentUrnOpt, provIdOpt, providerRefs, depUrns, aliases, options)

  private def executeReadResourceRequest[R <: Resource](
    state: ResourceState,
    inputs: PreparedInputs,
    id: ResourceId
  ): Result[Either[Throwable, RawResourceResult]] =
    ctx.registerTask {
      Result
        .defer {
          ReadResourceRequest(
            `type` = state.typ,
            name = state.name,
            parent = inputs.parentUrn.getOrElse(URN.empty).asString, // protobuf expects empty string and not null
            id = id,
            properties = Some(inputs.serializedArgs), // TODO when could this be None?
            dependencies = inputs.depUrns.allDeps.toList.map(_.asString),
            provider = inputs.providerId.getOrElse(""), // protobuf expects empty string and not null
            version = inputs.options.version,
            acceptSecrets = true, // TODO doing like java does it
            acceptResources = ctx.runInfo.acceptResources
          )
        }
        .flatMap { req =>
          for
            _    <- log.debug(s"Executing ReadResourceRequest for ${state.asLabel}")
            _    <- log.trace(s"ReadResourceRequest for ${state.asLabel}: ${pprint(req)}")
            resp <- ctx.monitor.readResource(req)
          yield resp
        }
        .flatMap { response =>
          for
            _                 <- log.debug(s"Received ReadResourceResponse for ${state.asLabel}")
            _                 <- log.trace(s"ReadResourceResponse for ${state.asLabel}: ${pprint(response)}")
            rawResourceResult <- RawResourceResult.fromResponse(response, id)
          yield rawResourceResult
        }
        .either
    }

  private def executeRegisterResourceRequest[R <: Resource](
    resource: Resource,
    state: ResourceState,
    inputs: PreparedInputs
  ): Result[Either[Throwable, RawResourceResult]] =
    ctx.registerTask {
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
            parent = inputs.parentUrn.getOrElse(URN.empty).asString, // protobuf expects empty string and not null
            custom = resource.isCustom,
            `object` = Some(inputs.serializedArgs), // TODO when could this be None?
            protect = inputs.options.protect, // TODO we don't do what pulumi-java does, we do what pulumi-go does
            dependencies = inputs.depUrns.allDeps.toList.map(_.asString),
            provider = inputs.providerId.getOrElse(""), // protobuf expects empty string and not null
            providers = inputs.providerRefs,
            propertyDependencies = inputs.depUrns.allDepsByProperty.map { case (key, value) =>
              key -> PropertyDependencies(value.toList.map(_.asString))
            }.toMap,
            deleteBeforeReplaceDefined = true,
            deleteBeforeReplace = inputs.options match
              case CustomResourceOptions(_, _, deleteBeforeReplace, _, _) => deleteBeforeReplace
              case _                                                      => false
            ,
            version = inputs.options.version,
            ignoreChanges = inputs.options.ignoreChanges,
            acceptSecrets = true, // TODO doing like java does it
            acceptResources = ctx.runInfo.acceptResources,
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
              pulumirpc.alias.Alias(pulumirpc.alias.Alias.Alias.Urn(alias))
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
            _    <- log.debug(s"Executing RegisterResourceRequest for ${state.asLabel}")
            _    <- log.trace(s"RegisterResourceRequest for ${state.asLabel}: ${pprint(req)}")
            resp <- ctx.monitor.registerResource(req)
          yield resp
        }
        .flatMap { response =>
          for
            _                 <- log.debug(s"Received RegisterResourceResponse for ${state.asLabel}")
            _                 <- log.trace(s"RegisterResourceResponse for ${state.asLabel}: ${pprint(response)}")
            rawResourceResult <- RawResourceResult.fromResponse(response)
          yield rawResourceResult
        }
        .either
    }

  private def addChildToParentResource(resource: Resource, maybeParent: Option[Resource]): Result[Unit] =
    maybeParent match
      case None         => Result.unit
      case Some(parent) => ctx.resources.updateStateFor(parent)(_.addChild(resource))

  // This method returns an Option of Resource because for Stack there is no parent resource,
  // for any other resource the parent is either explicitly set in ResourceOptions or the stack is the parent.
  private def resolveParentUrn(typ: ResourceType, resourceOptions: ResourceOptions): Result[Option[URN]] =
    if typ == Stack.RootPulumiStackTypeName then Result.pure(None)
    else
      resourceOptions.parent match
        case Some(parent) =>
          log.trace(s"resolveParent - parent found in ResourceOptions: $parent") *>
            parent.urn.getValue
        case None =>
          for
            parentUrn <- ctx.getParentURN
            _         <- log.trace(s"resolveParent - parent not found in ResourceOptions, from Context: $parentUrn")
          yield Some(parentUrn)

  private def resolveParentTransformations(@unused typ: ResourceType, @unused resourceOptions: ResourceOptions): Result[List[Unit]] =
    Result.pure(List.empty) // TODO parent transformations

  private def applyTransformations(
    resourceOptions: ResourceOptions,
    @unused parentTransformations: List[Unit] // TODO this needs transformations from ResourceState, not Resource
  ): Result[ResourceOptions] =
    Result.pure(resourceOptions) // TODO resource transformations

  private def collapseAliases(@unused opts: ResourceOptions): Result[List[Output[String]]] =
    Result.pure(List.empty) // TODO aliases

  private def mergeProviders(typ: String, opts: ResourceOptions): Result[Providers] =
    def getParentProviders = opts.parent match
      case None         => Result.pure(Map.empty)
      case Some(parent) => ctx.resources.getStateFor(parent).map(_.providers)

    def overwriteWithProvidersFromOptions(initialProviders: Providers): Result[Providers] =
      opts match
        case CustomResourceOptions(common, provider, _, _, _) =>
          provider match
            case None => Result.pure(initialProviders)
            case Some(provider) =>
              ctx.resources.getStateFor(provider).map { prs =>
                initialProviders + (prs.pkg -> provider)
              }

        case ComponentResourceOptions(_, providers) =>
          Result
            .sequence(
              providers.map(provider => ctx.resources.getStateFor(provider).map(rs => rs.pkg -> provider))
            )
            .map(_.toMap)
            // overwrite overlapping initialProviders with providers from ComponentResourceOptions
            .map(initialProviders ++ _)

        case StackReferenceResourceOptions(_, _) => Result.pure(initialProviders)

    for
      initialProviders <- getParentProviders
      _                <- log.trace(s"mergeProviders for $typ - initialProviders: $initialProviders")
      providers        <- overwriteWithProvidersFromOptions(initialProviders)
      _                <- log.trace(s"final providers for $typ - initialProviders: $initialProviders")
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
            ctx.resources.getStateFor(providerFromOpts).flatMap { prs =>
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

  private def createResourceState(
    typ: ResourceType,
    name: NonEmptyString,
    resource: Resource,
    resourceOptions: ResourceOptions
  ): Result[ResourceState] =
    for
      _                     <- log.debug(s"createResourceState")
      parentTransformations <- resolveParentTransformations(typ, resourceOptions)
      opts                  <- applyTransformations(resourceOptions, parentTransformations) // todo add logging
      aliases               <- collapseAliases(opts) // todo add logging
      providers             <- mergeProviders(typ, opts)
      maybeProvider         <- getProvider(typ, providers, opts)
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
        case ComponentBase(urn) =>
          ComponentResourceState(common = commonRS) // TODO: ComponentBase should not be registered"
    }

  extension (invokeOpts: InvokeOptions)
    def getNestedProvider(token: FunctionToken)(using Context): Result[Option[ProviderResource]] =
      invokeOpts.provider match
        case Some(passedProvider) => Result(Some(passedProvider))
        case None =>
          invokeOpts.parent match
            case Some(explicitParent) =>
              Context().resources.getStateFor(explicitParent).map { parentState =>
                parentState.providers.get(token.getPackage)
              }
            case None => Result(None)
end ResourceOps
