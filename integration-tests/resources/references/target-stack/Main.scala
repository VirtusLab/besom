import besom.*
import besom.api.tls
import besom.internal.StackReferenceResourceOptions
import spray.json.*

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  val sourceStackName = config.requireString("sourceStack").map(NonEmptyString(_).get)
  val sourceStack = besom.StackReference(
    "stackRef",
    StackReferenceArgs(sourceStackName),
    StackReferenceResourceOptions()
  )

  val sshKeyUrn = sourceStack.flatMap(
    _.getOutput("sshKeyUrn")
      .map {
        case Some(JsString(s)) => s
        case other             => throw RuntimeException(s"Expected string, got $other")
      }
      .flatMap(URN.parse(_))
  )

  val fetchedResource =
    for
      u <- sshKeyUrn
      fetched <- tls.PrivateKey(
        "sshKey",
        tls.PrivateKeyArgs(
          algorithm = "RSA",
          rsaBits = 4096
        ),
        CustomResourceOptions(urn = u)
      )
    yield fetched

  Output {
    exports(
      theSshKeyUrn = fetchedResource.urn
    )
  }
}
