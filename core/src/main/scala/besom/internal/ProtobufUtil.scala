package besom.internal

import besom.internal.Constants.*
import besom.types.URN
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

  given ToValue[SpecialSig] with
    extension (s: SpecialSig) def asValue: Value = s.asString.asValue

  given ToValue[SecretValue] with
    extension (s: SecretValue)
      def asValue: Value = Map(
        SpecialSig.Key -> SpecialSig.SecretSig.asValue,
        ValueName -> s.value
      ).asValue

  given ToValue[OutputValue] with
    extension (o: OutputValue)
      def asValue: Value = {
        Map(
          SpecialSig.Key -> SpecialSig.OutputSig.asValue
        ) ++ (if o.isKnown then Map(ValueName -> o.value) else Map.empty)
          ++ (if o.isSecret then Map(SecretName -> true.asValue) else Map.empty)
          ++ (if o.dependencies.nonEmpty
              then Map(DependenciesName -> o.dependencies.map(_.asString.asValue).asValue)
              else Map.empty)
      }.asValue

  extension (v: Value)
    def asJsonString: Either[Throwable, String] = Try(printer.print(Value.toJavaProto(v))).toEither
    def asJsonStringOrThrow: String             = asJsonString.fold(t => throw Exception("Expected a JSON", t), identity)

    def struct: Option[Struct]           = v.kind.structValue
    def outputValue: Option[OutputValue] = v.struct.flatMap(_.outputValue)
    def secretValue: Option[SecretValue] = v.struct.flatMap(_.secretValue)
    def isKnown: Boolean                 = !v.kind.isNullValue

    def asSecretValue: Value = SecretValue(v).asValue
    def asOutputValue(
      isSecret: Boolean,
      dependencies: List[URN]
    ): Value = OutputValue(
      value = v,
      isSecret = isSecret,
      dependencies = dependencies
    ).asValue

    def withSpecialSignature[A](f: PartialFunction[(Struct, SpecialSig), A]): Option[A] =
      v.struct.flatMap(_.withSpecialSignature(f))

  end extension

  extension (s: Struct)
    def specialSignatureString: Option[String] =
      s.fields.get(SpecialSig.Key).flatMap(_.kind.stringValue)
    def specialSignature: Option[SpecialSig] =
      s.specialSignatureString.flatMap(SpecialSig.fromString)
    def withSpecialSignature[A](f: PartialFunction[(Struct, SpecialSig), A]): Option[A] =
      val maybeSpecial =
        for sig: SpecialSig <- s.specialSignature
        yield (s, sig)
      maybeSpecial.collect(f)
    end withSpecialSignature
    def outputValue: Option[OutputValue] =
      withSpecialSignature { case (struct, SpecialSig.OutputSig) =>
        val value            = struct.fields.getOrElse(ValueName, Null)
        val isSecret         = struct.fields.get(SecretName).flatMap(_.kind.boolValue).getOrElse(false)
        val dependencyValues = struct.fields.get(DependenciesName).flatMap(_.kind.listValue).map(_.values.toList).getOrElse(Nil)
        val dependencies     = dependencyValues.flatMap(_.kind.stringValue).flatMap(s => URN.from(s).toOption)
        OutputValue(value, isSecret, dependencies)
      }
    end outputValue
    def secretValue: Option[SecretValue] =
      withSpecialSignature { case (struct, SpecialSig.SecretSig) =>
        val value = struct.fields.getOrElse(ValueName, Null)
        SecretValue(value)
      }

end ProtobufUtil

import ProtobufUtil.*

case class SecretValue(value: Value):
  def isKnown: Boolean  = !notKnown
  def notKnown: Boolean = value.kind.isNullValue
object SecretValue:
  def unapply(value: Value): Option[SecretValue]   = value.secretValue
  def unapply(struct: Struct): Option[SecretValue] = struct.secretValue

case class OutputValue(value: Value, isSecret: Boolean, dependencies: List[URN]):
  def isKnown: Boolean  = value.isKnown
  def notKnown: Boolean = !isKnown

object OutputValue:
  def unapply(value: Value): Option[OutputValue]   = value.outputValue
  def unapply(struct: Struct): Option[OutputValue] = struct.outputValue
  def unknown(isSecret: Boolean, dependencies: List[URN]): OutputValue =
    OutputValue(Null, isSecret, dependencies)
