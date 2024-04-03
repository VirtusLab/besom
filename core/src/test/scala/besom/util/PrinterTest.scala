package besom.util

import besom.internal.ProtobufUtil.given

class PrinterTest extends munit.FunSuite:

  Vector(
    ("hello".asValue, """Value(kind = StringValue(value = "hello"))"""),
    (123.asValue, """Value(kind = NumberValue(value = 123.0))"""),
    (true.asValue, """Value(kind = BoolValue(value = true))"""),
    (
      List(1, 2, 3).asValue,
      """Value(
        |  kind = ListValue(
        |    value = ListValue(
        |      values = List(Value(kind = NumberValue(value = 1.0)), Value(kind = NumberValue(value = 2.0)), Value(kind = NumberValue(value = 3.0)))
        |    )
        |  )
        |)""".stripMargin
    ),
    (Some(1.asValue), """Some(value = Value(kind = NumberValue(value = 1.0)))""")
  ).zipWithIndex.foreach { case ((v, expected), idx) =>
    test(s"skips the protobuf unknownFields [$idx]") {
      assertEquals(besom.util.printer.render(v).plainText, expected)
    }
  }
