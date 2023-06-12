package besom.internal

import com.google.protobuf.struct.*, Value.Kind
import Constants.*
import Decoder.*
import ProtobufUtil.*
import besom.util.Types.Label

object DecoderTest:
  case class TestCaseClass(
    foo: Int,
    bar: List[String],
    optNone1: Option[String],
    optNone2: Option[Int],
    optSome: Option[String]
  ) derives Decoder


  sealed abstract class TestEnum(val name: String, val value: String) extends besom.internal.StringEnum

  object TestEnum extends besom.internal.EnumCompanion[TestEnum]("TestEnum"):
    object A extends TestEnum("A", "A value")
    object B extends TestEnum("B", "B value")

    override val allInstances: Seq[TestEnum] = Seq(A, B)


class DecoderTest extends munit.FunSuite:
  import DecoderTest.*
  val dummyLabel = Label.fromNameAndType("dummy", "dummy:pkg:Dummy")

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

    d.decode(v, dummyLabel) match
      case Left(ex) => throw ex
      case Right(OutputData.Known(res, isSecret, value)) =>
        assert(value == Some(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))))
      case Right(_) => throw Exception("Unexpected unknown!")
  }

  test("decode enum") {
    val v = "A".asValue
    val d = summon[Decoder[TestEnum]]

    d.decode(v, dummyLabel) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some(TestEnum.A))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
  }

  test("decode int/string union") {
    val d = summon[Decoder[Int | String]]
    d.decode("A".asValue, dummyLabel) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some("A"))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
    d.decode(1.asValue, dummyLabel) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some(1))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
  }

  test("decode string/custom resource union") {
    val d = summon[Decoder[Int | String]]
    d.decode("A".asValue, dummyLabel) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some("A"))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
    d.decode(1.asValue, dummyLabel) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some(1))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
  }
