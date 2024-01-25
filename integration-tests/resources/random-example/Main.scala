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

  Stack(
    strOutput,
    strOutput // checking memoization
  ).exports(
    randomString = str.map(_.result),
    resourceName = str.map(_.pulumiResourceName),
    org = Pulumi.pulumiOrganization,
    proj = Pulumi.pulumiProject,
    stack = Pulumi.pulumiStack
  )
}
