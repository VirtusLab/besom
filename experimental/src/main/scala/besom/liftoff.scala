//> using dep "org.virtuslab::besom-kubernetes:0.0.1-SNAPSHOT"

import besom.*, api.{kubernetes => k8s}

import k8s.core.v1.inputs.*
import k8s.apps.v1.inputs.*
import k8s.meta.v1.inputs.*
import k8s.core.v1.ConfigMap
import k8s.apps.v1.{deployment, DeploymentArgs}
import k8s.core.v1.{configMap, ConfigMapArgs, service, ServiceArgs}

@main
def main(): Unit = Pulumi.run {
  val labels = Map("app" -> "nginx")

  val html =
    """<!DOCTYPE html>
      <html>
      |  <head>
      |    <title>Infrastructure as Types: Pulumi and Scala</title>
      |  </head>
      |  <body>
      |    <h1>Hello world!</h1>
      |    <h3>Infrastructure as Types: Pulumi and Scala</h3>
      |  </body>
      |</html>""".stripMargin

  val indexHtmlConfigMap = configMap(
    "index-html-configmap",
    ConfigMapArgs(
      metadata = ObjectMetaArgs(
        name = "index-html-configmap",
        labels = labels
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
            labels = labels
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
                  name = indexHtmlConfigMap.flatMap(_.metadata.map(_.name.get))
                )
              )
            )
          )
        )
      )
    )
  )

  val nginxService = service(
    "nginx",
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        ports = List(
          ServicePortArgs(name = "http", port = 80)
        )
      )
    )
  )

  for
    _       <- indexHtmlConfigMap
    nginx   <- nginxDeployment
    service <- nginxService
    exports <- Pulumi.exports("name" -> nginx.metadata.map(_.name))
  yield exports
}
