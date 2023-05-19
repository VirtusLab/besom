package besom.api

import besom.util.*
import besom.internal.*
import scala.util.Try
import spray.json.*

object k8s:

  case class ManagedFieldsEntry(
    apiVersion: Option[String],
    fieldsType: Option[String],
    fieldsV1: Option[JsValue],
    manager: Option[String],
    operation: Option[String],
    subresource: Option[String],
    time: Option[String]
  ) derives Decoder
  case class OwnerReference(
    apiVersion: String,
    blockOwnerDeletion: Option[Boolean],
    controller: Option[Boolean],
    kind: String,
    name: String,
    uid: String
  ) derives Decoder

  case class ObjectMeta(
    annotations: Map[String, String] = Map.empty,
    clusterName: Option[String],
    creationTimestamp: Option[String],
    deletionGracePeriodSeconds: Option[Int],
    deletionTimestamp: Option[String],
    finalizers: List[String] = List.empty,
    generateName: Option[String],
    generation: Option[Int],
    labels: Map[String, String] = Map.empty,
    managedFields: List[ManagedFieldsEntry] = List.empty,
    name: Option[String],
    namespace: Option[String],
    ownerReferences: List[OwnerReference] = List.empty,
    resourceVersion: Option[String],
    selfLink: Option[String],
    uid: Option[String]
  ) derives Decoder

  case class NodeSelectorRequirement(key: String, operator: String, values: List[String] = List.empty) derives Decoder

  case class NodeSelectorTerm(
    matchExpressions: List[NodeSelectorRequirement] = List.empty,
    matchFields: List[NodeSelectorRequirement] = List.empty
  ) derives Decoder

  case class NodeSelector(nodeSelectorTerms: List[NodeSelectorTerm]) derives Decoder
  case class PreferredSchedulingTerm(preference: NodeSelectorTerm, weight: Int) derives Decoder

  case class WeightedPodAffinityTerm(podAffinityTerm: PodAffinityTerm, weight: Int) derives Decoder
  case class LabelSelectorRequirement(key: String, operator: String, values: List[String] = List.empty) derives Decoder
  case class LabelSelector(
    matchExpressions: List[LabelSelectorRequirement] = List.empty,
    matchLabels: Map[String, String] = Map.empty
  ) derives Decoder
  case class PodAffinityTerm(
    labelSelector: Option[LabelSelector],
    namespaceSelector: Option[LabelSelector],
    namespaces: List[String] = List.empty,
    topologyKey: String
  ) derives Decoder

  case class NodeAffinity(
    preferredDuringSchedulingIgnoredDuringExecution: List[PreferredSchedulingTerm] = List.empty,
    requiredDuringSchedulingIgnoredDuringExecution: Option[NodeSelector]
  ) derives Decoder
  case class PodAffinity(
    preferredDuringSchedulingIgnoredDuringExecution: List[WeightedPodAffinityTerm] = List.empty,
    requiredDuringSchedulingIgnoredDuringExecution: List[PodAffinityTerm] = List.empty
  ) derives Decoder
  case class PodAntiAffinity(
    preferredDuringSchedulingIgnoredDuringExecution: List[WeightedPodAffinityTerm] = List.empty,
    requiredDuringSchedulingIgnoredDuringExecution: List[PodAffinityTerm] = List.empty
  ) derives Decoder

  case class Affinity(
    nodeAffinity: Option[NodeAffinity],
    podAffinity: Option[PodAffinity],
    podAntiAffinity: Option[PodAntiAffinity]
  ) derives Decoder
  case class EnvVar(name: String, value: Option[String], valueFrom: Option[EnvVarSource]) derives Decoder
  case class EnvVarSource(
    configMapKeyRef: Option[ConfigMapKeySelector],
    fieldRef: Option[ObjectFieldSelector],
    resourceFieldRef: Option[ResourceFieldSelector],
    secretKeyRef: Option[SecretKeySelector]
  ) derives Decoder
  case class ConfigMapKeySelector(key: String, name: Option[String], optional: Option[Boolean]) derives Decoder
  case class ObjectFieldSelector(apiVersion: Option[String], fieldPath: String) derives Decoder
  case class ResourceFieldSelector(containerName: Option[String], divisor: Option[String], resource: String)
      derives Decoder
  case class SecretKeySelector(key: String, name: Option[String], optional: Option[Boolean]) derives Decoder
  case class EnvFromSource(
    configMapRef: Option[ConfigMapEnvSource],
    prefix: Option[String],
    secretRef: Option[SecretEnvSource]
  ) derives Decoder
  case class ConfigMapEnvSource(name: Option[String], optional: Option[Boolean]) derives Decoder
  case class SecretEnvSource(name: Option[String], optional: Option[Boolean]) derives Decoder
  case class Lifecycle(postStart: Option[LifecycleHandler], preStop: Option[LifecycleHandler]) derives Decoder
  case class LifecycleHandler(
    exec: Option[ExecAction],
    httpGet: Option[HTTPGetAction],
    tcpSocket: Option[TCPSocketAction]
  ) derives Decoder
  case class ExecAction(command: List[String] = List.empty) derives Decoder
  case class HTTPGetAction(
    host: Option[String],
    httpHeaders: List[HTTPHeader] = List.empty,
    path: Option[String],
    port: Int | String,
    scheme: Option[String]
  ) derives Decoder
  case class HTTPHeader(name: String, value: String) derives Decoder
  case class TCPSocketAction(host: Option[String], port: Int | String) derives Decoder
  case class Probe(
    exec: Option[ExecAction],
    failureThreshold: Option[Int],
    grpc: Option[GRPCAction],
    httpGet: Option[HTTPGetAction],
    initialDelaySeconds: Option[Int],
    periodSeconds: Option[Int],
    successThreshold: Option[Int],
    tcpSocket: Option[TCPSocketAction],
    terminationGracePeriodSeconds: Option[Int],
    timeoutSeconds: Option[Int]
  ) derives Decoder
  case class GRPCAction(port: Int, service: Option[String]) derives Decoder
  case class ContainerPort(
    containerPort: Int,
    hostIP: Option[String],
    hostPort: Option[Int],
    name: Option[String],
    protocol: Option[String]
  ) derives Decoder
  case class ResourceRequirements(
    claims: List[ResourceClaim] = List.empty,
    limits: Map[String, String] = Map.empty,
    requests: Map[String, String] = Map.empty
  ) derives Decoder
  case class ResourceClaim(name: String) derives Decoder
  case class SecurityContext(
    allowPrivilegeEscalation: Option[Boolean],
    capabilities: Option[Capabilities],
    privileged: Option[Boolean],
    procMount: Option[String],
    readOnlyRootFilesystem: Option[Boolean],
    runAsGroup: Option[Int],
    runAsNonRoot: Option[Boolean],
    runAsUser: Option[Int],
    seLinuxOptions: Option[SELinuxOptions],
    seccompProfile: Option[SeccompProfile],
    windowsOptions: Option[WindowsSecurityContextOptions]
  ) derives Decoder
  case class Capabilities(add: List[String] = List.empty, drop: List[String] = List.empty) derives Decoder
  case class SELinuxOptions(level: Option[String], role: Option[String], `type`: Option[String], user: Option[String])
      derives Decoder
  case class SeccompProfile(localhostProfile: Option[String], `type`: String) derives Decoder
  case class WindowsSecurityContextOptions(
    gmsaCredentialSpec: Option[String],
    gmsaCredentialSpecName: Option[String],
    hostProcess: Option[Boolean],
    runAsUserName: Option[String]
  ) derives Decoder
  case class VolumeDevice(devicePath: String, name: String) derives Decoder
  case class VolumeMount(
    mountPath: String,
    mountPropagation: Option[String],
    name: String,
    readOnly: Option[Boolean],
    subPath: Option[String],
    subPathExpr: Option[String]
  ) derives Decoder
  case class Container(
    args: List[String] = List.empty,
    command: List[String] = List.empty,
    env: List[EnvVar] = List.empty,
    envFrom: List[EnvFromSource] = List.empty,
    image: Option[String],
    lifecycle: Option[Lifecycle],
    livenessProbe: Option[Probe],
    name: String,
    ports: List[ContainerPort] = List.empty,
    readinessProbe: Option[Probe],
    resources: Option[ResourceRequirements],
    securityContext: Option[SecurityContext],
    startupProbe: Option[Probe],
    stdin: Option[Boolean],
    stdinOnce: Option[Boolean],
    terminationMessagePath: Option[String],
    terminationMessagePolicy: Option[String],
    tty: Option[Boolean],
    volumeDevices: List[VolumeDevice] = List.empty,
    volumeMounts: List[VolumeMount] = List.empty,
    workingDir: Option[String]
  ) derives Decoder
  case class PodDNSConfig(
    nameservers: List[String] = List.empty,
    options: List[PodDNSConfigOption] = List.empty,
    searches: List[String] = List.empty
  ) derives Decoder
  case class PodDNSConfigOption(name: Option[String], value: Option[String]) derives Decoder
  case class EphemeralContainer(
    args: List[String] = List.empty,
    command: List[String] = List.empty,
    env: List[EnvVar] = List.empty,
    envFrom: List[EnvFromSource] = List.empty,
    image: Option[String],
    imagePullPolicy: Option[String],
    lifecycle: Option[Lifecycle],
    livenessProbe: Option[Probe],
    name: String,
    ports: List[ContainerPort] = List.empty,
    readinessProbe: Option[Probe],
    resources: Option[ResourceRequirements],
    securityContext: Option[SecurityContext],
    startupProbe: Option[Probe],
    stdin: Option[Boolean],
    stdinOnce: Option[Boolean],
    targetContainerName: Option[String],
    terminationMessagePath: Option[String],
    terminationMessagePolicy: Option[String],
    tty: Option[Boolean],
    volumeDevices: List[VolumeDevice] = List.empty,
    volumeMounts: List[VolumeMount] = List.empty,
    workingDir: Option[String]
  ) derives Decoder
  case class HostAlias(hostnames: List[String] = List.empty, ip: Option[String]) derives Decoder
  case class LocalObjectReference(name: Option[String]) derives Decoder
  case class PodOS(name: String) derives Decoder
  case class PodReadinessGate(conditionType: String) derives Decoder
  case class PodResourceClaim(name: String, source: Option[ClaimSource]) derives Decoder
  case class ClaimSource(resourceClaimName: Option[String], resourceClaimTemplateName: Option[String]) derives Decoder
  case class PodSchedulingGate(name: String) derives Decoder
  case class PodSecurityContext(
    fsGroup: Option[Int],
    fsGroupChangePolicy: Option[String],
    runAsGroup: Option[Int],
    runAsNonRoot: Option[Boolean],
    runAsUser: Option[Int],
    seLinuxOptions: Option[SELinuxOptions],
    seccompProfile: Option[SeccompProfile],
    supplementalGroups: List[Int] = List.empty,
    sysctls: List[Sysctl] = List.empty,
    windowsOptions: Option[WindowsSecurityContextOptions]
  ) derives Decoder
  case class Sysctl(name: String, value: String) derives Decoder
  case class Toleration(
    effect: Option[String],
    key: Option[String],
    operator: Option[String],
    tolerationSeconds: Option[Int],
    value: Option[String]
  ) derives Decoder
  case class TopologySpreadConstraint(
    labelSelector: Option[LabelSelector],
    matchLabelKeys: List[String] = List.empty,
    maxSkew: Int,
    minDomains: Option[Int],
    nodeAffinityPolicy: Option[String],
    nodeTaintsPolicy: Option[String],
    topologyKey: String,
    whenUnsatisfiable: String
  ) derives Decoder
  case class Volume(
    awsElasticBlockStore: Option[AWSElasticBlockStoreVolumeSource],
    azureDisk: Option[AzureDiskVolumeSource],
    azureFile: Option[AzureFileVolumeSource],
    cephfs: Option[CephFSVolumeSource],
    cinder: Option[CinderVolumeSource],
    configMap: Option[ConfigMapVolumeSource],
    csi: Option[CSIVolumeSource],
    downwardAPI: Option[DownwardAPIVolumeSource],
    emptyDir: Option[EmptyDirVolumeSource],
    ephemeral: Option[EphemeralVolumeSource],
    fc: Option[FCVolumeSource],
    flexVolume: Option[FlexVolumeSource],
    flocker: Option[FlockerVolumeSource],
    gcePersistentDisk: Option[GCEPersistentDiskVolumeSource],
    gitRepo: Option[GitRepoVolumeSource],
    glusterfs: Option[GlusterfsVolumeSource],
    hostPath: Option[HostPathVolumeSource],
    iscsi: Option[ISCSIVolumeSource],
    name: String,
    nfs: Option[NFSVolumeSource],
    persistentVolumeClaim: Option[PersistentVolumeClaimVolumeSource],
    photonPersistentDisk: Option[PhotonPersistentDiskVolumeSource],
    portworxVolume: Option[PortworxVolumeSource],
    projected: Option[ProjectedVolumeSource],
    quobyte: Option[QuobyteVolumeSource],
    rbd: Option[RBDVolumeSource],
    scaleIO: Option[ScaleIOVolumeSource],
    secret: Option[SecretVolumeSource],
    storageos: Option[StorageOSVolumeSource],
    vsphereVolume: Option[VsphereVirtualDiskVolumeSource]
  ) derives Decoder
  case class AWSElasticBlockStoreVolumeSource() derives Decoder // not implemented on purpose
  case class AzureDiskVolumeSource() derives Decoder // not implemented on purpose
  case class AzureFileVolumeSource() derives Decoder // not implemented on purpose
  case class CephFSVolumeSource() derives Decoder // not implemented on purpose
  case class CinderVolumeSource() derives Decoder // not implemented on purpose
  case class ConfigMapVolumeSource(
    defaultMode: Option[Int],
    items: List[KeyToPath] = List.empty,
    name: Option[String],
    optional: Option[Boolean]
  ) derives Decoder
  case class CSIVolumeSource() derives Decoder // not implemented on purpose
  case class DownwardAPIVolumeSource() derives Decoder // not implemented on purpose
  case class EmptyDirVolumeSource() derives Decoder // not implemented on purpose
  case class EphemeralVolumeSource() derives Decoder // not implemented on purpose
  case class FCVolumeSource() derives Decoder // not implemented on purpose
  case class FlexVolumeSource() derives Decoder // not implemented on purpose
  case class FlockerVolumeSource() derives Decoder // not implemented on purpose
  case class GCEPersistentDiskVolumeSource() derives Decoder // not implemented on purpose
  case class GitRepoVolumeSource() derives Decoder // not implemented on purpose
  case class GlusterfsVolumeSource() derives Decoder // not implemented on purpose
  case class HostPathVolumeSource() derives Decoder // not implemented on purpose
  case class ISCSIVolumeSource() derives Decoder // not implemented on purpose
  case class NFSVolumeSource() derives Decoder // not implemented on purpose
  case class PersistentVolumeClaimVolumeSource() derives Decoder // not implemented on purpose
  case class PhotonPersistentDiskVolumeSource() derives Decoder // not implemented on purpose
  case class PortworxVolumeSource() derives Decoder // not implemented on purpose
  case class ProjectedVolumeSource() derives Decoder // not implemented on purpose
  case class QuobyteVolumeSource() derives Decoder // not implemented on purpose
  case class RBDVolumeSource() derives Decoder // not implemented on purpose
  case class ScaleIOVolumeSource() derives Decoder // not implemented on purpose
  case class SecretVolumeSource(
    defaultMode: Option[Int],
    items: List[KeyToPath] = List.empty,
    optional: Option[Boolean],
    secretName: Option[String]
  ) derives Decoder
  case class StorageOSVolumeSource() derives Decoder // not implemented on purpose
  case class VsphereVirtualDiskVolumeSource() derives Decoder // not implemented on purpose

  case class KeyToPath(key: String, mode: Option[Int], path: String) derives Decoder

  case class PodCondition(
    lastProbeTime: Option[String],
    lastTransitionTime: Option[String],
    message: Option[String],
    reason: Option[String],
    status: String,
    `type`: String
  ) derives Decoder
  case class ContainerStatus(
    containerID: Option[String],
    image: String,
    imageID: String,
    lastState: Option[ContainerState],
    name: String,
    ready: Boolean,
    restartCount: Int,
    started: Option[Boolean],
    state: Option[ContainerState]
  ) derives Decoder
  case class ContainerState(
    running: Option[ContainerStateRunning],
    terminated: Option[ContainerStateTerminated],
    waiting: Option[ContainerStateWaiting]
  ) derives Decoder
  case class ContainerStateRunning(startedAt: Option[String]) derives Decoder
  case class ContainerStateTerminated(
    containerID: Option[String],
    exitCode: Int,
    finishedAt: Option[String],
    message: Option[String],
    reason: Option[String],
    signal: Option[String],
    startedAt: Option[String]
  ) derives Decoder
  case class ContainerStateWaiting(message: Option[String], reason: Option[String]) derives Decoder
  case class PodIP(ip: Option[String]) derives Decoder

  case class PodSpec(
    activeDeadlineSeconds: Option[Int],
    affinity: Option[Affinity],
    automountServiceAccountToken: Option[Boolean],
    containers: List[Container],
    dnsConfig: Option[PodDNSConfig],
    enableServiceLinks: Option[Boolean],
    ephemeralContainers: List[EphemeralContainer] = List.empty,
    hostAliases: List[HostAlias] = List.empty,
    hostIPC: Option[Boolean],
    hostNetwork: Option[Boolean],
    hostPID: Option[Boolean],
    hostUsers: Option[Boolean],
    hostname: Option[String],
    imagePullSecrets: List[LocalObjectReference] = List.empty,
    initContainers: List[Container] = List.empty,
    nodeName: Option[String],
    nodeSelector: Map[String, String] = Map.empty,
    os: Option[PodOS],
    overhead: Map[String, String] = Map.empty,
    preemptionPolicy: Option[String],
    priority: Option[Int],
    priorityClassName: Option[String],
    readinessGates: List[PodReadinessGate],
    resourceClaims: List[PodResourceClaim],
    restartPolicy: Option[String],
    runtimeClassName: Option[String],
    schedulerName: Option[String],
    schedulingGates: List[PodSchedulingGate] = List.empty,
    securityContext: Option[PodSecurityContext],
    serviceAccount: Option[String],
    serviceAccountName: Option[String],
    setHostnameAsFQDN: Option[Boolean],
    shareProcessNamespace: Option[Boolean],
    subdomain: Option[String],
    terminationGracePeriodSeconds: Option[Int],
    tolerations: List[Toleration] = List.empty,
    topologySpreadConstraints: List[TopologySpreadConstraint] = List.empty,
    volumes: List[Volume] = List.empty
  ) derives Decoder

  case class PodStatus(
    conditions: List[PodCondition] = List.empty,
    containerStatuses: List[ContainerStatus] = List.empty,
    ephemeralContainerStatuses: List[ContainerStatus] = List.empty,
    hostIP: Option[String],
    initContainerStatuses: List[ContainerStatus] = List.empty,
    message: Option[String],
    nominatedNodeName: Option[String],
    phase: Option[String],
    podIP: Option[String],
    podIPs: List[PodIP] = List.empty,
    qosClass: Option[String],
    reason: Option[String],
    startTime: Option[String]
  ) derives Decoder

  case class Pod(
    urn: Output[String],
    id: Output[String],
    apiVersion: Output[Option[String]],
    kind: Output[Option[String]],
    metadata: Output[Option[ObjectMeta]],
    spec: Output[Option[PodSpec]],
    status: Output[Option[PodStatus]]
  ) extends CustomResource
      derives ResourceDecoder

  given ResourceDecoder[Pod] with
    def makeResolver(using Context): Result[(Pod, ResourceResolver[Pod])] =
      Promise[OutputData[String]] zip
        Promise[OutputData[String]] zip
        Promise[OutputData[Option[String]]] zip
        Promise[OutputData[Option[String]]] zip
        Promise[OutputData[Option[ObjectMeta]]] zip
        Promise[OutputData[Option[PodSpec]]] zip
        Promise[OutputData[Option[PodStatus]]] map {
          case (urnPromise, idPromise, apiVersionPromise, kindPromise, metadataPromise, specPromise, statusPromise) =>
            val resource = Pod(
              urn = Output(urnPromise.get),
              id = Output(idPromise.get),
              apiVersion = Output(apiVersionPromise.get),
              kind = Output(kindPromise.get),
              metadata = Output(metadataPromise.get),
              spec = Output(specPromise.get),
              status = Output(statusPromise.get)
            )

            // just a hint for another impl
            def failAllPromises2(err: Throwable): Result[Unit] =
              Result
                .sequence(
                  Vector[Promise[?]](
                    urnPromise,
                    idPromise,
                    apiVersionPromise,
                    kindPromise,
                    metadataPromise,
                    specPromise,
                    statusPromise
                  ).map(_.fail(err))
                )
                .void
                .flatMap(_ => Result.fail(err))

            def failAllPromises(err: Throwable): Result[Unit] =
              for
                _ <- urnPromise.fail(err)
                _ <- idPromise.fail(err)
                _ <- apiVersionPromise.fail(err)
                _ <- kindPromise.fail(err)
                _ <- metadataPromise.fail(err)
                _ <- specPromise.fail(err)
                _ <- statusPromise.fail(err)
                _ <- Result.fail(err) // todo validate that resolve also bubbles up errors in other sdks
              yield ()

            val resolver = new ResourceResolver[Pod]:
              def resolve(errorOrResourceResult: Either[Throwable, RawResourceResult])(using
                ctx: Context
              ): Result[Unit] =
                errorOrResourceResult match
                  case Left(err) => failAllPromises(err)

                  case Right(rawResourceResult) =>
                    val apiVersionDecoder = summon[Decoder[Option[String]]]
                    val kindDecoder       = summon[Decoder[Option[String]]]
                    val metadataDecoder   = summon[Decoder[Option[ObjectMeta]]]
                    // val specDecoder = summon[Decoder[Option[PodSpec]]] // fails to compile
                    val statusDecoder = summon[Decoder[Option[PodStatus]]]

                    val urnOutputData = OutputData(rawResourceResult.urn).withDependency(resource)

                    // TODO what if id is a blank string? does this happen? wtf?
                    val idOutputData = rawResourceResult.id.map(OutputData(_).withDependency(resource)).getOrElse {
                      OutputData.unknown().withDependency(resource)
                    }

                    val fields = rawResourceResult.data.fields

                    try
                      val apiVersionDeps = rawResourceResult.dependencies.get("apiVersion").getOrElse(Set.empty)
                      val apiVersionOutputData = fields
                        .get("apiVersion")
                        .map { value =>
                          apiVersionDecoder.decode(value).map(_.withDependency(resource)) match
                            case Left(err)    => throw err
                            case Right(value) => value
                        }
                        .getOrElse {
                          if ctx.isDryRun then OutputData.unknown().withDependency(resource)
                          else OutputData.empty(Set(resource))
                        }
                        .withDependencies(apiVersionDeps)

                      val kindVersionDeps = rawResourceResult.dependencies.get("kind").getOrElse(Set.empty)
                      val kindOutputData = fields
                        .get("kind")
                        .map { value =>
                          kindDecoder.decode(value).map(_.withDependency(resource)) match
                            case Left(err)    => throw err
                            case Right(value) => value
                        }
                        .getOrElse {
                          if ctx.isDryRun then OutputData.unknown().withDependency(resource)
                          else OutputData.empty(Set(resource))
                        }
                        .withDependencies(kindVersionDeps)

                      val metadataDeps = rawResourceResult.dependencies.get("metadata").getOrElse(Set.empty)
                      val metadataOutputData = fields
                        .get("metadata")
                        .map { value =>
                          metadataDecoder.decode(value).map(_.withDependency(resource)) match
                            case Left(err)    => throw err
                            case Right(value) => value
                        }
                        .getOrElse {
                          if ctx.isDryRun then OutputData.unknown().withDependency(resource)
                          else OutputData.empty(Set(resource))
                        }
                        .withDependencies(metadataDeps)

                      val specDeps = rawResourceResult.dependencies.get("spec").getOrElse(Set.empty)
                      val specOutputData = fields
                        .get("spec")
                        .map { value =>
                          // specDecoder.decode(value).map(_.withDependency(resource)) match
                          //   case Left(err) => throw err
                          //   case Right(value) => value
                          OutputData.unknown().withDependency(resource) // TODO FIX
                        }
                        .getOrElse {
                          if ctx.isDryRun then OutputData.unknown().withDependency(resource)
                          else OutputData.empty(Set(resource))
                        }
                        .withDependencies(specDeps)

                      val statusDeps = rawResourceResult.dependencies.get("status").getOrElse(Set.empty)
                      val statusOutputData = fields
                        .get("status")
                        .map { value =>
                          statusDecoder.decode(value).map(_.withDependency(resource)) match
                            case Left(err)    => throw err
                            case Right(value) => value
                        }
                        .getOrElse {
                          if ctx.isDryRun then OutputData.unknown().withDependency(resource)
                          else OutputData.empty(Set(resource))
                        }
                        .withDependencies(statusDeps)

                      for
                        urn        <- urnPromise.fulfill(urnOutputData)
                        id         <- idPromise.fulfill(idOutputData)
                        apiVersion <- apiVersionPromise.fulfill(apiVersionOutputData)
                        kind       <- kindPromise.fulfill(kindOutputData)
                        metadata   <- metadataPromise.fulfill(metadataOutputData)
                        spec       <- specPromise.fulfill(specOutputData)
                        status     <- statusPromise.fulfill(statusOutputData)
                      yield ()
                    catch case err: DecodingError => failAllPromises(err)

            (resource, resolver)
        }

  def pod(using
    ctx: Context
  )(name: String, args: PodArgs, opts: CustomResourceOptions = CustomResourceOptions()): Output[Pod] =
    // ctx.readOrRegisterResource[Pod](name, typ, args, opts)
    ???

  case class Provider(
    urn: Output[String],
    id: Output[String]
  ) extends ProviderResource
      derives ResourceDecoder

  def provider(using
    ctx: Context
  )(
    name: NonEmptyString,
    args: ProviderArgs,
    options: CustomResourceOptions = CustomResourceOptions()
  ): Output[Provider] =
    summon[ProviderArgsEncoder[ProviderArgs]]
    ctx.registerProvider("pulumi:providers:kubernetes", name, args, options)

  case class ProviderArgs(`type`: Output[String], metadata: Output[ObjectMetaArgs]) derives ProviderArgsEncoder

  case class Deployment(
    urn: Output[NonEmptyString],
    id: Output[NonEmptyString],
    metadata: Output[ObjectMeta]
  ) extends CustomResource
      derives ResourceDecoder

  // def deployment(using
  //   ctx: Context
  // )(
  //   name: String,
  //   args: DeploymentArgs,
  //   opts: ResourceOptions = CustomResourceOptions()
  // ): Output[Deployment] =
  //   // val (deployment, fulfillable) = summon[ResourceDecoder[Deployment]].makeFulfillable
  //   // ctx.readOrRegisterResource[Deployment](name, typ, args, opts)
  //   ???

  case class PodArgs(
    apiVersion: Output[String],
    kind: Output[String],
    metadata: Output[ObjectMetaArgs],
    spec: Output[PodSpecArgs]
  ) derives Encoder

  object PodArgs:
    def apply(
      apiVersion: String | Output[String] | NotProvided = NotProvided,
      kind: String | Output[String] | NotProvided = NotProvided,
      metadata: ObjectMetaArgs | Output[ObjectMetaArgs] | NotProvided = NotProvided,
      spec: PodSpecArgs | Output[PodSpecArgs] | NotProvided = NotProvided
    )(using Context): PodArgs =
      new PodArgs(apiVersion.asOutput(), kind.asOutput(), metadata.asOutput(), spec.asOutput())

  // case class DeploymentArgs(spec: DeploymentSpecArgs) derives Encoder

  // case class LabelSelectorArgs(
  //   matchLabels: Output[Map[String, String]]
  // )
  // object LabelSelectorArgs:
  //   def apply(
  //     matchLabels: Map[String, String] | Map[String, Output[String]] | Output[Map[String, String]] | NotProvided =
  //       NotProvided
  //   )(using Context): LabelSelectorArgs = new LabelSelectorArgs(matchLabels.asOutput()Map)

  // case class DeploymentSpecArgs(
  //   selector: LabelSelectorArgs,
  //   replicas: Output[Int],
  //   template: PodTemplateSpecArgs
  // )
  // object DeploymentSpecArgs:
  //   def apply(
  //     selector: LabelSelectorArgs,
  //     replicas: Int | Output[Int],
  //     template: PodTemplateSpecArgs
  //   )(using ctx: Context): DeploymentSpecArgs =
  //     new DeploymentSpecArgs(selector, replicas.asOutput(), template)

  // case class PodTemplateSpecArgs(metadata: ObjectMetaArgs, spec: PodSpecArgs)

  case class ObjectMetaArgs(
    annotations: Output[Map[String, String]],
    clusterName: Output[String],
    creationTimestamp: Output[String],
    deletionGracePeriodSeconds: Output[Int],
    deletionTimestamp: Output[String],
    finalizers: Output[List[String]],
    generateName: Output[String],
    generation: Output[Int],
    labels: Output[Map[String, String]],
    managedFields: Output[List[ManagedFieldsEntryArgs]],
    name: Output[String],
    namespace: Output[String],
    ownerReferences: Output[List[OwnerReferenceArgs]],
    resourceVersion: Output[String],
    selfLink: Output[String],
    uid: Output[String]
  ) derives Encoder
  object ObjectMetaArgs:
    def apply(
      annotations: Map[String, String] | Map[String, Output[String]] | Output[Map[String, String]] | NotProvided =
        NotProvided,
      clusterName: String | Output[String] | NotProvided = NotProvided,
      creationTimestamp: String | Output[String] | NotProvided = NotProvided,
      deletionGracePeriodSeconds: Int | Output[Int] | NotProvided = NotProvided,
      deletionTimestamp: String | Output[String] | NotProvided = NotProvided,
      finalizers: List[String] | Output[List[String]] | NotProvided = NotProvided,
      generateName: String | Output[String] | NotProvided = NotProvided,
      generation: Int | Output[Int] | NotProvided = NotProvided,
      labels: Map[String, String] | Map[String, Output[String]] | Output[Map[String, String]] | NotProvided =
        NotProvided,
      managedFields: List[ManagedFieldsEntryArgs] | Output[List[ManagedFieldsEntryArgs]] | NotProvided = NotProvided,
      name: String | Output[String] | NotProvided = NotProvided,
      namespace: String | Output[String] | NotProvided = NotProvided,
      ownerReferences: List[OwnerReferenceArgs] | Output[List[OwnerReferenceArgs]] | NotProvided = NotProvided,
      resourceVersion: String | Output[String] | NotProvided = NotProvided,
      selfLink: String | Output[String] | NotProvided = NotProvided,
      uid: String | Output[String] | NotProvided = NotProvided
    )(using ctx: Context): ObjectMetaArgs =
      new ObjectMetaArgs(
        annotations.asOutputMap(),
        clusterName.asOutput(),
        creationTimestamp.asOutput(),
        deletionGracePeriodSeconds.asOutput(),
        deletionTimestamp.asOutput(),
        finalizers.asOutput(),
        generateName.asOutput(),
        generation.asOutput(),
        labels.asOutputMap(),
        managedFields.asOutput(),
        name.asOutput(),
        namespace.asOutput(),
        ownerReferences.asOutput(),
        resourceVersion.asOutput(),
        selfLink.asOutput(),
        uid.asOutput()
      )

  case class ManagedFieldsEntryArgs(
    apiVersion: Output[String],
    fieldsType: Output[String],
    fieldsV1: Output[JsValue],
    manager: Output[String],
    operation: Output[String],
    subresource: Output[String],
    time: Output[String]
  ) derives Encoder
  object ManagedFieldsEntryArgs:
    def apply(
      apiVersion: String | Output[String] | NotProvided = NotProvided,
      fieldsType: String | Output[String] | NotProvided = NotProvided,
      fieldsV1: JsValue | Output[JsValue] | NotProvided = NotProvided,
      manager: String | Output[String] | NotProvided = NotProvided,
      operation: String | Output[String] | NotProvided = NotProvided,
      subresource: String | Output[String] | NotProvided = NotProvided,
      time: String | Output[String] | NotProvided = NotProvided
    )(using Context): ManagedFieldsEntryArgs =
      new ManagedFieldsEntryArgs(
        apiVersion.asOutput(),
        fieldsType.asOutput(),
        fieldsV1.asOutput(),
        manager.asOutput(),
        operation.asOutput(),
        subresource.asOutput(),
        time.asOutput()
      )

  case class OwnerReferenceArgs(
    apiVersion: Output[String],
    blockOwnerDeletion: Output[Boolean],
    controller: Output[Boolean],
    kind: Output[String],
    name: Output[String],
    uid: Output[String]
  ) derives Encoder
  object OwnerReferenceArgs:
    def apply(
      apiVersion: String | Output[String] | NotProvided = NotProvided,
      blockOwnerDeletion: Boolean | Output[Boolean] | NotProvided = NotProvided,
      controller: Boolean | Output[Boolean] | NotProvided = NotProvided,
      kind: String | Output[String] | NotProvided = NotProvided,
      name: String | Output[String] | NotProvided = NotProvided,
      uid: String | Output[String] | NotProvided = NotProvided
    )(using Context): OwnerReferenceArgs =
      new OwnerReferenceArgs(
        apiVersion.asOutput(),
        blockOwnerDeletion.asOutput(),
        controller.asOutput(),
        kind.asOutput(),
        name.asOutput(),
        uid.asOutput()
      )

  case class PodSpecArgs(
    activeDeadlineSeconds: Output[Int],
    affinity: Output[AffinityArgs],
    automountServiceAccountToken: Output[Boolean],
    containers: Output[List[ContainerArgs]],
    dnsConfig: Output[PodDNSConfigArgs],
    dnsPolicy: Output[String],
    enableServiceLinks: Output[Boolean],
    ephemeralContainers: Output[List[EphemeralContainerArgs]],
    hostAliases: Output[List[HostAliasArgs]],
    hostIPC: Output[Boolean],
    hostNetwork: Output[Boolean],
    hostPID: Output[Boolean],
    hostUsers: Output[Boolean],
    hostname: Output[String],
    imagePullSecrets: Output[List[LocalObjectReferenceArgs]],
    initContainers: Output[List[ContainerArgs]],
    nodeName: Output[String],
    nodeSelector: Output[Map[String, String]],
    os: Output[PodOSArgs],
    overhead: Output[Map[String, String]],
    preemptionPolicy: Output[String],
    priority: Output[Int],
    priorityClassName: Output[String],
    readinessGates: Output[List[PodReadinessGateArgs]],
    resourceClaims: Output[List[PodResourceClaimArgs]],
    restartPolicy: Output[String],
    runtimeClassName: Output[String],
    schedulerName: Output[String],
    schedulingGates: Output[List[PodSchedulingGateArgs]],
    securityContext: Output[PodSecurityContextArgs],
    serviceAccount: Output[String],
    serviceAccountName: Output[String],
    setHostnameAsFQDN: Output[Boolean],
    shareProcessNamespace: Output[Boolean],
    subdomain: Output[String],
    terminationGracePeriodSeconds: Output[Int],
    tolerations: Output[List[TolerationArgs]],
    topologySpreadConstraints: Output[List[TopologySpreadConstraintArgs]],
    volumes: Output[List[VolumeArgs]]
  ) derives Encoder
  object PodSpecArgs:
    def apply(
      activeDeadlineSeconds: Int | Output[Int] | NotProvided = NotProvided,
      affinity: AffinityArgs | Output[AffinityArgs] | NotProvided = NotProvided,
      automountServiceAccountToken: Boolean | Output[Boolean] | NotProvided = NotProvided,
      containers: List[ContainerArgs] | Output[List[ContainerArgs]], // NON NULL
      dnsConfig: PodDNSConfigArgs | Output[PodDNSConfigArgs] | NotProvided = NotProvided,
      dnsPolicy: String | Output[String] | NotProvided = NotProvided,
      enableServiceLinks: Boolean | Output[Boolean] | NotProvided = NotProvided,
      ephemeralContainers: List[EphemeralContainerArgs] | Output[List[EphemeralContainerArgs]] | NotProvided =
        NotProvided,
      hostAliases: List[HostAliasArgs] | Output[List[HostAliasArgs]] | NotProvided = NotProvided,
      hostIPC: Boolean | Output[Boolean] | NotProvided = NotProvided,
      hostNetwork: Boolean | Output[Boolean] | NotProvided = NotProvided,
      hostPID: Boolean | Output[Boolean] | NotProvided = NotProvided,
      hostUsers: Boolean | Output[Boolean] | NotProvided = NotProvided,
      hostname: String | Output[String] | NotProvided = NotProvided,
      imagePullSecrets: List[LocalObjectReferenceArgs] | Output[List[LocalObjectReferenceArgs]] | NotProvided =
        NotProvided,
      initContainers: List[ContainerArgs] | Output[List[ContainerArgs]] | NotProvided = NotProvided,
      nodeName: String | Output[String] | NotProvided = NotProvided,
      nodeSelector: Map[String, String] | Map[String, Output[String]] | Output[Map[String, String]] | NotProvided =
        NotProvided,
      os: PodOSArgs | Output[PodOSArgs] | NotProvided = NotProvided,
      overhead: Map[String, String] | Map[String, Output[String]] | Output[Map[String, String]] | NotProvided =
        NotProvided,
      preemptionPolicy: String | Output[String] | NotProvided = NotProvided,
      priority: Int | Output[Int] | NotProvided = NotProvided,
      priorityClassName: String | Output[String] | NotProvided = NotProvided,
      readinessGates: List[PodReadinessGateArgs] | Output[List[PodReadinessGateArgs]] | NotProvided = NotProvided,
      resourceClaims: List[PodResourceClaimArgs] | Output[List[PodResourceClaimArgs]] | NotProvided = NotProvided,
      restartPolicy: String | Output[String] | NotProvided = NotProvided,
      runtimeClassName: String | Output[String] | NotProvided = NotProvided,
      schedulerName: String | Output[String] | NotProvided = NotProvided,
      schedulingGates: List[PodSchedulingGateArgs] | Output[List[PodSchedulingGateArgs]] | NotProvided = NotProvided,
      securityContext: PodSecurityContextArgs | Output[PodSecurityContextArgs] | NotProvided = NotProvided,
      serviceAccount: String | Output[String] | NotProvided = NotProvided,
      serviceAccountName: String | Output[String] | NotProvided = NotProvided,
      setHostnameAsFQDN: Boolean | Output[Boolean] | NotProvided = NotProvided,
      shareProcessNamespace: Boolean | Output[Boolean] | NotProvided = NotProvided,
      subdomain: String | Output[String] | NotProvided = NotProvided,
      terminationGracePeriodSeconds: Int | Output[Int] | NotProvided = NotProvided,
      tolerations: List[TolerationArgs] | Output[List[TolerationArgs]] | NotProvided = NotProvided,
      topologySpreadConstraints: List[TopologySpreadConstraintArgs] | Output[List[TopologySpreadConstraintArgs]] |
        NotProvided = NotProvided,
      volumes: List[VolumeArgs] | Output[List[VolumeArgs]] | NotProvided = NotProvided
    )(using ctx: Context): PodSpecArgs =
      new PodSpecArgs(
        activeDeadlineSeconds = activeDeadlineSeconds.asOutput(),
        affinity = affinity.asOutput(),
        automountServiceAccountToken = automountServiceAccountToken.asOutput(),
        containers = containers.asOutput(),
        dnsConfig = dnsConfig.asOutput(),
        dnsPolicy = dnsPolicy.asOutput(),
        enableServiceLinks = enableServiceLinks.asOutput(),
        ephemeralContainers = ephemeralContainers.asOutput(),
        hostAliases = hostAliases.asOutput(),
        hostIPC = hostIPC.asOutput(),
        hostNetwork = hostNetwork.asOutput(),
        hostPID = hostPID.asOutput(),
        hostUsers = hostUsers.asOutput(),
        hostname = hostname.asOutput(),
        imagePullSecrets = imagePullSecrets.asOutput(),
        initContainers = initContainers.asOutput(),
        nodeName = nodeName.asOutput(),
        nodeSelector = nodeSelector.asOutputMap(),
        os = os.asOutput(),
        overhead = overhead.asOutputMap(),
        preemptionPolicy = preemptionPolicy.asOutput(),
        priority = priority.asOutput(),
        priorityClassName = priorityClassName.asOutput(),
        readinessGates = readinessGates.asOutput(),
        resourceClaims = resourceClaims.asOutput(),
        restartPolicy = restartPolicy.asOutput(),
        runtimeClassName = runtimeClassName.asOutput(),
        schedulerName = schedulerName.asOutput(),
        schedulingGates = schedulingGates.asOutput(),
        securityContext = securityContext.asOutput(),
        serviceAccount = serviceAccount.asOutput(),
        serviceAccountName = serviceAccountName.asOutput(),
        setHostnameAsFQDN = setHostnameAsFQDN.asOutput(),
        shareProcessNamespace = shareProcessNamespace.asOutput(),
        subdomain = subdomain.asOutput(),
        terminationGracePeriodSeconds = terminationGracePeriodSeconds.asOutput(),
        tolerations = tolerations.asOutput(),
        topologySpreadConstraints = topologySpreadConstraints.asOutput(),
        volumes = volumes.asOutput()
      )

  case class AffinityArgs(
    nodeAffinity: Output[NodeAffinityArgs],
    podAffinity: Output[PodAffinityArgs],
    podAntiAffinity: Output[PodAntiAffinityArgs]
  ) derives Encoder
  object AffinityArgs:
    def apply(
      nodeAffinity: NodeAffinityArgs | Output[NodeAffinityArgs] | NotProvided = NotProvided,
      podAffinity: PodAffinityArgs | Output[PodAffinityArgs] | NotProvided = NotProvided,
      podAntiAffinity: PodAntiAffinityArgs | Output[PodAntiAffinityArgs] | NotProvided = NotProvided
    )(using Context): AffinityArgs =
      new AffinityArgs(nodeAffinity.asOutput(), podAffinity.asOutput(), podAntiAffinity.asOutput())

  case class NodeAffinityArgs(
    preferredDuringSchedulingIgnoredDuringExecution: Output[List[PreferredSchedulingTermArgs]],
    requiredDuringSchedulingIgnoredDuringExecution: Output[NodeSelectorArgs]
  ) derives Encoder
  object NodeAffinityArgs:
    def apply(
      preferredDuringSchedulingIgnoredDuringExecution: List[PreferredSchedulingTermArgs] |
        Output[List[PreferredSchedulingTermArgs]] | NotProvided = NotProvided,
      requiredDuringSchedulingIgnoredDuringExecution: NodeSelectorArgs | Output[NodeSelectorArgs] | NotProvided =
        NotProvided
    )(using Context): NodeAffinityArgs = new NodeAffinityArgs(
      preferredDuringSchedulingIgnoredDuringExecution.asOutput(),
      requiredDuringSchedulingIgnoredDuringExecution.asOutput()
    )

  case class PodAffinityArgs(
    preferredDuringSchedulingIgnoredDuringExecution: Output[List[WeightedPodAffinityTermArgs]],
    requiredDuringSchedulingIgnoredDuringExecution: Output[List[PodAffinityTermArgs]]
  ) derives Encoder
  object PodAffinityArgs:
    def apply(
      preferredDuringSchedulingIgnoredDuringExecution: List[WeightedPodAffinityTermArgs] |
        Output[List[WeightedPodAffinityTermArgs]] | NotProvided = NotProvided,
      requiredDuringSchedulingIgnoredDuringExecution: List[PodAffinityTermArgs] | Output[List[PodAffinityTermArgs]] |
        NotProvided = NotProvided
    )(using Context): PodAffinityArgs =
      new PodAffinityArgs(
        preferredDuringSchedulingIgnoredDuringExecution = preferredDuringSchedulingIgnoredDuringExecution.asOutput(),
        requiredDuringSchedulingIgnoredDuringExecution = requiredDuringSchedulingIgnoredDuringExecution.asOutput()
      )

  case class PodAntiAffinityArgs() derives Encoder
  object PodAntiAffinityArgs:
    def apply()(using Context): PodAntiAffinityArgs = new PodAntiAffinityArgs()

  case class WeightedPodAffinityTermArgs() derives Encoder
  case class PodAffinityTermArgs() derives Encoder

  case class PreferredSchedulingTermArgs() derives Encoder

  case class NodeSelectorArgs() derives Encoder

  case class ContainerArgs(
    args: Output[List[String]],
    command: Output[List[String]],
    env: Output[List[EnvVarArgs]],
    envFrom: Output[List[EnvFromSourceArgs]],
    image: Output[NonEmptyString],
    imagePullPolicy: Output[String],
    lifecycle: Output[LifecycleArgs],
    livenessProbe: Output[ProbeArgs],
    name: Output[NonEmptyString],
    ports: Output[ContainerPortArgs],
    readinessProbe: Output[ProbeArgs],
    resources: Output[ResourceRequirementsArgs],
    securityContext: Output[SecurityContextArgs],
    startupProbe: Output[ProbeArgs],
    stdin: Output[Boolean],
    stdinOnce: Output[Boolean],
    terminationMessagePath: Output[String],
    terminationMessagePolicy: Output[String],
    tty: Output[Boolean],
    volumeDevices: Output[List[VolumeDeviceArgs]],
    volumeMounts: Output[List[VolumeMountArgs]],
    workingDir: Output[String]
  ) derives Encoder
  object ContainerArgs:
    def apply(
      args: List[String] | Output[List[String]] | NotProvided = NotProvided,
      command: List[String] | Output[List[String]] | NotProvided = NotProvided,
      env: List[EnvVarArgs] | Output[List[EnvVarArgs]] | NotProvided = NotProvided,
      envFrom: List[EnvFromSourceArgs] | Output[List[EnvFromSourceArgs]] | NotProvided = NotProvided,
      image: NonEmptyString | Output[NonEmptyString] | NotProvided = NotProvided,
      imagePullPolicy: String | Output[String] | NotProvided = NotProvided,
      lifecycle: LifecycleArgs | Output[LifecycleArgs] | NotProvided = NotProvided,
      livenessProbe: ProbeArgs | Output[ProbeArgs] | NotProvided = NotProvided,
      name: NonEmptyString | Output[NonEmptyString],
      ports: ContainerPortArgs | Output[ContainerPortArgs] | NotProvided = NotProvided,
      readinessProbe: ProbeArgs | Output[ProbeArgs] | NotProvided = NotProvided,
      resources: ResourceRequirementsArgs | Output[ResourceRequirementsArgs] | NotProvided = NotProvided,
      securityContext: SecurityContextArgs | Output[SecurityContextArgs] | NotProvided = NotProvided,
      startupProbe: ProbeArgs | Output[ProbeArgs] | NotProvided = NotProvided,
      stdin: Boolean | Output[Boolean] | NotProvided = NotProvided,
      stdinOnce: Boolean | Output[Boolean] | NotProvided = NotProvided,
      terminationMessagePath: String | Output[String] | NotProvided = NotProvided,
      terminationMessagePolicy: String | Output[String] | NotProvided = NotProvided,
      tty: Boolean | Output[Boolean] | NotProvided = NotProvided,
      volumeDevices: List[VolumeDeviceArgs] | Output[List[VolumeDeviceArgs]] | NotProvided = NotProvided,
      volumeMounts: List[VolumeMountArgs] | Output[List[VolumeMountArgs]] | NotProvided = NotProvided,
      workingDir: String | Output[String] | NotProvided = NotProvided
    )(using ctx: Context): ContainerArgs = new ContainerArgs(
      args = args.asOutput(),
      command = command.asOutput(),
      env = env.asOutput(),
      envFrom = envFrom.asOutput(),
      image = image.asOutput(),
      imagePullPolicy = imagePullPolicy.asOutput(),
      lifecycle = lifecycle.asOutput(),
      livenessProbe = livenessProbe.asOutput(),
      name = name.asOutput(),
      ports = ports.asOutput(),
      readinessProbe = readinessProbe.asOutput(),
      resources = resources.asOutput(),
      securityContext = securityContext.asOutput(),
      startupProbe = startupProbe.asOutput(),
      stdin = stdin.asOutput(),
      stdinOnce = stdinOnce.asOutput(),
      terminationMessagePath = terminationMessagePath.asOutput(),
      terminationMessagePolicy = terminationMessagePolicy.asOutput(),
      tty = tty.asOutput(),
      volumeDevices = volumeDevices.asOutput(),
      volumeMounts = volumeMounts.asOutput(),
      workingDir = workingDir.asOutput()
    )

  case class LifecycleArgs() derives Encoder
  object LifecycleArgs:
    def apply()(using Context): LifecycleArgs = new LifecycleArgs()

  case class ResourceRequirementsArgs() derives Encoder
  object ResourceRequirementsArgs:
    def apply()(using Context): ResourceRequirementsArgs = new ResourceRequirementsArgs()

  case class ProbeArgs() derives Encoder
  object ProbeArgs:
    def apply()(using Context): ProbeArgs = new ProbeArgs()

  case class SecurityContextArgs() derives Encoder
  object SecurityContextArgs:
    def apply()(using Context): SecurityContextArgs = new SecurityContextArgs()

  case class VolumeDeviceArgs() derives Encoder
  object VolumeDeviceArgs:
    def apply()(using Context): VolumeDeviceArgs = new VolumeDeviceArgs()

  case class VolumeMountArgs() derives Encoder
  object VolumeMountArgs:
    def apply()(using Context): VolumeMountArgs = new VolumeMountArgs()

  case class EnvVarArgs() derives Encoder
  object EnvVarArgs:
    def apply()(using Context): EnvVarArgs = new EnvVarArgs()

  case class EnvFromSourceArgs() derives Encoder
  object EnvFromSourceArgs:
    def apply()(using Context): EnvFromSourceArgs = new EnvFromSourceArgs()

  case class ContainerPortArgs(containerPort: Output[Int]) derives Encoder
  object ContainerPortArgs:
    def apply(containerPort: Int | Output[Int] | NotProvided = NotProvided)(using ctx: Context): ContainerPortArgs =
      new ContainerPortArgs(containerPort.asOutput())

  case class PodDNSConfigArgs() derives Encoder
  object PodDNSConfigArgs:
    def apply()(using Context): PodDNSConfigArgs = ???

  case class LocalObjectReferenceArgs() derives Encoder
  object LocalObjectReferenceArgs:
    def apply()(using Context): LocalObjectReferenceArgs = ???

  case class EphemeralContainerArgs() derives Encoder
  object EphemeralContainerArgs:
    def apply()(using Context): EphemeralContainerArgs = ???

  case class HostAliasArgs() derives Encoder
  object HostAliasArgs:
    def apply()(using Context): HostAliasArgs = ???

  case class PodOSArgs() derives Encoder
  object PodOSArgs:
    def apply()(using Context): PodOSArgs = ???

  case class PodReadinessGateArgs() derives Encoder
  object PodReadinessGateArgs:
    def apply()(using Context): PodReadinessGateArgs = ???

  case class PodResourceClaimArgs() derives Encoder
  object PodResourceClaimArgs:
    def apply()(using Context): PodResourceClaimArgs = ???

  case class PodSchedulingGateArgs() derives Encoder
  object PodSchedulingGateArgs:
    def apply()(using Context): PodSchedulingGateArgs = ???

  case class PodSecurityContextArgs() derives Encoder
  object PodSecurityContextArgs:
    def apply()(using Context): PodSecurityContextArgs = ???

  case class TolerationArgs() derives Encoder
  object TolerationArgs:
    def apply()(using Context): TolerationArgs = ???

  case class TopologySpreadConstraintArgs() derives Encoder
  object TopologySpreadConstraintArgs:
    def apply()(using Context): TopologySpreadConstraintArgs = ???

  case class VolumeArgs() derives Encoder
  object VolumeArgs:
    def apply()(using Context): VolumeArgs = ???
