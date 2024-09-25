// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.provider

/** @param version
  *   the schema version.
  */
@SerialVersionUID(0L)
final case class GetSchemaRequest(
    version: _root_.scala.Int = 0,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[GetSchemaRequest] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = version
        if (__value != 0) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(1, __value)
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
        val __v = version
        if (__v != 0) {
          _output__.writeInt32(1, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def withVersion(__v: _root_.scala.Int): GetSchemaRequest = copy(version = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = version
          if (__t != 0) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PInt(version)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.provider.GetSchemaRequest.type = pulumirpc.provider.GetSchemaRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.GetSchemaRequest])
}

object GetSchemaRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.GetSchemaRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.GetSchemaRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.GetSchemaRequest = {
    var __version: _root_.scala.Int = 0
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 8 =>
          __version = _input__.readInt32()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.provider.GetSchemaRequest(
        version = __version,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.GetSchemaRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.provider.GetSchemaRequest(
        version = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Int]).getOrElse(0)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ProviderProto.javaDescriptor.getMessageTypes().get(0)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ProviderProto.scalaDescriptor.messages(0)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.provider.GetSchemaRequest(
    version = 0
  )
  implicit class GetSchemaRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.GetSchemaRequest]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.provider.GetSchemaRequest](_l) {
    def version: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Int] = field(_.version)((c_, f_) => c_.copy(version = f_))
  }
  final val VERSION_FIELD_NUMBER = 1
  def of(
    version: _root_.scala.Int
  ): _root_.pulumirpc.provider.GetSchemaRequest = _root_.pulumirpc.provider.GetSchemaRequest(
    version
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.GetSchemaRequest])
}
