package besom.internal

import com.google.protobuf.struct.*, Value.Kind
import Constants.*
import Decoder.*
import ProtobufUtil.*

object DecoderTest:
  case class TestCaseClass(foo: Int, bar: List[String]) derives Decoder

  enum TestEnum derives Decoder:
    case A
    case B

class DecoderTest extends munit.FunSuite:
  import DecoderTest.*

  test("special struct signature can be extracted") {
    val secretStructSample: Value = Map(
      SpecialSigKey -> SpecialSecretSig.asValue
    ).asValue

    assert(extractSpecialStructSignature(secretStructSample).get == SpecialSecretSig)
  }

  test("decode case class") {
    val v = Map(
      "foo" -> 10.asValue,
      "bar" -> List("qwerty".asValue).asValue
    ).asValue
    val d = summon[Decoder[TestCaseClass]]

    d.decode(v) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some(TestCaseClass(10, List("qwerty"))))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
  }

  test("decode enum") {
    val v = "A".asValue
    val d = summon[Decoder[TestEnum]]

    d.decode(v) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some(TestEnum.A))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
  }

  test("decode int/string union") {
    val d = summon[Decoder[Int | String]]
    d.decode("A".asValue) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some("A"))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
    d.decode(1.asValue) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some(1))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
  }

  

  test("decode string/custom resource union") {
    val d = summon[Decoder[Int | String]]
    d.decode("A".asValue) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some("A"))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
    d.decode(1.asValue) match
      case Left(ex)                                      => throw ex
      case Right(OutputData.Known(res, isSecret, value)) => assert(value == Some(1))
      case Right(_)                                      => throw Exception("Unexpected unknown!")
  }