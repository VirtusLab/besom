import besom.*
import besom.api.keycloak

case class KeycloakAPIArgs(certPem: Output[String], keycloakConfig: Output[KeycloakConfig])

case class KeycloakAPI private (
  url: Output[String],
  clientId: Output[String],
  keycloakRealmId: Output[String],
  keycloakUserId: Output[String]
)(using ComponentBase)
    extends ComponentResource
    derives RegistersOutputs

object KeycloakAPI:
  extension (o: Output[KeycloakAPI])
    def clientId: Output[String]        = o.flatMap(_.clientId)
    def keycloakRealmId: Output[String] = o.flatMap(_.keycloakRealmId)
    def keycloakUserId: Output[String]  = o.flatMap(_.keycloakUserId)
    def url: Output[String]             = o.flatMap(_.url)

  def apply(using
    Context
  )(
    name: NonEmptyString,
    args: KeycloakAPIArgs,
    options: ResourceOptsVariant.Component ?=> ComponentResourceOptions = ComponentResourceOptions()
  ): Output[KeycloakAPI] =
    component(name, "custom:resource:KeycloakAPI", options(using ResourceOptsVariant.Component)) {

      val keycloakProvider =
        keycloak.Provider(
          name = s"$name-keycloak-provider",
          keycloak.ProviderArgs(
            // https://github.com/pulumi/pulumi-keycloak/issues/128
            initialLogin = false,
            // Allows x509 calls using an unknown CA certificate (for development purposes)
            rootCaCertificate = args.certPem,
            clientId = "admin-cli",
            url = p"https://${args.keycloakConfig.host}",
            password = args.keycloakConfig.password,
            username = args.keycloakConfig.user
          )
        )

      val keycloakRealm =
        keycloak.Realm(
          name = s"$name-keycloak-realm",
          keycloak.RealmArgs(
            realm = "my-realm",
            enabled = true
          ),
          opts = opts(provider = keycloakProvider)
        )

      val client = keycloak.openid.Client(
        name = s"$name-keycloak-client",
        keycloak.openid.ClientArgs(
          realmId = keycloakRealm.id,
          accessType = "CONFIDENTIAL",
          clientId = "myClient",
          enabled = true
        ),
        opts = opts(provider = keycloakProvider)
      )

      val keycloakUser = keycloak.User(
        name = s"$name-keycloak-user",
        keycloak.UserArgs(
          realmId = keycloakRealm.id,
          username = "user_1",
          email = "user@example.com",
          enabled = true,
          emailVerified = true
        ),
        opts = opts(provider = keycloakProvider)
      )

      KeycloakAPI(
        url = keycloakProvider.url,
        clientId = client.id,
        keycloakRealmId = keycloakRealm.id,
        keycloakUserId = keycloakUser.id
      )
    }
end KeycloakAPI
