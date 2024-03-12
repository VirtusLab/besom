package besom.internal

import besom.internal.Constants.{SecretValueName, SpecialSecretSig, SpecialSigKey}
import com.google.protobuf.struct.*
import com.google.protobuf.struct.Value.Kind
import com.google.protobuf.util.JsonFormat

import scala.util.*

object ProtobufUtil:
  private val printer = JsonFormat.printer().omittingInsignificantWhitespace()

  val Null: Value = Value(Kind.NullValue(NullValue.NULL_VALUE))

  trait ToValue[A]:
    extension (a: A) def asValue: Value

  given ToValue[Int] with
    extension (i: Int) def asValue: Value = Value(Kind.NumberValue(i))

  given ToValue[String] with
    extension (s: String) def asValue: Value = Value(Kind.StringValue(s))

  given ToValue[Double] with
    extension (d: Double) def asValue: Value = Value(Kind.NumberValue(d))

  given ToValue[Boolean] with
    extension (b: Boolean) def asValue: Value = Value(Kind.BoolValue(b))

  given ToValue[Struct] with
    extension (s: Struct) def asValue: Value = Value(Kind.StructValue(s))

  given ToValue[Value] with
    extension (v: Value) def asValue: Value = v

  given [A: ToValue]: ToValue[List[A]] with
    extension (l: List[A]) def asValue: Value = Value(Kind.ListValue(ListValue(l.map(_.asValue))))

  given [A: ToValue]: ToValue[Vector[A]] with
    extension (v: Vector[A]) def asValue: Value = Value(Kind.ListValue(ListValue(v.map(_.asValue).toList)))

  given [A: ToValue]: ToValue[Map[String, A]] with
    extension (m: Map[String, A])
      def asValue: Value   = Value(Kind.StructValue(m.asStruct))
      def asStruct: Struct = Struct(m.map((k, v) => k -> v.asValue))

  given [A: ToValue]: ToValue[Option[A]] with
    extension (o: Option[A])
      def asValue: Value = o match
        case Some(a) => a.asValue
        case None    => Null

  extension (v: Value)
    def asJsonString: Either[Throwable, String] = Try(printer.print(Value.toJavaProto(v))).toEither
    def asJsonStringOrThrow: String             = asJsonString.fold(t => throw Exception("Expected a JSON", t), identity)

    def struct: Option[Struct] = v.kind.structValue

    def asSecret: Value = Map(
      SpecialSigKey -> SpecialSecretSig.asValue,
      SecretValueName -> v
    ).asValue

end ProtobufUtil
