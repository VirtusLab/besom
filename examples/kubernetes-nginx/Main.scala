import besom.*
import besom.api.kubernetes
import besom.api.kubernetes.core.v1.*
import besom.api.kubernetes.core.v1.inputs.*
import besom.api.kubernetes.apps.v1.*
import besom.api.kubernetes.apps.v1.inputs.*
import besom.api.kubernetes.meta.v1.*
import besom.api.kubernetes.meta.v1.inputs.*

@main def main = Pulumi.run {
  val nginxLabels = Map("app" -> "nginx")
  val nginxDeployment = kubernetes.apps.v1.Deployment(
    "nginx",
    kubernetes.apps.v1.DeploymentArgs(
      spec = DeploymentSpecArgs(
        replicas = config.getInt("replicas"),
        selector = LabelSelectorArgs(
          matchLabels = nginxLabels
        ),
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(
            labels = nginxLabels
          ),
          spec = PodSpecArgs(
            containers = List(
              ContainerArgs(
                name = "nginx",
                image = "nginx:stable",
                ports = List(
                  ContainerPortArgs(
                    containerPort = 80
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  Stack.exports(
    nginx = nginxDeployment.metadata.name
  )
}
