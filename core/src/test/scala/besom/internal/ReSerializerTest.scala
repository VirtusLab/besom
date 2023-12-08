package besom.internal

import besom.internal
import besom.internal.RunResult.{*, given}
import besom.types.{Output as _, *}
import besom.util.Validated
import com.google.protobuf.struct.*

class ReSerializerTest extends munit.FunSuite:

  test("serialize-deserialize common types") {
    assertEquals(reSerialize(Option.empty[String]).unsafeRunSync(), Option.empty[String])
    assertEquals(reSerialize("asdf").unsafeRunSync(), "asdf")
    assertEquals(reSerialize(123).unsafeRunSync(), 123)
    assertEquals(reSerialize(123.0).unsafeRunSync(), 123.0)
    assertEquals(reSerialize(true).unsafeRunSync(), true)
    assertEquals(reSerialize(false).unsafeRunSync(), false)
    assertEquals(reSerialize(List.empty[String]).unsafeRunSync(), List.empty[String])
    assertEquals(reSerialize(List("asdf", "qwer")).unsafeRunSync(), List("asdf", "qwer"))
    assertEquals(reSerialize(List(1, 2, 3)).unsafeRunSync(), List(1, 2, 3))
    assertEquals(reSerialize(List(1.0, 2.0, 3.0)).unsafeRunSync(), List(1.0, 2.0, 3.0))
    assertEquals(reSerialize(Map.empty[String, String]).unsafeRunSync(), Map.empty[String, String])
    assertEquals(reSerialize(Map("asdf" -> Option.empty[String])).unsafeRunSync(), Map.empty)
    assertEquals(reSerialize(Map("asdf" -> Option("qwer"))).unsafeRunSync(), Map("asdf" -> Option("qwer")))
    assertEquals(reSerialize(Map("asdf" -> List("qwer"))).unsafeRunSync(), Map("asdf" -> List("qwer")))

    case class Foo(a: Option[String], b: Option[Boolean]) derives Encoder, Decoder
    assertEquals(reSerialize(Foo(None, None)).unsafeRunSync(), Foo(None, None))
  }

  test("serialize-deserialize JSON") {
    val e: Encoder[spray.json.JsValue] = summon[Encoder[spray.json.JsValue]]
    val d: Decoder[spray.json.JsValue] = summon[Decoder[spray.json.JsValue]]

    assertEquals(reSerialize(spray.json.JsNull)(e, d).unsafeRunSync(), spray.json.JsNull)
    assertEquals(reSerialize(spray.json.JsObject.empty)(e, d).unsafeRunSync(), spray.json.JsObject.empty)
    assertEquals(reSerialize(spray.json.JsArray.empty)(e, d).unsafeRunSync(), spray.json.JsArray.empty)
    assertEquals(reSerialize(spray.json.JsString("asdf"))(e, d).unsafeRunSync(), spray.json.JsString("asdf"))
    assertEquals(reSerialize(spray.json.JsNumber(123))(e, d).unsafeRunSync(), spray.json.JsNumber(123))
    assertEquals(reSerialize(spray.json.JsBoolean(true))(e, d).unsafeRunSync(), spray.json.JsBoolean(true))
    assertEquals(reSerialize(spray.json.JsBoolean(false))(e, d).unsafeRunSync(), spray.json.JsBoolean(false))
    assertEquals(
      reSerialize(spray.json.JsObject("asdf" -> spray.json.JsNull))(e, d).unsafeRunSync(),
      spray.json.JsObject("asdf" -> spray.json.JsNull)
    )

    import spray.json.*
    import DefaultJsonProtocol.*
    assertEquals(reSerialize("""{}""".toJson)(e, d).unsafeRunSync(), """{}""".toJson)
    assertEquals(reSerialize("""[]""".toJson)(e, d).unsafeRunSync(), """[]""".toJson)
    assertEquals(reSerialize("""{"asdf": null}""".toJson)(e, d).unsafeRunSync(), """{"asdf": null}""".toJson)
    assertEquals(reSerialize("""{"asdf": 123}""".toJson)(e, d).unsafeRunSync(), """{"asdf": 123}""".toJson)
    assertEquals(reSerialize("""{"test":["test1", 1]}""".toJson)(e, d).unsafeRunSync(), """{"test":["test1", 1]}""".toJson)
  }

  def reSerialize[A: Encoder: Decoder](value: A): Result[A] = {
    val dec = summon[Decoder[A]]
    val enc = summon[Encoder[A]]
    enc.encode(value).flatMap { case (_, v) =>
      dec.decode(v, Label.fromNameAndType("test", "a:b:c")).asResult.map {
        case Validated.Valid(a) =>
          a.getValue.getOrElse(throw RuntimeException("No value"))
        case Validated.Invalid(e) => throw AggregatedDecodingError(e)
      }
    }
  }
end ReSerializerTest
