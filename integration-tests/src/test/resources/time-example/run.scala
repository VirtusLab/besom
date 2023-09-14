//> using scala "3.3.0"
//> using lib "org.virtuslab::besom-core:0.0.1-SNAPSHOT"
//> using lib "org.virtuslab::besom-time:0.0.1-SNAPSHOT"

import besom.*
import besom.api.time.*

@main
def main(): Unit = Pulumi.run {

  val rotatingTime = rotating(
    name = "rotating-time",
    args = RotatingArgs(
      rotationMinutes = 1
    )
  )

  for
    _ <- rotatingTime
  yield Pulumi.exports(
    rotatingTime = rotatingTime.id
  )
}
