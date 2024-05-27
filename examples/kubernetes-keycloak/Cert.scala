import besom.*
import besom.api.tls
import besom.api.kubernetes as k8s

case class CertArgs(namespaceName: Output[String])
object CertArgs:
  extension (o: Output[CertArgs]) def namespaceName: Output[String] = o.flatMap(_.namespaceName)

case class Cert private (certPem: Output[String], privateKeyPem: Output[String], secretName: Output[String])(using ComponentBase)
    extends ComponentResource derives RegistersOutputs

object Cert:
  extension (o: Output[Cert])
    def certPem: Output[String]       = o.flatMap(_.certPem)
    def privateKeyPem: Output[String] = o.flatMap(_.privateKeyPem)
    def secretName: Output[String]    = o.flatMap(_.secretName)

  def apply(using
    Context
  )(
    name: NonEmptyString,
    args: CertArgs,
    options: ResourceOptsVariant.Component ?=> ComponentResourceOptions = ComponentResourceOptions()
  ): Output[Cert] =
    component(name, "custom:resource:Cert", options(using ResourceOptsVariant.Component)) {

      val privateKey = tls.PrivateKey(
        name = s"$name-private-key",
        tls.PrivateKeyArgs(
          algorithm = "RSA",
          rsaBits = 2048
        )
      )

      val selfSignedCert = tls.SelfSignedCert(
        name = s"$name-self-signed-cert",
        tls.SelfSignedCertArgs(
          privateKeyPem = privateKey.privateKeyPem,
          subject = tls.inputs.SelfSignedCertSubjectArgs(
            commonName = "app-localhost",
            organization = "App"
          ),
          isCaCertificate = true,
          validityPeriodHours = 12,
          allowedUses = List(
            "key_encipherment",
            "digital_signature",
            "server_auth"
          ),
          dnsNames = List("localhost", "keycloak.localhost"),
          ipAddresses = List("127.0.0.1")
        )
      )

      val tlsSecret = k8s.core.v1.Secret(
        name = s"$name-tls-secret",
        k8s.core.v1.SecretArgs(
          metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
            namespace = args.namespaceName
          ),
          `type` = "kubernetes.io/tls",
          stringData = Map(
            "tls.crt" -> selfSignedCert.certPem,
            "tls.key" -> selfSignedCert.privateKeyPem
          )
        )
      )

      Cert(
        certPem = selfSignedCert.certPem,
        privateKeyPem = selfSignedCert.privateKeyPem,
        secretName = tlsSecret.metadata.name.map(_.get)
      )
    }
end Cert
