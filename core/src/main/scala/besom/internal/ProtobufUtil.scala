package besom.internal

import besom.internal.Constants.*
import besom.types.{URN, ResourceId}
import com.google.protobuf.struct.*
import com.google.protobuf.struct.Value.Kind
import com.google.protobuf.util.JsonFormat

import scala.annotation.unused
import scala.util.*
import scala.collection.immutable.Iterable

object ProtobufUtil:
  private val printer = JsonFormat.printer().omittingInsignificantWhitespace()

  val Null: Value = Value(Kind.NullValue(NullValue.NULL_VALUE))

  trait ToValue[A]:
    extension (@unused a: A) def asValue: Value

  given ToValue[Int] with
    extension (i: Int) def asValue: Value = Value(Kind.NumberValue(i))

  given ToValue[Long] with
    extension (l: Long) def asValue: Value = Value(Kind.NumberValue(l.toDouble))

  given ToValue[String] with
    extension (s: String) def asValue: Value = Value(Kind.StringValue(s))

  given ToValue[Float] with
    extension (f: Float) def asValue: Value = Value(Kind.NumberValue(f))

  given ToValue[Double] with
    extension (d: Double) def asValue: Value = Value(Kind.NumberValue(d))

  given ToValue[Boolean] with
    extension (b: Boolean) def asValue: Value = Value(Kind.BoolValue(b))

  given ToValue[Struct] with
    extension (s: Struct) def asValue: Value = Value(Kind.StructValue(s))

  given ToValue[ListValue] with
    extension (l: ListValue) def asValue: Value = Value(Kind.ListValue(l))

  given ToValue[Value] with
    extension (v: Value) def asValue: Value = v

  given [A: ToValue]: ToValue[Iterable[A]] with
    extension (l: Iterable[A]) def asValue: Value = Value(Kind.ListValue(ListValue(l.map(_.asValue).toSeq)))

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

  extension (v: Value)
    def asJsonString: Either[Throwable, String] = Try(printer.print(Value.toJavaProto(v))).toEither
    def asJsonStringOrThrow: String             = asJsonString.fold(t => throw Exception("Expected a JSON", t), identity)

    def asSecretValue: Value = SecretValue(v).asValue
    def asOutputValue(
      isSecret: Boolean,
      dependencies: Iterable[URN]
    ): Value = OutputValue.known(value = v, isSecret = isSecret, dependencies = dependencies).asValue

    def withSpecialSignature[A](f: PartialFunction[(Struct, SpecialSig), A]): Option[A] =
      v.kind.structValue.flatMap(_.withSpecialSignature(f))

    def valueType: String = v.kind.getClass.getSimpleName

  end extension

  extension (s: Struct)
    private def specialSignatureString: Option[String] =
      s.fields.get(SpecialSig.Key).flatMap(_.kind.stringValue)
    def specialSignature: Option[SpecialSig] =
      s.specialSignatureString.flatMap(SpecialSig.fromString)
    def withSpecialSignature[A](f: PartialFunction[(Struct, SpecialSig), A]): Option[A] =
      val maybeSpecial =
        for sig: SpecialSig <- s.specialSignature
        yield (s, sig)
      maybeSpecial.collect(f)
    end withSpecialSignature

end ProtobufUtil

import ProtobufUtil.{*, given}

