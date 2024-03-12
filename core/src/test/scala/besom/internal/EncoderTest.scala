package besom.internal

import besom.internal.Encoder.*
import besom.internal.ProtobufUtil.{*, given}
import besom.internal.RunResult.{*, given}
import besom.types.{EnumCompanion, Label, StringEnum, Output as _, *}
import besom.util.*
import com.google.protobuf.struct.*

object EncoderTest:

  sealed abstract class TestEnum(val name: String, val value: String) extends StringEnum

  object TestEnum extends EnumCompanion[String, TestEnum]("TestEnum"):
    object Test1 extends TestEnum("Test1", "Test1 value")
    object AnotherTest extends TestEnum("AnotherTest", "AnotherTest value")
    object `weird-test` extends TestEnum("weird-test", "weird-test value")

    override val allInstances: Seq[TestEnum] = Seq(
      Test1,
      AnotherTest,
      `weird-test`
    )

  case class PlainCaseClass(data: String, moreData: Int) derives Encoder
  case class OptionCaseClass(data: Option[String], moreData: Option[Int]) derives Encoder
  case class InputOptionalCaseClass(value: Output[Option[String]], data: Output[Option[Map[String, Output[String]]]])
      derives Encoder,
        ArgsEncoder
  case class TestArgs(a: Output[String], b: Output[PlainCaseClass]) derives ArgsEncoder
  case class TestOptionArgs(a: Output[Option[String]], b: Output[Option[PlainCaseClass]]) derives ArgsEncoder

  case class TestCaseClass(
    foo: Int,
    bar: List[String],
    optNone1: Option[String],
    optNone2: Option[Int],
    optSome: Option[String]
  ) derives Encoder

  case class SpecialCaseClass(
    equals_ : Int,
    eq_ : String,
    normalOne: String
  ) derives Encoder

end EncoderTest

class EncoderTest extends munit.FunSuite with ValueAssertions:
  import EncoderTest.*

  val dummyLabel: Label = Label.fromNameAndType("dummy", "dummy:pkg:Dummy")

  test("encode case class") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[TestCaseClass]]

    val (_, encoded) = e.encode(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))).unsafeRunSync()
    val expected = Map(
      "foo" -> 10.asValue,
      "bar" -> List("qwerty".asValue).asValue,
      "optNone1" -> Null,
      "optSome" -> Some("abc").asValue
    ).asValue

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }

  test("encode null") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[Option[String]]]

    val (_, encoded) = e.encode(None).unsafeRunSync()
    val expected     = Null

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }

  test("encode output null") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[Output[Option[String]]]]

    val (_, encoded) = e.encode(Output(None)).unsafeRunSync()
    val expected     = Null

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }

  test("encode secret null") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[Output[Option[String]]]]

    val (_, encoded) = e.encode(Output.secret(None)).unsafeRunSync()
    val expected     = Null.asSecret

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }

  test("encode secret input map") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[Output[Option[Map[String, String]]]]]

    val data: besom.types.Input.Optional[Map[String, besom.types.Input[String]]] = None
    val (_, encoded) = e.encode(data.asOptionOutput(isSecret = true)).unsafeRunSync()
    val expected     = Null.asSecret

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }

  test("encode class with secret inputs") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[InputOptionalCaseClass]]

    val (_, encoded) = e.encode(InputOptionalCaseClass(Output.secret(None), Output.secret(None))).unsafeRunSync()
    val expected     = Map.empty[String, Value].asValue

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }

  test("encode special case class with unmangling") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[SpecialCaseClass]]

    val expected = Map(
      "equals" -> 10.asValue,
      "eq" -> "abc".asValue,
      "normalOne" -> "qwerty".asValue
    ).asValue
    val (_, encoded) = e.encode(SpecialCaseClass(10, "abc", "qwerty")).unsafeRunSync()

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }

  test("encoder enum") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[TestEnum]]

    assertEqualsValue(e.encode(TestEnum.Test1).unsafeRunSync()._2, "Test1 value".asValue)
    assertEqualsValue(e.encode(TestEnum.AnotherTest).unsafeRunSync()._2, "AnotherTest value".asValue)
    assertEqualsValue(e.encode(TestEnum.`weird-test`).unsafeRunSync()._2, "weird-test value".asValue)
  }

  test("encode optional") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[OptionCaseClass]]

    val (res, encoded) = e
      .encode(
        OptionCaseClass(None, None)
      )
      .unsafeRunSync()

    assert(res.isEmpty)
    assert(encoded.getStructValue.fields.isEmpty, encoded.toProtoString)
  }

  test("encode a union of string and case class") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[String | TestCaseClass]]

    val (_, encodedString) = e.encode("abc").unsafeRunSync()
    val expectedString     = "abc".asValue

    assertEqualsValue(encodedString, expectedString, encodedString.toProtoString)

    val (_, encodedCaseClass) = e.encode(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))).unsafeRunSync()
    val expectedCaseClass = Map(
      "foo" -> 10.asValue,
      "bar" -> List("qwerty".asValue).asValue,
      "optNone1" -> Null,
      "optSome" -> Some("abc").asValue
    ).asValue

    assertEqualsValue(encodedCaseClass, expectedCaseClass, encodedCaseClass.toProtoString)
  }

