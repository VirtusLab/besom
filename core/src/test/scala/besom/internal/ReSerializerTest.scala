package besom.internal

import besom.internal.RunResult.{*, given}
import besom.types.{Output as _, *}
import besom.util.Validated
import com.google.protobuf.struct.*
import scala.collection.immutable.Iterable

class ReSerializerTest extends munit.FunSuite:

  given Context = DummyContext().unsafeRunSync()

  test("serialize-deserialize common types") {
    assertEquals(reSerialize(Option.empty[String]).unsafeRunSync(), Option.empty[String])
    assertEquals(reSerialize("asdf").unsafeRunSync(), "asdf")
    assertEquals(reSerialize(123).unsafeRunSync(), 123)
    assertEquals(reSerialize(123.0).unsafeRunSync(), 123.0)
    assertEquals(reSerialize(true).unsafeRunSync(), true)
    assertEquals(reSerialize(false).unsafeRunSync(), false)
    assertEquals(reSerialize(Iterable.empty[String]).unsafeRunSync(), List.empty[String])
    assertEquals(reSerialize(Iterable("asdf", "qwer")).unsafeRunSync(), List("asdf", "qwer"))
    assertEquals(reSerialize(Iterable(1, 2, 3)).unsafeRunSync(), List(1, 2, 3))
    assertEquals(reSerialize(Iterable(1.0, 2.0, 3.0)).unsafeRunSync(), List(1.0, 2.0, 3.0))
    assertEquals(reSerialize(Map.empty[String, String]).unsafeRunSync(), Map.empty[String, String])
    assertEquals(reSerialize(Map("asdf" -> Option.empty[String])).unsafeRunSync(), Map.empty) // special case, we remove null values in maps
    assertEquals(reSerialize(Map("asdf" -> Option("qwer"))).unsafeRunSync(), Map("asdf" -> Option("qwer")))
    assertEquals(reSerialize(Map("asdf" -> Iterable("qwer"))).unsafeRunSync(), Map("asdf" -> Iterable("qwer")))

    case class Foo(a: Option[String], b: Option[Boolean]) derives Encoder, Decoder
    assertEquals(reSerialize(Foo(None, None)).unsafeRunSync(), Foo(None, None))
  }

  test("serialize-deserialize JSON") {
    val e: Encoder[besom.json.JsValue] = summon[Encoder[besom.json.JsValue]]
    val d: Decoder[besom.json.JsValue] = summon[Decoder[besom.json.JsValue]]

    assertEquals(reSerialize(besom.json.JsNull)(e, d).unsafeRunSync(), besom.json.JsNull)
    assertEquals(reSerialize(besom.json.JsObject.empty)(e, d).unsafeRunSync(), besom.json.JsObject.empty)
    assertEquals(reSerialize(besom.json.JsArray.empty)(e, d).unsafeRunSync(), besom.json.JsArray.empty)
    assertEquals(reSerialize(besom.json.JsString("asdf"))(e, d).unsafeRunSync(), besom.json.JsString("asdf"))
    assertEquals(reSerialize(besom.json.JsNumber(123))(e, d).unsafeRunSync(), besom.json.JsNumber(123))
    assertEquals(reSerialize(besom.json.JsBoolean(true))(e, d).unsafeRunSync(), besom.json.JsBoolean(true))
    assertEquals(reSerialize(besom.json.JsBoolean(false))(e, d).unsafeRunSync(), besom.json.JsBoolean(false))
    assertEquals(
      reSerialize(besom.json.JsObject("asdf" -> besom.json.JsNull))(e, d).unsafeRunSync(),
      besom.json.JsObject("asdf" -> besom.json.JsNull)
    )

    import besom.json.*
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
