package besom.model

class FullyQualifiedStackNameTest extends munit.FunSuite:
  test("local name") {
    val stackName = FullyQualifiedStackName("my-project", "my-stack")
    assertEquals(stackName, "organization/my-project/my-stack")
  }

  test("full name") {
    val stackName = FullyQualifiedStackName("my-organization", "my-project", "my-stack")
    assertEquals(stackName, "my-organization/my-project/my-stack")
  }
