package besom.internal

import besom.internal.ProtobufUtil.{*, given}
import besom.internal.RunResult.{*, given}
import besom.types.{Label, URN}
import besom.util.*
import besom.util.Validated.*
import com.google.protobuf.struct.*

class OutputValueEncoderTest extends munit.FunSuite with ValueAssertions:
  val dummyLabel: Label = Label.fromNameAndType("dummy", "dummy:pkg:Dummy")

  val fakeUrn1 = "urn:pulumi:stack::project::example:module:Resource::my-name-1"
  val fakeUrn2 = "urn:pulumi:stack::project::example:module:Resource::my-name-2"

  t(false)
  t(0)
  t(1)
  t("")
  t("hi")
  t(List.empty[Value])
  t(Map.empty[String, Value])

  def typeName(a: Any): String = if a == null then "Null" else a.getClass.getSimpleName
  def t[A: ToValue: Encoder](value: A): Unit =
    for
      isKnown  <- List(true, false)
      isSecret <- List(true, false)
      deps     <- List(List.empty, List(fakeUrn1, fakeUrn2))
    do {
      val name = s"basics: ${typeName(value)} (isKnown: $isKnown, isSecret: $isSecret, deps: ${deps.length})"
      test(name) {
        given Context = DummyContext().unsafeRunSync()

        val urns: List[URN]          = deps.map(URN.from(_).get)
        val resources: Set[Resource] = urns.map(urn => DependencyResource(Output(urn))).toSet
        val expected = {
          if isKnown
          then value.asValue
          else Null
        }.asOutputValue(isSecret = isSecret, dependencies = urns)

        val maybeValue = Option(value)
        val input = Output.ofData(
          if isKnown
          then OutputData.Known(resources, isSecret, maybeValue)
          else OutputData.Unknown(resources, isSecret)
        )

        val e = summon[Encoder[Output[A]]]
        e.encode(input)
          .map { (r: Set[Resource], v: Value) =>
            assertEqualsValue(v, expected, name)
            assertEquals(r.flatMap(_.urn.getValue.unsafeRunSync()), urns.toSet)
          }
          .unsafeRunSync()
      }
    }
    end for
  end t
end OutputValueEncoderTest

class OutputValueDecoderTest extends munit.FunSuite with ValueAssertions:
  val dummyLabel: Label = Label.fromNameAndType("dummy", "dummy:pkg:Dummy")

  val fakeUrn1 = "urn:pulumi:stack::project::example:module:Resource::my-name-1"
  val fakeUrn2 = "urn:pulumi:stack::project::example:module:Resource::my-name-2"

  t(false)
  t(0)
  t(1)
  t("")
  t("hi")
  t(List.empty[Value])
  t(Map.empty[String, Value])

  def typeName(a: Any): String = if a == null then "Null" else a.getClass.getSimpleName
  def t[A: ToValue: Decoder](value: A): Unit =
    for
      isKnown  <- List(true, false)
      isSecret <- List(true, false)
      deps     <- List(List.empty, List(fakeUrn1, fakeUrn2))
    do {
      val name = s"basics: ${typeName(value)} (isKnown: $isKnown, isSecret: $isSecret, deps: ${deps.length})"
      test(name) {
        given Context = DummyContext().unsafeRunSync()

        val urns: List[URN] = deps.map(URN.from(_).get)
        val maybeValue      = Option(value)
        val input = OutputValue(
          value = if isKnown then value.asValue else Null,
          isSecret = isSecret,
          dependencies = urns
        ).asValue

        val d = summon[Decoder[A]]
        d.decode(input, dummyLabel)
          .asResult
          .map({
            case Validated.Valid(d) =>
              d match
                case OutputData.Known(r, s, v) =>
                  if !isKnown then fail("Unexpected known", clues(name))
                  assertEquals(v, maybeValue, name)
                  assertEquals(s, isSecret, name)
                  assertEquals(r.map(_.urn.getValue.unsafeRunSync().get), urns.toSet, name)
                case OutputData.Unknown(r, s) =>
                  if isKnown then fail("Unexpected unknown", clues(name))
                  assertEquals(s, isSecret, name)
                  assertEquals(r.map(_.urn.getValue.unsafeRunSync().get), urns.toSet, name)
            case Validated.Invalid(ex) => fail("Validated.Invalid", ex.head)
          })
          .unsafeRunSync()
      }
    }
    end for
  end t
end OutputValueDecoderTest

class OutputValueTest extends munit.FunSuite with ValueAssertions:
  import besom.internal.Constants.*

  val fakeUrn1 = "urn:pulumi:stack::project::example:module:Resource::my-name-1"
  val fakeUrn2 = "urn:pulumi:stack::project::example:module:Resource::my-name-2"

  case class Data(value: Value, outputValue: OutputValue, isKnown: Boolean)

  Vector(
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        ValueName -> "asdf".asValue,
        SecretName -> true.asValue,
        DependenciesName -> List(fakeUrn1, fakeUrn2).asValue
      ).asValue,
      OutputValue(
        "asdf".asValue,
        true,
        List(fakeUrn1, fakeUrn2).map(URN.from(_).get)
      ),
      isKnown = true
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        ValueName -> "asdf".asValue
      ).asValue,
      OutputValue(
        "asdf".asValue,
        false,
        List.empty
      ),
      isKnown = true
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        SecretName -> true.asValue,
        DependenciesName -> List(fakeUrn1, fakeUrn2).asValue
      ).asValue,
      OutputValue(
        Null,
        true,
        List(fakeUrn1, fakeUrn2).map(URN.from(_).get)
      ),
      isKnown = false
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        SecretName -> true.asValue
      ).asValue,
      OutputValue(
        Null,
        true,
        List.empty
      ),
      isKnown = false
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        ValueName -> Null
      ).asValue,
      OutputValue(
        Null,
        false,
        List.empty
      ),
      isKnown = false
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue
      ).asValue,
      OutputValue(
        Null,
        false,
        List.empty
      ),
      isKnown = false
    )
  ).zipWithIndex.foreach { case (Data(v: Value, o: OutputValue, isKnown: Boolean), i: Int) =>
    test(s"[$i] round trip") {
      // check Value -> OutputValue
      val outputValueResult: OutputValue = v.outputValue.get
      assertEquals(outputValueResult, o)
      assert(outputValueResult.isKnown == isKnown)

      // check OutputValue -> Value
      val valueResult: Value = o.asValue
      assertEqualsValue(valueResult, v, v.toProtoString)

      // check if we've ended up with the same structure we started with
      assertEqualsValue(v.outputValue.get.asValue, v, v.toProtoString)
    }
  }
end OutputValueTest
