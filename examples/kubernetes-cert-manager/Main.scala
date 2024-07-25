import besom.*
import besom.api.kubernetes as k8s
import besom.api.kubernetescertmanager.*

@main def main = Pulumi.run {
  val certManager = CertManager(
    "cert-manager",
    CertManagerArgs(
      installCRDs = true,
      helmOptions = inputs.ReleaseArgs(
        chart = "cert-manager",
        version = "v1.5.3"
      )
    )
  )

  val issuerSpec = issuer.v1.Issuer(
    selfSigned = issuer.v1.SelfSigned(Some(Seq("test")))
  )

  val cr = k8s.apiextensions.CustomResource[issuer.v1.Issuer](
    "test-cr",
    k8s.apiextensions.CustomResourceArgs(
      apiVersion = "cert-manager.io/v1",
      kind = "Issuer",
      spec = issuerSpec
    ),
    opts(dependsOn = certManager)
  )

  Stack.exports(
    crlDistributionPoints = cr.spec.map(_.selfSigned.map(_.get.crlDistributionPoints))
  )
}
