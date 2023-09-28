//> using scala "3.3.0"
//> using lib "org.virtuslab::besom-core:0.1.1-SNAPSHOT"
//> using lib "org.virtuslab::besom-random:4.13.2-core.0.1.1-SNAPSHOT"

import besom.*
import besom.api.random.*

@main
def main(): Unit = Pulumi.run {

  def strOutput = RandomString(
    name = "random-string",
    args = RandomStringArgs(
      length = 10
    )
  )

  for
    str  <- strOutput
    str2 <- strOutput // checks memoization too
  yield Pulumi.exports(
    randomString = str.result
  )
}
