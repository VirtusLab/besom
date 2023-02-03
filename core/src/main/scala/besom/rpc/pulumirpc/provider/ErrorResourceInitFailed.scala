// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.provider

/** ErrorResourceInitFailed is sent as a Detail `ResourceProvider.{Create, Update}` fail because a
  * resource was created successfully, but failed to initialize.
  *
  * @param id
  *   the ID of the created resource.
  * @param properties
  *   any properties that were computed during updating.
  * @param reasons
  *   error messages associated with initialization failure.
  * @param inputs
  *   the current inputs to this resource (only applicable for Read)
  */
@SerialVersionUID(0L)
final case class ErrorResourceInitFailed(
    id: _root_.scala.Predef.String = "",
    properties: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
    reasons: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
    inputs: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[ErrorResourceInitFailed] {
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = id
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      if (properties.isDefined) {
        val __value = properties.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      reasons.foreach { __item =>
        val __value = __item
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, __value)
      }
      if (inputs.isDefined) {
        val __value = inputs.get
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
        val __v = id
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      properties.foreach { __v =>
        val __m = __v
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      reasons.foreach { __v =>
        val __m = __v
        _output__.writeString(3, __m)
      };
      inputs.foreach { __v =>
        val __m = __v
        _output__.writeTag(4, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      unknownFields.writeTo(_output__)
    }
    def withId(__v: _root_.scala.Predef.String): ErrorResourceInitFailed = copy(id = __v)
    def getProperties: com.google.protobuf.struct.Struct = properties.getOrElse(com.google.protobuf.struct.Struct.defaultInstance)
    def clearProperties: ErrorResourceInitFailed = copy(properties = _root_.scala.None)
    def withProperties(__v: com.google.protobuf.struct.Struct): ErrorResourceInitFailed = copy(properties = Option(__v))
    def clearReasons = copy(reasons = _root_.scala.Seq.empty)
    def addReasons(__vs: _root_.scala.Predef.String *): ErrorResourceInitFailed = addAllReasons(__vs)
    def addAllReasons(__vs: Iterable[_root_.scala.Predef.String]): ErrorResourceInitFailed = copy(reasons = reasons ++ __vs)
    def withReasons(__v: _root_.scala.Seq[_root_.scala.Predef.String]): ErrorResourceInitFailed = copy(reasons = __v)
    def getInputs: com.google.protobuf.struct.Struct = inputs.getOrElse(com.google.protobuf.struct.Struct.defaultInstance)
    def clearInputs: ErrorResourceInitFailed = copy(inputs = _root_.scala.None)
    def withInputs(__v: com.google.protobuf.struct.Struct): ErrorResourceInitFailed = copy(inputs = Option(__v))
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = id
          if (__t != "") __t else null
        }
        case 2 => properties.orNull
        case 3 => reasons
        case 4 => inputs.orNull
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(id)
        case 2 => properties.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 3 => _root_.scalapb.descriptors.PRepeated(reasons.iterator.map(_root_.scalapb.descriptors.PString(_)).toVector)
        case 4 => inputs.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.provider.ErrorResourceInitFailed.type = pulumirpc.provider.ErrorResourceInitFailed
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.ErrorResourceInitFailed])
}

object ErrorResourceInitFailed extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.ErrorResourceInitFailed] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.ErrorResourceInitFailed] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.ErrorResourceInitFailed = {
    var __id: _root_.scala.Predef.String = ""
    var __properties: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None
    val __reasons: _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String] = new _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String]
    var __inputs: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __id = _input__.readStringRequireUtf8()
        case 18 =>
          __properties = Option(__properties.fold(_root_.scalapb.LiteParser.readMessage[com.google.protobuf.struct.Struct](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 26 =>
          __reasons += _input__.readStringRequireUtf8()
        case 34 =>
          __inputs = Option(__inputs.fold(_root_.scalapb.LiteParser.readMessage[com.google.protobuf.struct.Struct](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.provider.ErrorResourceInitFailed(
        id = __id,
        properties = __properties,
        reasons = __reasons.result(),
        inputs = __inputs,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.ErrorResourceInitFailed] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.provider.ErrorResourceInitFailed(
        id = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        properties = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[_root_.scala.Option[com.google.protobuf.struct.Struct]]),
        reasons = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Seq[_root_.scala.Predef.String]]).getOrElse(_root_.scala.Seq.empty),
        inputs = __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[_root_.scala.Option[com.google.protobuf.struct.Struct]])
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ProviderProto.javaDescriptor.getMessageTypes().get(24)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ProviderProto.scalaDescriptor.messages(24)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 2 => __out = com.google.protobuf.struct.Struct
      case 4 => __out = com.google.protobuf.struct.Struct
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.provider.ErrorResourceInitFailed(
    id = "",
    properties = _root_.scala.None,
    reasons = _root_.scala.Seq.empty,
    inputs = _root_.scala.None
  )
  implicit class ErrorResourceInitFailedLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.ErrorResourceInitFailed]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, pulumirpc.provider.ErrorResourceInitFailed](_l) {
    def id: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.id)((c_, f_) => c_.copy(id = f_))
    def properties: _root_.scalapb.lenses.Lens[UpperPB, com.google.protobuf.struct.Struct] = field(_.getProperties)((c_, f_) => c_.copy(properties = Option(f_)))
    def optionalProperties: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[com.google.protobuf.struct.Struct]] = field(_.properties)((c_, f_) => c_.copy(properties = f_))
    def reasons: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Seq[_root_.scala.Predef.String]] = field(_.reasons)((c_, f_) => c_.copy(reasons = f_))
    def inputs: _root_.scalapb.lenses.Lens[UpperPB, com.google.protobuf.struct.Struct] = field(_.getInputs)((c_, f_) => c_.copy(inputs = Option(f_)))
    def optionalInputs: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[com.google.protobuf.struct.Struct]] = field(_.inputs)((c_, f_) => c_.copy(inputs = f_))
  }
  final val ID_FIELD_NUMBER = 1
  final val PROPERTIES_FIELD_NUMBER = 2
  final val REASONS_FIELD_NUMBER = 3
  final val INPUTS_FIELD_NUMBER = 4
  def of(
    id: _root_.scala.Predef.String,
    properties: _root_.scala.Option[com.google.protobuf.struct.Struct],
    reasons: _root_.scala.Seq[_root_.scala.Predef.String],
    inputs: _root_.scala.Option[com.google.protobuf.struct.Struct]
  ): _root_.pulumirpc.provider.ErrorResourceInitFailed = _root_.pulumirpc.provider.ErrorResourceInitFailed(
    id,
    properties,
    reasons,
    inputs
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.ErrorResourceInitFailed])
}
