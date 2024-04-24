import besom.*
import besom.api.kubernetes as k8s

@main def main = Pulumi.run {

  val crd =
    k8s.yaml.v2.ConfigGroup(
      name = "config-group",
      k8s.yaml.v2.ConfigGroupArgs(
        yaml = """apiVersion: apiextensions.k8s.io/v1
          |kind: CustomResourceDefinition
          |metadata:
          |  # name must match the spec fields below, and be in the form: <plural>.<group>
          |  name: crontabs.stable.example.com
          |spec:
          |  # group name to use for REST API: /apis/<group>/<version>
          |  group: stable.example.com
          |  # list of versions supported by this CustomResourceDefinition
          |  versions:
          |    - name: v1
          |      # Each version can be enabled/disabled by Served flag.
          |      served: true
          |      # One and only one version must be marked as the storage version.
          |      storage: true
          |      schema:
          |        openAPIV3Schema:
          |          type: object
          |          properties:
          |            spec:
          |              type: object
          |              properties:
          |                cronSpec:
          |                  type: string
          |                image:
          |                  type: string
          |                replicas:
          |                  type: integer
          |  # either Namespaced or Cluster
          |  scope: Namespaced
          |  names:
          |    # plural name to be used in the URL: /apis/<group>/<version>/<plural>
          |    plural: crontabs
          |    # singular name to be used as an alias on the CLI and for display
          |    singular: crontab
          |    # kind is normally the CamelCased singular type. Your resource manifests use this.
          |    kind: CronTab
          |    # shortNames allow shorter string to match your resource on the CLI
          |    shortNames:
          |    - ct
          |    """.stripMargin
      )
    )

  val cronTab =
    k8s.yaml.v2.ConfigGroup(
      name = "cron-tab",
      k8s.yaml.v2.ConfigGroupArgs(
        yaml = """apiVersion: "stable.example.com/v1"
            |kind: CronTab
            |metadata:
            |  name: my-new-cron-object
            |spec:
            |  cronSpec: "* * * * */5"
            |  image: my-awesome-cron-image
            |  """.stripMargin
      ),
      opts = opts(dependsOn = crd)
    )

  Stack.exports(
    crdResource = crd.resources,
    cronTabResource = cronTab.resources
  )
}