end EncoderTest

class ArgsEncoderTest extends munit.FunSuite with ValueAssertions:
  import EncoderTest.*

  test("simple args") {
    given Context = DummyContext().unsafeRunSync()
    val ae        = summon[ArgsEncoder[TestArgs]]

    val (res, encoded) = ae
      .encode(
        TestArgs(
          Output("SOME-TEST-PROVIDER"),
          Output(PlainCaseClass(data = "werks?", moreData = 123))
        ),
        _ => false
      )
      .unsafeRunSync()

    assert(res.contains("a"), res)
    assert(res.contains("b"), res)

    val expected =
      Map(
        "a" -> "SOME-TEST-PROVIDER".asValue,
        "b" -> Map(
          "data" -> "werks?".asValue,
          "moreData" -> 123.asValue
        ).asValue
      ).asValue

    assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
  }

  test("optional args") {
    given Context = DummyContext().unsafeRunSync()
    val ae        = summon[ArgsEncoder[TestOptionArgs]]

    val (res, encoded) = ae
      .encode(
        TestOptionArgs(
          Output(None),
          Output(None)
        ),
        _ => false
      )
      .unsafeRunSync()

    val expected =
      Map(
        "a" -> Null,
        "b" -> Null
      ).asValue

    assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
    assert(res.isEmpty, res)
  }

  test("empty secret args") {
    given Context = DummyContext().unsafeRunSync()
    val ae        = summon[ArgsEncoder[InputOptionalCaseClass]]

    val (res, encoded) = ae
      .encode(
        InputOptionalCaseClass(Output.secret(None), Output.secret(None)),
        _ => false
      )
      .unsafeRunSync()

    val expected =
      Map.empty[String, Value].asValue

    assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
    assert(res.isEmpty, res)
  }

  test("secrets args") {
    given Context = DummyContext().unsafeRunSync()
    val ae        = summon[ArgsEncoder[InputOptionalCaseClass]]

    val (res, encoded) = ae
      .encode(
        InputOptionalCaseClass(
          Output.secret(Some("secret")),
          Output.secret(Some(Map("key" -> Output.secret("value"))))
        ),
        _ => false
      )
      .unsafeRunSync()

    val expected = Map(
      "value" -> "secret".asValue.asSecret,
      "data" -> Map("key" -> "value".asValue.asSecret).asValue.asSecret
    ).asValue

    assert(res.keySet == Set("value", "data"), res)
    assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
  }
end ArgsEncoderTest

