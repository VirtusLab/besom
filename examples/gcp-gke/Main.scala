import besom.*
import besom.api.gcp

@main def main = Pulumi.run {
  val kubernetesEngine = gcp.projects.Service(
    name = "enable-kubernetes-engine",
    gcp.projects.ServiceArgs(
      service = "container.googleapis.com",
      /* if true - at every destroy this will disable the dependent services for the whole project */
      disableDependentServices = true,
      /* if true - at every destroy this will disable the service for the whole project */
      disableOnDestroy = true
    )
  )

  val k8sCluster = gcp.container.Cluster(
    name = "cluster",
    gcp.container.ClusterArgs(
      deletionProtection = false,
      initialNodeCount = 1,
      nodeConfig = gcp.container.inputs.ClusterNodeConfigArgs(
        machineType = "n1-standard-1",
        oauthScopes = List(
          "https://www.googleapis.com/auth/compute",
          "https://www.googleapis.com/auth/devstorage.read_only",
          "https://www.googleapis.com/auth/logging.write",
          "https://www.googleapis.com/auth/monitoring"
        )
      )
    ),
    opts = opts(dependsOn = kubernetesEngine)
  )

  val context = p"${k8sCluster.project}_${k8sCluster.location}_${k8sCluster.name}"
  val kubeconfig =
    p"""apiVersion: v1
       |clusters:
       |- cluster:
       |    certificate-authority-data: ${k8sCluster.masterAuth.clusterCaCertificate.map(_.get).asPlaintext}
       |    server: https://${k8sCluster.endpoint}
       |  name: $context
       |contexts:
       |- context:
       |    cluster: $context
       |    user: $context
       |  name: $context
       |current-context: $context
       |kind: Config
       |preferences: {}
       |users:
       |- name: $context
       |  user:
       |    exec:
       |      apiVersion: client.authentication.k8s.io/v1beta1
       |      command: gke-gcloud-auth-plugin
       |      installHint: Install gke-gcloud-auth-plugin for use with kubectl by following
       |        https://cloud.google.com/blog/products/containers-kubernetes/kubectl-auth-changes-in-gke
       |      provideClusterInfo: true
       |""".stripMargin

  Stack.exports(
    clusterName = k8sCluster.name,
    kubeconfig = kubeconfig
  )
}
