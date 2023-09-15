//> using scala "3.3.0"
//> using lib "org.typelevel::cats-effect:3.5.1"
//> using lib "org.virtuslab::besom-cats:0.0.1-SNAPSHOT"
//> using lib "org.virtuslab::besom-purrl:0.0.1-SNAPSHOT"

import cats.effect.*
import besom.cats.{*, given}
import besom.api.purrl.*

@main
def main(): Unit = Pulumi.run {

  val url = Output.eval(IO("https://httpbin.org/get"))

  def purrlCommand(url: String) = purrl(
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
    command <- purrlCommand(urlValue)
  yield Pulumi.exports(
    purrlCommand = command.response
  )
}
