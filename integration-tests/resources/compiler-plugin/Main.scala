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
