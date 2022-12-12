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

trait Context {
  type F[+A]
  // given F: Monad[F]
  private[besom] val runInfo: RunInfo
  private[besom] val keepResources: Boolean
  private[besom] val keepOutputValues: Boolean
  private[besom] val monitor: Monitor[F]
  private[besom] val engine: Engine[F]
  private[besom] val workgroup: WorkGroup[F]

  private[besom] def registerTask[A](fa: => F[A]): F[A]

  private[besom] def waitForAllTasks: F[Unit]

  private[besom] def readOrRegisterResource[A](): F[RawResourceResult]
  private[besom] def registerResource[A](): F[RawResourceResult]
  private[besom] def readResource[A](): F[RawResourceResult]

  private[besom] def close: F[Unit]
  private[besom] def monad: Monad[F]
}

object Context:
  def apply[M[+_]](
      runInfo: RunInfo,
      keepResources: Boolean,
      keepOutputValues: Boolean,
      monitorF0: Monitor[M],
      engineF0: Engine[M],
      workgroupF0: WorkGroup[M]
  )(using F: Monad[M]): Context { type F[A] = M[A] } =
    new Context:
      type F[+A] = M[A]

      val projectName: NonEmptyString       = projectName
      val stackName: NonEmptyString         = stackName
      val config: Config                    = config
      private val keepResources: Boolean    = keepResources
      private val keepOutputValues: Boolean = keepOutputValues
      private val monitor: Monitor[F]       = monitorF0
      private val engine: Engine[F]         = engineF0
      private val workgroup: WorkGroup[F]   = workgroupF0

      override private[besom] def registerTask[A](fa: => F[A]): F[A] = workgroup.runInWorkGroup(fa)

      override private[besom] def waitForAllTasks: F[Unit] = workgroup.waitForAll

      override private[besom] def close: F[Unit] =
        for
          _ <- monitor.close()
          _ <- engine.close()
        yield ()

      override private[besom] def readOrRegisterResource[A](): F[RawResourceResult] = ???
      override private[besom] def registerResource[A](): F[RawResourceResult]       = ???
      override private[besom] def readResource[A](): F[RawResourceResult]           = ???

      override private[besom] def monad: Monad[F] = F

  def apply[F[+_]](
      runInfo: RunInfo,
      monitor: Monitor[F],
      engine: Engine[F],
      keepResources: Boolean,
      keepOutputValues: Boolean
  )(using F: Monad[F]): F[Context] =
    WorkGroup[F].map { wg =>
      apply(runInfo, keepResources, keepOutputValues, monitor, engine, wg)
    }

  def apply[F[+_]](runInfo: RunInfo)(using F: Monad[F]): F[Context] =
    for
      monitor          <- Monitor[F](runInfo.monitorAddress)
      engine           <- Engine[F](runInfo.engineAddress)
      keepResources    <- monitor.supportsFeature(SupportsFeatureRequest("resourceReferences")).map(_.hasSupport)
      keepOutputValues <- monitor.supportsFeature(SupportsFeatureRequest("outputValues")).map(_.hasSupport)
      ctx              <- apply(runInfo, monitor, engine, keepResources, keepOutputValues)
    yield ctx
