//> using scala "3.3.0"
//> using lib "dev.zio::zio:2.0.17"
//> using lib "org.virtuslab::besom-zio:0.0.1-SNAPSHOT"
//> using lib "org.virtuslab::besom-tls:4.10.0-SNAPSHOT.0.0.1"

import zio.*
import besom.zio.*
import besom.api.tls.*

@main
def main(): Unit = Pulumi.run {

  val algorithm = Output.eval[Task, String](ZIO.succeed("ECDSA"))

  val interruptedIOOutput = Output.eval[Task, String](ZIO.succeed("Don't interrupt me")).flatMap[Task, String] { _ =>
    for 
      fib <- (ZIO.sleep(3.seconds) *> ZIO.succeed("xd")).fork
      _   <- (ZIO.sleep(1.second) *> fib.interrupt).fork
      res <- fib.join
    yield res
  }

  def key(algorithm: String) = PrivateKey(
    name = "my-private-key",
    args = PrivateKeyArgs(
      algorithm = algorithm,
      ecdsaCurve = "P384",
    )
  )

  for
    alg <- algorithm
    _ <- interruptedIOOutput
    k <- key(alg)
  yield Pulumi.exports(
    privateKey = k.id
  )
}
