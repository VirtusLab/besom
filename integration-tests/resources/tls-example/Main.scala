import besom.*
import besom.api.tls
import besom.api.tls.GetPublicKeyResult

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  val sshKey = tls.PrivateKey(
    "sshKey",
    tls.PrivateKeyArgs(
      algorithm = "RSA",
      rsaBits = 4096
    )
  )

  val public1: Output[GetPublicKeyResult] = tls.getPublicKey(
    tls.GetPublicKeyArgs(
      privateKeyOpenssh = sshKey.privateKeyOpenssh
    )
  )

  val sanityCheck = for
    p  <- sshKey.publicKeyOpenssh
    p1 <- public1.publicKeyOpenssh
  yield require(p.trim == p1.trim)

  Stack(sanityCheck).exports(
    p = sshKey.publicKeyOpenssh.map(_.trim),
    p1 = public1.publicKeyOpenssh.map(_.trim)
  )
}
