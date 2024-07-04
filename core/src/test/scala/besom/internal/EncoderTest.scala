package besom.internal

import besom.internal.Encoder.*
import besom.internal.ProtobufUtil.{*, given}
import besom.internal.RunResult.{*, given}
import besom.types.{EnumCompanion, Label, StringEnum, Output as _, *}
import besom.util.*
import com.google.protobuf.struct.*

def runWithBothOutputCodecs(body: Context ?=> Unit)(using munit.Location): Unit =
  Vector(true, false).foreach(keepOutputValues => {
    given Context =
      DummyContext(featureSupport = DummyContext.dummyFeatureSupport.copy(keepOutputValues = keepOutputValues)).unsafeRunSync()
    body
  })

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

  runWithBothOutputCodecs {
    test(s"encode case class (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[TestCaseClass]]

      val (_, encoded) = e.encode(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))).unsafeRunSync()
      val expected = Map(
        "foo" -> 10.asValue,
        "bar" -> List("qwerty".asValue).asValue,
        "optSome" -> Some("abc").asValue
      ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode null (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[Option[String]]]

      val (metadata, encoded) = e.encode(None).unsafeRunSync()
      val expected            = Null
      val expectedMetadata    = Metadata(known = true, secret = false, empty = true, Nil)

      assertEqualsValue(encoded, expected, encoded.toProtoString)
      assertEquals(metadata, expectedMetadata)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode optional (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[Option[String]]]

      val (metadata, encoded) = e.encode(Some("abc")).unsafeRunSync()
      val expected            = "abc".asValue
      val expectedMetadata    = Metadata(known = true, secret = false, empty = false, Nil)

      assertEqualsValue(encoded, expected, encoded.toProtoString)
      assertEquals(metadata, expectedMetadata)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode output null (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[Output[Option[String]]]]

      val (_, encoded) = e.encode(Output(None)).unsafeRunSync()
      val expected =
        if Context().featureSupport.keepOutputValues
        then Null.asOutputValue(isSecret = false, dependencies = Nil)
        else Null

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode secret null (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[Output[Option[String]]]]

      val (_, encoded) = e.encode(Output.secret(None)).unsafeRunSync()
      val expected =
        if Context().featureSupport.keepOutputValues
        then Null.asOutputValue(isSecret = true, dependencies = Nil)
        else Null.asSecretValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode secret input map (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[Output[Option[Map[String, String]]]]]

      val data: Input.Optional[Map[String, besom.types.Input[String]]] = None
      val (_, encoded)                                                 = e.encode(data.asOptionOutput(isSecret = true)).unsafeRunSync()

      val expected =
        if Context().featureSupport.keepOutputValues
        then Null.asValue.asOutputValue(isSecret = true, dependencies = Nil)
        else Null.asSecretValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode class with secret inputs (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[InputOptionalCaseClass]]

      val (_, encoded) = e.encode(InputOptionalCaseClass(Output.secret(None), Output.secret(None))).unsafeRunSync()
      val expected     = Map.empty[String, Value].asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode special case class with unmangling (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[SpecialCaseClass]]

      val (_, encoded) = e.encode(SpecialCaseClass(10, "abc", "qwerty")).unsafeRunSync()
      val expected = Map(
        "equals" -> 10.asValue,
        "eq" -> "abc".asValue,
        "normalOne" -> "qwerty".asValue
      ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"encoder enum (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[TestEnum]]

      assertEqualsValue(e.encode(TestEnum.Test1).unsafeRunSync()._2, "Test1 value".asValue)
      assertEqualsValue(e.encode(TestEnum.AnotherTest).unsafeRunSync()._2, "AnotherTest value".asValue)
      assertEqualsValue(e.encode(TestEnum.`weird-test`).unsafeRunSync()._2, "weird-test value".asValue)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode optional case class (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[OptionCaseClass]]

      val (m, encoded) = e
        .encode(
          OptionCaseClass(None, None)
        )
        .unsafeRunSync()

      assert(encoded.getStructValue.fields.isEmpty, encoded.toProtoString)
      assert(m.dependencies.isEmpty)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode a union of string and case class (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[String | TestCaseClass]]

      val (_, encodedString) = e.encode("abc").unsafeRunSync()
      val expectedString     = "abc".asValue

      assertEqualsValue(encodedString, expectedString, encodedString.toProtoString)

      val (_, encodedCaseClass) = e.encode(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))).unsafeRunSync()
      val expectedCaseClass = Map(
        "foo" -> 10.asValue,
        "bar" -> List("qwerty".asValue).asValue,
        "optSome" -> Some("abc").asValue
      ).asValue

      assertEqualsValue(encodedCaseClass, expectedCaseClass, encodedCaseClass.toProtoString)
    }
  }
