import besom.*
import besom.api.{kubernetes => k8s}

import k8s.core.v1.inputs.*
import k8s.apps.v1.inputs.*
import k8s.meta.v1.inputs.*
import k8s.apps.v1.{deployment, DeploymentArgs, statefulSet, StatefulSetArgs}
import k8s.core.v1.{
  configMap,
  ConfigMapArgs,
  namespace,
  service,
  ServiceArgs,
  persistentVolume,
  PersistentVolume,
  PersistentVolumeArgs
}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.*

case class Redis(connectionString: Output[String])(using ComponentBase) extends ComponentResource
    derives RegistersOutputs

def redisCluster(name: NonEmptyString, nodes: Int :| Positive)(using Context): Output[Redis] =
  component(name, "besom:liftoff:Redis") {
    val redisNamespace = namespace(s"redis-cluster-namespace-$name")

    val labels = Map("app" -> name)

    def createHostPathVolume(num: Int): Output[PersistentVolume] =
      persistentVolume(
        s"redis-cluster-pv-$num-$name",
        PersistentVolumeArgs(
          metadata = ObjectMetaArgs(
            namespace = redisNamespace.metadata.name,
            labels = Map("type" -> "local") ++ labels,
            name = s"redis-cluster-pv-$num-$name"
          ),
          spec = PersistentVolumeSpecArgs(
            storageClassName = "manual",
            capacity = Map("storage" -> "128Mi"),
            accessModes = List("ReadWriteOnce"),
            hostPath = HostPathVolumeSourceArgs(path = s"/tmp/redis/data-$num")
          )
        )
      )

    val redisConfigMap = configMap(
      s"redis-cluster-configmap-$name",
      ConfigMapArgs(
        metadata = ObjectMetaArgs(
          namespace = redisNamespace.metadata.name,
          labels = labels,
          name = s"redis-cluster-configmap-$name"
        ),
        data = Map("redis.conf" -> s"""cluster-enabled yes
                            |cluster-require-full-coverage no
                            |cluster-node-timeout 15000
                            |cluster-config-file /data/nodes.conf
                            |cluster-migration-barrier 1
                            |appendonly yes
                            |protected-mode no
                            |bind 0.0.0.0
                            |port 6379""".stripMargin)
      )
    )

    val redisStatefulSet = statefulSet(
      s"redis-cluster-statefulset-$name",
      StatefulSetArgs(
        metadata = ObjectMetaArgs(
          name = s"redis-cluster-statefulset-$name",
          namespace = redisNamespace.metadata.name,
          labels = labels
        ),
        spec = StatefulSetSpecArgs(
          serviceName = s"redis-cluster-statefulset-$name",
          replicas = nodes,
          selector = LabelSelectorArgs(
            matchLabels = labels
          ),
          template = PodTemplateSpecArgs(
            metadata = ObjectMetaArgs(
              name = s"redis-cluster-statefulset-$name",
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
                  ContainerPortArgs(name = "client", containerPort = 6379),
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
                    claimName = "data"
                  )
                )
              )
            )
          ),
          volumeClaimTemplates = List(
            PersistentVolumeClaimArgs(
              metadata = ObjectMetaArgs(
                name = "data",
                labels = labels,
                namespace = redisNamespace.metadata.name
              ),
              spec = PersistentVolumeClaimSpecArgs(
                accessModes = List("ReadWriteOnce"),
                storageClassName = "manual",
                resources = ResourceRequirementsArgs(
                  requests = Map("storage" -> "128Mi")
                )
              )
            )
          )
        )
      )
    )

    val redisService = service(
      s"redis-cluster-service-$name",
      ServiceArgs(
        metadata = ObjectMetaArgs(
          name = s"redis-cluster-service-$name",
          labels = labels,
          namespace = redisNamespace.metadata.name
        ),
        spec = ServiceSpecArgs(
          ports = List(
            ServicePortArgs(
              name = "client",
              port = 6379,
              targetPort = 6379
            )
          ),
          selector = labels
        )
      )
    )

    for
      _ <- redisNamespace
      _ <- Output.sequence((1 to nodes).map(createHostPathVolume))
      _ <- redisConfigMap
      _ <- redisStatefulSet
      _ <- redisService
    yield Redis(Output(s"redis://redis-cluster-service-$name:6379"))
  }

@main def main = Pulumi.run {
  val labels                                      = Map("app" -> "nginx")
  val appNamespace: Output[k8s.core.v1.Namespace] = namespace("liftoff")

  val html =
    "<h1>Welcome to Besom: Functional Infrastructure in Scala 3</h1>"

  val indexHtmlConfigMap: Output[k8s.core.v1.ConfigMap] = configMap(
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

  val nginxDeployment = deployment(
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
                ContainerPortArgs(name = "http", containerPort = 80)
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

  val nginxService = service(
    "nginx",
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        ports = List(
          ServicePortArgs(name = "http", port = 1337)
        )
      ),
      metadata = ObjectMetaArgs(
        namespace = appNamespace.metadata.name,
        labels = labels
      )
    )
  )

  for
    nginx   <- nginxDeployment
    service <- nginxService
    redis   <- redisCluster("cache", 3)
  yield Pulumi.exports(
    namespace = appNamespace.metadata.name,
    nginxDeploymentName = nginx.metadata.name,
    serviceName = nginxService.metadata.name,
    serviceClusterIp = nginxService.spec.clusterIP,
    redisConnString = redis.connectionString
  )
}
