import besom.*
import besom.api.kubernetes
import besom.api.kubernetes.apps.v1.inputs.DeploymentSpecArgs
import besom.api.kubernetes.apps.v1.*
import besom.api.kubernetes.core
import besom.api.kubernetes.core.v1.*
import besom.api.kubernetes.core.v1.enums.*
import besom.api.kubernetes.core.v1.inputs.{
  ContainerArgs,
  ContainerPortArgs,
  EnvVarArgs,
  PodSpecArgs,
  PodTemplateSpecArgs,
  ResourceRequirementsArgs,
  ServicePortArgs,
  ServiceSpecArgs,
  VolumeArgs,
  VolumeMountArgs
}
import besom.api.kubernetes.meta.v1.*
import besom.api.kubernetes.meta.v1.inputs.*

case class RedisEndpoint(url: Output[String], fqdn: Output[String]) derives Encoder
object RedisEndpoint:
  extension (r: Output[RedisEndpoint])
    def url: Output[String]  = r.flatMap(_.url)
    def fqdn: Output[String] = r.flatMap(_.fqdn)
case class Redis private (
  namespace: Output[String],
  leader: Output[RedisEndpoint],
  replica: Output[RedisEndpoint]
)(using ComponentBase)
    extends ComponentResource
    derives RegistersOutputs
