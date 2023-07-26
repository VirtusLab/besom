package besom.util

import besom.util.Types.URN

class URNTest extends munit.FunSuite with CompileAssertions:

  val exampleResourceString =
    "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"

  val exampleStackString =
    "urn:pulumi:stack::project::pulumi:pulumi:Stack::stack-name"

  val example = URN(
    "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
  )

  val exampleStack = URN(
    "urn:pulumi:stack::project::pulumi:pulumi:Stack::stack-name"
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
    val maybeParsed = URN.from(exampleResourceString)
    assert(maybeParsed.isSuccess)
    assert(maybeParsed.get == example)
    assert(URN.from("wrong").isFailure)
  }

  test("URN should expose correct values") {
    assert(example.stack == "stack", s"Expected stack to be 'stack', got ${example.stack}")
    assert(example.project == "project", s"Expected project to be 'project', got ${example.project}")
    assert(
      example.parentType == "custom:resources:Resource",
      s"Expected parentType to be 'custom:resources:Resource', got ${example.parentType}"
    )
    assert(
      example.resourceType.get == "besom:testing/test:Resource",
      s"Expected resourceType to be 'besom:testing/test:Resource', got ${example.resourceType}"
    )
    assert(
      example.resourceName == "my-test-resource",
      s"Expected resourceName to be 'my-test-resource', got ${example.resourceName}"
    )

    assert(exampleStack.stack == "stack", s"Expected stack to be 'stack', got ${exampleStack.stack}")
    assert(exampleStack.project == "project", s"Expected project to be 'project', got ${exampleStack.project}")
    assert(
      exampleStack.parentType == "pulumi:pulumi:Stack",
      s"Expected parentType to be 'pulumi:pulumi:Stack', got ${exampleStack.parentType}"
    )
    assert(
      exampleStack.resourceType.isEmpty,
      s"Expected resourceType to be empty, got ${exampleStack.resourceType.get}"
    )
    assert(
      exampleStack.resourceName == "stack-name",
      s"Expected resourceName to be 'stack-name', got ${exampleStack.resourceName}"
    )
  }
