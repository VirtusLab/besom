package besom.internal

import besom.internal.Constants.*
import besom.internal.Decoder.*
import besom.internal.ProtobufUtil.{*, given}
import besom.internal.RunResult.{*, given}
import besom.types.{Label, PulumiAny}
import besom.util.*
import besom.util.Validated.*
import com.google.protobuf.struct.*

object DecoderTest:
  case class TestCaseClass(
    foo: Int,
    bar: List[String],
    optNone1: Option[String],
    optNone2: Option[Int],
    optSome: Option[String]
  ) derives Decoder

  case class SpecialCaseClass(
    equals_ : Int,
    eq_ : String,
    normalOne: String
  ) derives Decoder

  sealed abstract class TestEnum(val name: String, val value: String) extends besom.types.StringEnum

  object TestEnum extends besom.types.EnumCompanion[String, TestEnum]("TestEnum"):
    object A extends TestEnum("A", "A value")
    object B extends TestEnum("B", "B value")

    override val allInstances: Seq[TestEnum] = Seq(A, B)

  final case class DiscriminatedUnionTypeA(
    value: String,
    `type`: String
  ) derives Decoder

  case class DiscriminatedUnionTypeB(
    value: Int,
    `type`: String
  ) derives Decoder
  case class DiscriminatedUnionTypeC(
    value: Boolean,
    `type`: String
  ) derives Decoder

  case class DiscriminatedUnionResult(
    value: scala.Option[List[DiscriminatedUnionTypeA | DiscriminatedUnionTypeB | DiscriminatedUnionTypeC]]
  )
  object DiscriminatedUnionResult:
    given decoder: besom.types.Decoder[DiscriminatedUnionResult] =
      besom.internal.Decoder.derived[DiscriminatedUnionResult]

    given discriminatedDecoderValue: besom.types.Decoder[DiscriminatedUnionTypeA | DiscriminatedUnionTypeB | DiscriminatedUnionTypeC] =
      Decoder.discriminated(
        "type",
        Map(
          ("A", summon[Decoder[DiscriminatedUnionTypeA]]),
          ("B", summon[Decoder[DiscriminatedUnionTypeB]]),
          ("C", summon[Decoder[DiscriminatedUnionTypeC]])
        )
      )

  case class NonDiscriminatedUnionResult(
    value: scala.Option[List[String | Double | Int | besom.types.PulumiAny | Boolean]]
  )
  object NonDiscriminatedUnionResult:
    given decoder: besom.types.Decoder[NonDiscriminatedUnionResult] =
      besom.internal.Decoder.derived[NonDiscriminatedUnionResult]

    given discriminatedDecoderValue: besom.types.Decoder[String | Double | Int | besom.types.PulumiAny | Boolean] =
      Decoder.nonDiscriminated(
        Map(
          0 -> summon[Decoder[Int]],
          1 -> summon[Decoder[Double]],
          2 -> summon[Decoder[String]],
          3 -> summon[Decoder[Boolean]],
          4 -> summon[Decoder[besom.types.PulumiAny]]
        )
      )

end DecoderTest

