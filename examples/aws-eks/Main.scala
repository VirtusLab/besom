import besom.*
import besom.api.aws
import besom.api.eks
import besom.api.kubernetes as k8s

@main def main = Pulumi.run {
  // Get the default VPC and select the default subnet
  val vpc = aws.ec2.getVpc(aws.ec2.GetVpcArgs(default = true))
  val subnet = vpc.flatMap(vpc =>
    aws.ec2.getSubnet(
      aws.ec2.GetSubnetArgs(
        vpcId = vpc.id,
        defaultForAz = true
      )
    )
  )

  // Create an EKS cluster using the default VPC and subnet
  val cluster = eks.Cluster(
    "my-cluster",
    eks.ClusterArgs(
      vpcId = vpc.id,
      subnetIds = List(subnet.id),
      instanceType = "t2.medium",
      desiredCapacity = 2,
      minSize = 1,
      maxSize = 3,
      storageClasses = "gp2"
    )
  )

  val k8sProvider = k8s.Provider(
    "k8s-provider",
    k8s.ProviderArgs(
      kubeconfig = cluster.kubeconfigJson
    )
  )

  val pod = k8s.core.v1.Pod(
    "mypod",
    k8s.core.v1.PodArgs(
      spec = k8s.core.v1.inputs.PodSpecArgs(
        containers = List(
          k8s.core.v1.inputs.ContainerArgs(
            name = "echo",
            image = "k8s.gcr.io/echoserver:1.4"
          )
        )
      )
    ),
    opts = opts(
      provider = k8sProvider,
      dependsOn = cluster,
      deletedWith = cluster // skip deletion to save time, since it will be deleted with the cluster
    )
  )

  // Export the cluster's kubeconfig
  Stack(cluster, pod).exports(kubeconfig = cluster.kubeconfig)
}
