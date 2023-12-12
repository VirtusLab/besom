package besom.util

import besom.types.*

class URNTest extends munit.FunSuite with CompileAssertions:

  val exampleResourceString =
    "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"

  val exampleNestedResourceString = URN(
    "urn:pulumi:stack::project::some:happy:component$custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
  )

  val exampleStackString =
    "urn:pulumi:stack::project::pulumi:pulumi:Stack::stack-name"

  val shortResourceTypeUrnString =
    URN("urn:pulumi:stack::project::some:happy:component$custom:Resource$besom:Resource::my-test-resource") // two segments in resourceType

  val example = URN(
    "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
  )

  val exampleStack = URN(
    "urn:pulumi:stack::project::pulumi:pulumi:Stack::stack-name"
  )

  test("URN.apply should only work for correct URNs") {
    failsToCompile("""
      import besom.types.URN
      URN("well::it's::not::a::urn")
      """)

    compiles("""
      import besom.types.URN
      URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource")
      """)
  }

  test("runtime URN should parse correctly") {
    val maybeParsed = URN.from(exampleResourceString)
    assert(maybeParsed.isSuccess)
    assert(maybeParsed.get == example)
    assert(URN.from("wrong").isFailure)
  }

  test("URN should expose stack correctly") {
    assert(example.stack == "stack", s"Expected stack to be 'stack', got ${example.stack}")
  }

  test("URN should expose project correctly") {
    assert(example.project == "project", s"Expected project to be 'project', got ${example.project}")
  }

  test("URN should expose parentType correctly") {
    assert(
      example.parentType == Vector("custom:resources:Resource"),
      s"""Expected parentType to be 'Vector("custom:resources:Resource"), got ${example.parentType}"""
    )
  }

  test("URN should expose resourceType correctly") {
    assert(
      example.resourceType == "besom:testing/test:Resource",
      s"Expected resourceType to be 'besom:testing/test:Resource', got ${example.resourceType}"
    )
  }

  test("URN should expose resourceName correctly") {
    assert(
      example.resourceName == "my-test-resource",
      s"Expected resourceName to be 'my-test-resource', got ${example.resourceName}"
    )
  }

  test("URN should expose stack correctly for stacks") {
    assert(exampleStack.stack == "stack", s"Expected stack to be 'stack', got ${exampleStack.stack}")
  }

  test("URN should expose project correctly for stacks") {
    assert(exampleStack.project == "project", s"Expected project to be 'project', got ${exampleStack.project}")
  }

  test("URN should expose parentType correctly for stacks") {
    assert(
      exampleStack.parentType.isEmpty,
      s"Expected parentType to be empty, got ${exampleStack.parentType}"
    )
  }

  test("URN should expose resourceType correctly for stacks") {
    assert(
      exampleStack.resourceType == "pulumi:pulumi:Stack",
      s"Expected resourceType to be 'pulumi:pulumi:Stack', got ${exampleStack.resourceType}"
    )
  }

  test("URN should expose resourceName correctly for stacks") {
    assert(
      exampleStack.resourceName == "stack-name",
      s"Expected resourceName to be 'stack-name', got ${exampleStack.resourceName}"
    )
  }

  test("URN should expose parentType correctly for nested resources") {
    assert(
      exampleNestedResourceString.parentType == Vector("some:happy:component", "custom:resources:Resource"),
      s"""Expected parentType to be Vector("some:happy:component", "custom:resources:Resource"), got ${exampleNestedResourceString.parentType}"""
    )
  }

  test("URN should expose resourceType correctly for nested resources") {
    assert(
      exampleNestedResourceString.resourceType == "besom:testing/test:Resource",
      s"Expected resourceType to be 'besom:testing/test:Resource', got ${exampleNestedResourceString.resourceType}"
    )
  }

  test("URN should expose resourceName correctly for nested resources") {
    assert(
      exampleNestedResourceString.resourceName == "my-test-resource",
      s"Expected resourceName to be 'my-test-resource', got ${exampleNestedResourceString.resourceName}"
    )
  }

  test("URN should expose parentType correctly for short resource types") {
    assert(
      shortResourceTypeUrnString.parentType == Vector("some:happy:component", "custom:Resource"),
      s"""Expected parentType to be Vector("custom:resources:Resource"), got ${shortResourceTypeUrnString.parentType}"""
    )
  }

  test("URN should expose resourceType correctly for short resource types") {
    assert(
      shortResourceTypeUrnString.resourceType == "besom:Resource",
      s"Expected resourceType to be 'besom:Resource', got ${shortResourceTypeUrnString.resourceType}"
    )
  }

  test("URN should expose resourceName correctly for short resource types") {
    assert(
      shortResourceTypeUrnString.resourceName == "my-test-resource",
      s"Expected resourceName to be 'my-test-resource', got ${shortResourceTypeUrnString.resourceName}"
    )
  }

end URNTest
