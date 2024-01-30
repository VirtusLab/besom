package besom.api.testing

import besom.*
import besom.internal.ArgsEncoder
import besom.internal.DummyContext
import besom.internal.RunResult.given
import besom.internal.RunOutput.{*, given}

// tests shapes available for codegen of libraries under besom.api
class ResourceConstructorTest extends munit.FunSuite {
  case class CustomStruct(a: String, b: Double) derives Decoder

  case class TestResourceArgs(property1: Output[Option[String]], property2: Output[Int]) derives ArgsEncoder
  object TestResourceArgs:
    def apply(using Context)(property1: Input.Optional[String], property2: Input[Int]): TestResourceArgs =
      TestResourceArgs(property1.asOptionOutput(), property2.asOutput())

  case class TestResource(
    urn: Output[URN],
    id: Output[ResourceId],
    property1: Output[Option[String]],
    property2: Output[Int],
    property3: Output[CustomStruct]
  ) extends CustomResource
      derives ResourceDecoder

  object TestResource:
    def apply(using
      Context
    )(
      name: NonEmptyString,
      args: TestResourceArgs,
      opts: ResourceOptsVariant.Custom ?=> CustomResourceOptions = CustomResourceOptions()
    ): Output[TestResource] =
      Output {
        val o = opts(using ResourceOptsVariant.Custom).resolve.unsafeRunSync()
        assert(o.protect, true) // just for testing that values are passed through
        new TestResource(
          urn = Output(URN.empty),
          id = Output(ResourceId("fakeId")),
          property1 = args.property1,
          property2 = args.property2,
          property3 = Output(CustomStruct("xyz", 1.0))
        )
      }

  test("resource constructor syntax for packages compiles and works as advertised") {
    given Context = DummyContext().unsafeRunSync()

    val resource = TestResource(
      name = "my-test-resource",
      args = TestResourceArgs(
        property1 = "abc",
        property2 = 123
      ),
      opts(protect = true)
    )

    resource.unsafeRunSync() match
      case Some(result) =>
        assertEquals(result.property1.unsafeRunSync().get, Some("abc"))
        assertEquals(result.property2.unsafeRunSync().get, 123)
        assertEquals(result.property3.unsafeRunSync().get, CustomStruct("xyz", 1.0))
      case None => fail("expected a resource")
  }

}
