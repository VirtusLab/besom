import besom.*
import besom.api.kubernetes.apps.v1.{deployment, DeploymentArgs}
import besom.api.kubernetes.core.v1.inputs.{ContainerArgs, ContainerPortArgs, PodSpecArgs, PodTemplateSpecArgs}
import besom.api.kubernetes.meta.v1.inputs.{LabelSelectorArgs, ObjectMetaArgs}
import besom.api.kubernetes.apps.v1.inputs.DeploymentSpecArgs

@main def main = Pulumi.run {
  val appLabels = Map("app" -> "nginx")
  for nginxDeployment <- deployment(
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
  yield Pulumi.exports(
    name = nginxDeployment.metadata.name
  )
}
