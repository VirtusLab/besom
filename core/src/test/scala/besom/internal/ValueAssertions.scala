package besom.internal

import com.google.protobuf.struct.*, Value.Kind

trait ValueAssertions:
  self: munit.Assertions =>

  def assertEqualsMaybeValue(actual: Option[Value], expected: Option[Value]): Unit =
    (actual, expected) match
      case (None, None)                              => ()
      case (None, Some(Value(Kind.NullValue(_), _))) => ()
      case (Some(Value(Kind.NullValue(_), _)), None) => ()
      case (Some(a), Some(b))                        => assertEqualsValue(a, b)
      case _                                         => throw Exception(s"Values not equal: $actual != $expected")

  def assertEqualsValue(actual: Value, expected: Value): Unit = (actual.kind, expected.kind) match
    case (Kind.NullValue(_), Kind.NullValue(_))     => ()
    case (Kind.NumberValue(a), Kind.NumberValue(b)) => assertEquals(actual, expected)
    case (Kind.StringValue(a), Kind.StringValue(b)) => assertEquals(actual, expected)
    case (Kind.BoolValue(a), Kind.BoolValue(b))     => assertEquals(actual, expected)
    case (Kind.StructValue(a), Kind.StructValue(b)) =>
      (a.fields.keys ++ b.fields.keys).toSeq.distinct.foreach { key =>
        val actualFieldValue   = a.fields.get(key)
        val expectedFieldValue = b.fields.get(key)
        assertEqualsMaybeValue(actualFieldValue, expectedFieldValue)
      }
    case (Kind.ListValue(a), Kind.ListValue(b)) => assertEquals(actual, expected)
    case _                                      => throw Exception(s"Values not equal: $actual != $expected")
