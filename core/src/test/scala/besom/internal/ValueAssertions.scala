package besom.internal

import com.google.protobuf.struct.*, Value.Kind

trait ValueAssertions extends munit.Assertions:

  def assertEqualsMaybeValue(
    actual: Option[Value],
    expected: Option[Value],
    clue: => Any = "Option values are not the same"
  )(implicit
    loc: munit.Location,
    compare: munit.Compare[Option[Value], Option[Value]]
  ): Unit =
    (actual, expected) match
      case (None, None)                              => ()
      case (None, Some(Value(Kind.NullValue(_), _))) => ()
      case (Some(Value(Kind.NullValue(_), _)), None) => ()
      case (Some(a), Some(b))                        => assertEqualsValue(a, b, clue)
      case _ =>
        compare.failEqualsComparison(
          obtained = actual,
          expected = expected,
          title = clue,
          loc = loc,
          assertions = this
        )

  def assertEqualsValue(
    actual: Value,
    expected: Value,
    clue: => Any = "values are not the same"
  )(implicit
    loc: munit.Location,
    compare: munit.Compare[Value, Value]
  ): Unit =
    (actual.kind, expected.kind) match
      case (Kind.NullValue(_), Kind.NullValue(_))     => ()
      case (Kind.NumberValue(_), Kind.NumberValue(_)) => assertEquals(actual, expected, clue)
      case (Kind.StringValue(_), Kind.StringValue(_)) => assertEquals(actual, expected, clue)
      case (Kind.BoolValue(_), Kind.BoolValue(_))     => assertEquals(actual, expected, clue)
      case (Kind.StructValue(a), Kind.StructValue(b)) =>
        (a.fields.keys ++ b.fields.keys).toSeq.distinct.foreach { key =>
          val actualFieldValue   = a.fields.get(key)
          val expectedFieldValue = b.fields.get(key)
          assertEqualsMaybeValue(
            actualFieldValue,
            expectedFieldValue,
            s"""values not equal for key '$key'
               |-> parent clue: $clue""".stripMargin
          )
        }
      case (Kind.ListValue(_), Kind.ListValue(_)) => assertEquals(actual, expected, clue)
      case _ =>
        compare.failEqualsComparison(
          obtained = actual,
          expected = expected,
          title = clue,
          loc = loc,
          assertions = this
        )
end ValueAssertions
