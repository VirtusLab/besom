// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.resource

/** @param type
  *   the type of the resource.
  * @param name
  *   the name of the resource.
  * @param custom
  *   true if the resource is a custom resource, else it's a component resource.
  * @param parent
  *   the parent of the resource, this can't be changed by the transform.
  * @param properties
  *   the input properties of the resource.
  * @param options
  *   the options for the resource.
  */
@SerialVersionUID(0L)
final case class TransformRequest(
    `type`: _root_.scala.Predef.String = "",
    name: _root_.scala.Predef.String = "",
    custom: _root_.scala.Boolean = false,
    parent: _root_.scala.Predef.String = "",
    properties: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
    options: _root_.scala.Option[pulumirpc.resource.TransformResourceOptions] = _root_.scala.None,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[TransformRequest] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = `type`
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      
      {
        val __value = name
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, __value)
        }
      };
      
      {
        val __value = custom
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(3, __value)
        }
      };
      
      {
        val __value = parent
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, __value)
        }
      };
      if (properties.isDefined) {
        val __value = properties.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      if (options.isDefined) {
        val __value = options.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
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
        val __v = `type`
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      {
        val __v = name
        if (!__v.isEmpty) {
          _output__.writeString(2, __v)
        }
      };
      {
        val __v = custom
        if (__v != false) {
          _output__.writeBool(3, __v)
        }
      };
      {
        val __v = parent
        if (!__v.isEmpty) {
          _output__.writeString(4, __v)
        }
      };
      properties.foreach { __v =>
        val __m = __v
        _output__.writeTag(5, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      options.foreach { __v =>
        val __m = __v
        _output__.writeTag(6, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      unknownFields.writeTo(_output__)
    }
    def withType(__v: _root_.scala.Predef.String): TransformRequest = copy(`type` = __v)
    def withName(__v: _root_.scala.Predef.String): TransformRequest = copy(name = __v)
    def withCustom(__v: _root_.scala.Boolean): TransformRequest = copy(custom = __v)
    def withParent(__v: _root_.scala.Predef.String): TransformRequest = copy(parent = __v)
    def getProperties: com.google.protobuf.struct.Struct = properties.getOrElse(com.google.protobuf.struct.Struct.defaultInstance)
    def clearProperties: TransformRequest = copy(properties = _root_.scala.None)
    def withProperties(__v: com.google.protobuf.struct.Struct): TransformRequest = copy(properties = Option(__v))
    def getOptions: pulumirpc.resource.TransformResourceOptions = options.getOrElse(pulumirpc.resource.TransformResourceOptions.defaultInstance)
    def clearOptions: TransformRequest = copy(options = _root_.scala.None)
    def withOptions(__v: pulumirpc.resource.TransformResourceOptions): TransformRequest = copy(options = Option(__v))
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = `type`
          if (__t != "") __t else null
        }
        case 2 => {
          val __t = name
          if (__t != "") __t else null
        }
        case 3 => {
          val __t = custom
          if (__t != false) __t else null
        }
        case 4 => {
          val __t = parent
          if (__t != "") __t else null
        }
        case 5 => properties.orNull
        case 6 => options.orNull
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(`type`)
        case 2 => _root_.scalapb.descriptors.PString(name)
        case 3 => _root_.scalapb.descriptors.PBoolean(custom)
        case 4 => _root_.scalapb.descriptors.PString(parent)
        case 5 => properties.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 6 => options.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.resource.TransformRequest.type = pulumirpc.resource.TransformRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.TransformRequest])
}

object TransformRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.resource.TransformRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.resource.TransformRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.resource.TransformRequest = {
    var __type: _root_.scala.Predef.String = ""
    var __name: _root_.scala.Predef.String = ""
    var __custom: _root_.scala.Boolean = false
    var __parent: _root_.scala.Predef.String = ""
    var __properties: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None
    var __options: _root_.scala.Option[pulumirpc.resource.TransformResourceOptions] = _root_.scala.None
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __type = _input__.readStringRequireUtf8()
        case 18 =>
          __name = _input__.readStringRequireUtf8()
        case 24 =>
          __custom = _input__.readBool()
        case 34 =>
          __parent = _input__.readStringRequireUtf8()
        case 42 =>
          __properties = _root_.scala.Option(__properties.fold(_root_.scalapb.LiteParser.readMessage[com.google.protobuf.struct.Struct](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 50 =>
          __options = _root_.scala.Option(__options.fold(_root_.scalapb.LiteParser.readMessage[pulumirpc.resource.TransformResourceOptions](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.resource.TransformRequest(
        `type` = __type,
        name = __name,
        custom = __custom,
        parent = __parent,
        properties = __properties,
        options = __options,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.resource.TransformRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.resource.TransformRequest(
        `type` = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        name = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        custom = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Boolean]).getOrElse(false),
        parent = __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        properties = __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[_root_.scala.Option[com.google.protobuf.struct.Struct]]),
        options = __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[_root_.scala.Option[pulumirpc.resource.TransformResourceOptions]])
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.resource.ResourceProto.javaDescriptor.getMessageTypes().get(10)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.resource.ResourceProto.scalaDescriptor.messages(10)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
    (__number: @_root_.scala.unchecked) match {
      case 5 => __out = com.google.protobuf.struct.Struct
      case 6 => __out = pulumirpc.resource.TransformResourceOptions
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.resource.TransformRequest(
    `type` = "",
    name = "",
    custom = false,
    parent = "",
    properties = _root_.scala.None,
    options = _root_.scala.None
  )
  implicit class TransformRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.resource.TransformRequest]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.resource.TransformRequest](_l) {
    def `type`: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.`type`)((c_, f_) => c_.copy(`type` = f_))
    def name: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.name)((c_, f_) => c_.copy(name = f_))
    def custom: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.custom)((c_, f_) => c_.copy(custom = f_))
    def parent: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.parent)((c_, f_) => c_.copy(parent = f_))
    def properties: _root_.scalapb.lenses.Lens[UpperPB, com.google.protobuf.struct.Struct] = field(_.getProperties)((c_, f_) => c_.copy(properties = _root_.scala.Option(f_)))
    def optionalProperties: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[com.google.protobuf.struct.Struct]] = field(_.properties)((c_, f_) => c_.copy(properties = f_))
    def options: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.resource.TransformResourceOptions] = field(_.getOptions)((c_, f_) => c_.copy(options = _root_.scala.Option(f_)))
    def optionalOptions: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[pulumirpc.resource.TransformResourceOptions]] = field(_.options)((c_, f_) => c_.copy(options = f_))
  }
  final val TYPE_FIELD_NUMBER = 1
  final val NAME_FIELD_NUMBER = 2
  final val CUSTOM_FIELD_NUMBER = 3
  final val PARENT_FIELD_NUMBER = 4
  final val PROPERTIES_FIELD_NUMBER = 5
  final val OPTIONS_FIELD_NUMBER = 6
  def of(
    `type`: _root_.scala.Predef.String,
    name: _root_.scala.Predef.String,
    custom: _root_.scala.Boolean,
    parent: _root_.scala.Predef.String,
    properties: _root_.scala.Option[com.google.protobuf.struct.Struct],
    options: _root_.scala.Option[pulumirpc.resource.TransformResourceOptions]
  ): _root_.pulumirpc.resource.TransformRequest = _root_.pulumirpc.resource.TransformRequest(
    `type`,
    name,
    custom,
    parent,
    properties,
    options
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.TransformRequest])
}
