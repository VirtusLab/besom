package besom.internal

import com.google.protobuf.struct.*, Value.Kind, Kind.*
import Constants.*
import Decoder.*

class DecoderTest extends munit.FunSuite:
  test("special struct signature can be extracted") {
    val secretStructSample: Value = Value.of(
      StructValue(
        Struct.of(
          Map(
            SpecialSigKey -> Value.of(StringValue(SpecialSecretSig))
          )
        )
      )
    )

    assert(extractSpecialStructSignature(secretStructSample).get == SpecialSecretSig)
  }
