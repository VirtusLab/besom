import besom.*
import besom.api.{awsx, eks}
import besom.api.kubernetes.apps.v1.inputs.DeploymentSpecArgs
import besom.api.kubernetes.apps.v1.{Deployment, DeploymentArgs}
import besom.api.kubernetes.core.v1.enums.ServiceSpecType
import besom.api.kubernetes.core.v1.inputs.*
import besom.api.kubernetes.core.v1.{Namespace, Service, ServiceArgs}
import besom.api.kubernetes.meta.v1.inputs.{LabelSelectorArgs, ObjectMetaArgs}

@main def main = Pulumi.run {
  val appName = "hello-world"
  val appLabels = Map("appClass" -> appName)
  val appPort = 80

  val vpc = awsx.ec2.Vpc("my-vpc")

  val cluster = eks.Cluster(
    name = appName,
    eks.ClusterArgs(
      vpcId = vpc.vpcId,
      subnetIds = vpc.publicSubnetIds,
      desiredCapacity = 2,
      minSize = 1,
      maxSize = 2,
      storageClasses = "gp2"
    )
  )

  val namespace = Namespace(name = appName, opts = opts(provider = cluster.core.provider))
  val namespaceName = namespace.metadata.name

  val deployment = Deployment(
    name = appName,
    DeploymentArgs(
      metadata = ObjectMetaArgs(namespace = namespaceName, labels = appLabels),
      spec = DeploymentSpecArgs(
        selector = LabelSelectorArgs(matchLabels = appLabels),
        replicas = 1,
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(labels = appLabels),
          spec = PodSpecArgs(
            containers = ContainerArgs(
              name = appName,
              image = "nginx:latest",
              ports = List(
                ContainerPortArgs(containerPort = appPort)
              )
            ) :: Nil
          )
        )
      )
    ),
    opts = opts(provider = cluster.core.provider)
  )

  val service = Service(
    name = appName,
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = appLabels,
        `type` = ServiceSpecType.LoadBalancer,
        ports = List(
          ServicePortArgs(port = appPort, targetPort = appPort)
        )
      ),
      metadata = ObjectMetaArgs(namespace = namespaceName, labels = appLabels)
    ),
    opts = opts(provider = cluster.core.provider, dependsOn = deployment)
  )

  Stack(cluster, service)
    .exports(
      deploymentName = deployment.metadata.name,
      serviceName = service.metadata.name,
      serviceHostname = service.status.loadBalancer.ingress.map(_.map(_.head.hostname)),
      kubeconfig = cluster.kubeconfig
    )
}