end EncoderTest

class ArgsEncoderTest extends munit.FunSuite with ValueAssertions:
  import EncoderTest.*

  runWithBothOutputCodecs {
    test(s"simple args (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val ae = summon[ArgsEncoder[TestArgs]]

      val (res, encoded) = ae
        .encode(
          TestArgs(
            Output("SOME-TEST-PROVIDER"),
            Output(PlainCaseClass(data = "werks?", moreData = 123))
          ),
          _ => false
        )
        .unsafeRunSync()

      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "a" -> "SOME-TEST-PROVIDER".asValue.asOutputValue(isSecret = false, dependencies = Nil),
            "b" -> Map(
              "data" -> "werks?".asValue,
              "moreData" -> 123.asValue
            ).asValue.asOutputValue(isSecret = false, dependencies = Nil)
          ).asValue
        else
          Map(
            "a" -> "SOME-TEST-PROVIDER".asValue,
            "b" -> Map(
              "data" -> "werks?".asValue,
              "moreData" -> 123.asValue
            ).asValue
          ).asValue

      assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
      assert(res.contains("a"), res)
      assert(res.contains("b"), res)
    }
  }

  runWithBothOutputCodecs {
    test(s"optional args (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val ae = summon[ArgsEncoder[TestOptionArgs]]

      val (res, encoded) = ae
        .encode(
          TestOptionArgs(
            Output(None),
            Output(None)
          ),
          _ => false
        )
        .unsafeRunSync()

      val expected = Map.empty[String, Value].asValue

      assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)

      assert(res.isEmpty, res)
    }
  }

  runWithBothOutputCodecs {
    test(s"empty secret args (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val ae = summon[ArgsEncoder[InputOptionalCaseClass]]

      val (res, encoded) = ae
        .encode(
          InputOptionalCaseClass(Output.secret(None), Output.secret(None)),
          _ => false
        )
        .unsafeRunSync()

      val expected = Map.empty[String, Value].asValue

      assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
      assert(res.isEmpty, res)
    }
  }

  runWithBothOutputCodecs {
    test(s"secret args (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val ae = summon[ArgsEncoder[InputOptionalCaseClass]]

      val (res, encoded) = ae
        .encode(
          InputOptionalCaseClass(
            Output.secret(Some("secret1")),
            Output.secret(Some(Map("key" -> Output.secret("value1"))))
          ),
          _ => false
        )
        .unsafeRunSync()

      val expected =
        if Context().featureSupport.keepOutputValues then
          Map(
            "value" -> "secret1".asValue.asOutputValue(isSecret = true, dependencies = Nil),
            "data" -> Map(
              "key" -> ("value1" +
                "").asValue.asOutputValue(isSecret = true, dependencies = Nil)
            ).asValue.asOutputValue(isSecret = true, dependencies = Nil)
          ).asValue
        else
          Map(
            "value" -> "secret1".asValue.asSecretValue,
            "data" -> Map("key" -> "value1".asValue.asSecretValue).asValue.asSecretValue
          ).asValue

      assert(res.keySet == Set("value", "data"), res)
      assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
    }
  }
end ArgsEncoderTest

