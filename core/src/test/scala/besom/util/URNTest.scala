package besom.util

import besom.util.Types.URN

class URNTest extends munit.FunSuite with CompileAssertions:

  val exampleString =
    "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
  val example = URN(
    "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
  )

  test("URN.apply should only work for correct URNs") {
    failsToCompile("""
      import besom.util.Types.URN
      URN("well::it's::not::a::urn")
      """)

    compiles("""
      import besom.util.Types.URN
      URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource")
      """)
  }

  test("runtime URN should parse correctly") {
    val maybeParsed = URN.from(exampleString)
    assert(maybeParsed.isSuccess)
    assert(maybeParsed.get == example)
    assert(URN.from("wrong").isFailure)
  }

  test("URN should expose correct values") {
    assert(example.stack == "stack")
    assert(example.project == "project")
    assert(example.parentType == "custom:resources:Resource")
    assert(example.resourceType == "besom:testing/test:Resource")
    assert(example.resourceName == "my-test-resource")
  }
