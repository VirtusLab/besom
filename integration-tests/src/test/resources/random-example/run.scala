//> using scala "3.3.0"
//> using lib "org.virtuslab::besom-core:0.0.1-SNAPSHOT"
//> using lib "org.virtuslab::besom-random:0.0.1-SNAPSHOT"

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
    _ <- strOutput
  yield Pulumi.exports(
    randomString = strOutput.result
  )
}
