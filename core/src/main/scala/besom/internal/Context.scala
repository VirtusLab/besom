package besom.internal

import besom.util.*
import com.google.protobuf.struct.Struct
import pulumirpc.resource.SupportsFeatureRequest
import besom.util.Types.ProviderType
import io.grpc.internal.DnsNameResolver.ResourceResolver

case class RawResourceResult(urn: String, id: Option[String], data: Struct, dependencies: Map[String, Set[Resource]])

case class Stack()

trait ResourceResolver[A]:
  def resolve(errorOrResourceResult: Either[Throwable, RawResourceResult])(using Context): Result[Unit]

trait ResourceDecoder[A <: Resource]: // TODO rename to something more sensible
  def makeResolver(using Context): Result[(A, ResourceResolver[A])]

object ResourceDecoder:
  inline def derived[A <: Resource]: ResourceDecoder[A] =
    new ResourceDecoder[A]:
      def makeResolver(using Context): Result[(A, ResourceResolver[A])] = ???

// type ResourceState struct {
// 	m sync.RWMutex

// 	urn URNOutput `pulumi:"urn"`

// 	rawOutputs        Output
// 	children          resourceSet
// 	providers         map[string]ProviderResource
// 	provider          ProviderResource
// 	version           string
// 	pluginDownloadURL string
// 	aliases           []URNOutput
// 	name              string
// 	transformations   []ResourceTransformation

// 	remoteComponent bool
// }

sealed trait ResourceState:
  def urn: Output[String] // TODO BALEET, URN is in custom resource anyway
  def rawOutputs: Output[_] // TODO BALEET this is for StackReference only and is a hack used by pulumi-go, we'll use the non-hacky way from pulumi-java
  def children: Set[Resource]
  def providers: Map[String, ProviderResource] // TODO: Move to ComponentResourceState
  def provider: ProviderResource
  def version: String
  def pluginDownloadURL: String
  // def aliases: List[Output[F, String]]
  def name: String
  // def transformations: List[ResourceTransformation]
  def remoteComponent: Boolean

case class CommonResourceState(
  urn: Output[String], // TODO BALEET, URN is in custom resource anyway
  rawOutputs: Output[_], // TODO BALEET this is for StackReference only and is a hack used by pulumi-go, we'll use the non-hacky way from pulumi-java
  children: Set[Resource],
  providers: Map[String, ProviderResource],
  provider: ProviderResource,
  version: String,
  pluginDownloadURL: String,
  // aliases: List[Output[F, String]],
  name: String,
  // transformations: List[ResourceTransformation],
  remoteComponent: Boolean
) extends ResourceState

case class CustomResourceState(
  common: ResourceState,
  id: Output[String]
) extends ResourceState:
  export common.*

case class ComponentResourceState(
)

case class ProviderResourceState(
  custom: CustomResourceState,
  pkg: String
) extends ResourceState:
  export custom.*

// needed for parent/child relationship tracking
class ResourceManager(private val resources: Ref[Map[Resource, ResourceState]])

trait Resource:
  def urn: Output[String]

trait CustomResource extends Resource:
  def id: Output[String]

trait ComponentResource extends Resource

trait ProviderResource extends CustomResource

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

  private[besom] def isDryRun: Boolean = runInfo.dryRun

  private[besom] def registerTask[A](fa: => Result[A]): Result[A]

  private[besom] def waitForAllTasks: Result[Unit]

  private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
    typ: ProviderType, // TODO: ProviderType
    name: NonEmptyString,
    args: A,
    options: CustomResourceOptions
  ): Output[R]

  private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder](
    typ: NonEmptyString,
    name: NonEmptyString
  ): Output[R]

  private[besom] def registerResource[R <: Resource: ResourceDecoder](
    typ: NonEmptyString,
    name: NonEmptyString
  ): Output[R]

  private[besom] def readResource[R <: Resource: ResourceDecoder](
    typ: NonEmptyString,
    name: NonEmptyString
  ): Output[R]

  private[besom] def createResourceState(typ: NonEmptyString, name: NonEmptyString): Result[ResourceState]

  private[besom] def close: Result[Unit]
}

object Context:

  def apply(
    _runInfo: RunInfo,
    _keepResources: Boolean,
    _keepOutputValues: Boolean,
    _monitor: Monitor,
    _engine: Engine,
    _workgroup: WorkGroup
  ): Context =
    new Context:
      val projectName: NonEmptyString              = _runInfo.project
      val stackName: NonEmptyString                = _runInfo.stack
      val config: Config                           = _runInfo.config
      private[besom] val runInfo: RunInfo          = _runInfo
      private[besom] val keepResources: Boolean    = _keepResources
      private[besom] val keepOutputValues: Boolean = _keepOutputValues
      private[besom] val monitor: Monitor          = _monitor
      private[besom] val engine: Engine            = _engine
      private[besom] val workgroup: WorkGroup      = _workgroup

      private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
        typ: ProviderType, // TODO: ProviderType
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

      override private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder](
        typ: NonEmptyString,
        name: NonEmptyString
      ): Output[R] = ???
      
      override private[besom] def registerResource[R <: Resource: ResourceDecoder](
        typ: NonEmptyString,
        name: NonEmptyString
      ): Output[R] = ???
      override private[besom] def readResource[R <: Resource: ResourceDecoder](
        typ: NonEmptyString,
        name: NonEmptyString
      ): Output[R] = ???
      // summon[ResourceDecoder[R]].makeFulfillable(using this) match
      //  case (r, fulfillable) =>

      override private[besom] def createResourceState(
        typ: NonEmptyString,
        name: NonEmptyString
      ): Result[ResourceState] = ???

  def apply(
    runInfo: RunInfo,
    monitor: Monitor,
    engine: Engine,
    keepResources: Boolean,
    keepOutputValues: Boolean
  ): Result[Context] =
    WorkGroup().map { wg =>
      apply(runInfo, keepResources, keepOutputValues, monitor, engine, wg)
    }

  def apply(runInfo: RunInfo): Result[Context] =
    for
      monitor          <- Monitor(runInfo.monitorAddress)
      engine           <- Engine(runInfo.engineAddress)
      keepResources    <- monitor.supportsFeature(SupportsFeatureRequest("resourceReferences")).map(_.hasSupport)
      keepOutputValues <- monitor.supportsFeature(SupportsFeatureRequest("outputValues")).map(_.hasSupport)
      ctx              <- apply(runInfo, monitor, engine, keepResources, keepOutputValues)
    yield ctx

object Providers:
  val ProviderResourceTypePrefix: NonEmptyString = "pulumi:providers:"
  def providerResourceType(`package`: NonEmptyString): NonEmptyString =
    ProviderResourceTypePrefix +++ `package`
