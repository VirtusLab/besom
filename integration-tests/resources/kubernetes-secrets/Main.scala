import besom.*
import besom.api.kubernetes as k8s

@main def main = Pulumi.run {
  val provider = k8s.Provider(
    "k8s",
    k8s.ProviderArgs(
      renderYamlToDirectory = "output",
      cluster = Output.secret[Option[String]](None)
    )
  )

  val secret1 = k8s.core.v1.Secret(
    "test-secret1",
    k8s.core.v1.SecretArgs(
      metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
        name = "test-secret1",
        namespace = Output.secret[Option[String]](None)
      ),
//      data = Map(), // WORKAROUND
      stringData = Map(
        "username" -> "test-user",
        "password" -> "test-password"
      )
    ),
    opts = opts(provider = provider)
  )

/*  val secret2 = k8s.core.v1.Secret(
    "test-secret2",
    k8s.core.v1.SecretArgs(
      metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
        name = "test-secret2"
      ),
      data = Map(
        "username" -> "dGVzdC11c2Vy",
        "password" -> "dGVzdC1wYXNzd29yZA=="
      ),
      stringData = Map() // WORKAROUND
    ),
    opts = opts(provider = provider)
  )

  val secret3 = k8s.core.v1.Secret(
    "test-secret3",
    k8s.core.v1.SecretArgs(
      metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
        name = "test-secret3"
      ),
      data = Map(), // WORKAROUND
      stringData = Map() // WORKAROUND
    ),
    opts = opts(provider = provider)
  )*/

  Stack(secret1/*, secret2, secret3*/)
}