object ProviderArgsEncoderTest:
  import EncoderTest.*

  case class TestProviderArgs(`type`: Output[String], pcc: Output[PlainCaseClass]) derives ProviderArgsEncoder
  case class TestProviderOptionArgs(`type`: Output[Option[String]], pcc: Output[Option[PlainCaseClass]]) derives ProviderArgsEncoder

  case class ExampleResourceArgs(
    str: Output[Option[String]],
    b: Output[Option[Boolean]],
    helper: Output[Option[HelperArgs]]
  ) derives ProviderArgsEncoder

  case class HelperArgs(
    int: Output[Option[Int]]
  ) derives Encoder,
        ArgsEncoder

  final case class ProviderArgs private (
    cluster: besom.types.Output[scala.Option[String]],
    context: besom.types.Output[scala.Option[String]],
    deleteUnreachable: besom.types.Output[scala.Option[Boolean]],
    enableConfigMapMutable: besom.types.Output[scala.Option[Boolean]],
    enableServerSideApply: besom.types.Output[scala.Option[Boolean]],
    kubeconfig: besom.types.Output[scala.Option[String]],
    namespace: besom.types.Output[scala.Option[String]],
    renderYamlToDirectory: besom.types.Output[scala.Option[String]],
    skipUpdateUnreachable: besom.types.Output[scala.Option[Boolean]],
    suppressDeprecationWarnings: besom.types.Output[scala.Option[Boolean]],
    suppressHelmHookWarnings: besom.types.Output[scala.Option[Boolean]]
  ) derives ProviderArgsEncoder

  object ProviderArgs:
    def apply(
      cluster: besom.types.Input.Optional[String] = None,
      context: besom.types.Input.Optional[String] = None,
      deleteUnreachable: besom.types.Input.Optional[Boolean] = None,
      enableConfigMapMutable: besom.types.Input.Optional[Boolean] = None,
      enableServerSideApply: besom.types.Input.Optional[Boolean] = None,
      kubeconfig: besom.types.Input.Optional[String] = None,
      namespace: besom.types.Input.Optional[String] = None,
      renderYamlToDirectory: besom.types.Input.Optional[String] = None,
      skipUpdateUnreachable: besom.types.Input.Optional[Boolean] = None,
      suppressDeprecationWarnings: besom.types.Input.Optional[Boolean] = None,
      suppressHelmHookWarnings: besom.types.Input.Optional[Boolean] = None
    )(using besom.types.Context): ProviderArgs =
      new ProviderArgs(
        cluster = cluster.asOptionOutput(isSecret = false),
        context = context.asOptionOutput(isSecret = false),
        deleteUnreachable = deleteUnreachable.asOptionOutput(isSecret = false),
        enableConfigMapMutable = enableConfigMapMutable.asOptionOutput(isSecret = false),
        enableServerSideApply = enableServerSideApply.asOptionOutput(isSecret = false),
        kubeconfig = kubeconfig.asOptionOutput(isSecret = false),
        namespace = namespace.asOptionOutput(isSecret = false),
        renderYamlToDirectory = renderYamlToDirectory.asOptionOutput(isSecret = false),
        skipUpdateUnreachable = skipUpdateUnreachable.asOptionOutput(isSecret = false),
        suppressDeprecationWarnings = suppressDeprecationWarnings.asOptionOutput(isSecret = false),
        suppressHelmHookWarnings = suppressHelmHookWarnings.asOptionOutput(isSecret = false)
      )

end ProviderArgsEncoderTest

class ProviderArgsEncoderTest extends munit.FunSuite with ValueAssertions:
  import EncoderTest.*
  import ProviderArgsEncoderTest.*

  test("simple args") {
    given Context = DummyContext().unsafeRunSync()
    val pae       = summon[ProviderArgsEncoder[TestProviderArgs]]

    val (res, encoded) = pae
      .encode(
        TestProviderArgs(
          Output("SOME-TEST-PROVIDER"),
          Output(PlainCaseClass(data = "werks?", moreData = 123))
        ),
        _ => false
      )
      .unsafeRunSync()

    assert(res.contains("type"), res)
    assert(res.contains("pcc"), res)

    val expected =
      Map(
        "type" -> "SOME-TEST-PROVIDER".asValue,
        "pcc" -> Map(
          "data" -> "werks?".asValue,
          "moreData" -> 123.asValue
        ).asValue.asJsonStringOrThrow.asValue
      ).asValue

    assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
  }

  test("optional args") {
    given Context = DummyContext().unsafeRunSync()
    val pae       = summon[ProviderArgsEncoder[TestProviderOptionArgs]]

    val args = TestProviderOptionArgs(
      Output(None),
      Output(None)
    )
    val (res, encoded) = pae.encode(args, _ => false).unsafeRunSync()

    assert(res.isEmpty, res)

    val expected =
      Map(
        "type" -> Null,
        "pcc" -> Null
      ).asValue

    assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
  }

  test("encode Kubernetes ProviderArgs") {
    given Context = DummyContext().unsafeRunSync()
    val pae       = summon[ProviderArgsEncoder[ProviderArgs]]

    val args         = ProviderArgs(kubeconfig = "abcd")
    val (_, encoded) = pae.encode(args, _ => false).unsafeRunSync()

    val expected = Map("kubeconfig" -> "abcd".asValue).asValue

    assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
  }

  // FIXME: This test is failing because the `ProviderArgsEncoder` is not encoding the `kubeconfig` as a secret
  test("encode json secret".ignore) {
    given Context = DummyContext().unsafeRunSync()
    val pae       = summon[ProviderArgsEncoder[ProviderArgs]]

    val args         = ProviderArgs(kubeconfig = Output.secret("abcd"))
    val (_, encoded) = pae.encode(args, _ => false).unsafeRunSync()

    val expected = Map("kubeconfig" -> "abcd".asValue.asSecret).asValue

    assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
  }

  test("encode json") {
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

    val encoded = res.serialized.asValue

    val expected =
      Map(
        "str" -> "x".asValue,
        "b" -> true.asValue.asJsonStringOrThrow.asValue,
        "helper" -> Map("int" -> 1.asValue).asValue.asJsonStringOrThrow.asValue
      ).asValue

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }

  test("encode empty json secrets") {
    given Context = DummyContext().unsafeRunSync()

    val res = PropertiesSerializer
      .serializeResourceProperties(
        ExampleResourceArgs(
          Output.secret(None),
          Output.secret(None),
          Output.secret(None)
        )
      )
      .unsafeRunSync()

    val encoded  = res.serialized.asValue
    val expected = Map.empty[String, Value].asValue

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }
end ProviderArgsEncoderTest

