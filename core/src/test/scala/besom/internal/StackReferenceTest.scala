package besom.internal

import besom.*
import besom.json.*
import RunResult.{*, given}
import besom.internal.logging.BesomLogger
import com.google.protobuf.struct.Value

class StackReferenceTest extends munit.FunSuite:

  test("convert stack reference to case class") {
    given Context = DummyContext().unsafeRunSync()
    case class Test(s: String, i: Int) derives JsonReader
    val expected = Test("value1", 2)
    val outputs  = Map("s" -> JsString("value1"), "i" -> JsNumber(2))

    val requireObject = StackReference.requireObject[Test](Output(outputs), Output(Set.empty))
    assertEquals(requireObject.getData.unsafeRunSync(), OutputData(expected))
  }

  test("fail when convert stack reference to case class with missing data") {
    given Context = DummyContext().unsafeRunSync()
    case class Test(s: String, i: Int) derives JsonReader
    val outputs = Map("s" -> JsString("value1"))

    val requireObject = StackReference.requireObject[Test](Output(outputs), Output(Set.empty))
    intercept[besom.json.DeserializationException](requireObject.getData.unsafeRunSync())
  }

  test("convert stack reference to case class with secret field") {
    given Context = DummyContext().unsafeRunSync()
    case class Test(s: String, i: Int) derives JsonReader
    val expected          = Test("value1", 2)
    val outputs           = Map("s" -> JsString("value1"), "i" -> JsNumber(2))
    val secretOutputNames = Set("i")

    val requireObject = StackReference.requireObject[Test](Output(outputs), Output(secretOutputNames))
    assertEquals(requireObject.getData.unsafeRunSync(), OutputData(expected).withIsSecret(true))
  }

  test("propagate secret field to whole typed stack reference") {
    given Context = DummyContext().unsafeRunSync()

    case class Test(s: String, i: Int) derives JsonReader
    val outputs           = Map("s" -> JsString("value1"), "i" -> JsNumber(2))
    val secretOutputNames = Set("i")

    val typedStackReference =
      StackReference
        .requireObject[Test](Output(outputs), Output(secretOutputNames))
        .map(test =>
          TypedStackReference(
            urn = Output(URN.empty),
            id = Output(ResourceId.empty),
            name = Output(""),
            outputs = test,
            secretOutputNames = Output(secretOutputNames)
          )
        )

    assertEquals(typedStackReference.getData.unsafeRunSync().secret, true)
  }

  test("correctly read typed stack references") {
    val outputs = Map("s" -> JsString("value1"), "i" -> JsNumber(2))

    class MockStackContext(
      override val runInfo: RunInfo,
      override val featureSupport: FeatureSupport,
      override val config: Config,
      override val logger: BesomLogger,
      override val monitor: Monitor,
      override val engine: Engine,
      override val taskTracker: TaskTracker,
      override val resources: Resources,
      override val memo: Memo,
      val stackPromise: Promise[StackResource]
    ) extends StackContext(
          runInfo,
          featureSupport,
          config,
          logger,
          monitor,
          engine,
          taskTracker,
          resources,
          memo,
          stackPromise
        ):
      override def readOrRegisterResource[R <: Resource: ResourceDecoder, A: ArgsEncoder](
        typ: ResourceType,
        name: NonEmptyString,
        args: A,
        options: ResourceOptions
      )(using Context): Output[R] = Output(
        new StackReference(
          urn = Output(URN.empty),
          id = Output(ResourceId.empty),
          name = Output("test"),
          outputs = Output(outputs),
          secretOutputNames = Output(Set.empty)
        ).asInstanceOf[R]
      )

    given Context = DummyContext.mockContext()(new MockStackContext(_, _, _, _, _, _, _, _, _, _)).unsafeRunSync()

    case class Test(s: String, i: Int) derives JsonReader

    val expected = Test("value1", 2)

    val typedStackReference = StackReference[Test]("test")

    val outputData = typedStackReference.getData.unsafeRunSync()

    val obtainedTypedStackReference = outputData.getValue.getOrElse {
      fail("expected TypedStackReference in output data")
    }

    assertEquals(obtainedTypedStackReference.outputs, expected)
  }
  test("unwrap secret envelopes in nested stack outputs") {
    given Context = DummyContext().unsafeRunSync()

    case class PostgreSQLOutputs(
      host: String,
      port: Int,
      database: String,
      username: String,
      password: String
    ) derives JsonReader

    case class ServicesOutputs(postgresql: PostgreSQLOutputs) derives JsonReader

    val secretSigKey   = Constants.SpecialSig.Key
    val secretSigValue = Constants.SpecialSig.SecretSig.asString

    def secretEnvelope(value: JsValue): JsObject =
      JsObject(secretSigKey -> JsString(secretSigValue), Constants.ValueName -> value)

    val outputs = Map(
      "postgresql" -> JsObject(
        "host" -> JsString("pg-host.svc.local"),
        "port" -> JsNumber(5432),
        "database" -> secretEnvelope(JsString("app")),
        "username" -> secretEnvelope(JsString("app")),
        "password" -> secretEnvelope(JsString("hunter2"))
      )
    )

    val expected = ServicesOutputs(
      PostgreSQLOutputs(
        host = "pg-host.svc.local",
        port = 5432,
        database = "app",
        username = "app",
        password = "hunter2"
      )
    )

    val result = StackReference.requireObject[ServicesOutputs](Output(outputs), Output(Set.empty))
    val od     = result.getData.unsafeRunSync()
    assertEquals(od, OutputData(expected, isSecret = true))
  }

  test("unwrap secret envelopes and combine with secretOutputNames") {
    given Context = DummyContext().unsafeRunSync()

    case class Test(s: String, i: Int) derives JsonReader

    val secretSigKey   = Constants.SpecialSig.Key
    val secretSigValue = Constants.SpecialSig.SecretSig.asString

    val outputs = Map(
      "s" -> JsObject(secretSigKey -> JsString(secretSigValue), Constants.ValueName -> JsString("secret-value")),
      "i" -> JsNumber(42)
    )

    val result = StackReference.requireObject[Test](Output(outputs), Output(Set("s")))
    val od     = result.getData.unsafeRunSync()
    assertEquals(od, OutputData(Test("secret-value", 42), isSecret = true))
  }

  test("no secret envelopes, no secretOutputNames — result is not secret") {
    given Context = DummyContext().unsafeRunSync()

    case class Test(s: String, i: Int) derives JsonReader
    val outputs = Map("s" -> JsString("plain"), "i" -> JsNumber(1))

    val result = StackReference.requireObject[Test](Output(outputs), Output(Set.empty))
    val od     = result.getData.unsafeRunSync()
    assertEquals(od, OutputData(Test("plain", 1)))
    assertEquals(od.secret, false)
  }

  test("unwrap output envelope with secret flag") {
    given Context = DummyContext().unsafeRunSync()

    case class Test(s: String) derives JsonReader

    val outputSigValue = Constants.SpecialSig.OutputSig.asString
    val sigKey         = Constants.SpecialSig.Key

    val outputs = Map(
      "s" -> JsObject(
        sigKey -> JsString(outputSigValue),
        Constants.ValueName -> JsString("output-val"),
        Constants.SecretName -> JsTrue
      )
    )

    val result = StackReference.requireObject[Test](Output(outputs), Output(Set.empty))
    val od     = result.getData.unsafeRunSync()
    assertEquals(od, OutputData(Test("output-val"), isSecret = true))
  }

  test("unwrap secret envelopes in arrays") {
    given Context = DummyContext().unsafeRunSync()

    case class Test(items: List[String]) derives JsonReader

    val secretSigKey   = Constants.SpecialSig.Key
    val secretSigValue = Constants.SpecialSig.SecretSig.asString

    val outputs = Map(
      "items" -> JsArray(
        JsObject(secretSigKey -> JsString(secretSigValue), Constants.ValueName -> JsString("a")),
        JsString("b"),
        JsObject(secretSigKey -> JsString(secretSigValue), Constants.ValueName -> JsString("c"))
      )
    )

    val result = StackReference.requireObject[Test](Output(outputs), Output(Set.empty))
    val od     = result.getData.unsafeRunSync()
    assertEquals(od, OutputData(Test(List("a", "b", "c")), isSecret = true))
  }

  test("stripSecretEnvelopes preserves non-envelope objects") {
    val obj = JsObject(
      "name" -> JsString("test"),
      "nested" -> JsObject("a" -> JsNumber(1), "b" -> JsNumber(2))
    )
    val (stripped, hasSecrets) = StackReference.stripSecretEnvelopes(obj)
    assertEquals(stripped, obj)
    assertEquals(hasSecrets, false)
  }

  test("typed export round-trip: serialize with Output fields, deserialize with unwrapped mirror case class") {
    given Context = DummyContext(
      featureSupport =
        FeatureSupport(keepResources = true, keepOutputValues = false, deletedWith = true, aliasSpecs = true, transforms = true)
    ).unsafeRunSync()

    // export side: case class with Output fields
    case class InfraExports(vpcId: Output[String], zone: Output[String]) derives Encoder

    // read side: mirror case class with unwrapped fields
    case class InfraOutputs(vpcId: String, zone: String) derives JsonReader

    // encode via typed export
    val stack        = Stack.exports(InfraExports(vpcId = Output("vpc-123"), zone = Output("us-east-1")))
    val exportStruct = stack.getExports.result.unsafeRunSync()

    // convert protobuf struct fields to JsValue map (simulating what Pulumi runtime does between stacks)
    val jsOutputs = exportStruct.fields.map { case (k, v) =>
      v.kind match
        case Value.Kind.StringValue(s) => k -> JsString(s)
        case Value.Kind.NumberValue(n) => k -> JsNumber(n)
        case Value.Kind.BoolValue(b)   => k -> JsBoolean(b)
        case other                     => fail(s"Unexpected value kind: $other")
    }

    // read via typed stack reference
    val result = StackReference.requireObject[InfraOutputs](Output(jsOutputs), Output(Set.empty))
    assertEquals(result.getData.unsafeRunSync(), OutputData(InfraOutputs("vpc-123", "us-east-1")))
  }
end StackReferenceTest
