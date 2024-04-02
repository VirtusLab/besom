import besom.*
import besom.api.tls

case class SomeTlsResources(x: Output[String], y: Output[String])(using ComponentBase) extends ComponentResource derives RegistersOutputs

object SomeTlsResources:
  def apply(name: NonEmptyString, sshKey: Output[tls.PrivateKey])(using Context): Output[SomeTlsResources] =
    component(name, "user:component:SomeTlsResources", ComponentResourceOptions()) {
      val selfSignedCert = tls.SelfSignedCert(
        s"${name}-selfSignedCert",
        tls.SelfSignedCertArgs(
          allowedUses = List("server_auth"),
          validityPeriodHours = 12,
          privateKeyPem = sshKey.privateKeyPem
        )
      )

      SomeTlsResources(selfSignedCert.privateKeyPem, sshKey.publicKeyPem)
    }

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  // this seems like it would create this resource on every call of this function
  // ps this runs register resource call under the hood
  def sshKey = tls.PrivateKey(
    "sshKey",
    tls.PrivateKeyArgs(
      algorithm = "RSA",
      rsaBits = 4096
    )
  )

  // but it shouldn't because resource constructors should be idempotent
  val sshKeyRsaBits = sshKey.rsaBits
  val sshKeyAlgo    = sshKey.algorithm

  // intermission for component idempotence testing
  def tlsResources = SomeTlsResources("tlsResources", sshKey)

  val manyEvalsOfComponent = for
    _    <- tlsResources
    _    <- tlsResources
    last <- tlsResources
  yield last
  // end intermission

  // intermission for idempotence of get methods (which execute ReadResource calls (sic!))
  def readResource = tls.PrivateKey.get("sshKey", sshKey.id)

  val manyEvalsOfReadResource = for
    _    <- readResource
    _    <- readResource
    last <- readResource
  yield last
  // end intermission

  // intermission for idempotence of get resource calls
  def getResource = tls.PrivateKey("read-sshKey", tls.PrivateKeyArgs(algorithm = "RSA", rsaBits = 4096), opts(urn = sshKey.urn))
  val manyEvalsOfGetResource = for
    _    <- getResource
    _    <- getResource
    last <- getResource
  yield last
  // end intermission

  // intermission for idempotence of invoke methods
  def getGoogleCert = tls.getCertificate(tls.GetCertificateArgs(url = "https://www.google.com"))

  val manyEvalsOfInvoke = for
    _    <- getGoogleCert
    _    <- getGoogleCert
    last <- getGoogleCert
  yield last
  // end intermission

  // so this should work without any issues
  Stack(manyEvalsOfComponent, manyEvalsOfReadResource, manyEvalsOfInvoke, manyEvalsOfGetResource).exports(
    sshKeyRsaBits = sshKeyRsaBits,
    sshKeyUrn = sshKey.urn,
    sshKeyAlgo = sshKeyAlgo
  )
}
