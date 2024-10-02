package besom.internal

import besom.types.URN

class OutputDataTest extends munit.FunSuite:

  test("unknown values short circuit") {
    val data = OutputData.unknown(false)

    var called = false

    data.map { _ =>
      called = true
      ()
    }

    assert(!called, "map lambda should not be called on unknown values")
  }

  test("unknown values keep secrecy") {
    val data       = OutputData.unknown(true)
    val derivative = data.map(_ => ()).flatMap(_ => OutputData(()))

    assert(derivative.secret, "unknown values should keep secrecy")
  }

  test("unknown values propagate dependent resources") {
    val urn = URN(
      "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
    )
    val res        = DependencyResource(Output.pure(urn))
    val data       = OutputData.unknown(false, resources = Set(res))
    val derivative = data.map(_ => ()).flatMap(_ => OutputData(()))

    assertEquals(derivative.getResources, Set(res), "unknown values should propagate dependent resources")
  }

  test("known values propagate secrecy") {
    val data       = OutputData("foo", isSecret = true)
    val derivative = data.map(_ => ()).flatMap(_ => OutputData(()))

    assert(derivative.secret, "known values should propagate secrecy")
  }

  test("known values propagate dependent resources") {
    val urn = URN(
      "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
    )
    val res        = DependencyResource(Output.pure(urn))
    val data       = OutputData("foo", resources = Set(res))
    val derivative = data.map(_ => ()).flatMap(_ => OutputData(()))

    assertEquals(derivative.getResources, Set(res), "known values should propagate dependent resources")
  }

  test("zip operator propagates properties and dependencies") {
    val urn = URN(
      "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
    )
    val res1       = DependencyResource(Output.pure(urn))
    val res2       = DependencyResource(Output.pure(urn))
    val data1      = OutputData("foo", resources = Set(res1), isSecret = true)
    val data2      = OutputData("bar", resources = Set(res2))
    val derivative = data1.zip(data2)

    assertEquals(derivative.getResources, Set(res1, res2), "zip operator should propagate dependent resources")
    assert(derivative.secret, "zip operator should propagate secrecy")
  }

  test("map operator propagates properties and dependencies") {
    val urn = URN(
      "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
    )
    val res1       = DependencyResource(Output.pure(urn))
    val data       = OutputData("foo", resources = Set(res1), isSecret = true)
    val derivative = data.map(_ => "bar")

    assertEquals(derivative.getResources, Set(res1), "map operator should propagate dependent resources")
    assert(derivative.secret, "map operator should propagate secrecy")
  }

  test("flatMap operator propagates properties and dependencies") {
    val urn = URN(
      "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"
    )
    val res1       = DependencyResource(Output.pure(urn))
    val res2       = DependencyResource(Output.pure(urn))
    val data       = OutputData("foo", resources = Set(res1), isSecret = true)
    val derivative = data.flatMap(_ => OutputData("bar", resources = Set(res2)))

    assertEquals(derivative.getResources, Set(res1, res2), "flatMap operator should propagate dependent resources")
    assert(derivative.secret, "flatMap operator should propagate secrecy")
  }

end OutputDataTest
