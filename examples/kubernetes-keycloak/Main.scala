import besom.*
import besom.api.kubernetes as k8s

@main def main = Pulumi.run {
  val appName = "app"

  val appConfig = config
    .getObject[AppConfig]("app")
    .map(_.getOrElse(AppConfig.default))

  val namespaceName = k8s.core.v1
    .Namespace(
      name = s"$appName-namespace",
      k8s.core.v1.NamespaceArgs(
        metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
          name = "app"
        )
      )
    )
    .metadata
    .name
    .map(_.get)

  val certResource     = Cert(appName, CertArgs(namespaceName))
  val postgresResource = Postgres(appName, PostgresArgs(namespaceName, appConfig.adminDb, appConfig.keycloakDb))
  val keycloakResource = Keycloak(appName, KeycloakArgs(namespaceName, appConfig.keycloakDb, postgresResource, appConfig.keycloak))
  val nginxResource    = certResource.flatMap(_ => Nginx(appName, NginxArgs()))
  val ingressResource  = nginxResource.flatMap(_ => Ingress(appName, IngressArgs(namespaceName, certResource.secretName, keycloakResource)))
  val keycloakApiResource =
    for
      // add ingressResource and nginxResource to dependsOn when issue https://github.com/VirtusLab/besom/issues/488 will be resolved
      _ <- ingressResource
      _ <- nginxResource
      out <- KeycloakAPI(
        name = appName,
        args = KeycloakAPIArgs(certResource.certPem, appConfig.keycloak),
        options = opts(deletedWith = keycloakResource)
      )
    yield out

  Stack(ingressResource, nginxResource).exports(
    clientId = keycloakApiResource.clientId,
    keycloakRealmId = keycloakApiResource.keycloakRealmId,
    keycloakUserId = keycloakApiResource.keycloakUserId,
    certificatePem = certResource.certPem,
    privateKeyPem = certResource.privateKeyPem,
    dbServiceName = postgresResource.name,
    keycloakServiceName = keycloakResource.name,
    keycloakUrl = keycloakApiResource.url
  )
}
