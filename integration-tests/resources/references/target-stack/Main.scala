import besom.*
import besom.api.tls
import besom.internal.StackReferenceResourceOptions

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  val sourceStackName = config.requireString("sourceStack").map(NonEmptyString(_).get)
  val sourceStack = besom.StackReference(
    "stackRef",
    StackReferenceArgs(sourceStackName),
    StackReferenceResourceOptions()
  )
  val sshKeyUrn = sourceStack.map(_.getOutput("sshKeyUrn").map(URN(_.toString)))

  val fetchedResource =
    for u <- sshKeyUrn
    yield tls.PrivateKey(
      "sshKey",
      EmptyArgs(),
      CustomResourceOptions(urn = u)
    )

  Output {
    exports(
      theSshKeyUrn = fetchedResource.urn
    )
  }
}
