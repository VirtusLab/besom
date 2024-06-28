package besom.internal

import besom.*
import besom.json.*

import RunResult.{given, *}

class StackReferenceTest extends munit.FunSuite:

  test("convert stack reference to case class") {
    given Context = DummyContext().unsafeRunSync()
    case class Test(s: String, i: Int) derives JsonReader
    val expected       = Test("value1", 2)
    val stackReference = defaultStackReference(Map("s" -> JsString("value1"), "i" -> JsNumber(2)))

    val requireObject = stackReference.requireObject[Test]
    val getObject     = stackReference.getObject[Test]
    assertEquals(requireObject.getData.unsafeRunSync(), OutputData(expected))
    assertEquals(getObject.getData.unsafeRunSync(), OutputData(Some(expected)))
  }

  test("fail when convert stack reference to case class") {
    given Context = DummyContext().unsafeRunSync()
    case class Test(s: String, i: Int) derives JsonReader
    val stackReference = defaultStackReference(Map("s" -> JsString("value1")))

    val requireObject = stackReference.requireObject[Test]
    val getObject     = stackReference.getObject[Test]
    intercept[besom.json.DeserializationException](requireObject.getData.unsafeRunSync())
    assertEquals(getObject.getData.unsafeRunSync(), OutputData(None))
  }

  test("convert stack reference to case class with secret field") {
    given Context = DummyContext().unsafeRunSync()
    case class Test(s: String, i: Int) derives JsonReader
    val expected       = Test("value1", 2)
    val stackReference = defaultStackReference(Map("s" -> JsString("value1"), "i" -> JsNumber(2)), Set("i"))

    val requireObject = stackReference.requireObject[Test]
    val getObject     = stackReference.getObject[Test]
    assertEquals(requireObject.getData.unsafeRunSync(), OutputData(expected).withIsSecret(true))
    assertEquals(getObject.getData.unsafeRunSync(), OutputData(Some(expected)).withIsSecret(true))
  }

  private def defaultStackReference(outputs: Map[String, JsValue] = Map.empty, secretOutputNames: Set[String] = Set.empty)(using
    Context
  ): StackReference =
    new StackReference(
      urn = Output(URN.empty),
      id = Output(ResourceId.empty),
      name = Output(""),
      outputs = Output(outputs),
      secretOutputNames = Output(secretOutputNames)
    )
end StackReferenceTest
