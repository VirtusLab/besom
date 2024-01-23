import besom.*

@main
def main = Pulumi.run {
  val name: Output[String] = Output("Biden")

  val stringOut = Output(
    s"Joe ${name}"
  )

  Stack.exports(stringOut = stringOut)
}
