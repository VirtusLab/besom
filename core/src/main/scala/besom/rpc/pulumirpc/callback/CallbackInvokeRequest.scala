// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.callback

/** @param token
  *   the token for the callback.
  * @param request
  *   the serialized protobuf message of the arguments for this callback.
  */
@SerialVersionUID(0L)
final case class CallbackInvokeRequest(
    token: _root_.scala.Predef.String = "",
    request: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[CallbackInvokeRequest] {
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = token
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      
      {
        val __value = request
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBytesSize(2, __value)
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
        val __v = token
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      {
        val __v = request
        if (!__v.isEmpty) {
          _output__.writeBytes(2, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def withToken(__v: _root_.scala.Predef.String): CallbackInvokeRequest = copy(token = __v)
    def withRequest(__v: _root_.com.google.protobuf.ByteString): CallbackInvokeRequest = copy(request = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = token
          if (__t != "") __t else null
        }
        case 2 => {
          val __t = request
          if (__t != _root_.com.google.protobuf.ByteString.EMPTY) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(token)
        case 2 => _root_.scalapb.descriptors.PByteString(request)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.callback.CallbackInvokeRequest.type = pulumirpc.callback.CallbackInvokeRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.CallbackInvokeRequest])
}

object CallbackInvokeRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.callback.CallbackInvokeRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.callback.CallbackInvokeRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.callback.CallbackInvokeRequest = {
    var __token: _root_.scala.Predef.String = ""
    var __request: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __token = _input__.readStringRequireUtf8()
        case 18 =>
          __request = _input__.readBytes()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.callback.CallbackInvokeRequest(
        token = __token,
        request = __request,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.callback.CallbackInvokeRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.callback.CallbackInvokeRequest(
        token = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        request = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.com.google.protobuf.ByteString]).getOrElse(_root_.com.google.protobuf.ByteString.EMPTY)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = CallbackProto.javaDescriptor.getMessageTypes().get(1)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = CallbackProto.scalaDescriptor.messages(1)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.callback.CallbackInvokeRequest(
    token = "",
    request = _root_.com.google.protobuf.ByteString.EMPTY
  )
  implicit class CallbackInvokeRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.callback.CallbackInvokeRequest]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, pulumirpc.callback.CallbackInvokeRequest](_l) {
    def token: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.token)((c_, f_) => c_.copy(token = f_))
    def request: _root_.scalapb.lenses.Lens[UpperPB, _root_.com.google.protobuf.ByteString] = field(_.request)((c_, f_) => c_.copy(request = f_))
  }
  final val TOKEN_FIELD_NUMBER = 1
  final val REQUEST_FIELD_NUMBER = 2
  def of(
    token: _root_.scala.Predef.String,
    request: _root_.com.google.protobuf.ByteString
  ): _root_.pulumirpc.callback.CallbackInvokeRequest = _root_.pulumirpc.callback.CallbackInvokeRequest(
    token,
    request
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.CallbackInvokeRequest])
}
