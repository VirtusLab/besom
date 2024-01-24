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

  // verifying cancelation semantics - we ignore cancelation
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

  def purrlCommand(url: Output[String]) = Purrl(
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
      )
    )
  )

  Stack(
    cancelledIOOutput1,
    cancelledIOOutput2.map(out => assert(out == "A valid result"))
  ).exports(
    purrlCommand = purrlCommand(url).response
  )
}
