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

  val issuerSpec = issuer.v1.Spec(
    venafi = None,
    ca = None,
    acme = None,
    selfSigned = Some(issuer.v1.spec.SelfSigned(Some(Seq("test")))),
    vault = None
  )

  val cr = k8s.apiextensions.CustomResource[issuer.v1.Spec](
    "test-cr",
    k8s.apiextensions.CustomResourceArgs(
      apiVersion = "cert-manager.io/v1",
      kind = "Issuer",
      spec = Output(issuerSpec)
    ),
    opts(dependsOn = certManager)
  )

  Stack.exports(
    crlDistributionPoints = cr.spec.map(_.selfSigned.get.crlDistributionPoints.get)
  )
}
