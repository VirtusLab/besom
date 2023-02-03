package besom.internal

import com.google.protobuf.util.JsonFormat
import com.google.protobuf.struct.*, Value.Kind
import scala.util.*

object ProtobufUtil:
  private val printer = JsonFormat.printer().omittingInsignificantWhitespace()

  extension (v: Value) def asJsonString: Either[Throwable, String] = Try(printer.print(Value.toJavaProto(v))).toEither

  extension (s: String) def asValue: Value = Value(Kind.StringValue(s))

  extension (i: Int) def asValue: Value = Value(Kind.NumberValue(i))

  extension (d: Double) def asValue: Value = Value(Kind.NumberValue(d))

  extension (b: Boolean) def asValue: Value = Value(Kind.BoolValue(b))

  extension (m: Map[String, Value]) def asValue: Value = Value(Kind.StructValue(Struct(m)))

  extension (l: List[Value]) def asValue: Value = Value(Kind.ListValue(ListValue(l)))

  extension (v: Vector[Value]) def asValue: Value = Value(Kind.ListValue(ListValue(v)))

  val Null: Value = Value(Kind.NullValue(NullValue.NULL_VALUE))
