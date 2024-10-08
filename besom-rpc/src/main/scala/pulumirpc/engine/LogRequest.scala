// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.engine

/** @param severity
  *   the logging level of this message.
  * @param message
  *   the contents of the logged message.
  * @param urn
  *   the (optional) resource urn this log is associated with.
  * @param streamId
  *   the (optional) stream id that a stream of log messages can be associated with. This allows
  *   clients to not have to buffer a large set of log messages that they all want to be
  *   conceptually connected.  Instead the messages can be sent as chunks (with the same stream id)
  *   and the end display can show the messages as they arrive, while still stitching them together
  *   into one total log message.
  *  
  *   0/not-given means: do not associate with any stream.
  * @param ephemeral
  *   Optional value indicating whether this is a status message.
  */
@SerialVersionUID(0L)
final case class LogRequest(
    severity: pulumirpc.engine.LogSeverity = pulumirpc.engine.LogSeverity.DEBUG,
    message: _root_.scala.Predef.String = "",
    urn: _root_.scala.Predef.String = "",
    streamId: _root_.scala.Int = 0,
    ephemeral: _root_.scala.Boolean = false,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[LogRequest] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = severity.value
        if (__value != 0) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeEnumSize(1, __value)
        }
      };
      
      {
        val __value = message
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, __value)
        }
      };
      
      {
        val __value = urn
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, __value)
        }
      };
      
      {
        val __value = streamId
        if (__value != 0) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(4, __value)
        }
      };
      
      {
        val __value = ephemeral
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(5, __value)
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
        val __v = severity.value
        if (__v != 0) {
          _output__.writeEnum(1, __v)
        }
      };
      {
        val __v = message
        if (!__v.isEmpty) {
          _output__.writeString(2, __v)
        }
      };
      {
        val __v = urn
        if (!__v.isEmpty) {
          _output__.writeString(3, __v)
        }
      };
      {
        val __v = streamId
        if (__v != 0) {
          _output__.writeInt32(4, __v)
        }
      };
      {
        val __v = ephemeral
        if (__v != false) {
          _output__.writeBool(5, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def withSeverity(__v: pulumirpc.engine.LogSeverity): LogRequest = copy(severity = __v)
    def withMessage(__v: _root_.scala.Predef.String): LogRequest = copy(message = __v)
    def withUrn(__v: _root_.scala.Predef.String): LogRequest = copy(urn = __v)
    def withStreamId(__v: _root_.scala.Int): LogRequest = copy(streamId = __v)
    def withEphemeral(__v: _root_.scala.Boolean): LogRequest = copy(ephemeral = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = severity.javaValueDescriptor
          if (__t.getNumber() != 0) __t else null
        }
        case 2 => {
          val __t = message
          if (__t != "") __t else null
        }
        case 3 => {
          val __t = urn
          if (__t != "") __t else null
        }
        case 4 => {
          val __t = streamId
          if (__t != 0) __t else null
        }
        case 5 => {
          val __t = ephemeral
          if (__t != false) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PEnum(severity.scalaValueDescriptor)
        case 2 => _root_.scalapb.descriptors.PString(message)
        case 3 => _root_.scalapb.descriptors.PString(urn)
        case 4 => _root_.scalapb.descriptors.PInt(streamId)
        case 5 => _root_.scalapb.descriptors.PBoolean(ephemeral)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.engine.LogRequest.type = pulumirpc.engine.LogRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.LogRequest])
}

object LogRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.engine.LogRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.engine.LogRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.engine.LogRequest = {
    var __severity: pulumirpc.engine.LogSeverity = pulumirpc.engine.LogSeverity.DEBUG
    var __message: _root_.scala.Predef.String = ""
    var __urn: _root_.scala.Predef.String = ""
    var __streamId: _root_.scala.Int = 0
    var __ephemeral: _root_.scala.Boolean = false
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 8 =>
          __severity = pulumirpc.engine.LogSeverity.fromValue(_input__.readEnum())
        case 18 =>
          __message = _input__.readStringRequireUtf8()
        case 26 =>
          __urn = _input__.readStringRequireUtf8()
        case 32 =>
          __streamId = _input__.readInt32()
        case 40 =>
          __ephemeral = _input__.readBool()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.engine.LogRequest(
        severity = __severity,
        message = __message,
        urn = __urn,
        streamId = __streamId,
        ephemeral = __ephemeral,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.engine.LogRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.engine.LogRequest(
        severity = pulumirpc.engine.LogSeverity.fromValue(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scalapb.descriptors.EnumValueDescriptor]).getOrElse(pulumirpc.engine.LogSeverity.DEBUG.scalaValueDescriptor).number),
        message = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        urn = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        streamId = __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).map(_.as[_root_.scala.Int]).getOrElse(0),
        ephemeral = __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).map(_.as[_root_.scala.Boolean]).getOrElse(false)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.engine.EngineProto.javaDescriptor.getMessageTypes().get(0)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.engine.EngineProto.scalaDescriptor.messages(0)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = {
    (__fieldNumber: @_root_.scala.unchecked) match {
      case 1 => pulumirpc.engine.LogSeverity
    }
  }
  lazy val defaultInstance = pulumirpc.engine.LogRequest(
    severity = pulumirpc.engine.LogSeverity.DEBUG,
    message = "",
    urn = "",
    streamId = 0,
    ephemeral = false
  )
  implicit class LogRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.engine.LogRequest]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.engine.LogRequest](_l) {
    def severity: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.engine.LogSeverity] = field(_.severity)((c_, f_) => c_.copy(severity = f_))
    def message: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.message)((c_, f_) => c_.copy(message = f_))
    def urn: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.urn)((c_, f_) => c_.copy(urn = f_))
    def streamId: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Int] = field(_.streamId)((c_, f_) => c_.copy(streamId = f_))
    def ephemeral: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.ephemeral)((c_, f_) => c_.copy(ephemeral = f_))
  }
  final val SEVERITY_FIELD_NUMBER = 1
  final val MESSAGE_FIELD_NUMBER = 2
  final val URN_FIELD_NUMBER = 3
  final val STREAMID_FIELD_NUMBER = 4
  final val EPHEMERAL_FIELD_NUMBER = 5
  def of(
    severity: pulumirpc.engine.LogSeverity,
    message: _root_.scala.Predef.String,
    urn: _root_.scala.Predef.String,
    streamId: _root_.scala.Int,
    ephemeral: _root_.scala.Boolean
  ): _root_.pulumirpc.engine.LogRequest = _root_.pulumirpc.engine.LogRequest(
    severity,
    message,
    urn,
    streamId,
    ephemeral
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.LogRequest])
}
