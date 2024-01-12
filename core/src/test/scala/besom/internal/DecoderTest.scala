package besom.internal

import com.google.protobuf.struct.*
import Constants.*
import Decoder.*
import ProtobufUtil.*
import besom.types.Label
import besom.util.*
import RunResult.{given, *}
import Validated.*

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

class DecoderTest extends munit.FunSuite:
  import DecoderTest.*
  val dummyLabel = Label.fromNameAndType("dummy", "dummy:pkg:Dummy")

  extension [E, A](vr: ValidatedResult[E, A]) def verify(f: Validated[E, A] => Unit): Unit = vr.asResult.map(f).unsafeRunSync()

  test("special struct signature can be extracted") {
    val secretStructSample: Value = Map(
      SpecialSigKey -> SpecialSecretSig.asValue
    ).asValue

    assert(extractSpecialStructSignature(secretStructSample).get == SpecialSecretSig)
  }

  test("decode case class") {
    val v = Map(
      "foo" -> 10.asValue,
      "bar" -> List("qwerty".asValue).asValue,
      "optNone1" -> Null,
      "optSome" -> Some("abc").asValue
    ).asValue
    val d = summon[Decoder[TestCaseClass]]

    d.decode(v, dummyLabel).verify {
      case Validated.Invalid(ex) => throw ex.head
      case Validated.Valid(OutputData.Known(res, isSecret, value)) =>
        assert(value == Some(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))))
      case Validated.Valid(_) => throw Exception("Unexpected unknown!")
    }
  }

  test("decode case class from null value") {
    val v = Null
    val d = summon[Decoder[TestCaseClass]]
    d.decode(v, dummyLabel).verify {
      case Validated.Invalid(es) =>
        es.head match
          case DecodingError(m, _, _) =>
            assertEquals(m, "dummy[dummy:pkg:Dummy]: Expected a struct to deserialize Product[TestCaseClass], got: 'NullValue(NULL_VALUE)'")
      case Validated.Valid(_) => throw Exception("Unexpected, valid")
    }
  }

  test("decode enum") {
    val v = "A value".asValue
    val d = summon[Decoder[TestEnum]]

    d.decode(v, dummyLabel).verify {
      case Validated.Invalid(ex)                                   => throw ex.head
      case Validated.Valid(OutputData.Known(res, isSecret, value)) => assert(value == Some(TestEnum.A))
      case Validated.Valid(_)                                      => throw Exception("Unexpected unknown!")
    }
  }

  test("decode int/string union") {
    val d = summon[Decoder[Int | String]]
    d.decode("A".asValue, dummyLabel).verify {
      case Validated.Invalid(ex)                                   => throw ex.head
      case Validated.Valid(OutputData.Known(res, isSecret, value)) => assert(value == Some("A"))
      case Validated.Valid(_)                                      => throw Exception("Unexpected unknown!")
    }
    d.decode(1.asValue, dummyLabel).verify {
      case Validated.Invalid(ex)                                   => throw ex.head
      case Validated.Valid(OutputData.Known(res, isSecret, value)) => assert(value == Some(1))
      case Validated.Valid(_)                                      => throw Exception("Unexpected unknown!")
    }
  }

  test("decode bool/string union") {
    val d = summon[Decoder[Boolean | String]]
    d.decode("A".asValue, dummyLabel).verify {
      case Validated.Invalid(ex)                                   => throw ex.head
      case Validated.Valid(OutputData.Known(res, isSecret, value)) => assert(value == Some("A"))
      case Validated.Valid(_)                                      => throw Exception("Unexpected unknown!")
    }
    d.decode(true.asValue, dummyLabel).verify {
      case Validated.Invalid(ex)                                   => throw ex.head
      case Validated.Valid(OutputData.Known(res, isSecret, value)) => assert(value == Some(true))
      case Validated.Valid(_)                                      => throw Exception("Unexpected unknown!")
    }
  }

  test("decode string/custom resource union") {
    val d = summon[Decoder[Int | String]]
    d.decode("A".asValue, dummyLabel).verify {
      case Validated.Invalid(ex)                                   => throw ex.head
      case Validated.Valid(OutputData.Known(res, isSecret, value)) => assert(value == Some("A"))
      case Validated.Valid(_)                                      => throw Exception("Unexpected unknown!")
    }
    d.decode(1.asValue, dummyLabel).verify {
      case Validated.Invalid(ex)                                   => throw ex.head
      case Validated.Valid(OutputData.Known(res, isSecret, value)) => assert(value == Some(1))
      case Validated.Valid(_)                                      => throw Exception("Unexpected unknown!")
    }
  }

  test("decode special case class with unmangling") {
    val v = Map(
      "equals" -> 10.asValue,
      "eq" -> "abc".asValue,
      "normalOne" -> "qwerty".asValue
    ).asValue
    val d = summon[Decoder[SpecialCaseClass]]

    d.decode(v, dummyLabel).verify {
      case Validated.Invalid(ex)                                   => throw ex.head
      case Validated.Valid(OutputData.Known(res, isSecret, value)) => assert(value == Some(SpecialCaseClass(10, "abc", "qwerty")))
      case Validated.Valid(_)                                      => throw Exception("Unexpected unknown!")
    }
  }
end DecoderTest
