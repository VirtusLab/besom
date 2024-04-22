import besom.*
import besom.api.kubernetes as k8s

@main def main = Pulumi.run {

  val namespace = k8s.yaml.v2.ConfigFile(
    name = "config-file",
    k8s.yaml.v2.ConfigFileArgs(
      file = "./yaml/namespace/namespace.yaml"
    )
  )

  val deploymentWithService = k8s.yaml.v2.ConfigGroup(
    name = "config-group",
    k8s.yaml.v2.ConfigGroupArgs(
      files = List("./yaml/*.yaml")
    ),
    opts = opts(dependsOn = namespace)
  )

  Stack.exports(
    deploymentWithServiceResource = deploymentWithService.resources,
    namespaceResource = namespace.resources
  )
}
