package besom.internal

import besom.internal.Encoder.*
import besom.internal.ProtobufUtil.*
import besom.internal.RunResult.{*, given}
import besom.types.{EnumCompanion, Label, StringEnum, Output as _, *}
import besom.util.*
import com.google.protobuf.struct.*
import com.google.protobuf.struct.Struct.toJavaProto
import com.google.protobuf.struct.Value.Kind

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
    val e         = summon[Encoder[TestCaseClass]]

    val expected = Map(
      "foo" -> 10.asValue,
      "bar" -> List("qwerty".asValue).asValue,
      "optNone1" -> Null,
      "optSome" -> Some("abc").asValue
    ).asValue
    val (_, encoded) = e.encode(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))).unsafeRunSync()
    assertEqualsValue(encoded, expected)
  }

  test("encode null") {
    val e         = summon[Encoder[Option[String]]]

    val (_, encoded) = e.encode(None).unsafeRunSync()

    assertEqualsValue(encoded, Null)
  }

  test("encode special case class with unmangling") {
    val e         = summon[Encoder[SpecialCaseClass]]

    val expected = Map(
      "equals" -> 10.asValue,
      "eq" -> "abc".asValue,
      "normalOne" -> "qwerty".asValue
    ).asValue
    val (_, encoded) = e.encode(SpecialCaseClass(10, "abc", "qwerty")).unsafeRunSync()

    assertEqualsValue(encoded, expected)
  }

  test("encoder enum") {
    val e         = summon[Encoder[TestEnum]]

    assertEqualsValue(e.encode(TestEnum.Test1).unsafeRunSync()._2, "Test1 value".asValue)
    assertEqualsValue(e.encode(TestEnum.AnotherTest).unsafeRunSync()._2, "AnotherTest value".asValue)
    assertEqualsValue(e.encode(TestEnum.`weird-test`).unsafeRunSync()._2, "weird-test value".asValue)
  }

  test("optional Encoder test") {
    val e         = summon[Encoder[OptionCaseClass]]

    val (res, value) = e
      .encode(
        OptionCaseClass(None, None)
      )
      .unsafeRunSync()

    assert(res.isEmpty)
    assert(value.getStructValue.fields.isEmpty, value)
  }

class ArgsEncoderTest extends munit.FunSuite with ValueAssertions:
  import EncoderTest.*

  test("ArgsEncoder works") {
    given Context = DummyContext().unsafeRunSync()
    val ae        = summon[ArgsEncoder[TestArgs]]

    val (res, value) = ae
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
    assert(value.fields("a").kind.isStringValue, value)
    assert(value.fields("b").kind.isStructValue, value)
    assertEquals(value.fields("a").getStringValue, "SOME-TEST-PROVIDER")
    assertEquals(value.fields("b").getStructValue.fields("data").getStringValue, "werks?")
  }

  test("optional ArgsEncoder test") {
    given Context = DummyContext().unsafeRunSync()
    val ae        = summon[ArgsEncoder[TestOptionArgs]]

    val (res, value) = ae
      .encode(
        TestOptionArgs(
          Output(None),
          Output(None)
        ),
        _ => false
      )
      .unsafeRunSync()

    assert(res.isEmpty, res)
    assert(value.fields.isEmpty, value)
  }

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

  test("simple ProviderArgsEncoder test") {
    given Context = DummyContext().unsafeRunSync()
    val pae       = summon[ProviderArgsEncoder[TestProviderArgs]]

    val (res, value) = pae
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
    assert(value.fields("type").kind.isStringValue, value.fields("type"))
    assert(value.fields("pcc").kind.isStringValue, value.fields("pcc"))
    assertEquals(value.fields("type").getStringValue, "SOME-TEST-PROVIDER")
    assertEquals(value.fields("pcc").getStringValue, "{\"data\":\"werks?\",\"moreData\":123.0}")
  }

  test("optional ProviderArgsEncoder test") {
    given Context = DummyContext().unsafeRunSync()
    val pae       = summon[ProviderArgsEncoder[TestProviderOptionArgs]]

    val args = TestProviderOptionArgs(
      Output(None),
      Output(None)
    )
    val (res, value) = pae.encode(args, _ => false).unsafeRunSync()

    assert(res.isEmpty, res)
    assert(value.fields.isEmpty, value)
  }

  test("encode ProviderArgs") {
    given Context = DummyContext().unsafeRunSync()
    val pae       = summon[ProviderArgsEncoder[ProviderArgs]]

    val expected = Map("kubeconfig" -> "abcd".asValue).asValue

    val args         = ProviderArgs(kubeconfig = "abcd")
    val (_, encoded) = pae.encode(args, _ => false).unsafeRunSync()
    assertEqualsValue(Value(Kind.StructValue(encoded)), expected)
  }

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