case class ResourceValue private (urn: Value, id: Option[Value])
object ResourceValue:
  def apply(urn: Value, id: Value): Either[Exception, ResourceValue] =
    val fixedIdValue = id match
      case OutputValue(o)                                             => Right(o.value.getOrElse(ResourceId.empty.asValue))
      case Value(Kind.StringValue(id), _) if id == UnknownStringValue => Right(ResourceId.empty.asValue)
      case v @ Value(Kind.StringValue(_), _)                          => Right(v)
      case v                                                          => Left(Exception(s"Unexpected id value type: ${v.valueType}"))
    for
      base <- apply(urn)
      id   <- fixedIdValue
    yield base.copy(id = Some(id))
  def apply(urn: Value): Either[Exception, ResourceValue] =
    urn match
      case OutputValue(o)                  => Right(ResourceValue(o.value.getOrElse(URN.empty.asString.asValue), None))
      case Value(Kind.StringValue(urn), _) => Right(ResourceValue(urn.asValue, None))
      case v                               => Left(Exception(s"Unexpected urn value type: ${v.valueType}"))

  extension (r: ResourceValue)
    def asValue: Value = {
      Map(
        SpecialSig.Key -> SpecialSig.ResourceSig.asValue,
        ResourceUrnName -> r.urn
      ) ++ r.id.map(i => Map(ResourceIdName -> i)).getOrElse(Map.empty)
    }.asValue

case class SecretValue(value: Value):
  def isKnown: Boolean = value.kind.stringValue.map(_ == UnknownStringValue).getOrElse(false)

object SecretValue:
  def unknown: SecretValue = SecretValue(UnknownStringValue.asValue)
  def unapply(value: Value): Option[SecretValue] =
    value.withSpecialSignature { case (struct, SpecialSig.SecretSig) =>
      val value = struct.fields.getOrElse(ValueName, Null)
      SecretValue(value)
    }

  extension (s: SecretValue)
    def asValue: Value = Map(
      SpecialSig.Key -> SpecialSig.SecretSig.asValue,
      ValueName -> s.value
    ).asValue

case class OutputValue(value: Option[Value], isSecret: Boolean, dependencies: Iterable[URN]):
  def isKnown: Boolean = value.isDefined

object OutputValue:
  def known(value: Value, isSecret: Boolean, dependencies: Iterable[URN]): OutputValue = OutputValue(Some(value), isSecret, dependencies)
  def unknown(isSecret: Boolean, dependencies: Iterable[URN]): OutputValue             = OutputValue(None, isSecret, dependencies)
  def unapply(value: Value): Option[OutputValue] =
    value.withSpecialSignature { case (struct, SpecialSig.OutputSig) =>
      val value            = struct.fields.get(ValueName)
      val isSecret         = struct.fields.get(SecretName).flatMap(_.kind.boolValue).getOrElse(false)
      val dependencyValues = struct.fields.get(DependenciesName).flatMap(_.kind.listValue).map(_.values.toList).getOrElse(Nil)
      val dependencies     = dependencyValues.flatMap(_.kind.stringValue).flatMap(s => URN.from(s).toOption)
      OutputValue(value, isSecret, dependencies)
    }

  extension (o: OutputValue)
    // noinspection ScalaUnusedSymbol
    private[internal] def asRawValueOrUnknown: Value = o.value.getOrElse(UnknownStringValue.asValue)
    def asValue: Value = {
      Map(
        SpecialSig.Key -> SpecialSig.OutputSig.asValue
      ) ++ (if o.isKnown then Map(ValueName -> o.value.get) else Map.empty)
        ++ (if o.isSecret then Map(SecretName -> true.asValue) else Map.empty)
        ++ (if o.dependencies.nonEmpty
            then Map(DependenciesName -> o.dependencies.map(_.asString.asValue).asValue)
            else Map.empty)
    }.asValue

case class Metadata(
  known: Boolean,
  secret: Boolean,
  empty: Boolean,
  dependencies: Iterable[URN]
):
  def unknown: Boolean = !known
  def combine(that: Metadata): Metadata =
    Metadata(
      known && that.known,
      secret || that.secret,
      empty && that.empty,
      dependencies ++ that.dependencies
    )

  def render(value: Value)(using ctx: Context): Value =
    if ctx.featureSupport.keepOutputValues then OutputValue(if known then Some(value) else None, secret, dependencies).asValue
    else
      val v = if known then value else UnknownStringValue.asValue
      if secret then SecretValue(v).asValue else v

end Metadata
object Metadata:
  /* Default values that can be used safely with combine, e.g. as zero in fold operation */
  def empty: Metadata = Metadata(known = true, secret = false, empty = false, Nil)
