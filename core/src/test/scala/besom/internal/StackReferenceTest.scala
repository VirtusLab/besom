package besom.internal

import besom.*
import besom.json.*
import RunResult.{*, given}
import besom.internal.logging.BesomLogger

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
end StackReferenceTest
