import besom.*
import besom.api.random.*
import besom.internal.SpecResourceAlias

@main
def main(): Unit = Pulumi.run {
  val oldName = "random-string"
  val newName = "random-string-2"

  val testAlias = sys.env.getOrElse("TEST_ALIAS", "false").toBoolean

  def strOutput = RandomString(
    name = if (testAlias) newName else oldName,
    args = RandomStringArgs(
      length = 10
    ),
    opts = opts(aliases = if (testAlias) List(SpecResourceAlias(name = Some(oldName))) else List.empty)
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
