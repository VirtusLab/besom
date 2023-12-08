// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.provider

/** GetMappingResponse returns convert plugin specific data for this provider. This will normally be human
  * readable JSON, but the engine doesn't mandate any form.
  *
  * @param provider
  *   the provider key this is mapping for. For example the Pulumi provider "terraform-template" would return "template" for this.
  * @param data
  *   the conversion plugin specific data.
  */
@SerialVersionUID(0L)
final case class GetMappingResponse(
    provider: _root_.scala.Predef.String = "",
    data: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[GetMappingResponse] {
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = provider
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      
      {
        val __value = data
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
        val __v = provider
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      {
        val __v = data
        if (!__v.isEmpty) {
          _output__.writeBytes(2, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def withProvider(__v: _root_.scala.Predef.String): GetMappingResponse = copy(provider = __v)
    def withData(__v: _root_.com.google.protobuf.ByteString): GetMappingResponse = copy(data = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = provider
          if (__t != "") __t else null
        }
        case 2 => {
          val __t = data
          if (__t != _root_.com.google.protobuf.ByteString.EMPTY) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(provider)
        case 2 => _root_.scalapb.descriptors.PByteString(data)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.provider.GetMappingResponse.type = pulumirpc.provider.GetMappingResponse
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.GetMappingResponse])
}

object GetMappingResponse extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.GetMappingResponse] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.GetMappingResponse] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.GetMappingResponse = {
    var __provider: _root_.scala.Predef.String = ""
    var __data: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __provider = _input__.readStringRequireUtf8()
        case 18 =>
          __data = _input__.readBytes()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.provider.GetMappingResponse(
        provider = __provider,
        data = __data,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.GetMappingResponse] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.provider.GetMappingResponse(
        provider = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        data = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.com.google.protobuf.ByteString]).getOrElse(_root_.com.google.protobuf.ByteString.EMPTY)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ProviderProto.javaDescriptor.getMessageTypes().get(26)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ProviderProto.scalaDescriptor.messages(26)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.provider.GetMappingResponse(
    provider = "",
    data = _root_.com.google.protobuf.ByteString.EMPTY
  )
  implicit class GetMappingResponseLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.GetMappingResponse]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, pulumirpc.provider.GetMappingResponse](_l) {
    def provider: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.provider)((c_, f_) => c_.copy(provider = f_))
    def data: _root_.scalapb.lenses.Lens[UpperPB, _root_.com.google.protobuf.ByteString] = field(_.data)((c_, f_) => c_.copy(data = f_))
  }
  final val PROVIDER_FIELD_NUMBER = 1
  final val DATA_FIELD_NUMBER = 2
  def of(
    provider: _root_.scala.Predef.String,
    data: _root_.com.google.protobuf.ByteString
  ): _root_.pulumirpc.provider.GetMappingResponse = _root_.pulumirpc.provider.GetMappingResponse(
    provider,
    data
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.GetMappingResponse])
}