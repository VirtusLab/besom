package besom.internal

import RunResult.{given, *}
import besom.types.{Output => _, *}
import besom.internal.ProtobufUtil.*
import com.google.protobuf.struct.*, Value.Kind

class ExportsTest extends munit.FunSuite with ValueAssertions:

  test("exports alone work as intended") {
    given Context = DummyContext().unsafeRunSync()

    val stackOutputs = Stack.exports(foo = Output("bar")).getExports.result.unsafeRunSync()

    val encoded = Value(Kind.StructValue(stackOutputs))

    val expected = Map(
      "foo" -> "bar".asValue
    ).asValue

    assertEqualsValue(encoded, expected)
  }

  test("stack dependencies work as intended") {
    given Context = DummyContext().unsafeRunSync()

    val atomicBoolean = new java.util.concurrent.atomic.AtomicBoolean(false)

    val stackDeps = Stack(Output("foo"), Output("bar"), Output(Result.defer { atomicBoolean.set(true); "baz" })).getDependsOn

    assertEquals(stackDeps.size, 3)

    val sequencedDataOfAll = Output.sequence(stackDeps).getData.unsafeRunSync()

    sequencedDataOfAll.getValue match
      case Some(vec: Vector[String] @unchecked) =>
        assert(vec.size == 3)
        assertEquals(vec(0), "foo")
        assertEquals(vec(1), "bar")
        assertEquals(vec(2), "baz")
        assert(atomicBoolean.get(), "deferred value should be evaluated")
      case x => fail(s"sequencedDataOfAll - Value should not be $x")
  }

  test("exports with dependencies work as intended") {
    given Context = DummyContext().unsafeRunSync()

    val stack =
      Stack(Output("foo"), Output("bar"))
        .exports(baz = Output("baz"), qux = Output(Result.defer { "qux" }))

    val stackOutputs = stack.getExports.result.unsafeRunSync()

    val encoded = Value(Kind.StructValue(stackOutputs))

    val expected = Map(
      "baz" -> "baz".asValue,
      "qux" -> "qux".asValue
    ).asValue

    assertEqualsValue(encoded, expected)

    val stackDeps = stack.getDependsOn

    assertEquals(stackDeps.size, 2)

    val sequencedDataOfAll = Output.sequence(stackDeps).getData.unsafeRunSync()

    sequencedDataOfAll.getValue match
      case Some(vec: Vector[String] @unchecked) =>
        assert(vec.size == 2)
        assertEquals(vec(0), "foo")
        assertEquals(vec(1), "bar")
      case x => fail(s"sequencedDataOfAll - Value should not be $x")
  }

  test("multiple export clauses aggregate instead of replacing exported values") {
    given Context = DummyContext().unsafeRunSync()

    val stack =
      Stack
        .exports(foo = Output("foo"), bar = Output("bar"))
        .exports(baz = Output("baz"), qux = Output("qux"))

    val stackOutputs = stack.getExports.result.unsafeRunSync()

    val encoded = Value(Kind.StructValue(stackOutputs))

    val expected = Map(
      "foo" -> "foo".asValue,
      "bar" -> "bar".asValue,
      "baz" -> "baz".asValue,
      "qux" -> "qux".asValue
    ).asValue

    assertEqualsValue(encoded, expected)
  }
end ExportsTest