class DecoderTest extends munit.FunSuite:
  import DecoderTest.*
  val dummyLabel: Label = Label.fromNameAndType("dummy", "dummy:pkg:Dummy")

  extension [E, A](vr: ValidatedResult[E, A]) def verify(f: Validated[E, A] => Unit): Unit = vr.asResult.map(f).unsafeRunSync()

  runWithBothOutputCodecs {
    test(s"special struct signature can be extracted (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val secretStructSample: Value = Map(
        SpecialSig.Key -> SpecialSig.SecretSig.asValue
      ).asValue

      assert(secretStructSample.withSpecialSignature(_ => true).getOrElse(false))
    }
  }

  runWithBothOutputCodecs {
    test(s"decode unknown null value (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val v =
        if Context().featureSupport.keepOutputValues
        then OutputValue.unknown(isSecret = false, dependencies = Nil).asValue
        else UnknownStringValue.asValue
      val d = summon[Decoder[Boolean]]

      d.decode(v, dummyLabel).verify {
        case Validated.Invalid(ex)                                => throw ex.head
        case Validated.Valid(o @ OutputData.Unknown(_, isSecret)) => assert(!isSecret, o)
        case Validated.Valid(_)                                   => throw Exception("Unexpected known!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode unknown null secret value (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val v =
        if Context().featureSupport.keepOutputValues
        then OutputValue.unknown(isSecret = true, dependencies = Nil).asValue
        else SecretValue.unknown.asValue
      val d = summon[Decoder[Boolean]]

      d.decode(v, dummyLabel).verify {
        case Validated.Invalid(ex)                                => throw ex.head
        case Validated.Valid(o @ OutputData.Unknown(_, isSecret)) => assert(isSecret, o)
        case Validated.Valid(_)                                   => throw Exception("Unexpected known!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode case class (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val v =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "foo" -> 10.asValue.asOutputValue(isSecret = false, Nil),
            "bar" -> List("qwerty".asValue).asValue.asOutputValue(isSecret = false, Nil),
            "optNone1" -> Null.asOutputValue(isSecret = false, Nil),
            "optSome" -> Some("abc").asValue.asOutputValue(isSecret = false, Nil)
          ).asValue
        else
          Map(
            "foo" -> 10.asValue,
            "bar" -> List("qwerty".asValue).asValue,
            "optNone1" -> Null,
            "optSome" -> Some("abc").asValue
          ).asValue

      val d        = summon[Decoder[TestCaseClass]]
      val expected = TestCaseClass(10, List("qwerty"), None, None, Some("abc"))

      d.decode(v, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some(expected))
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode case class from null value (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val v = Null
      val d = summon[Decoder[TestCaseClass]]

      d.decode(v, dummyLabel).verify {
        case Validated.Invalid(es) =>
          es.head match
            case DecodingError(m, _, _) =>
              assertEquals(
                m,
                "dummy[dummy:pkg:Dummy]: Expected a struct to deserialize Product[TestCaseClass], got: 'NullValue(NULL_VALUE)'"
              )
        case Validated.Valid(_) => throw Exception("Unexpected, valid")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode enum (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val v = "A value".asValue
      val d = summon[Decoder[TestEnum]]

      d.decode(v, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some(TestEnum.A))
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode int/string union (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val d = summon[Decoder[Int | String]]
      d.decode("A".asValue, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some("A"))
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
      d.decode(1.asValue, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some(1))
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode bool/string union (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val d = summon[Decoder[Boolean | String]]
      d.decode("A".asValue, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some("A"))
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
      d.decode(true.asValue, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some(true))
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode string/custom resource union (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val d = summon[Decoder[Int | String]]
      d.decode("A".asValue, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some("A"))
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
      d.decode(1.asValue, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some(1))
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode crazy union (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val a = List("A".asValue, 1.asValue, true.asValue).asValue

      given ld: Decoder[String | Double | besom.types.PulumiAny | Boolean] = Decoder.nonDiscriminated(
        Map(
          0 -> summon[Decoder[String]],
          1 -> summon[Decoder[Double]],
          2 -> summon[Decoder[Boolean]],
          3 -> summon[Decoder[besom.types.PulumiAny]]
        )
      )

      val d = summon[besom.internal.Decoder[List[String | Double | besom.types.PulumiAny | Boolean]]]
      d.decode(a, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some(List("A", 1, true)), value)
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode special case class with unmangling (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val v = Map(
        "equals" -> 10.asValue,
        "eq" -> "abc".asValue,
        "normalOne" -> "qwerty".asValue
      ).asValue
      val d = summon[Decoder[SpecialCaseClass]]

      d.decode(v, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some(SpecialCaseClass(10, "abc", "qwerty")))
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode a generic union of 2 (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val v1 = Map(
        "foo" -> 10.asValue,
        "bar" -> List("qwerty".asValue).asValue,
        "optNone1" -> Null,
        "optSome" -> Some("abc").asValue
      ).asValue
      val v2 = Map(
        "equals" -> 10.asValue,
        "eq" -> "abc".asValue,
        "normalOne" -> "qwerty".asValue
      ).asValue

      val d = unionDecoder2[TestCaseClass, SpecialCaseClass]
      d.decode(v1, dummyLabel).verify {
        case Validated.Invalid(ex) => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) =>
          assert(value == Some(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))))
        case Validated.Valid(_) => throw Exception("Unexpected unknown!")
      }
      d.decode(v2, dummyLabel).verify {
        case Validated.Invalid(ex) => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) =>
          assert(value == Some(SpecialCaseClass(10, "abc", "qwerty")))
        case Validated.Valid(_) => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode a discriminated union - by key (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val a = Map(
        "value" -> List(
          Map(
            "type" -> "A".asValue,
            "value" -> "abc".asValue
          ).asValue
        ).asValue
      ).asValue
      val b = Map(
        "value" -> List(
          Map(
            "type" -> "B".asValue,
            "value" -> 1.asValue
          ).asValue
        ).asValue
      ).asValue
      val c = Map(
        "value" -> List(
          Map(
            "type" -> "C".asValue,
            "value" -> false.asValue
          ).asValue
        ).asValue
      ).asValue

      val d = summon[Decoder[DiscriminatedUnionResult]]

      d.decode(a, dummyLabel).verify {
        case Validated.Invalid(ex) => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) =>
          assert(value == Some(DiscriminatedUnionResult(Some(List(DiscriminatedUnionTypeA("abc", "A"))))), clue = value)
        case Validated.Valid(_) => throw Exception("Unexpected unknown!")
      }
      d.decode(b, dummyLabel).verify {
        case Validated.Invalid(ex) => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) =>
          assert(value == Some(DiscriminatedUnionResult(Some(List(DiscriminatedUnionTypeB(1, "B"))))), clue = value)
        case Validated.Valid(_) => throw Exception("Unexpected unknown!")
      }
      d.decode(c, dummyLabel).verify {
        case Validated.Invalid(ex) => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) =>
          assert(value == Some(DiscriminatedUnionResult(Some(List(DiscriminatedUnionTypeC(false, "C"))))), clue = value)
        case Validated.Valid(_) => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode non-discriminated union - by index (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val a = Map(
        "value" -> List(
          "A".asValue,
          1.asValue,
          2.3.asValue,
          true.asValue,
          Map("z" -> 0.asValue).asValue
        ).asValue
      ).asValue

      val d = summon[Decoder[NonDiscriminatedUnionResult]]
      val expected = NonDiscriminatedUnionResult(
        Some(List("A", 1, 2.3, true, besom.json.JsObject("z" -> besom.json.JsNumber(0)).asInstanceOf[PulumiAny]))
      )

      d.decode(a, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some(expected), clue = value)
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
    }
  }

  runWithBothOutputCodecs {
    test(s"decode skipping internal (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val v = Map(
        "foo" -> "bar".asValue,
        "__default" -> "buzz".asValue
      ).asValue

      val d        = summon[Decoder[Map[String, String]]]
      val expected = Map("foo" -> "bar")

      d.decode(v, dummyLabel).verify {
        case Validated.Invalid(ex)                          => throw ex.head
        case Validated.Valid(OutputData.Known(_, _, value)) => assert(value == Some(expected), clue = value)
        case Validated.Valid(_)                             => throw Exception("Unexpected unknown!")
      }
    }
  }
end DecoderTest
