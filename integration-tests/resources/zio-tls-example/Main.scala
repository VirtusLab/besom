import zio.*
import besom.zio.*
import besom.api.tls.*

@main
def main(): Unit = Pulumi.run {

  val algorithm = Output.eval(ZIO.succeed("ECDSA"))

  // verifying interruption semantics - we ignore interruption
  val interruptedIOOutput = Output.eval(ZIO.succeed("Don't interrupt me")).flatMap { _ =>
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
      ecdsaCurve = "P384"
    )
  )

  val k = for
    alg <- algorithm
    _   <- interruptedIOOutput
    k   <- key(alg)
  yield k

  Stack.exports(
    privateKey = k.map(_.id)
  )
}