object ProviderArgsEncoderTest:
  import EncoderTest.*

  case class TestProviderArgs(
    `type`: Output[String],
    pcc: Output[PlainCaseClass],
    ls: Output[List[Output[String]]]
  ) derives ProviderArgsEncoder
  case class TestProviderOptionArgs(
    `type`: Output[Option[String]],
    pcc: Output[Option[PlainCaseClass]],
    ls: Output[Option[List[Output[String]]]]
  ) derives ProviderArgsEncoder

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
    cluster: Output[Option[String]],
    context: Output[Option[String]],
    deleteUnreachable: Output[Option[Boolean]],
    enableConfigMapMutable: Output[Option[Boolean]],
    enableServerSideApply: Output[Option[Boolean]],
    kubeconfig: Output[Option[String]],
    namespace: Output[Option[String]],
    renderYamlToDirectory: Output[Option[String]],
    skipUpdateUnreachable: Output[Option[Boolean]],
    suppressDeprecationWarnings: Output[Option[Boolean]],
    suppressHelmHookWarnings: Output[Option[Boolean]]
  ) derives ProviderArgsEncoder

  object ProviderArgs:
    def apply(
      cluster: Input.Optional[String] = None,
      context: Input.Optional[String] = None,
      deleteUnreachable: Input.Optional[Boolean] = None,
      enableConfigMapMutable: Input.Optional[Boolean] = None,
      enableServerSideApply: Input.Optional[Boolean] = None,
      kubeconfig: Input.Optional[String] = None,
      namespace: Input.Optional[String] = None,
      renderYamlToDirectory: Input.Optional[String] = None,
      skipUpdateUnreachable: Input.Optional[Boolean] = None,
      suppressDeprecationWarnings: Input.Optional[Boolean] = None,
      suppressHelmHookWarnings: Input.Optional[Boolean] = None
    )(using Context): ProviderArgs =
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

  runWithBothOutputCodecs {
    test(s"simple args (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val pae = summon[ProviderArgsEncoder[TestProviderArgs]]

      val (res, encoded) = pae
        .encode(
          TestProviderArgs(
            Output("SOME-TEST-PROVIDER"),
            Output(PlainCaseClass(data = "werks?", moreData = 123)),
            Output(List(Output("a"), Output("b"), Output("c")))
          ),
          _ => false
        )
        .unsafeRunSync()

      assert(res.contains("type"), res)
      assert(res.contains("pcc"), res)

      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "type" -> "SOME-TEST-PROVIDER".asValue.asOutputValue(isSecret = false, dependencies = Nil).asValue,
            "pcc" -> Map(
              "data" -> "werks?".asValue,
              "moreData" -> 123.asValue
            ).asValue.asJsonStringOrThrow.asValue.asOutputValue(isSecret = false, dependencies = Nil).asValue,
            "ls" -> List("a", "b", "c").asValue.asJsonStringOrThrow.asValue.asOutputValue(isSecret = false, dependencies = Nil).asValue
          ).asValue
        else
          Map(
            "type" -> "SOME-TEST-PROVIDER".asValue,
            "pcc" -> Map(
              "data" -> "werks?".asValue,
              "moreData" -> 123.asValue
            ).asValue.asJsonStringOrThrow.asValue,
            "ls" -> List("a", "b", "c").asValue.asJsonStringOrThrow.asValue
          ).asValue

      assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"optional args (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val pae = summon[ProviderArgsEncoder[TestProviderOptionArgs]]

      val args = TestProviderOptionArgs(
        Output(None),
        Output(None),
        Output(None)
      )
      val (res, encoded) = pae.encode(args, _ => false).unsafeRunSync()

      val expected = Map.empty[String, Value].asValue

      assertEqualsValue(encoded.asValue, expected)
      assert(res.isEmpty, res)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode Kubernetes ProviderArgs (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val pae = summon[ProviderArgsEncoder[ProviderArgs]]

      val args         = ProviderArgs(kubeconfig = "abcd")
      val (_, encoded) = pae.encode(args, _ => false).unsafeRunSync()

      // strings and nulls are not converted to JSON by the underlying implementation
      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "kubeconfig" -> "abcd".asValue.asOutputValue(isSecret = false, dependencies = Nil)
          ).asValue
        else Map("kubeconfig" -> "abcd".asValue).asValue

      assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
      assertEquals(encoded.fields.size, 1)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode json secret (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val pae = summon[ProviderArgsEncoder[ProviderArgs]]

      val args         = ProviderArgs(kubeconfig = Output.secret("abcd"))
      val (_, encoded) = pae.encode(args, _ => false).unsafeRunSync()

      // strings and nulls are not converted to JSON by the underlying implementation
      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "kubeconfig" -> "abcd".asValue.asOutputValue(isSecret = true, dependencies = Nil)
          ).asValue
        else Map("kubeconfig" -> "abcd".asValue.asSecretValue).asValue

      assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
    }
  }
