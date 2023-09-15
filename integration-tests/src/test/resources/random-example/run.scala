//> using scala "3.3.0"
//> using lib "org.virtuslab::besom-core:0.0.1-beta"
//> using lib "org.virtuslab::besom-random:4.13.2-beta.0.0.1"

import besom.*
import besom.api.random.*

@main
def main(): Unit = Pulumi.run {

  val strOutput = randomString(
    name = "random-string",
    args = RandomStringArgs(
      length = 10
    )
  )

  for
    str <- strOutput
  yield Pulumi.exports(
    randomString = str.result
  )
}
