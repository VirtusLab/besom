import besom.*
import besom.json.*

case class AppConfig(
  adminDb: DbConfig,
  keycloakDb: DbConfig,
  keycloak: KeycloakConfig
) derives JsonFormat
object AppConfig:
  extension (o: Output[AppConfig])
    def adminDb: Output[DbConfig]        = o.map(_.adminDb)
    def keycloakDb: Output[DbConfig]     = o.map(_.keycloakDb)
    def keycloak: Output[KeycloakConfig] = o.map(_.keycloak)

  val default = AppConfig(
    adminDb = DbConfig(
      user = "postgres",
      password = "password",
      database = "postgres"
    ),
    keycloakDb = DbConfig(
      user = "keycloak",
      password = "SUPERsecret",
      database = "keycloak"
    ),
    keycloak = KeycloakConfig(
      user = "admin",
      password = "password",
      host = "keycloak.localhost"
    )
  )
