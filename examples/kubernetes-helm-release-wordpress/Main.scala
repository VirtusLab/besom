import besom.*
import besom.api.kubernetes as k8s
import besom.json.{JsObject, JsString}

@main def main = Pulumi.run {

  // Deploy the bitnami/wordpress chart.
  val wordpress = k8s.helm.v3.Release(
    name = "wpdev",
    k8s.helm.v3.ReleaseArgs(
      chart = "wordpress",
      repositoryOpts = k8s.helm.v3.inputs.RepositoryOptsArgs(
        repo = "https://charts.bitnami.com/bitnami"
      ),
      version = "15.0.5",
      // Force to use ClusterIP so no assumptions on support for LBs etc. is required.
      values = Map(
        "service" ->
          JsObject(
            "type" -> JsString("ClusterIP")
          )
      )
    )
  )

  // Get the status field from the wordpress service, and then grab a reference to the spec.
  val svcName       = p"${wordpress.status.namespace.map(_.get)}/${wordpress.status.name.map(_.get)}-wordpress"
  val svcResourceId = svcName.map(name => ResourceId(NonEmptyString(name).get))
  val svc           = k8s.core.v1.Service.get(name = "wpdev-wordpress", id = svcResourceId)

  Stack.exports(
    // Export the Cluster IP for Wordpress.
    frontendIp = svc.spec.clusterIP,
    // Command to run to access the wordpress frontend on localhost:8080
    portForwardCommand = p"kubectl port-forward svc/${svc.metadata.name.map(_.get)} 8080:80"
  )
}
