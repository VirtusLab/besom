package besom.internal

import besom.util.*
import com.google.protobuf.Struct
import ujson.Bool
import pulumirpc.resource.SupportsFeatureRequest

case class RawResourceResult(urn: String, id: String, data: Struct, dependencies: Map[String, Set[Resource]])

case class Stack()

trait ProviderResource
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
  def urn: Output[String]
  def rawOutputs: Output[_]
  def children: Set[Resource]
  def providers: Map[String, ProviderResource]
  def provider: ProviderResource
  def version: String
  def pluginDownloadURL: String
  // def aliases: List[Output[F, String]]
  def name: String
  // def transformations: List[ResourceTransformation]
  def remoteComponent: Boolean

case class CommonResourceState(
  urn: Output[String],
  rawOutputs: Output[_],
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

case class ProviderResourceState(
  custom: CustomResourceState,
  pkg: String
) extends ResourceState:
  export custom.*

class ResourceManager(private val resources: Ref[Map[Resource, ResourceState]])

trait Context {

  def projectName: NonEmptyString
  def stackName: NonEmptyString
  def config: Config

  private[besom] val runInfo: RunInfo
  private[besom] val keepResources: Boolean
  private[besom] val keepOutputValues: Boolean
  private[besom] val monitor: Monitor
  private[besom] val engine: Engine
  private[besom] val workgroup: WorkGroup

  private[besom] def registerTask[A](fa: => Result[A]): Result[A]

  private[besom] def waitForAllTasks: Result[Unit]

  private[besom] def readOrRegisterResource[A](): Result[RawResourceResult]
  private[besom] def registerResource[A](): Result[RawResourceResult]
  private[besom] def readResource[A](): Result[RawResourceResult]

  private[besom] def close: Result[Unit]
}

object Context:

  def apply(
    _runInfo: RunInfo,
    _keepResources: Boolean,
    _keepOutputValues: Boolean,
    monitor: Monitor,
    engine: Engine,
    workgroup: WorkGroup
  ): Context =
    new Context:
      val projectName: NonEmptyString              = _runInfo.project
      val stackName: NonEmptyString                = _runInfo.stack
      val config: Config                           = _runInfo.config
      private[besom] val runInfo: RunInfo          = _runInfo
      private[besom] val keepResources: Boolean    = _keepResources
      private[besom] val keepOutputValues: Boolean = _keepOutputValues
      private[besom] val monitor: Monitor          = monitor
      private[besom] val engine: Engine            = engine
      private[besom] val workgroup: WorkGroup      = workgroup

      override private[besom] def registerTask[A](fa: => Result[A]): Result[A] = workgroup.runInWorkGroup(fa)

      override private[besom] def waitForAllTasks: Result[Unit] = workgroup.waitForAll

      override private[besom] def close: Result[Unit] =
        for
          _ <- monitor.close()
          _ <- engine.close()
        yield ()

      override private[besom] def readOrRegisterResource[A](): Result[RawResourceResult] = ???
      override private[besom] def registerResource[A](): Result[RawResourceResult]       = ???
      override private[besom] def readResource[A](): Result[RawResourceResult]           = ???

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
