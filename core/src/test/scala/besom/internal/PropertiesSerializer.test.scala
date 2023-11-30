package besom.internal

import RunResult.{*, given}
import com.google.protobuf.struct.*
import besom.types.{Output as _, *}
import com.google.protobuf.struct.Struct.toJavaProto

case class ExampleResourceArgs(
  str: Output[Option[String]],
  b: Output[Option[Boolean]],
  helper: Output[Option[HelperArgs]]
) derives ProviderArgsEncoder

case class HelperArgs(
  int: Output[Option[Int]]
) derives Encoder,
      ArgsEncoder

class ProviderArgsEncoderTest extends munit.FunSuite:

  test("json ProviderArgsEncoder test") {
    // This is currently only used on arguments to provider resources.
    // When serializing properties with ProviderArgsEncoder to protobuf Struct, instead of sending their native
    // Struct representation, we send a protobuf String value with a JSON-formatted Struct representation
    // inside. In the tests below this will look like a double-JSON encoded string since we write asserts
    // against a JSON rendering of the proto struct.
    //
    // For a practical example of where this applies, see Provider resource in pulumi-kubernetes.

    given Context = DummyContext().unsafeRunSync()

    val res = PropertiesSerializer
      .serializeResourceProperties(
        ExampleResourceArgs(
          Output(Some("x")),
          Output(Some(true)),
          Output(Some(HelperArgs(Output(Some(1)))))
        )
      )
      .unsafeRunSync()

    val json = com.google.protobuf.util.JsonFormat
      .printer()
      .omittingInsignificantWhitespace()
      .sortingMapKeys()
      .print(toJavaProto(res.serialized))

    assertNoDiff(
      json,
      """{"b":"true","helper":"{\"int\":1.0}","str":"x"}"""
    )
  }
