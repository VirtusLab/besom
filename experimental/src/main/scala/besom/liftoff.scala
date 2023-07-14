//> using dep "org.virtuslab::besom-kubernetes:0.0.1-SNAPSHOT"

import besom.*
import besom.util.NonEmptyString
import besom.api.{kubernetes => k8s}

import k8s.core.v1.inputs.*
import k8s.apps.v1.inputs.*
import k8s.meta.v1.inputs.*
import k8s.apps.v1.{deployment, DeploymentArgs}
import k8s.core.v1.{configMap, ConfigMapArgs, namespace, service, ServiceArgs}
import besom.internal.Output

@main def main = Pulumi.run {
  val labels                                      = Map("app" -> "nginx")
  val appNamespace: Output[k8s.core.v1.Namespace] = namespace("liftoff")

  val html =
    "<h1>Welcome to Besom: Functional Infrastructure in Scala 3</h1>"

  val indexHtmlConfigMap: Output[k8s.core.v1.ConfigMap] = configMap(
    "index-html-configmap",
    ConfigMapArgs(
      metadata = ObjectMetaArgs(
        name = "index-html-configmap",
        labels = labels,
        namespace = appNamespace.metadata.name.orEmpty
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
            labels = labels,
            namespace = appNamespace.metadata.name.orEmpty
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
                  name = indexHtmlConfigMap.metadata.name.orEmpty
                )
              )
            )
          )
        )
      ),
      metadata = ObjectMetaArgs(
        namespace = appNamespace.metadata.name.orEmpty
      )
    )
  )

  val nginxService = service(
    "nginx",
    ServiceArgs(
      spec = ServiceSpecArgs(
        selector = labels,
        ports = List(
          ServicePortArgs(name = "http", port = 1337)
        )
      ),
      metadata = ObjectMetaArgs(
        namespace = appNamespace.metadata.name.orEmpty,
        labels = labels
      )
    )
  )

  for
    nginx   <- nginxDeployment
    service <- nginxService
  yield Pulumi.exports(
    namespace = appNamespace.metadata.name,
    nginxDeploymentName = nginx.metadata.name,
    serviceName = nginxService.metadata.name,
    serviceClusterIp = nginxService.spec.clusterIP
  )
}
