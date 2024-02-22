import besom.*
import besom.api.kubernetes
import besom.api.kubernetes.core.v1.{Service, ServiceArgs}
import besom.api.kubernetes.core.v1.inputs.*
import besom.api.kubernetes.apps.v1.{Deployment, DeploymentArgs}
import besom.api.kubernetes.apps.v1.inputs.*
import besom.api.kubernetes.meta.v1.*
import besom.api.kubernetes.meta.v1.inputs.*
import besom.api.kubernetes.core.v1.enums.ServiceSpecType

@main def main = Pulumi.run {
  val isMinikube = config
    .getBoolean("isMinikube")
    .getOrElse(false)

  //
  // REDIS LEADER.
  //
  val redisLeaderLabels = Map("app" -> "redis-leader")
  val redisLeaderPort   = 6379

  val redisLeaderDeployment = Deployment(
    "redis-leader",
    DeploymentArgs(
      spec = DeploymentSpecArgs(
        selector = LabelSelectorArgs(matchLabels = redisLeaderLabels),
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(labels = redisLeaderLabels),
          spec = PodSpecArgs(containers =
            List(
              ContainerArgs(
                name = "redis-leader",
                image = "redis",
                resources = ResourceRequirementsArgs(
                  requests = Map(
                    "cpu" -> "100m",
                    "memory" -> "100Mi"
                  )
                ),
                ports = List(ContainerPortArgs(containerPort = redisLeaderPort))
              )
            )
          )
        )
      )
    )
  )

  val redisLeaderService = Service(
    "redis-leader",
    ServiceArgs(
      metadata = ObjectMetaArgs(labels = redisLeaderLabels, name = "redis-leader"),
      spec = ServiceSpecArgs(
        ports = List(ServicePortArgs(port = redisLeaderPort, targetPort = redisLeaderPort)),
        selector = redisLeaderLabels
      )
    ),
    opts = opts(dependsOn = redisLeaderDeployment)
  )

  //
  // REDIS REPLICA.
  //
  val redisReplicaLabels = Map("app" -> "redis-replica")
  val redisReplicaPort   = 6379

  val redisReplicaDeployment = Deployment(
    "redis-replica",
    DeploymentArgs(
      spec = DeploymentSpecArgs(
        selector = LabelSelectorArgs(matchLabels = redisReplicaLabels),
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(labels = redisReplicaLabels),
          spec = PodSpecArgs(containers =
            List(
              ContainerArgs(
                name = "replica",
                image = "pulumi/guestbook-redis-replica",
                resources = ResourceRequirementsArgs(
                  requests = Map(
                    "cpu" -> "100m",
                    "memory" -> "100Mi"
                  )
                ),
                // If your cluster config does not include a dns service, then to instead access an environment
                // variable to find the leader's host, change `value: "dns"` to read `value: "env"`.
                env = List(EnvVarArgs("name", "GET_HOSTS_FROM"), EnvVarArgs("value", "dns")),
                ports = List(ContainerPortArgs(containerPort = redisReplicaPort))
              )
            )
          )
        )
      )
    )
  )

  val redisReplicaService = Service(
    "redis-replica",
    ServiceArgs(
      metadata = ObjectMetaArgs(labels = redisReplicaLabels, name = "redis-replica"),
      spec = ServiceSpecArgs(
        ports = List(ServicePortArgs(port = redisReplicaPort, targetPort = redisReplicaPort)),
        selector = redisReplicaLabels
      )
    ),
    opts = opts(dependsOn = redisReplicaDeployment)
  )

  //
  // FRONTEND
  //
  val frontendLabels = Map("app" -> "frontend")
  val frontendPort   = 80

  val frontendDeployment = Deployment(
    "frontend",
    DeploymentArgs(
      spec = DeploymentSpecArgs(
        selector = LabelSelectorArgs(matchLabels = frontendLabels),
        replicas = 3,
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(labels = frontendLabels),
          spec = PodSpecArgs(containers =
            List(
              ContainerArgs(
                name = "frontend",
                image = "pulumi/guestbook-php-redis",
                resources = ResourceRequirementsArgs(
                  requests = Map(
                    "cpu" -> "100m",
                    "memory" -> "100Mi"
                  )
                ),
                // If your cluster config does not include a dns service, then to instead access an environment
                // variable to find the master service's host, change `value: "dns"` to read `value: "env"`.
                env = List(
                  EnvVarArgs("name", "GET_HOSTS_FROM"),
                  EnvVarArgs("value", "dns") /*, EnvVarArgs("value", "env")*/
                ),
                ports = List(ContainerPortArgs(containerPort = frontendPort))
              )
            )
          )
        )
      )
    )
  )

  val frontendService = Service(
    "frontend",
    ServiceArgs(
      metadata = ObjectMetaArgs(labels = frontendLabels, name = "frontend"),
      spec = ServiceSpecArgs(
        `type` = isMinikube.map(if _ then ServiceSpecType.ClusterIP else ServiceSpecType.LoadBalancer),
        ports = List(ServicePortArgs(port = frontendPort, targetPort = frontendPort)),
        selector = frontendLabels
      )
    ),
    opts = opts(dependsOn = frontendDeployment)
  )

  Stack(redisLeaderService, redisReplicaService, frontendService)
    .exports(
      ipAddress = isMinikube.flatMap(
        // Export the frontend IP.
        if _ then frontendService.spec.clusterIP
        else frontendService.status.loadBalancer.ingress.map(_.map(_.head)).ip
      )
    )
}
