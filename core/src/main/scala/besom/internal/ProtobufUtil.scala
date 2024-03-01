package besom.internal

import besom.internal.Constants.*
import besom.types.URN
import com.google.protobuf.struct.*
import com.google.protobuf.struct.Value.Kind
import com.google.protobuf.util.JsonFormat

import scala.util.*

object ProtobufUtil:
  private val printer = JsonFormat.printer().omittingInsignificantWhitespace()

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

/*  given [A: ToValue]: ToValue[OutputData[A]] with
    import besom.util.*
    extension (d: OutputData[A])
      private def deps(r: Set[Resource])(using Context): Result[List[URN]] = r.toList.map(_.urn.getValue)
      def asValue: Value = d match
        case OutputData.Known(r, s, v) => v.asValue.asOutputValue(isKnown = true, isSecret = s, dependencies = r)
        case OutputData.Unknown(r, s)  => Null.asOutputValue(isKnown = false, isSecret = s, dependencies = r)*/

  extension (v: Value)
    def asString: String                        = v.toProtoString
    def asJsonString: Either[Throwable, String] = Try(printer.print(Value.toJavaProto(v))).toEither
    def asSecret: Value = Map(
      SpecialSigKey -> SpecialSecretSig.asValue,
      ValueName -> v
    ).asValue
    def asOutputValue(isKnown: Boolean, isSecret: Boolean, dependencies: List[URN]): Value = {
      Map(
        SpecialSigKey -> SpecialOutputValueSig.asValue
      ) ++ (if isKnown then Map(ValueName -> v) else Map.empty)
        ++ (if isSecret then Map(SecretName -> true.asValue) else Map.empty)
        ++ (if dependencies.nonEmpty then Map(DependenciesName -> dependencies.map(_.asString.asValue).asValue)
            else Map.empty)
    }.asValue

  val Null: Value = Value(Kind.NullValue(NullValue.NULL_VALUE))
end ProtobufUtil
