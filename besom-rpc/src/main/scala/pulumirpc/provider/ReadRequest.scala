// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.provider

/** @param id
  *   the ID of the resource to read.
  * @param urn
  *   the Pulumi URN for this resource.
  * @param properties
  *   the current state (sufficiently complete to identify the resource).
  * @param inputs
  *   the current inputs, if any (only populated during refresh).
  * @param name
  *   the Pulumi name for this resource.
  * @param type
  *   the Pulumi type for this resource.
  */
@SerialVersionUID(0L)
final case class ReadRequest(
    id: _root_.scala.Predef.String = "",
    urn: _root_.scala.Predef.String = "",
    properties: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
    inputs: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
    name: _root_.scala.Predef.String = "",
    `type`: _root_.scala.Predef.String = "",
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[ReadRequest] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = id
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      
      {
        val __value = urn
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, __value)
        }
      };
      if (properties.isDefined) {
        val __value = properties.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      if (inputs.isDefined) {
        val __value = inputs.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      
      {
        val __value = name
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, __value)
        }
      };
      
      {
        val __value = `type`
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(6, __value)
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
        val __v = id
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      {
        val __v = urn
        if (!__v.isEmpty) {
          _output__.writeString(2, __v)
        }
      };
      properties.foreach { __v =>
        val __m = __v
        _output__.writeTag(3, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      inputs.foreach { __v =>
        val __m = __v
        _output__.writeTag(4, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      {
        val __v = name
        if (!__v.isEmpty) {
          _output__.writeString(5, __v)
        }
      };
      {
        val __v = `type`
        if (!__v.isEmpty) {
          _output__.writeString(6, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def withId(__v: _root_.scala.Predef.String): ReadRequest = copy(id = __v)
    def withUrn(__v: _root_.scala.Predef.String): ReadRequest = copy(urn = __v)
    def getProperties: com.google.protobuf.struct.Struct = properties.getOrElse(com.google.protobuf.struct.Struct.defaultInstance)
    def clearProperties: ReadRequest = copy(properties = _root_.scala.None)
    def withProperties(__v: com.google.protobuf.struct.Struct): ReadRequest = copy(properties = Option(__v))
    def getInputs: com.google.protobuf.struct.Struct = inputs.getOrElse(com.google.protobuf.struct.Struct.defaultInstance)
    def clearInputs: ReadRequest = copy(inputs = _root_.scala.None)
    def withInputs(__v: com.google.protobuf.struct.Struct): ReadRequest = copy(inputs = Option(__v))
    def withName(__v: _root_.scala.Predef.String): ReadRequest = copy(name = __v)
    def withType(__v: _root_.scala.Predef.String): ReadRequest = copy(`type` = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = id
          if (__t != "") __t else null
        }
        case 2 => {
          val __t = urn
          if (__t != "") __t else null
        }
        case 3 => properties.orNull
        case 4 => inputs.orNull
        case 5 => {
          val __t = name
          if (__t != "") __t else null
        }
        case 6 => {
          val __t = `type`
          if (__t != "") __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(id)
        case 2 => _root_.scalapb.descriptors.PString(urn)
        case 3 => properties.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 4 => inputs.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 5 => _root_.scalapb.descriptors.PString(name)
        case 6 => _root_.scalapb.descriptors.PString(`type`)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.provider.ReadRequest.type = pulumirpc.provider.ReadRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.ReadRequest])
}

object ReadRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.ReadRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.ReadRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.ReadRequest = {
    var __id: _root_.scala.Predef.String = ""
    var __urn: _root_.scala.Predef.String = ""
    var __properties: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None
    var __inputs: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None
    var __name: _root_.scala.Predef.String = ""
    var __type: _root_.scala.Predef.String = ""
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __id = _input__.readStringRequireUtf8()
        case 18 =>
          __urn = _input__.readStringRequireUtf8()
        case 26 =>
          __properties = _root_.scala.Option(__properties.fold(_root_.scalapb.LiteParser.readMessage[com.google.protobuf.struct.Struct](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 34 =>
          __inputs = _root_.scala.Option(__inputs.fold(_root_.scalapb.LiteParser.readMessage[com.google.protobuf.struct.Struct](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 42 =>
          __name = _input__.readStringRequireUtf8()
        case 50 =>
          __type = _input__.readStringRequireUtf8()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.provider.ReadRequest(
        id = __id,
        urn = __urn,
        properties = __properties,
        inputs = __inputs,
        name = __name,
        `type` = __type,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.ReadRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.provider.ReadRequest(
        id = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        urn = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        properties = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[_root_.scala.Option[com.google.protobuf.struct.Struct]]),
        inputs = __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[_root_.scala.Option[com.google.protobuf.struct.Struct]]),
        name = __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        `type` = __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.provider.ProviderProto.javaDescriptor.getMessageTypes().get(19)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.provider.ProviderProto.scalaDescriptor.messages(19)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
    (__number: @_root_.scala.unchecked) match {
      case 3 => __out = com.google.protobuf.struct.Struct
      case 4 => __out = com.google.protobuf.struct.Struct
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.provider.ReadRequest(
    id = "",
    urn = "",
    properties = _root_.scala.None,
    inputs = _root_.scala.None,
    name = "",
    `type` = ""
  )
  implicit class ReadRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.ReadRequest]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.provider.ReadRequest](_l) {
    def id: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.id)((c_, f_) => c_.copy(id = f_))
    def urn: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.urn)((c_, f_) => c_.copy(urn = f_))
    def properties: _root_.scalapb.lenses.Lens[UpperPB, com.google.protobuf.struct.Struct] = field(_.getProperties)((c_, f_) => c_.copy(properties = _root_.scala.Option(f_)))
    def optionalProperties: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[com.google.protobuf.struct.Struct]] = field(_.properties)((c_, f_) => c_.copy(properties = f_))
    def inputs: _root_.scalapb.lenses.Lens[UpperPB, com.google.protobuf.struct.Struct] = field(_.getInputs)((c_, f_) => c_.copy(inputs = _root_.scala.Option(f_)))
    def optionalInputs: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[com.google.protobuf.struct.Struct]] = field(_.inputs)((c_, f_) => c_.copy(inputs = f_))
    def name: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.name)((c_, f_) => c_.copy(name = f_))
    def `type`: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.`type`)((c_, f_) => c_.copy(`type` = f_))
  }
  final val ID_FIELD_NUMBER = 1
  final val URN_FIELD_NUMBER = 2
  final val PROPERTIES_FIELD_NUMBER = 3
  final val INPUTS_FIELD_NUMBER = 4
  final val NAME_FIELD_NUMBER = 5
  final val TYPE_FIELD_NUMBER = 6
  def of(
    id: _root_.scala.Predef.String,
    urn: _root_.scala.Predef.String,
    properties: _root_.scala.Option[com.google.protobuf.struct.Struct],
    inputs: _root_.scala.Option[com.google.protobuf.struct.Struct],
    name: _root_.scala.Predef.String,
    `type`: _root_.scala.Predef.String
  ): _root_.pulumirpc.provider.ReadRequest = _root_.pulumirpc.provider.ReadRequest(
    id,
    urn,
    properties,
    inputs,
    name,
    `type`
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.ReadRequest])
}
