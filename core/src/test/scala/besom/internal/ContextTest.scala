package besom.internal

import besom.types.{Output => _, *}

class ContextTest extends munit.FunSuite:

  case class TestResource(urn: Output[URN], id: Output[ResourceId], url: Output[String]) extends CustomResource
  case class AnotherTestResource(urn: Output[URN], id: Output[ResourceId], url: Output[String]) extends CustomResource

  test("resource identity - empty outputs, same class") {
    val v1 = TestResource(Output.empty(), Output.empty(), Output.empty())
    val v2 = TestResource(Output.empty(), Output.empty(), Output.empty())

    assert(v1 != v2)
    assert(v1 == v1)
    assert(v2 == v2)
  }
