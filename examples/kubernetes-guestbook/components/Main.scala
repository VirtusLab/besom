import besom.*
import besom.api.kubernetes
import besom.api.kubernetes.core.v1.{Service, ServiceArgs}
import besom.api.kubernetes.core.v1.inputs.*
import besom.api.kubernetes.apps.v1.{Deployment, DeploymentArgs}
import besom.api.kubernetes.apps.v1.inputs.*
import besom.api.kubernetes.meta.v1.*
import besom.api.kubernetes.meta.v1.inputs.*
import besom.api.kubernetes.core.v1.enums.ServiceSpecType

case class ServiceDeploymentArgs(
  image: String,
  replicas: Int = 1,
  resources: Option[ResourceRequirementsArgs] = None,
  ports: List[Int] = List.empty,
  allocateIPAddress: Option[Boolean] = None,
  serviceType: Option[ServiceSpecType] = None,
  env: List[EnvVarArgs] = List.empty
)

case class ServiceDeployment(ipAddress: Output[Option[String]])(using ComponentBase) extends ComponentResource
    derives RegistersOutputs

def serviceDeployment(using Context)(
  name: NonEmptyString,
  args: ServiceDeploymentArgs,
  componentResourceOptions: ComponentResourceOptions = ComponentResourceOptions()
): Output[ServiceDeployment] =
  component(name, "k8sx:service:ServiceDeployment", componentResourceOptions) {
    val labels = Map("app" -> name)
    val deploymentPorts = args.ports
      .map(port => ContainerPortArgs(containerPort = port))

    val container = ContainerArgs(
      name = name,
      image = args.image,
      resources = args.resources.getOrElse(
        ResourceRequirementsArgs(
          requests = Map(
            "cpu" -> "100m",
            "memory" -> "100Mi"
          )
        )
      ),
      env = args.env,
      ports = deploymentPorts
    )

    val deployment = Deployment(
      name,
      DeploymentArgs(
        spec = DeploymentSpecArgs(
          selector = LabelSelectorArgs(matchLabels = labels),
          replicas = args.replicas,
          template = PodTemplateSpecArgs(
            metadata = ObjectMetaArgs(labels = labels),
            spec = PodSpecArgs(containers = List(container))
          )
        )
      )
    )

    val servicePorts = args.ports
      .map(port => ServicePortArgs(port = port, targetPort = port))

    val service = Service(
      name,
      ServiceArgs(
        metadata = ObjectMetaArgs(labels = labels, name = name),
        spec = ServiceSpecArgs(
          `type` = args.allocateIPAddress
            .flatMap(Option.when(_)(args.serviceType.getOrElse(ServiceSpecType.LoadBalancer))),
          ports = servicePorts,
          selector = labels
        )
      )
    )

    val ipAddress =
      for
        hasIp       <- args.allocateIPAddress
        serviceType <- args.serviceType if hasIp
      yield
        if serviceType == ServiceSpecType.ClusterIP
        then service.spec.clusterIP
        else service.status.loadBalancer.ingress.map(_.map(_.head)).ip

    for
      _ <- deployment
      _ <- service
    yield ServiceDeployment(ipAddress.getOrElse(Output(None)))
  }

@main def main = Pulumi.run {
  val redisLeader = serviceDeployment(
    name = "redis-leader",
    args = ServiceDeploymentArgs(
      image = "redis",
      ports = List(6379)
    )
  )

  val redisReplica = serviceDeployment(
    name = "redis-replica",
    args = ServiceDeploymentArgs(
      image = "pulumi/guestbook-redis-replica",
      ports = List(6379)
    )
  )

  val frontend = config
    .getBoolean("isMinikube")
    .getOrElse(false)
    .flatMap(isMinikube =>
      serviceDeployment(
        name = "frontend",
        args = ServiceDeploymentArgs(
          replicas = 3,
          image = "pulumi/guestbook-php-redis",
          ports = List(80),
          allocateIPAddress = Some(true),
          serviceType = Some(if isMinikube then ServiceSpecType.ClusterIP else ServiceSpecType.LoadBalancer)
        )
      )
    )

  Stack(redisLeader, redisReplica)
    .exports(
      ipAddress = frontend.flatMap(_.ipAddress)
    )
}
