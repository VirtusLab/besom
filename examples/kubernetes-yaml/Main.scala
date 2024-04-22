import besom.*
import besom.api.kubernetes as k8s

@main def main = Pulumi.run {

  val configGroup = k8s.yaml.v2.ConfigGroup(
    name = "config-group",
    k8s.yaml.v2.ConfigGroupArgs(
      files = List("./yaml/*.yaml")
    )
  )

  Stack.exports(
    resources = configGroup.resources
  )
}
