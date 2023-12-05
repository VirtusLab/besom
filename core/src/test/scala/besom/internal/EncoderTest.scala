package besom.internal

import com.google.protobuf.struct.*
import Encoder.*
import ProtobufUtil.*
import besom.types.Label
import besom.util.*
import RunResult.{given, *}

object EncoderTest:
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

  sealed abstract class TestEnum(val name: String, val value: String) extends besom.types.StringEnum

  object TestEnum extends besom.types.EnumCompanion[String, TestEnum]("TestEnum"):
    object A extends TestEnum("A", "A value")
    object B extends TestEnum("B", "B value")

    override val allInstances: Seq[TestEnum] = Seq(A, B)

class EncoderTest extends munit.FunSuite with ValueAssertions:
  import EncoderTest.*
  val dummyLabel = Label.fromNameAndType("dummy", "dummy:pkg:Dummy")

  test("encode case class") {
    val e = summon[Encoder[TestCaseClass]]
    val expected = Map(
      "foo" -> 10.asValue,
      "bar" -> List("qwerty".asValue).asValue,
      "optNone1" -> Null,
      "optSome" -> Some("abc").asValue
    ).asValue
    val (_, encoded) = e.encode(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))).unsafeRunSync()
    assertEqualsValue(encoded, expected)
  }

  test("encoder enum") {
    val e             = summon[Encoder[TestEnum]]
    val (_, encodedA) = e.encode(TestEnum.A).unsafeRunSync()
    assertEqualsValue(encodedA, "A value".asValue)
    val (_, encodedB) = e.encode(TestEnum.B).unsafeRunSync()
    assertEqualsValue(encodedB, "B value".asValue)
  }

  test("encode special case class with unmangling") {
    val e = summon[Encoder[SpecialCaseClass]]
    val expected = Map(
      "equals" -> 10.asValue,
      "eq" -> "abc".asValue,
      "normalOne" -> "qwerty".asValue
    ).asValue
    val (_, encoded) = e.encode(SpecialCaseClass(10, "abc", "qwerty")).unsafeRunSync()
    assertEqualsValue(encoded, expected)
  }

  test("encode a union of string and case class") {
    val e = summon[Encoder[String | TestCaseClass]]

    val (_, encodedString) = e.encode("abc").unsafeRunSync()
    assertEqualsValue(encodedString, "abc".asValue)

    val (_, encodedCaseClass) = e.encode(TestCaseClass(10, List("qwerty"), None, None, Some("abc"))).unsafeRunSync()
    val expected = Map(
      "foo" -> 10.asValue,
      "bar" -> List("qwerty".asValue).asValue,
      "optNone1" -> Null,
      "optSome" -> Some("abc").asValue
    ).asValue
    assertEqualsValue(encodedCaseClass, expected)
  }
end EncoderTest
