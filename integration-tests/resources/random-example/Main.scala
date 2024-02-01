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

  val str = strOutput

  // tests whether Get Functions feature work!
  val fetchedString = RandomString.get(
    name = "random-string-xd",
    id = str.id
  )

  val assertion = (str zip fetchedString).flatMap { case (a, b) => a.result zip b.result }.flatMap { case (a, b) =>
    assert(a == b, "fetched string should be the same as the created one")
    log.info(s"random-example: assertion passed: $a == $b")
  }

  Stack(
    strOutput,
    strOutput, // checking memoization
    assertion
  ).exports(
    randomString = str.map(_.result),
    resourceName = str.map(_.pulumiResourceName),
    org = Pulumi.pulumiOrganization,
    proj = Pulumi.pulumiProject,
    stack = Pulumi.pulumiStack
  )
}
