package besom.internal

import besom.internal.ProtobufUtil.{*, given}
import besom.internal.RunResult.{*, given}
import besom.types.{Label, URN}
import besom.util.*
import besom.util.Validated.*
import com.google.protobuf.struct.*
import scala.collection.immutable.Iterable

class OutputValueCodecTest extends munit.FunSuite with ValueAssertions:
  val dummyLabel: Label = Label.fromNameAndType("dummy", "dummy:pkg:Dummy")

  val fakeUrn1 = "urn:pulumi:stack::project::example:module:Resource::my-name-1"
  val fakeUrn2 = "urn:pulumi:stack::project::example:module:Resource::my-name-2"

  t[Option[Boolean]](None)
  t(false)
  t(0)
  t(1)
  t("")
  t("hi")
  t(Iterable.empty[Value])
  t(Map.empty[String, Value])

  def typeName(a: Any): String = if a == null then "null" else a.getClass.getSimpleName
  def t[A: ToValue: Encoder: Decoder](value: A): Unit =
    for
      isKnown  <- List(true, false)
      isSecret <- List(true, false)
      deps     <- List(List.empty, List(fakeUrn1, fakeUrn2))
    do
      val name = s"basics: ${typeName(value)} (isKnown: $isKnown, isSecret: $isSecret, deps: ${deps.length})"
      test(name) {
        given Context = DummyContext().unsafeRunSync()

        // calculate the expected test values for various stages of the test
        val maybeValue               = Option(value)
        val urns: List[URN]          = deps.map(URN.from(_).get)
        val resources: Set[Resource] = urns.map(urn => DependencyResource(Output(urn))).toSet
        val protoValue               = value.asValue
        // we don't know with the unknown value is empty or not so we default to false to not short-circuit the Metadata#combine method
        val empty          = if !isKnown then false else protoValue.kind.isNullValue
        val metadataSample = Metadata(isKnown, isSecret, empty, urns)
        val decodedSample  = metadataSample.render(protoValue)
        val encodedData =
          if isKnown
          then OutputData.Known(resources, isSecret, maybeValue)
          else OutputData.Unknown(resources, isSecret)

        // test the serialization
        val encoder                 = summon[Encoder[Output[A]]]
        val (m: Metadata, v: Value) = encoder.encode(Output.ofData(encodedData)).unsafeRunSync()
        assertEquals(m, metadataSample, name)
        if isKnown then assertEqualsValue(v, decodedSample, name)

        // test the deserialization
        val decoder       = summon[Decoder[A]]
        val decodedResult = decoder.decode(decodedSample, dummyLabel).asResult.unsafeRunSync()
        decodedResult match
          case Validated.Invalid(ex) => fail("Validated.Invalid", ex.head)
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
      }
    end for
  end t
end OutputValueCodecTest

class OutputValueTest extends munit.FunSuite with ValueAssertions:
  import besom.internal.Constants.*

  val fakeUrn1 = "urn:pulumi:stack::project::example:module:Resource::my-name-1"
  val fakeUrn2 = "urn:pulumi:stack::project::example:module:Resource::my-name-2"

  case class Data(value: Value, outputValue: OutputValue)

  Vector(
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        ValueName -> "asdf".asValue,
        SecretName -> true.asValue,
        DependenciesName -> List(fakeUrn1, fakeUrn2).asValue
      ).asValue,
      OutputValue.known("asdf".asValue, true, List(fakeUrn1, fakeUrn2).map(URN.from(_).get))
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        ValueName -> "asdf".asValue
      ).asValue,
      OutputValue.known("asdf".asValue, false, List.empty)
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        SecretName -> true.asValue,
        DependenciesName -> List(fakeUrn1, fakeUrn2).asValue
      ).asValue,
      OutputValue.unknown(true, List(fakeUrn1, fakeUrn2).map(URN.from(_).get))
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        SecretName -> true.asValue
      ).asValue,
      OutputValue.unknown(true, List.empty)
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue,
        ValueName -> Null
      ).asValue,
      OutputValue.known(Null, false, List.empty)
    ),
    Data(
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue
      ).asValue,
      OutputValue.unknown(false, List.empty)
    )
  ).zipWithIndex.foreach { case (Data(v: Value, o: OutputValue), i: Int) =>
    test(s"[$i] round trip") {
      // check Value -> OutputValue
      val outputValueResult: OutputValue = OutputValue.unapply(v).get
      assertEquals(outputValueResult, o)
      assert(outputValueResult.isKnown == o.isKnown)

      // check OutputValue -> Value
      assertEqualsValue(o.asValue, v, v.toProtoString)

      // check if we've ended up with the same structure we started with
      assertEqualsValue(OutputValue.unapply(v).get.asValue, v, v.toProtoString)
    }
  }
end OutputValueTest