end ProviderArgsEncoderTest

class PropertiesSerializerTest extends munit.FunSuite with ValueAssertions:
  import EncoderTest.*
  import ProviderArgsEncoderTest.*
  import PropertiesSerializerTest.*

  runWithBothOutputCodecs {
    test(s"encode normal secrets (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val res = PropertiesSerializer
        .serializeResourceProperties(
          InputOptionalCaseClass(
            Output.secret(Some("secret1")),
            Output(Some(Map("key" -> Output.secret("value1"))))
          )
        )
        .unsafeRunSync()

      val encoded = res.serialized.asValue
      val expected =
        if Context().featureSupport.keepOutputValues then
          Map(
            "value" -> "secret1".asValue.asOutputValue(isSecret = true, dependencies = Nil),
            "data" -> Map(
              "key" -> "value1".asValue.asOutputValue(isSecret = true, dependencies = Nil)
            ).asValue.asOutputValue(isSecret = true, dependencies = Nil)
          ).asValue
        else
          Map(
            "value" -> "secret1".asValue.asSecretValue,
            "data" -> Map("key" -> "value1".asValue.asSecretValue).asValue.asSecretValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
      assertEquals(res.containsUnknowns, false)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode json (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      // This is currently only used on arguments to provider resources.
      // When serializing properties with ProviderArgsEncoder to protobuf Struct, instead of sending their native
      // Struct representation, we send a protobuf String value with a JSON-formatted Struct representation
      // inside. In the tests below this will look like a double-JSON encoded string since we write asserts
      // against a JSON rendering of the proto struct.
      //
      // For a practical example of where this applies, see Provider resource in pulumi-kubernetes.

      val res = PropertiesSerializer
        .serializeResourceProperties(
          ExampleResourceArgs(
            Output(Some("x")),
            Output.secret(Some(true)),
            Output(Some(HelperArgs(Output(Some(1)))))
          )
        )
        .unsafeRunSync()

      val encoded = res.serialized.asValue

      val expected =
        if Context().featureSupport.keepOutputValues
        then
          Map(
            "str" -> "x".asValue.asOutputValue(isSecret = false, dependencies = Nil).asValue,
            "b" -> true.asValue.asJsonStringOrThrow.asValue.asOutputValue(isSecret = true, dependencies = Nil).asValue,
            "helper" ->
              Map("int" -> 1.asValue).asValue.asJsonStringOrThrow.asValue
                .asOutputValue(isSecret = false, dependencies = Nil)
                .asValue
          ).asValue
        else
          Map(
            "str" -> "x".asValue,
            "b" -> true.asValue.asJsonStringOrThrow.asValue.asSecretValue,
            "helper" -> Map("int" -> 1.asValue).asValue.asJsonStringOrThrow.asValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode empty json secrets (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val res = PropertiesSerializer
        .serializeResourceProperties(
          ExampleResourceArgs(
            Output.secret(None),
            Output.secret(None),
            Output.secret(None)
          )
        )
        .unsafeRunSync()

      val encoded = res.serialized.asValue
      val expected =
        if Context().featureSupport.keepOutputValues
        then Map.empty[String, Value].asValue
        else Map.empty[String, Value].asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode optional secrets (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val res = PropertiesSerializer
        .serializeResourceProperties(
          GetPublicKeyArgs("my-private-key")
        )
        .unsafeRunSync()

      val encoded = res.serialized.asValue
      val expected =
        if Context().featureSupport.keepOutputValues then
          Map(
            "privateKeyOpenssh" -> "my-private-key".asValue.asOutputValue(isSecret = true, dependencies = Nil)
          ).asValue
        else
          Map(
            "privateKeyOpenssh" -> "my-private-key".asValue.asSecretValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
      assertEquals(res.containsUnknowns, false)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode unknown optional secrets (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val res = PropertiesSerializer
        .serializeResourceProperties(
          GetPublicKeyArgs(Output.ofData(OutputData.unknown(isSecret = true)))
        )
        .unsafeRunSync()

      val encoded = res.serialized.asValue
      val expected =
        if Context().featureSupport.keepOutputValues then
          Map(
            "privateKeyOpenssh" -> OutputValue.unknown(isSecret = true, dependencies = Nil).asValue
          ).asValue
        else
          Map(
            "privateKeyOpenssh" -> SecretValue.unknown.asValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
      assertEquals(res.containsUnknowns, true)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode kubernetes secret with metadata (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val res = PropertiesSerializer
        .serializeResourceProperties(
          SecretArgs(
            metadata = ObjectMetaArgs(
              name = "test-secret2",
              namespace = "test"
              // intentionally skip uid and let it be default
            ),
            data = Map(
              "username" -> "dGVzdC11c2Vy",
              "password" -> "dGVzdC1wYXNzd29yZA=="
            )
          )
        )
        .unsafeRunSync()

      val encoded = res.serialized.asValue
      val expected =
        if Context().featureSupport.keepOutputValues then
          Map(
            "kind" -> "Secret".asValue.asOutputValue(isSecret = false, Nil),
            "apiVersion" -> "v1".asValue.asOutputValue(isSecret = false, Nil),
            "metadata" -> Map(
              "name" -> "test-secret2".asValue.asOutputValue(isSecret = false, Nil),
              "namespace" -> "test".asValue.asOutputValue(isSecret = false, Nil)
            ).asValue.asOutputValue(isSecret = false, Nil),
            "data" -> Map(
              "username" -> "dGVzdC11c2Vy".asValue,
              "password" -> "dGVzdC1wYXNzd29yZA==".asValue
            ).asValue.asOutputValue(isSecret = true, Nil)
          ).asValue
        else
          Map(
            "kind" -> "Secret".asValue,
            "apiVersion" -> "v1".asValue,
            "metadata" -> Map(
              "name" -> "test-secret2".asValue,
              "namespace" -> "test".asValue
            ).asValue,
            "data" -> Map(
              "username" -> "dGVzdC11c2Vy".asValue,
              "password" -> "dGVzdC1wYXNzd29yZA==".asValue
            ).asValue.asSecretValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
      assertEquals(res.containsUnknowns, false)
    }
  }

  runWithBothOutputCodecs {
    test(s"encode nested resource (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val testId  = "my-id"
      val testUrn = "urn:pulumi:test::issue::awsx:lb:ApplicationLoadBalancer$aws:lb/targetGroup:TargetGroup::loadbalancer"
      val res = PropertiesSerializer
        .serializeResourceProperties(
          FargateServiceTaskDefinitionArgs(
            container = Output(
              Some(
                TaskDefinitionContainerDefinitionArgs(
                  portMappings = Output(
                    Some(
                      List(
                        TaskDefinitionPortMappingArgs(
                          targetGroup = Output(
                            Some(
                              TargetGroup(
                                urn = URN.parse(testUrn),
                                id = Output(testId)
                              )
                            )
                          )
                        )
                      )
                    )
                  )
                )
              )
            ),
            containers = Output(None)
          )
        )
        .unsafeRunSync()

      val encoded = res.serialized.asValue
      val expected =
        if Context().featureSupport.keepOutputValues then
          Map(
            "container" -> Map(
              "portMappings" -> List(
                Map(
                  "targetGroup" -> Map(
                    Constants.SpecialSig.Key -> Constants.SpecialSig.ResourceSig.asValue,
                    "urn" -> testUrn.asValue,
                    "id" -> testId.asValue
                  ).asValue.asOutputValue(isSecret = false, Nil)
                ).asValue
              ).asValue.asOutputValue(isSecret = false, Nil)
            ).asValue.asOutputValue(isSecret = false, Nil)
          ).asValue
        else
          Map(
            "container" -> Map(
              "portMappings" -> List(
                Map(
                  "targetGroup" -> Map(
                    Constants.SpecialSig.Key -> Constants.SpecialSig.ResourceSig.asValue,
                    "urn" -> testUrn.asValue,
                    "id" -> testId.asValue
                  ).asValue
                ).asValue
              ).asValue
            ).asValue
          ).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
      assertEquals(res.containsUnknowns, false)
    }
  }

end PropertiesSerializerTest
object PropertiesSerializerTest:
  final case class GetPublicKeyArgs private (
    privateKeyOpenssh: Output[Option[String]],
    privateKeyPem: Output[Option[String]]
  )
  object GetPublicKeyArgs:
    def apply(
      privateKeyOpenssh: Input.Optional[String] = scala.None,
      privateKeyPem: Input.Optional[String] = scala.None
    )(using Context): GetPublicKeyArgs =
      new GetPublicKeyArgs(
        privateKeyOpenssh = privateKeyOpenssh.asOptionOutput(isSecret = true),
        privateKeyPem = privateKeyPem.asOptionOutput(isSecret = true)
      )
    given encoder(using Context): Encoder[GetPublicKeyArgs]         = Encoder.derived[GetPublicKeyArgs]
    given argsEncoder(using Context): ArgsEncoder[GetPublicKeyArgs] = ArgsEncoder.derived[GetPublicKeyArgs]

  final case class SecretArgs private (
    apiVersion: Output[String],
    data: Output[Option[Map[String, String]]],
    kind: Output[String],
    metadata: Output[Option[ObjectMetaArgs]]
  )
  object SecretArgs:
    def apply(
      data: Input.Optional[Map[String, besom.types.Input[String]]] = scala.None,
      metadata: Input.Optional[ObjectMetaArgs] = scala.None
    )(using Context): SecretArgs =
      new SecretArgs(
        apiVersion = besom.types.Output("v1"),
        data = data.asOptionOutput(isSecret = true),
        kind = besom.types.Output("Secret"),
        metadata = metadata.asOptionOutput(isSecret = false)
      )
    given encoder(using Context): Encoder[SecretArgs]         = Encoder.derived[SecretArgs]
    given argsEncoder(using Context): ArgsEncoder[SecretArgs] = ArgsEncoder.derived[SecretArgs]

  final case class ObjectMetaArgs private (
    name: Output[Option[String]],
    namespace: Output[Option[String]],
    uid: Output[scala.Option[String]]
  )
  object ObjectMetaArgs:
    def apply(
      name: Input.Optional[String] = scala.None,
      namespace: Input.Optional[String] = scala.None,
      uid: Input.Optional[String] = scala.None
    )(using Context): ObjectMetaArgs =
      new ObjectMetaArgs(
        name = name.asOptionOutput(isSecret = false),
        namespace = namespace.asOptionOutput(isSecret = false),
        uid = uid.asOptionOutput(isSecret = false)
      )
    given encoder(using Context): Encoder[ObjectMetaArgs]         = Encoder.derived[ObjectMetaArgs]
    given argsEncoder(using Context): ArgsEncoder[ObjectMetaArgs] = ArgsEncoder.derived[ObjectMetaArgs]

  final case class FargateServiceTaskDefinitionArgs(
    container: Output[Option[TaskDefinitionContainerDefinitionArgs]],
    containers: Output[Option[Map[String, TaskDefinitionContainerDefinitionArgs]]]
  )
  object FargateServiceTaskDefinitionArgs:
    given encoder(using Context): Encoder[FargateServiceTaskDefinitionArgs]         = Encoder.derived[FargateServiceTaskDefinitionArgs]
    given argsEncoder(using Context): ArgsEncoder[FargateServiceTaskDefinitionArgs] = ArgsEncoder.derived[FargateServiceTaskDefinitionArgs]
  final case class TaskDefinitionContainerDefinitionArgs(portMappings: Output[Option[List[TaskDefinitionPortMappingArgs]]])
  object TaskDefinitionContainerDefinitionArgs:
    given encoder(using Context): Encoder[TaskDefinitionContainerDefinitionArgs] =
      Encoder.derived[TaskDefinitionContainerDefinitionArgs]
    given argsEncoder(using Context): ArgsEncoder[TaskDefinitionContainerDefinitionArgs] =
      ArgsEncoder.derived[TaskDefinitionContainerDefinitionArgs]
  final case class TaskDefinitionPortMappingArgs(targetGroup: Output[Option[TargetGroup]])
  object TaskDefinitionPortMappingArgs:
    given encoder(using Context): Encoder[TaskDefinitionPortMappingArgs]         = Encoder.derived[TaskDefinitionPortMappingArgs]
    given argsEncoder(using Context): ArgsEncoder[TaskDefinitionPortMappingArgs] = ArgsEncoder.derived[TaskDefinitionPortMappingArgs]
  final case class TargetGroup(urn: Output[URN], id: Output[ResourceId]) extends besom.CustomResource
end PropertiesSerializerTest

class Regression383Test extends munit.FunSuite with ValueAssertions:
  import Regression383Test.*

  runWithBothOutputCodecs {
    test(s"#383 regression (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[ArgsEncoder[SecretArgs]]

      val (_, encoded) = e.encode(SecretArgs(), _ => false).unsafeRunSync()
      val expected     = Map.empty[String, Value].asValue

      assertEqualsValue(encoded.asValue, expected, encoded.asValue.toProtoString)
    }
  }

object Regression383Test:
  final case class SecretArgs private (data: Output[Option[Map[String, String]]])
  object SecretArgs:
    def apply(data: Input.Optional[Map[String, Input[String]]] = None)(using Context): SecretArgs =
      new SecretArgs(data = data.asOptionOutput(isSecret = true))
    given encoder(using Context): Encoder[SecretArgs]         = Encoder.derived[SecretArgs]
    given argsEncoder(using Context): ArgsEncoder[SecretArgs] = ArgsEncoder.derived[SecretArgs]
end Regression383Test

class RecurrentArgsTest extends munit.FunSuite with ValueAssertions:
  case class Recurrent(value: Option[Recurrent])
  object Recurrent:
    given encoder(using Context): Encoder[Recurrent] = Encoder.derived[Recurrent]

  runWithBothOutputCodecs {
    test(s"encode recurrent type (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      val e = summon[Encoder[Recurrent]]

      // the empty structs annot collapse to just one empty map, the structure need to be preserved
      val (_, encoded) = e.encode(Recurrent(Some(Recurrent(Some(Recurrent(Some(Recurrent(None)))))))).unsafeRunSync()
      val expected =
        Map("value" -> Map("value" -> Map("value" -> Map.empty[String, Value].asValue).asValue).asValue).asValue

      assertEqualsValue(encoded, expected, encoded.toProtoString)
    }
  }
end RecurrentArgsTest

class InternalTest extends munit.FunSuite with ValueAssertions:
  import Constants.*

  test("SpecialSig from String") {
    assertEquals(SpecialSig.fromString(SpecialSig.AssetSig.asString), Some(SpecialSig.AssetSig))
    assertEquals(SpecialSig.fromString(SpecialSig.ArchiveSig.asString), Some(SpecialSig.ArchiveSig))
    assertEquals(SpecialSig.fromString(SpecialSig.SecretSig.asString), Some(SpecialSig.SecretSig))
    assertEquals(SpecialSig.fromString(SpecialSig.ResourceSig.asString), Some(SpecialSig.ResourceSig))
    assertEquals(SpecialSig.fromString(SpecialSig.OutputSig.asString), Some(SpecialSig.OutputSig))
    assertEquals(SpecialSig.fromString("wrong"), None)
  }

  test("ResourceValue - apply - without id") {
    val urn = "urn-value".asValue
    ResourceValue(urn) match
      case Left(e) => fail(s"Unexpected error: $e")
      case Right(v) =>
        assertEquals(v.urn, urn)
        assertEquals(v.id, None)
        assertEqualsValue(
          v.asValue,
          Map(
            SpecialSig.Key -> SpecialSig.ResourceSig.asValue,
            ResourceUrnName -> urn
          ).asValue,
          v.asValue.toProtoString
        )
  }

  test("ResourceValue - apply - with id") {
    val id  = "id-value".asValue
    val urn = "urn-value".asValue
    ResourceValue(urn, id) match
      case Left(e) => fail(s"Unexpected error: $e")
      case Right(v) =>
        assertEquals(v.urn, urn)
        assertEquals(v.id, Some(id))
        assertEqualsValue(
          v.asValue,
          Map(
            SpecialSig.Key -> SpecialSig.ResourceSig.asValue,
            ResourceUrnName -> urn,
            ResourceIdName -> id
          ).asValue,
          v.asValue.toProtoString
        )
  }

  test("ResourceValue - apply - output") {
    val id     = "id-value".asValue
    val idOut  = id.asOutputValue(isSecret = false, dependencies = Nil)
    val urn    = "urn-value".asValue
    val urnOut = urn.asOutputValue(isSecret = false, dependencies = Nil)
    ResourceValue(urnOut, idOut) match
      case Left(e) => fail(s"Unexpected error: $e")
      case Right(v) =>
        assertEquals(v.urn, urn)
        assertEquals(v.id, Some(id))
        assertEqualsValue(
          v.asValue,
          Map(
            SpecialSig.Key -> SpecialSig.ResourceSig.asValue,
            ResourceUrnName -> urn,
            ResourceIdName -> id
          ).asValue,
          v.asValue.toProtoString
        )
  }

  test("ResourceValue - apply - unknown id") {
    val id  = UnknownStringValue.asValue
    val urn = "urn-value".asValue
    ResourceValue(urn, id) match
      case Left(e) => fail(s"Unexpected error: $e")
      case Right(v) =>
        assertEquals(v.urn, urn)
        assertEquals(v.id, Some(ResourceId.empty.asValue))
        assertEqualsValue(
          v.asValue,
          Map(
            SpecialSig.Key -> SpecialSig.ResourceSig.asValue,
            ResourceUrnName -> urn,
            ResourceIdName -> ResourceId.empty.asValue
          ).asValue,
          v.asValue.toProtoString
        )
  }

  test("AggregatedDecodingError - aggregated message") {
    val errors = NonEmptyVector(
      DecodingError("error1", label = Label.fromNameAndType("dummy1", "dummy:pkg:Dummy1"), cause = Exception("cause1")),
      DecodingError("error2", label = Label.fromNameAndType("dummy2", "dummy:pkg:Dummy1")),
      DecodingError("error3", label = Label.fromNameAndType("dummy3", "dummy:pkg:Dummy1"))
    )
    val aggregated = AggregatedDecodingError(errors)
    val expectedMessage =
      """Decoding Errors [3]:
        |  error1
        |  error2
        |  error3
        |(with aggregate stack trace)""".stripMargin
    assertEquals(aggregated.message, expectedMessage)

    val t = interceptMessage[AggregatedDecodingError](expectedMessage) {
      throw aggregated
    }

    val stackTrace = com.google.common.base.Throwables.getStackTraceAsString(t)

    assert(stackTrace.contains("Caused by: besom.internal.DecodingError: [dummy1[dummy:pkg:Dummy1]] error1"), stackTrace)
    assert(stackTrace.contains("Suppressed: besom.internal.DecodingError: [dummy2[dummy:pkg:Dummy1]] error2"), stackTrace)
    assert(stackTrace.contains("Suppressed: besom.internal.DecodingError: [dummy3[dummy:pkg:Dummy1]] error3"), stackTrace)
    assert(stackTrace.contains("Caused by: java.lang.Exception: cause1"), stackTrace)
  }

end InternalTest
