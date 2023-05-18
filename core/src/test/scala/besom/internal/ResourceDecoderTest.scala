package besom.internal

import RunResult.{given, *}
import com.google.protobuf.struct.*
import ProtobufUtil.*

class ResourceDecoderTest extends munit.FunSuite:
  case class TestResource(
    urn: Output[String],
    id: Output[String],
    property1: Output[Option[String]],
    property2: Output[Int]
  ) extends CustomResource derives ResourceDecoder


  test("resource resolver - happy path") {
    val resourceDecoder = summon[ResourceDecoder[TestResource]]

    given Context = DummyContext().unsafeRunSync()

    val dependencyResource = new Resource:
      override def urn = ???

    val errorOrResourceResult = Right(RawResourceResult(
      urn = "fakeUrn",
      id = Some("fakeId"),
      data = Struct(Map(
        "property1" -> Some("abc").asValue,
        "property2" -> 123.asValue
      )),
      dependencies = Map(
        "property1" -> Set.empty,
        "property2" -> Set(dependencyResource)
      )
    ))

    val (resource, resourceResolver) = resourceDecoder.makeResolver.unsafeRunSync()

    resourceResolver.resolve(errorOrResourceResult).unsafeRunSync()

    def checkOutput[A](output: Output[A])(expectedValue: A, expectedDependencies: Set[Resource], expectedIsSecret: Boolean) =
      (output.getData.unsafeRunSync(): @unchecked) match
      case OutputData.Known(dependencies, isSecret, Some(value)) =>
        assert(clue(value) == clue(expectedValue))
        assert(clue(dependencies) == clue(expectedDependencies))
        assert(clue(isSecret) == clue(expectedIsSecret)) // TODO: test some output with isSecret = true? 

    checkOutput(resource.urn)("fakeUrn", Set(resource), false)
    checkOutput(resource.id)("fakeId", Set(resource), false)
    checkOutput(resource.property1)(Some("abc"), Set(resource), false)
    checkOutput(resource.property2)(123, Set(resource, dependencyResource), false)
  }
