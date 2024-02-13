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

  val secret0 = k8s.core.v1.Secret(
    "test-secret0",
    k8s.core.v1.SecretArgs(
      metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
        name = "test-secret0",
        namespace = Output.secret[Option[String]](None)
      )
    ),
    opts = opts(provider = provider)
  )

  val secret1 = k8s.core.v1.Secret(
    "test-secret1",
    k8s.core.v1.SecretArgs(
      metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
        name = "test-secret1",
        namespace = "test"
      ),
      // will be promoted to "data" by the provider
      stringData = Map(
        "username" -> "test-user",
        "password" -> "test-password"
      )
    ),
    opts = opts(provider = provider)
  )

  val secret2 = k8s.core.v1.Secret(
    "test-secret2",
    k8s.core.v1.SecretArgs(
      metadata = k8s.meta.v1.inputs.ObjectMetaArgs(
        name = "test-secret2",
        namespace = "test"
      ),
      data = Map(
        "username" -> "dGVzdC11c2Vy",
        "password" -> "dGVzdC1wYXNzd29yZA=="
      )
    ),
    opts = opts(provider = provider)
  )

  Stack(secret0, secret1, secret2)
}
