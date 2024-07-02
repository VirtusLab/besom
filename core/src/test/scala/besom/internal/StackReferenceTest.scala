package besom.internal

import besom.*
import besom.json.*

import RunResult.{given, *}

class StackReferenceTest extends munit.FunSuite:

  test("convert stack reference to case class") {
    given Context = DummyContext().unsafeRunSync()
    case class Test(s: String, i: Int) derives JsonReader
    val expected = Test("value1", 2)
    val outputs  = Map("s" -> JsString("value1"), "i" -> JsNumber(2))

    val requireObject = StackReference.requireObject[Test](Output(outputs), Output(Set.empty))
    assertEquals(requireObject.getData.unsafeRunSync(), OutputData(expected))
  }

  test("fail when convert stack reference to case class") {
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

end StackReferenceTest
