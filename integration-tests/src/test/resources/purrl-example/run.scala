//> using scala "3.3.0"
//> using lib "org.typelevel::cats-effect:3.5.1"
//> using lib "org.virtuslab::besom-cats:0.0.2-SNAPSHOT"
//> using lib "org.virtuslab::besom-purrl:0.4.1-core.0.0.2-SNAPSHOT"

import cats.effect.*
import cats.effect.kernel.Outcome.*
import besom.cats.*
import besom.api.purrl.*
import scala.concurrent.duration.*

@main
def main(): Unit = Pulumi.run {

  val url = Output.eval(IO("https://httpbin.org/get"))

  val cancelledIOOutput1 = Output.eval(IO("Don't cancel me")).flatMap { _ =>
    IO.canceled
  }

  val cancelledIOOutput2 = Output.eval(IO("Don't cancel me")).flatMap { _ =>
    for 
      fib <- (IO.sleep(3.seconds) *> IO("A valid result")).uncancelable.start
      _   <- (IO.sleep(1.second) *> fib.cancel).start
      res <- fib.join
        .flatMap {
          case Succeeded(fa) => fa
          case Errored(e)    => IO.raiseError(e)
          case Canceled()    => IO.raiseError(new Exception("Unexpected cancelation!"))
        }
    yield res
  }

  def purrlCommand(url: String) = Purrl(
    name = "purrl",
    args = PurrlArgs(
      name = "purrl",
      url = url,
      method = "GET",
      headers = Map(
        "test" -> "test"
      ),
      responseCodes = List(
        "200"
      ),
      deleteMethod = "DELETE",
      deleteUrl = "https://httpbin.org/delete",
      deleteResponseCodes = List(
        "200"
      ),
    )
  )

  for
    urlValue <- url
    _ <- cancelledIOOutput1
    out2 <- cancelledIOOutput2
    _ = assert(out2 == "A valid result")
    command <- purrlCommand(urlValue)
  yield Pulumi.exports(
    purrlCommand = command.response
  )
}
