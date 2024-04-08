import besom.*
import besom.api.{kubernetes => k8s}

import k8s.core.v1.inputs.*
import k8s.apps.v1.inputs.*
import k8s.meta.v1.inputs.*
import k8s.apps.v1.{Deployment, DeploymentArgs, StatefulSet, StatefulSetArgs}
import k8s.core.v1.{ConfigMap, ConfigMapArgs, Namespace, Service, ServiceArgs, PersistentVolume, PersistentVolumeArgs}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.*

case class Redis private (connectionString: Output[String])(using ComponentBase) extends ComponentResource derives RegistersOutputs
object Redis:
  extension (r: Output[Redis]) def connectionString: Output[String] = r.flatMap(_.connectionString)

  def apply(using Context)(
    name: NonEmptyString,
    nodes: Input[Int :| Positive],
    options: ComponentResourceOptions = ComponentResourceOptions()
  ): Output[Redis] =
    component(name, "besom:liftoff:Redis", options) {
      val redisClusterPrefix = "redis-cluster"
      val redisNamespace     = Namespace(s"$redisClusterPrefix-namespace-$name")
      val labels             = Map("app" -> name)

      val persistentVolumeLabels = Map("type" -> "local") ++ labels
      val persistentVolumeTemplate = PersistentVolumeSpecArgs(
        storageClassName = "manual",
        capacity = Map("storage" -> "128Mi"),
        accessModes = List("ReadWriteOnce"),
        persistentVolumeReclaimPolicy = "Delete" // never do this in production
      )

      def createHostPathVolume(num: Int): Output[PersistentVolume] =
        PersistentVolume(
          s"$redisClusterPrefix-pv-$num-$name",
          PersistentVolumeArgs(
            metadata = ObjectMetaArgs(
              name = s"$redisClusterPrefix-pv-$num-$name",
              namespace = redisNamespace.metadata.name,
              labels = persistentVolumeLabels
            ),
            spec = PersistentVolumeSpecArgs(
              storageClassName = persistentVolumeTemplate.storageClassName,
              capacity = persistentVolumeTemplate.capacity,
              accessModes = persistentVolumeTemplate.accessModes,
              persistentVolumeReclaimPolicy = persistentVolumeTemplate.persistentVolumeReclaimPolicy,
              hostPath = HostPathVolumeSourceArgs(path = s"/tmp/redis/data-$num")
            )
          )
        )

      val nodeNumbers: Output[List[Int]] = nodes.asOutput().map(max => (1 to max).toList)
      val persistentVolumes              = nodeNumbers.map(_.map(createHostPathVolume)).flatMap(Output.sequence(_))

      val redisPersistentVolumeClaimName = "data"
      val redisClusterPortName           = "client"
      val redisClusterPortNumber         = 6379

      val redisConfigMap = ConfigMap(
        s"$redisClusterPrefix-configmap-$name",
        ConfigMapArgs(
          metadata = ObjectMetaArgs(
            namespace = redisNamespace.metadata.name,
            labels = labels,
            name = s"$redisClusterPrefix-configmap-$name"
          ),
          data = Map(
            "redis.conf" ->
              s"""cluster-enabled yes
                 |cluster-require-full-coverage no
                 |cluster-node-timeout 15000
                 |cluster-config-file /data/nodes.conf
                 |cluster-migration-barrier 1
                 |appendonly yes
                 |protected-mode no
                 |bind 0.0.0.0
                 |port $redisClusterPortNumber""".stripMargin
          )
        )
      )

      val redisStatefulSet = StatefulSet(
        s"$redisClusterPrefix-statefulset-$name",
        StatefulSetArgs(
          metadata = ObjectMetaArgs(
            name = s"$redisClusterPrefix-statefulset-$name",
            namespace = redisNamespace.metadata.name,
            labels = labels
          ),
          spec = StatefulSetSpecArgs(
            serviceName = s"$redisClusterPrefix-statefulset-$name",
            replicas = nodes,
            selector = LabelSelectorArgs(
              matchLabels = labels
            ),
            template = PodTemplateSpecArgs(
              metadata = ObjectMetaArgs(
                name = s"$redisClusterPrefix-statefulset-$name",
                namespace = redisNamespace.metadata.name,
                labels = labels
              ),
              spec = PodSpecArgs(
                containers = ContainerArgs(
                  name = "redis",
                  image = "redis:6.2.5",
                  command = List("redis-server"),
                  args = List("/conf/redis.conf"),
                  env = List(
                    EnvVarArgs(
                      name = "REDIS_CLUSTER_ANNOUNCE_IP",
                      valueFrom = EnvVarSourceArgs(
                        fieldRef = ObjectFieldSelectorArgs(
                          fieldPath = "status.podIP"
                        )
                      )
                    )
                  ),
                  ports = List(
                    ContainerPortArgs(name = redisClusterPortName, containerPort = redisClusterPortNumber),
                    ContainerPortArgs(name = "gossip", containerPort = 16379)
                  ),
                  volumeMounts = List(
                    VolumeMountArgs(
                      name = "conf",
                      mountPath = "/conf"
                    ),
                    VolumeMountArgs(
                      name = "data",
                      mountPath = "/data"
                    )
                  )
                ) :: Nil,
                volumes = List(
                  VolumeArgs(
                    name = "conf",
                    configMap = ConfigMapVolumeSourceArgs(
                      name = redisConfigMap.metadata.name
                    )
                  ),
                  VolumeArgs(
                    name = "data",
                    persistentVolumeClaim = PersistentVolumeClaimVolumeSourceArgs(
                      claimName = redisPersistentVolumeClaimName
                    )
                  )
                )
              )
            ),
            volumeClaimTemplates = List(
              PersistentVolumeClaimArgs(
                metadata = ObjectMetaArgs(
                  name = redisPersistentVolumeClaimName,
                  labels = labels,
                  namespace = redisNamespace.metadata.name
                ),
                // propagate the PV settings to the PVC
                spec = PersistentVolumeClaimSpecArgs(
                  /*selector = LabelSelectorArgs(
                    matchLabels = persistentVolumeLabels
                  ),*/
                  accessModes = persistentVolumeTemplate.accessModes,
                  storageClassName = persistentVolumeTemplate.storageClassName,
                  resources = VolumeResourceRequirementsArgs(
                    requests = persistentVolumeTemplate.capacity // request max capacity available on the volume
                  )
                )
              )
            ),
            persistentVolumeClaimRetentionPolicy = k8s.apps.v1.inputs.StatefulSetPersistentVolumeClaimRetentionPolicyArgs(
              whenDeleted = persistentVolumeTemplate.persistentVolumeReclaimPolicy, // propagate the retention policy to the PVs
              whenScaled = "Retain"
            )
          )
        ),
        opts(dependsOn = persistentVolumes)
      )

      val redisServiceName: NonEmptyString =
        s"$redisClusterPrefix-service-$name" // FIXME: explicit type needed for the compiler to infer the type of the constant
      val redisService = Service(
        redisServiceName,
        ServiceArgs(
          metadata = ObjectMetaArgs(
            name = redisServiceName,
            labels = labels,
            namespace = redisNamespace.metadata.name
          ),
          spec = ServiceSpecArgs(
            ports = List(
              ServicePortArgs(
                name = redisClusterPortName,
                port = redisClusterPortNumber,
                targetPort = redisClusterPortNumber
              )
            ),
            selector = labels
          )
        )
      )

      val url = for
        _ <- redisNamespace
        _ <- redisConfigMap
        _ <- redisStatefulSet
        _ <- redisService
      yield s"redis://$redisServiceName:$redisClusterPortNumber"

      Redis(url)
    }
