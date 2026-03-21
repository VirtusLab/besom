package besom.internal

import besom.internal.ProtobufUtil.{*, given}
import besom.internal.RunResult.{*, given}
import besom.types.{Output as _, *}
import com.google.protobuf.struct.*
import com.google.protobuf.struct.Value.Kind

class ExportsTest extends munit.FunSuite with ValueAssertions:

  runWithBothOutputCodecs {
    test(s"exports alone work as intended (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val stackOutputs = Stack.exports(foo = Output.pure("bar")).getExports.result.unsafeRunSync()

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
        Output.pure("foo"),
        Output.pure("bar"),
        Output.ofResult(Result.defer { atomicBoolean.set(true); "baz" })
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
          Output.pure("foo"),
          Output.pure("bar")
        ).exports(
          baz = Output.pure("baz"),
          qux = Output.ofResult(Result.defer {
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
            foo = Output.pure("foo"),
            bar = Output.secret("bar")
          )
          .exports(
            baz = Output.pure("baz"),
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
  case class TestOutputs(foo: Output[String], bar: Output[Int]) derives Encoder

  runWithBothOutputCodecs {
    test(s"Stack.export with case class works as intended (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val stack = Stack.exports(TestOutputs(foo = Output.pure("hello"), bar = Output.pure(42)))

      val stackOutputs = stack.getExports.result.unsafeRunSync()
      val encoded      = Value(Kind.StructValue(stackOutputs))

      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "foo" -> "hello".asValue.asOutputValue(isSecret = false, List.empty),
            "bar" -> 42.0.asValue.asOutputValue(isSecret = false, List.empty)
          ).asValue
        else
          Map(
            "foo" -> "hello".asValue,
            "bar" -> 42.0.asValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(
      s"Stack with dependencies then export preserves dependencies (keepOutputValues: ${Context().featureSupport.keepOutputValues})"
    ) {
      val dep   = Output.pure("dep")
      val stack = Stack(dep).exports(TestOutputs(foo = Output.pure("hello"), bar = Output.pure(42)))

      val stackDeps = stack.getDependsOn
      assertEquals(stackDeps.size, 1)

      val stackOutputs = stack.getExports.result.unsafeRunSync()
      assertEquals(stackOutputs.fields.size, 2)
    }
  }

  runWithBothOutputCodecs {
    test(
      s"Stack.export then exports merges correctly (keepOutputValues: ${Context().featureSupport.keepOutputValues})"
    ) {
      val stack = Stack
        .exports(TestOutputs(foo = Output.pure("hello"), bar = Output.pure(42)))
        .exports(extra = Output.pure("extra"))

      val stackOutputs = stack.getExports.result.unsafeRunSync()
      assertEquals(stackOutputs.fields.size, 3)
      assert(stackOutputs.fields.contains("foo"))
      assert(stackOutputs.fields.contains("bar"))
      assert(stackOutputs.fields.contains("extra"))
    }
  }

  runWithBothOutputCodecs {
    test(s"Stack.export handles secrets correctly (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      case class SecretOutputs(public: Output[String], secret: Output[String]) derives Encoder

      val stack = Stack.exports(SecretOutputs(public = Output.pure("visible"), secret = Output.secret("hidden")))

      val stackOutputs = stack.getExports.result.unsafeRunSync()
      val encoded      = stackOutputs.asValue

      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "public" -> "visible".asValue.asOutputValue(isSecret = false, List.empty),
            "secret" -> "hidden".asValue.asOutputValue(isSecret = true, List.empty)
          ).asValue
        else
          Map(
            "public" -> "visible".asValue,
            "secret" -> "hidden".asValue.asSecretValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  test("typed exports should not compile for a case class without Encoder") {
    val errors = compileErrors(
      """given Context = DummyContext().unsafeRunSync()
         case class BadOutputs(foo: String, bar: Int)
         Stack.exports(BadOutputs("hello", 42))"""
    )
    assert(
      errors.contains("Missing Encoder[BadOutputs] for typed export. Add `derives Encoder` to your case class."),
      s"Expected error about missing Encoder, got: $errors"
    )
  }
end ExportsTest
