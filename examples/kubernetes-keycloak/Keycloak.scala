import besom.*
import besom.api.kubernetes as k8s

case class KeycloakArgs(
  namespaceName: Output[String],
  dbConfig: Output[DbConfig],
  db: Output[Postgres],
  config: Output[KeycloakConfig]
)
object KeycloakArgs:
  extension (o: Output[KeycloakArgs])
    def namespaceName: Output[String]  = o.flatMap(_.namespaceName)
    def dbConfig: Output[DbConfig]     = o.flatMap(_.dbConfig)
    def db: Output[Postgres]           = o.flatMap(_.db)
    def config: Output[KeycloakConfig] = o.flatMap(_.config)

case class Keycloak private (host: Output[String], name: Output[String], port: Output[Int])(using ComponentBase) extends ComponentResource
    derives RegistersOutputs

object Keycloak:
  val port = 8080
  extension (o: Output[Keycloak])
    def host: Output[String] = o.flatMap(_.host)
    def name: Output[String] = o.flatMap(_.name)
    def port: Output[Int]    = o.flatMap(_.port)

  def apply(using
    Context
  )(name: NonEmptyString, args: KeycloakArgs, options: ComponentResourceOptions = ComponentResourceOptions()): Output[Keycloak] =
    component(name, "custom:resource:Keycloak", options) {
      val keycloakImagePort = 8080
      val appLabels         = Map("app" -> "keycloak")
      val imageTag          = "24.0.3"

      val secret = k8s.core.v1.Secret(
        name = s"$name-keycloak-secret",
        k8s.core.v1.SecretArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          `type` = "Opaque",
          stringData = Map(
            "KC_DB_PASSWORD" -> args.dbConfig.password,
            "KEYCLOAK_ADMIN_PASSWORD" -> args.config.password
          )
        )
      )

      val configMap = k8s.core.v1.ConfigMap(
        name = s"$name-keycloak-cm",
        k8s.core.v1.ConfigMapArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          data = Map(
            // All configs https://www.keycloak.org/server/all-config
            "KC_DB" -> "postgres",
            "KC_DB_URL_HOST" -> args.db.name,
            "KC_DB_URL_PORT" -> args.db.port.map(_.toString),
            "KC_DB_URL_DATABASE" -> args.dbConfig.database,
            "KC_DB_USERNAME" -> args.dbConfig.user,
            "KEYCLOAK_ADMIN" -> args.config.user,
            "KC_PROXY_ADDRESS_FORWARDING" -> "true",
            "KC_HOSTNAME_STRICT" -> "true",
            "KC_HOSTNAME" -> args.config.host,
            "KC_PROXY" -> "edge",
            "KC_HTTP_PORT" -> keycloakImagePort.toString,
            "KC_HTTP_ENABLED" -> "false"
          )
        )
      )

      val deployment = k8s.apps.v1.Deployment(
        name = s"$name-keycloak-deployment",
        k8s.apps.v1.DeploymentArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            labels = appLabels,
            namespace = args.namespaceName
          ),
          spec = k8s.apps.v1.inputs.DeploymentSpecArgs(
            selector = k8s.meta.v1.inputs.LabelSelectorArgs(matchLabels = appLabels),
            replicas = 1,
            template = k8s.core.v1.inputs.PodTemplateSpecArgs(
              metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
                name = name,
                labels = appLabels,
                namespace = args.namespaceName
              ),
              spec = k8s.core.v1.inputs.PodSpecArgs(
                containers = k8s.core.v1.inputs.ContainerArgs(
                  args = "start" :: Nil,
                  name = "keycloak",
                  image = s"quay.io/keycloak/keycloak:$imageTag",
                  imagePullPolicy = "IfNotPresent",
                  ports = k8s.core.v1.inputs.ContainerPortArgs(containerPort = keycloakImagePort) :: Nil,
                  envFrom = List(
                    k8s.core.v1.inputs.EnvFromSourceArgs(
                      secretRef = k8s.core.v1.inputs.SecretEnvSourceArgs(secret.metadata.name)
                    ),
                    k8s.core.v1.inputs.EnvFromSourceArgs(
                      configMapRef = k8s.core.v1.inputs.ConfigMapEnvSourceArgs(configMap.metadata.name)
                    )
                  )
                ) :: Nil
              )
            )
          )
        )
      )

      val service = k8s.core.v1.Service(
        name = s"$name-keycloak",
        k8s.core.v1.ServiceArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            labels = appLabels,
            namespace = args.namespaceName
          ),
          spec = k8s.core.v1.inputs.ServiceSpecArgs(
            selector = appLabels,
            `type` = k8s.core.v1.enums.ServiceSpecType.ClusterIP,
            ports = List(
              k8s.core.v1.inputs.ServicePortArgs(port = port, targetPort = keycloakImagePort)
            )
          )
        ),
        opts = opts(dependsOn = deployment)
      )

      Keycloak(
        host = args.config.host,
        name = service.metadata.name.map(_.get),
        port = service.spec.ports.map(_.flatMap(_.headOption)).port.map(_.get)
      )
    }
end Keycloak
