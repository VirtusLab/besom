//> using scala 3.3.1
//> using plugin "org.virtuslab::besom-compiler-plugin:0.0.2-SNAPSHOT"
//> using lib "org.virtuslab::besom-core:0.0.2-SNAPSHOT"

import besom.*

@main
def main = Pulumi.run {
  val name: Output[String] = Output("Biden")

  val stringOut = Output(
    s"Joe ${name}"
  )

  for
    _ <- stringOut
  yield
    Pulumi.exports(
      stringOut = stringOut
    )
}
