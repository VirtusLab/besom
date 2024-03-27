package besom.internal

import besom.internal.ProtobufUtil.{*, given}
import besom.internal.RunResult.{*, given}
import besom.types.{Output as _, *}
import com.google.protobuf.struct.*
import com.google.protobuf.struct.Value.Kind

class ExportsTest extends munit.FunSuite with ValueAssertions:

  runWithBothOutputCodecs {
    test(s"exports alone work as intended (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val stackOutputs = Stack.exports(foo = Output("bar")).getExports.result.unsafeRunSync()

      val encoded = Value(Kind.StructValue(stackOutputs))

      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "foo" -> "bar".asValue.asOutputValue(isSecret = false, dependencies = List.empty)
          ).asValue
        else
          Map(
            "foo" -> "bar".asValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"stack dependencies work as intended (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val atomicBoolean = new java.util.concurrent.atomic.AtomicBoolean(false)

      val stackDeps = Stack(
        Output("foo"),
        Output("bar"),
        Output(Result.defer { atomicBoolean.set(true); "baz" })
      ).getDependsOn

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
  }

  runWithBothOutputCodecs {
    test(s"exports with dependencies work as intended (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val stack =
        Stack(
          Output("foo"),
          Output("bar")
        ).exports(
          baz = Output("baz"),
          qux = Output(Result.defer {
            "qux"
          })
        )

      val stackOutputs = stack.getExports.result.unsafeRunSync()

      val encoded = Value(Kind.StructValue(stackOutputs))

      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "baz" -> "baz".asValue.asOutputValue(isSecret = false, List.empty),
            "qux" -> "qux".asValue.asOutputValue(isSecret = false, List.empty)
          ).asValue
        else
          Map(
            "baz" -> "baz".asValue,
            "qux" -> "qux".asValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)

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
  }

  runWithBothOutputCodecs {
    test(
      s"multiple export clauses aggregate instead of replacing exported values (keepOutputValues: ${Context().featureSupport.keepOutputValues})"
    ) {
      val stack =
        Stack
          .exports(
            foo = Output("foo"),
            bar = Output.secret("bar")
          )
          .exports(
            baz = Output("baz"),
            qux = Output.secret("qux")
          )

      val stackStruct = stack.getExports.result.unsafeRunSync()
      val encoded     = stackStruct.asValue

      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "foo" -> "foo".asValue.asOutputValue(isSecret = false, List.empty),
            "bar" -> "bar".asValue.asOutputValue(isSecret = true, List.empty),
            "baz" -> "baz".asValue.asOutputValue(isSecret = false, List.empty),
            "qux" -> "qux".asValue.asOutputValue(isSecret = true, List.empty)
          ).asValue
        else
          Map(
            "foo" -> "foo".asValue,
            "bar" -> "bar".asValue.asSecretValue,
            "baz" -> "baz".asValue,
            "qux" -> "qux".asValue.asSecretValue
          ).asValue

      assertEquals(stackStruct.fields.size, 4)
      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }
end ExportsTest