end Redis

@main def main(): Unit = Pulumi.run {
  val labels                                      = Map("app" -> "nginx")
  val appNamespace: Output[k8s.core.v1.Namespace] = Namespace("liftoff")

  val html =
    "<h1>Welcome to Besom: Functional Infrastructure in Scala 3</h1>"

  val indexHtmlConfigMap: Output[k8s.core.v1.ConfigMap] = ConfigMap(
    "index-html-configmap",
    ConfigMapArgs(
      metadata = ObjectMetaArgs(
        name = "index-html-configmap",
        labels = labels,
        namespace = appNamespace.metadata.name
      ),
      data = Map(
        "index.html" -> html
      )
    )
  )

  val nginxPortNumber = 80
  val nginxDeployment = Deployment(
    "nginx",
    DeploymentArgs(
      spec = DeploymentSpecArgs(
        selector = LabelSelectorArgs(matchLabels = labels),
        replicas = 1,
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(
            name = "nginx-deployment",
            labels = labels,
            namespace = appNamespace.metadata.name
          ),
          spec = PodSpecArgs(
            containers = ContainerArgs(
              name = "nginx",
              image = "nginx",
              ports = List(
                ContainerPortArgs(name = "http", containerPort = nginxPortNumber)
              ),
              volumeMounts = List(
                VolumeMountArgs(
                  name = "index-html",
                  mountPath = "/usr/share/nginx/html/index.html",
                  subPath = "index.html"
                )
              )
            ) :: Nil,
            volumes = List(
              VolumeArgs(
                name = "index-html",
                configMap = ConfigMapVolumeSourceArgs(
                  name = indexHtmlConfigMap.metadata.name
                )
              )
            )
          )
        )
      ),
      metadata = ObjectMetaArgs(
        namespace = appNamespace.metadata.name
      )
    )
  )

  val nginxService = Service(
    "nginx",
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        `type` = k8s.core.v1.enums.ServiceSpecType.LoadBalancer,
        ports = List(
          ServicePortArgs(name = "http", port = nginxPortNumber, targetPort = nginxPortNumber)
        )
      ),
      metadata = ObjectMetaArgs(
        namespace = appNamespace.metadata.name,
        labels = labels
      )
    ),
    opts(dependsOn = nginxDeployment)
  )

  val redis: Output[Redis] = Redis("cache", 3)

  extension [A](output: Output[Option[A]])
    def orElse[B >: A](alternative: Output[Option[B]]): Output[Option[B]] =
      output.flatMap(o => alternative.map(a => o.orElse(a)))

  extension [A](output: Output[Option[List[A]]]) def headOption: Output[Option[A]] = output.map(_.flatMap(_.headOption))

  val maybeIngress                         = nginxService.status.loadBalancer.ingress.headOption
  val hostnameOrIp: Output[Option[String]] = maybeIngress.hostname.orElse(maybeIngress.ip)

  Stack.exports(
    namespace = appNamespace.metadata.name,
    nginxDeploymentName = nginxDeployment.metadata.name,
    serviceName = nginxService.metadata.name,
    serviceClusterIp = nginxService.spec.clusterIP,
    redisConnString = redis.connectionString,
    nginxUrl = p"http://${hostnameOrIp.getOrElse("unknown")}:$nginxPortNumber"
  )
}
