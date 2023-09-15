//> using scala "3.3.0"
//> using lib "dev.zio::zio:2.0.16"
//> using lib "org.virtuslab::besom-zio:0.0.1-beta"
//> using lib "org.virtuslab::besom-tls:4.10.0-beta.0.0.1"

import zio.*
import besom.zio.{*, given }
import besom.api.tls.*

@main
def main(): Unit = Pulumi.run {

  val algorithm = Output.eval[Task, String](ZIO.succeed("ECDSA"))

  def key(algorithm: String) = privateKey(
    name = "my-private-key",
    args = PrivateKeyArgs(
      algorithm = algorithm,
      ecdsaCurve = "P384",
    )
  )
  for
    alg <- algorithm
    k <- key(alg)
  yield Pulumi.exports(
    privateKey = k.id
  )
}