class Regression383Test extends munit.FunSuite with ValueAssertions:
  import Regression383Test.*

  test("#383 regression") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[ArgsEncoder[SecretArgs]]

    val (_, encoded) = e.encode(SecretArgs(), _ => false).unsafeRunSync()
    val expected     = Map.empty[String, Value].asValue

    assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
  }

object Regression383Test:
  final case class SecretArgs private (
    data: besom.types.Output[Option[Map[String, String]]]
  )

  object SecretArgs:
    def apply(
      data: besom.types.Input.Optional[Map[String, Input[String]]] = None
    )(using besom.types.Context): SecretArgs =
      new SecretArgs(
        data = data.asOptionOutput(isSecret = true)
      )

  given encoder(using besom.types.Context): besom.types.Encoder[SecretArgs] =
    besom.internal.Encoder.derived[SecretArgs]
  given argsEncoder(using besom.types.Context): besom.types.ArgsEncoder[SecretArgs] =
    besom.internal.ArgsEncoder.derived[SecretArgs]
end Regression383Test

class RecurrentArgsTest extends munit.FunSuite with ValueAssertions:
  case class Recurrent(value: Option[Recurrent])
  object Recurrent:
    given encoder(using besom.types.Context): besom.types.Encoder[Recurrent] =
      besom.internal.Encoder.derived[Recurrent]

  test("encode recurrent type") {
    given Context = DummyContext().unsafeRunSync()
    val e         = summon[Encoder[Recurrent]]

    val (_, encoded) = e.encode(Recurrent(Some(Recurrent(Some(Recurrent(Some(Recurrent(None)))))))).unsafeRunSync()
    val expected =
      Map("value" -> Map("value" -> Map("value" -> Map.empty[String, Value].asValue).asValue).asValue).asValue

    assertEqualsValue(encoded, expected, encoded.toProtoString)
  }
end RecurrentArgsTest

class InternalTest extends munit.FunSuite:
  import ProtobufUtil.*

  for isSecret <- List(true, false)
  do {
    test(s"isEmptySecretValue (isSecret: $isSecret)") {
      val value = if isSecret then Null.asSecret else Null
      assertEquals(isEmptySecretValue(value), isSecret)
    }
  }

  test("SpecialSig from String") {
    import Constants.SpecialSig
    assertEquals(SpecialSig.fromString(SpecialSig.AssetSig.asString), Some(SpecialSig.AssetSig))
    assertEquals(SpecialSig.fromString(SpecialSig.ArchiveSig.asString), Some(SpecialSig.ArchiveSig))
    assertEquals(SpecialSig.fromString(SpecialSig.SecretSig.asString), Some(SpecialSig.SecretSig))
    assertEquals(SpecialSig.fromString(SpecialSig.ResourceSig.asString), Some(SpecialSig.ResourceSig))
    assertEquals(SpecialSig.fromString("wrong"), None)
  }
end InternalTest