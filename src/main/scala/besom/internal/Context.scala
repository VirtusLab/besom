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

sealed trait ResourceState[F[+_]]:
  def urn: Output[F, String]
  def rawOutputs: Output[F, _]
  def children: Set[Resource]
  def providers: Map[String, ProviderResource]
  def provider: ProviderResource
  def version: String
  def pluginDownloadURL: String
  // def aliases: List[Output[F, String]]
  def name: String
  // def transformations: List[ResourceTransformation]
  def remoteComponent: Boolean

case class CommonResourceState[F[+_]](
    urn: Output[F, String],
    rawOutputs: Output[F, _],
    children: Set[Resource],
    providers: Map[String, ProviderResource],
    provider: ProviderResource,
    version: String,
    pluginDownloadURL: String,
    // aliases: List[Output[F, String]],
    name: String,
    // transformations: List[ResourceTransformation],
    remoteComponent: Boolean
) extends ResourceState[F]

case class CustomResourceState[F[+_]](
    common: ResourceState[F],
    id: Output[F, String]
) extends ResourceState[F]:
  export common.*

case class ProviderResourceState[F[+_]](
    custom: CustomResourceState[F],
    pkg: String
) extends ResourceState[F]:
  export custom.*

class ResourceManager[F[+_]](private val resources: Ref[F, Map[Resource, ResourceState[F]]])

class Context[F[+_]] private (
    val projectName: NonEmptyString,
    val stackName: NonEmptyString,
    val config: Config,
    private val keepResources: Boolean,
    private val keepOutputValues: Boolean,
    private val monitor: Monitor[F],
    private val engine: Engine[F],
    private val workgroup: WorkGroup[F]
)(using F: Monad[F]):
  private[besom] def registerTask[A](fa: => F[A]): F[A] = workgroup.runInWorkGroup(fa)

  private[besom] def waitForAllTasks: F[Unit] = workgroup.waitForAll

  private[besom] def close: F[Unit] =
    for
      _ <- monitor.close()
      _ <- engine.close()
    yield ()

  private[besom] def readOrRegisterResource[A](): F[RawResourceResult] = ???
  private[besom] def registerResource[A](): F[RawResourceResult]       = ???
  private[besom] def readResource[A](): F[RawResourceResult]           = ???

  def monad: Monad[F] = F

object Context:
  def apply[F[+_]](
      runInfo: RunInfo,
      monitor: Monitor[F],
      engine: Engine[F],
      keepResources: Boolean,
      keepOutputValues: Boolean
  )(using F: Monad[F]): F[Context[F]] =
    WorkGroup[F].map { wg =>
      new Context(runInfo.project, runInfo.stack, runInfo.config, keepResources, keepOutputValues, monitor, engine, wg)
    }

  def apply[F[+_]](runInfo: RunInfo)(using F: Monad[F]): F[Context[F]] =
    for
      monitor          <- Monitor[F](runInfo.monitorAddress)
      engine           <- Engine[F](runInfo.engineAddress)
      keepResources    <- monitor.supportsFeature(SupportsFeatureRequest("resourceReferences")).map(_.hasSupport)
      keepOutputValues <- monitor.supportsFeature(SupportsFeatureRequest("outputValues")).map(_.hasSupport)
      ctx              <- apply(runInfo, monitor, engine, keepResources, keepOutputValues)
    yield ctx
