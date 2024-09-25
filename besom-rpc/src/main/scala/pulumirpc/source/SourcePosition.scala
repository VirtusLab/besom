// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.source

/** A SourcePosition represents a position in a source file.
  *
  * @param uri
  *   The URI of the file. Currently only the file scheme with an absolute path is supported.
  * @param line
  *   The line in the file
  * @param column
  *   The column in the line
  */
@SerialVersionUID(0L)
final case class SourcePosition(
    uri: _root_.scala.Predef.String = "",
    line: _root_.scala.Int = 0,
    column: _root_.scala.Int = 0,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[SourcePosition] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = uri
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      
      {
        val __value = line
        if (__value != 0) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(2, __value)
        }
      };
      
      {
        val __value = column
        if (__value != 0) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(3, __value)
        }
      };
      __size += unknownFields.serializedSize
      __size
    }
    override def serializedSize: _root_.scala.Int = {
      var __size = __serializedSizeMemoized
      if (__size == 0) {
        __size = __computeSerializedSize() + 1
        __serializedSizeMemoized = __size
      }
      __size - 1
      
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
      {
        val __v = uri
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      {
        val __v = line
        if (__v != 0) {
          _output__.writeInt32(2, __v)
        }
      };
      {
        val __v = column
        if (__v != 0) {
          _output__.writeInt32(3, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def withUri(__v: _root_.scala.Predef.String): SourcePosition = copy(uri = __v)
    def withLine(__v: _root_.scala.Int): SourcePosition = copy(line = __v)
    def withColumn(__v: _root_.scala.Int): SourcePosition = copy(column = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = uri
          if (__t != "") __t else null
        }
        case 2 => {
          val __t = line
          if (__t != 0) __t else null
        }
        case 3 => {
          val __t = column
          if (__t != 0) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(uri)
        case 2 => _root_.scalapb.descriptors.PInt(line)
        case 3 => _root_.scalapb.descriptors.PInt(column)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.source.SourcePosition.type = pulumirpc.source.SourcePosition
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.SourcePosition])
}

object SourcePosition extends scalapb.GeneratedMessageCompanion[pulumirpc.source.SourcePosition] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.source.SourcePosition] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.source.SourcePosition = {
    var __uri: _root_.scala.Predef.String = ""
    var __line: _root_.scala.Int = 0
    var __column: _root_.scala.Int = 0
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __uri = _input__.readStringRequireUtf8()
        case 16 =>
          __line = _input__.readInt32()
        case 24 =>
          __column = _input__.readInt32()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.source.SourcePosition(
        uri = __uri,
        line = __line,
        column = __column,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.source.SourcePosition] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.source.SourcePosition(
        uri = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        line = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Int]).getOrElse(0),
        column = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Int]).getOrElse(0)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = SourceProto.javaDescriptor.getMessageTypes().get(0)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = SourceProto.scalaDescriptor.messages(0)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.source.SourcePosition(
    uri = "",
    line = 0,
    column = 0
  )
  implicit class SourcePositionLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.source.SourcePosition]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.source.SourcePosition](_l) {
    def uri: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.uri)((c_, f_) => c_.copy(uri = f_))
    def line: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Int] = field(_.line)((c_, f_) => c_.copy(line = f_))
    def column: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Int] = field(_.column)((c_, f_) => c_.copy(column = f_))
  }
  final val URI_FIELD_NUMBER = 1
  final val LINE_FIELD_NUMBER = 2
  final val COLUMN_FIELD_NUMBER = 3
  def of(
    uri: _root_.scala.Predef.String,
    line: _root_.scala.Int,
    column: _root_.scala.Int
  ): _root_.pulumirpc.source.SourcePosition = _root_.pulumirpc.source.SourcePosition(
    uri,
    line,
    column
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.SourcePosition])
}
