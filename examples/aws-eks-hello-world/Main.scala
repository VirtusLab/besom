import besom.*
import besom.api.eks
import besom.api.kubernetes.apps.v1.inputs.DeploymentSpecArgs
import besom.api.kubernetes.apps.v1.{Deployment, DeploymentArgs}
import besom.api.kubernetes.core.v1.enums.ServiceSpecType
import besom.api.kubernetes.core.v1.inputs.*
import besom.api.kubernetes.core.v1.{Service, ServiceArgs}
import besom.api.kubernetes.meta.v1.inputs.{LabelSelectorArgs, ObjectMetaArgs}
import besom.api.kubernetes.{Provider, ProviderArgs}

@main def main = Pulumi.run {
  val appLabels           = Map("app" -> "hello-world")
  val helloKubernetesPort = 8080

  val cluster = eks.Cluster("cluster")

  val k8sProvider = Provider(
    name = "k8s-provider",
    ProviderArgs(kubeconfig = cluster.kubeconfigJson)
  )

  val deployment = Deployment(
    name = "hello-world-deployment",
    DeploymentArgs(
      spec = DeploymentSpecArgs(
        selector = LabelSelectorArgs(matchLabels = appLabels),
        replicas = 2,
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(appLabels = appLabels),
          spec = PodSpecArgs(
            containers = ContainerArgs(
              name = "hello-world",
              image = "paulbouwer/hello-kubernetes:1.5",
              ports = List(
                ContainerPortArgs(containerPort = helloKubernetesPort)
              )
            ) :: Nil
          )
        )
      )
    ),
    opts = opts(provider = k8sProvider)
  )

  val service = Service(
    name = "hello-world-service",
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = appLabels,
        `type` = ServiceSpecType.LoadBalancer,
        ports = List(
          ServicePortArgs(port = 80, targetPort = helloKubernetesPort)
        )
      ),
      metadata = ObjectMetaArgs(appLabels = appLabels)
    ),
    opts = opts(provider = k8sProvider, dependsOn = deployment)
  )

  Stack(cluster, service)
    .exports(
      serviceName = service.metadata.name,
      serviceHostname = service.status.loadBalancer.ingress.map(_.map(_.head.hostname)),
      kubeconfig = cluster.kubeconfig
    )
}
