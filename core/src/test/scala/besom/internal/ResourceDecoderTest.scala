package besom.internal

import RunResult.{given, *}
import com.google.protobuf.struct.*
import ProtobufUtil.*
import besom.util.*
import besom.types.*
import logging.*

class ResourceDecoderTest extends munit.FunSuite:
  case class CustomStruct(a: String, b: Double) derives Decoder

  case class TestResource(
    urn: Output[URN],
    id: Output[ResourceId],
    property1: Output[Option[String]],
    property2: Output[Int],
    property3: Output[CustomStruct]
  ) extends CustomResource
      derives ResourceDecoder

  val label        = Label.fromNameAndType("test", "test:pkg:TestResource")
  given MDC[Label] = logging.MDC(Key.LabelKey, label)

  def checkOutput[A](
    output: Output[A]
  )(expectedValue: A, expectedDependencies: Set[Resource], expectedIsSecret: Boolean) =
    (output.getData.unsafeRunSync(): @unchecked) match
      case OutputData.Known(dependencies, isSecret, Some(value)) =>
        assert(clue(value) == clue(expectedValue))
        assert(clue(dependencies) == clue(expectedDependencies))
        assert(clue(isSecret) == clue(expectedIsSecret)) // TODO: test some output with isSecret = true?
      case obtained => fail(s"Expected OutputData.Known", clues(expectedValue, obtained.getValue))

  def assertDecodingError[A](output: Output[A]) =
    try
      output.getData.unsafeRunSync()
      fail("Expected failed output")
    catch
      case de: besom.internal.DecodingError =>
      case t: Throwable                     => fail(s"Expected DecodingError", clues(t))

  test("resource resolver - happy path") {
    val resourceDecoder = summon[ResourceDecoder[TestResource]]

    given Context = DummyContext().unsafeRunSync()

    val dependencyResource = DependencyResource(Output(URN.empty))

    val errorOrResourceResult = Right(
      RawResourceResult(
        urn = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"),
        id = Some(ResourceId("fakeId")),
        data = Struct(
          Map(
            "property1" -> Some("abc").asValue,
            "property2" -> 123.asValue,
            "property3" -> Map(
              "a" -> "xyz".asValue,
              "b" -> 3.0d.asValue
            ).asValue
          )
        ),
        dependencies = Map(
          "property1" -> Set.empty,
          "property2" -> Set(dependencyResource)
        )
      )
    )

    val (resource, resourceResolver) = resourceDecoder.makeResolver.unsafeRunSync()

    resourceResolver.resolve(errorOrResourceResult).unsafeRunSync()

    checkOutput(resource.urn)(
      "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource",
      Set(resource),
      false
    )
    checkOutput(resource.id)("fakeId", Set(resource), false)
    checkOutput(resource.property1)(Some("abc"), Set(resource), false)
    checkOutput(resource.property2)(123, Set(resource, dependencyResource), false)
    checkOutput(resource.property3)(CustomStruct("xyz", 3.0d), Set(resource), false)
  }

  test("resource resolver - missing required property") {
    val resourceDecoder = summon[ResourceDecoder[TestResource]]

    given Context = DummyContext().unsafeRunSync()

    val errorOrResourceResult = Right(
      RawResourceResult(
        urn = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"),
        id = Some(ResourceId("fakeId")),
        data = Struct(
          Map(
            "property1" -> Some("abc").asValue,
            "property2" -> 123.asValue
            // property3 is missing
          )
        ),
        dependencies = Map(
          "property1" -> Set.empty,
          "property2" -> Set.empty
        )
      )
    )

    val (resource, resourceResolver) = resourceDecoder.makeResolver.unsafeRunSync()

    resourceResolver.resolve(errorOrResourceResult).unsafeRunSync()

    checkOutput(resource.urn)(
      "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource",
      Set(resource),
      false
    )
    checkOutput(resource.id)("fakeId", Set(resource), false)
    checkOutput(resource.property1)(Some("abc"), Set(resource), false)
    checkOutput(resource.property2)(123, Set(resource), false)
    assertDecodingError(resource.property3)
  }

  test("resource resolver - required property received as NullValue, issue #150") {
    val resourceDecoder = summon[ResourceDecoder[TestResource]]

    given Context = DummyContext().unsafeRunSync()

    val dependencyResource = DependencyResource(Output(URN.empty))

    val errorOrResourceResult = Right(
      RawResourceResult(
        urn = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource"),
        id = Some(ResourceId("fakeId")),
        data = Struct(
          Map(
            "property1" -> Some("abc").asValue,
            "property2" -> 123.asValue,
            "property3" -> Null
          )
        ),
        dependencies = Map(
          "property1" -> Set.empty,
          "property2" -> Set(dependencyResource)
        )
      )
    )

    val (resource, resourceResolver) = resourceDecoder.makeResolver.unsafeRunSync()

    resourceResolver.resolve(errorOrResourceResult).unsafeRunSync()

    checkOutput(resource.urn)(
      "urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource",
      Set(resource),
      false
    )
    checkOutput(resource.id)("fakeId", Set(resource), false)
    checkOutput(resource.property1)(Some("abc"), Set(resource), false)
    checkOutput(resource.property2)(123, Set(resource, dependencyResource), false)
    assertDecodingError(resource.property3)
  }