object Redis:
  extension (r: Output[Redis])
    def namespace: Output[String]      = r.flatMap(_.namespace)
    def leader: Output[RedisEndpoint]  = r.flatMap(_.leader)
    def replica: Output[RedisEndpoint] = r.flatMap(_.replica)

  def apply(using Context)(
    name: NonEmptyString,
    replicas: Input[Int] = 1,
    options: ComponentResourceOptions = ComponentResourceOptions()
  ): Output[Redis] =
    component(name, "besom:example:Redis", options) {
      val namespace                  = Namespace(s"redis-$name")
      val leaderName: NonEmptyString = "redis-leader"
      val leaderLabels               = Map("app" -> leaderName)

      val redisPortName   = "client"
      val redisPortNumber = 6379

      val leader = Deployment(
        leaderName,
        DeploymentArgs(
          metadata = ObjectMetaArgs(
            name = leaderName,
            namespace = namespace.metadata.name,
            labels = leaderLabels
          ),
          spec = DeploymentSpecArgs(
            selector = LabelSelectorArgs(
              matchLabels = leaderLabels
            ),
            template = PodTemplateSpecArgs(
              metadata = ObjectMetaArgs(
                name = leaderName,
                namespace = namespace.metadata.name,
                labels = leaderLabels
              ),
              spec = PodSpecArgs(
                containers = List(
                  ContainerArgs(
                    name = "redis",
                    image = "redis",
                    ports = List(
                      ContainerPortArgs(name = redisPortName, containerPort = redisPortNumber)
                    ),
                    resources = ResourceRequirementsArgs(
                      requests = Map(
                        "cpu" -> "100m",
                        "memory" -> "100Mi"
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )

      val leaderService = Service(
        leaderName,
        ServiceArgs(
          metadata = ObjectMetaArgs(
            name = leaderName,
            labels = leaderLabels,
            namespace = namespace.metadata.name
          ),
          spec = ServiceSpecArgs(
            ports = List(
              ServicePortArgs(
                name = redisPortName,
                port = redisPortNumber,
                targetPort = redisPortNumber
              )
            ),
            selector = leader.spec.template.metadata.labels
          )
        )
      )

      val replicaName: NonEmptyString = "redis-replica"
      val replicaLabels               = Map("app" -> replicaName)

      val replica = Deployment(
        replicaName,
        DeploymentArgs(
          metadata = ObjectMetaArgs(
            name = replicaName,
            namespace = namespace.metadata.name,
            labels = replicaLabels
          ),
          spec = DeploymentSpecArgs(
            selector = LabelSelectorArgs(
              matchLabels = replicaLabels
            ),
            replicas = replicas,
            template = PodTemplateSpecArgs(
              metadata = ObjectMetaArgs(
                name = replicaName,
                namespace = namespace.metadata.name,
                labels = replicaLabels
              ),
              spec = PodSpecArgs(
                containers = List(
                  ContainerArgs(
                    name = "redis",
                    image = "pulumi/guestbook-redis-replica",
                    env = List(
                      EnvVarArgs(name = "GET_HOSTS_FROM", value = "dns")
                    ),
                    ports = List(
                      ContainerPortArgs(name = redisPortName, containerPort = redisPortNumber)
                    ),
                    resources = ResourceRequirementsArgs(
                      requests = Map(
                        "cpu" -> "100m",
                        "memory" -> "100Mi"
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )

      val replicaService = Service(
        replicaName,
        ServiceArgs(
          metadata = ObjectMetaArgs(
            name = replicaName,
            labels = replicaLabels,
            namespace = namespace.metadata.name
          ),
          spec = ServiceSpecArgs(
            ports = List(
              ServicePortArgs(
                name = redisPortName,
                port = redisPortNumber,
                targetPort = redisPortNumber
              )
            ),
            selector = replica.spec.template.metadata.labels
          )
        )
      )

      new Redis(
        namespace = namespace.metadata.name.getOrFail(Exception("expected namespace name to be defined")),
        leader = Output(
          RedisEndpoint(
            url = p"redis://${serviceFqdn(leaderService, namespace)}:$redisPortNumber",
            fqdn = serviceFqdn(leaderService, namespace)
          )
        ),
        replica = Output(
          RedisEndpoint(
            url = p"redis://${serviceFqdn(replicaService, namespace)}:$redisPortNumber",
            fqdn = serviceFqdn(replicaService, namespace)
          )
        )
      )
    }
end Redis

case class ApplicationArgs(
  redis: Input[Redis],
  serviceType: Input[ServiceSpecType],
  image: Input[String] = "pulumi/guestbook-php-redis",
  replicas: Input[Int] = 1,
  resources: Input[Option[ResourceRequirementsArgs]] = None
)

case class Application private (
  namespace: Output[String],
  url: Output[String],
  fqdn: Output[String]
)(using ComponentBase)
    extends ComponentResource
    derives RegistersOutputs
object Application:
  extension (r: Output[Application])
    def namespace: Output[String] = r.flatMap(_.namespace)
    def url: Output[String]       = r.flatMap(_.url)
    def fqdn: Output[String]      = r.flatMap(_.fqdn)

  def apply(using Context)(
    name: NonEmptyString,
    args: ApplicationArgs,
    componentResourceOptions: ComponentResourceOptions = ComponentResourceOptions()
  ): Output[Application] =
    component(name, "besom:example:Application", componentResourceOptions) {
      val labels = Map("app" -> name)
      val namespace = Namespace(
        name,
        NamespaceArgs(
          metadata = ObjectMetaArgs(
            labels = labels
          )
        )
      )

      val appPortNumber = 80
      val deployment = Deployment(
        name,
        DeploymentArgs(
          metadata = ObjectMetaArgs(
            labels = labels,
            namespace = namespace.metadata.name
          ),
          spec = DeploymentSpecArgs(
            selector = LabelSelectorArgs(matchLabels = labels),
            replicas = args.replicas,
            template = PodTemplateSpecArgs(
              metadata = ObjectMetaArgs(labels = labels),
              spec = PodSpecArgs(
                containers = List(
                  ContainerArgs(
                    name = name,
                    image = args.image,
                    ports = List(
                      ContainerPortArgs(name = "http", containerPort = appPortNumber)
                    ),
                    env = List(
                      EnvVarArgs(name = "GET_HOSTS_FROM", value = "env"),
                      EnvVarArgs(name = "REDIS_LEADER_SERVICE_HOST", value = args.redis.asOutput().leader.fqdn),
                      EnvVarArgs(name = "REDIS_REPLICA_SERVICE_HOST", value = args.redis.asOutput().replica.fqdn)
                    ),
                    resources = args.resources
                      .asOptionOutput()
                      .getOrElse(
                        ResourceRequirementsArgs(
                          requests = Map(
                            "cpu" -> "100m",
                            "memory" -> "100Mi"
                          )
                        )
                      )
                  )
                )
              )
            )
          )
        )
      )

      val service = Service(
        name,
        ServiceArgs(
          metadata = ObjectMetaArgs(
            labels = labels,
            namespace = namespace.metadata.name
          ),
          spec = ServiceSpecArgs(
            selector = deployment.spec.template.metadata.labels,
            `type` = args.serviceType,
            ports = List(
              ServicePortArgs(name = "http", port = appPortNumber, targetPort = appPortNumber)
            )
          )
        )
      )

      val fqdn         = serviceFqdn(service, namespace)
      val hostnameOrIp = service.status.loadBalancer.ingress.getOrElse(Iterable.empty).flatMap { ingresses => 
        ingresses.headOption match
          case Some(ingress) => 
            for 
              maybeHostname <- ingress.hostname
              maybeIp <- ingress.ip
            yield maybeHostname.orElse(maybeIp)
          case None => 
            Output(None)
      }
      
      val url          = p"http://${hostnameOrIp.getOrElse(fqdn)}:$appPortNumber"

      new Application(
        namespace = namespace.metadata.name.getOrFail(Exception("expected namespace name to be defined")),
        url = url,
        fqdn = fqdn
      )
    }
end Application

private def serviceFqdn(service: Output[Service], namespace: Output[Namespace])(using Context): Output[String] =
  val serviceName   = service.metadata.name.getOrFail(Exception("expected service name to be defined"))
  val namespaceName = namespace.metadata.name.getOrFail(Exception("expected namespace name to be defined"))
  p"${serviceName}.${namespaceName}.svc.cluster.local"

@main def main(): Unit = Pulumi.run {
  val useLoadBalancer = config.getBoolean("useLoadBalancer").getOrElse(true)

  val redis = Redis("cache", 3)

  val frontend = Application(
    name = "frontend",
    args = ApplicationArgs(
      replicas = 3,
      serviceType = useLoadBalancer.map(if _ then ServiceSpecType.LoadBalancer else ServiceSpecType.ClusterIP),
      redis = redis
    )
  )

  Stack.exports(
    frontend = frontend.url,
    leader = redis.leader,
    replica = redis.replica
  )
}
