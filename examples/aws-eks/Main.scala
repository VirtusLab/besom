import besom.*
import besom.api.{awsx, eks, kubernetes as k8s}

@main def main = Pulumi.run {
  val appName = "hello-world"
  val appLabels = Map("appClass" -> appName)
  val appPort = 80

  // Get the default VPC
  val vpc = awsx.ec2.Vpc("my-vpc", awsx.ec2.VpcArgs(cidrBlock = "10.0.0.0/16"))

  // Create an EKS cluster using the default VPC and subnet
  val cluster = eks.Cluster(
    appName,
    eks.ClusterArgs(
      vpcId = vpc.vpcId,
      subnetIds = vpc.publicSubnetIds,
      instanceType = "t2.medium",
      desiredCapacity = 2,
      minSize = 1,
      maxSize = 3,
      storageClasses = "gp2"
    )
  )

  val defaultProvider = cluster.core.provider

  // Create a kubernetes namespace
  val namespace = k8s.core.v1.Namespace(
    name = appName,
    opts = opts(provider = defaultProvider)
  )
  val namespaceName = namespace.metadata.name

  // Create default metadata
  val defaultMetadata = k8s.meta.v1.inputs.ObjectMetaArgs(
    namespace = namespaceName,
    labels = appLabels
  )

  // Define the "Hello World" deployment.
  val deployment = k8s.apps.v1.Deployment(
    name = appName,
    k8s.apps.v1.DeploymentArgs(
      metadata = defaultMetadata,
      spec = k8s.apps.v1.inputs.DeploymentSpecArgs(
        selector = k8s.meta.v1.inputs.LabelSelectorArgs(matchLabels = appLabels),
        replicas = 1,
        template = k8s.core.v1.inputs.PodTemplateSpecArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(labels = appLabels),
          spec = k8s.core.v1.inputs.PodSpecArgs(
            containers = k8s.core.v1.inputs.ContainerArgs(
              name = appName,
              image = "nginx:latest",
              ports = List(
                k8s.core.v1.inputs.ContainerPortArgs(containerPort = 80)
              )
            ) :: Nil
          )
        )
      )
    ),
    opts = opts(provider = defaultProvider)
  )

  // Define the "Hello World" service.
  val service = k8s.core.v1.Service(
    name = appName,
    k8s.core.v1.ServiceArgs(
      spec = k8s.core.v1.inputs.ServiceSpecArgs(
        selector = appLabels,
        `type` = k8s.core.v1.enums.ServiceSpecType.LoadBalancer,
        ports = List(
          k8s.core.v1.inputs.ServicePortArgs(
            port = appPort,
            targetPort = 80
          )
        )
      ),
      metadata = defaultMetadata
    ),
    opts = opts(provider = defaultProvider, dependsOn = deployment)
  )

  val serviceHostname =
    service.status.loadBalancer.ingress.map(_.flatMap(_.head.hostname).get)

  // Export the cluster's kubeconfig and url
  Stack(cluster, service).exports(
    kubeconfig = cluster.kubeconfig,
    url = p"http://$serviceHostname:$appPort"
  )
}
