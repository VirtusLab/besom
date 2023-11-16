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
  yield exports(
    randomString = str.result,
    resourceName = str.pulumiResourceName,
    org = Pulumi.pulumiOrganization,
    proj = Pulumi.pulumiProject,
    stack = Pulumi.pulumiStack
  )
}
