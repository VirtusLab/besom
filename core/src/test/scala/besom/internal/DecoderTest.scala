package besom.internal

import com.google.protobuf.struct.*, Value.Kind
import Constants.*
import Decoder.*
import ProtobufUtil.*

enum TestEnum2:
  case A
  case B

class DecoderTest extends munit.FunSuite:
  test("special struct signature can be extracted") {
    val secretStructSample: Value = Map(
      SpecialSigKey -> SpecialSecretSig.asValue
    ).asValue

    assert(extractSpecialStructSignature(secretStructSample).get == SpecialSecretSig)
  }

  test("decode enum") {
    val v = "A".asValue
    // val d = summon[Decoder[TestEnum2]]
    val d = Decoder.derived[TestEnum2]

    println(d.decode(v))
  }
