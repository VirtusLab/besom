import besom.*
import besom.api.kubernetes.apps.v1.{Deployment, DeploymentArgs}
import besom.api.kubernetes.core.v1.inputs.{ContainerArgs, PodSpecArgs, PodTemplateSpecArgs}
import besom.api.kubernetes.meta.v1.inputs.{LabelSelectorArgs, ObjectMetaArgs}
import besom.api.kubernetes.apps.v1.inputs.DeploymentSpecArgs

@main def main = Pulumi.run {
  val appLabels = Map("app" -> "nginx")
  val nginxDeployment = Deployment(
    "nginx",
    DeploymentArgs(
      spec = DeploymentSpecArgs(
        selector = LabelSelectorArgs(matchLabels = appLabels),
        replicas = 1,
        template = PodTemplateSpecArgs(
          metadata = ObjectMetaArgs(
            labels = appLabels
          ),
          spec = PodSpecArgs(
            containers = List(ContainerArgs(name = "nginx", image = "nginx"))
          )
        )
      )
    )
  )

  Stack.exports(
    name = nginxDeployment.metadata.name
  )
}
