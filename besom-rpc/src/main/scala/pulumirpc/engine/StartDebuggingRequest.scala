// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.engine

/** @param config
  *   the debug configuration parameters.  These are meant to be in the right format for the DAP protocol to consume.
  * @param message
  *   the string to display to the user with instructions on how to connect to the debugger.
  */
@SerialVersionUID(0L)
final case class StartDebuggingRequest(
    config: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
    message: _root_.scala.Predef.String = "",
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[StartDebuggingRequest] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      if (config.isDefined) {
        val __value = config.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      
      {
        val __value = message
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, __value)
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
      config.foreach { __v =>
        val __m = __v
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      {
        val __v = message
        if (!__v.isEmpty) {
          _output__.writeString(2, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def getConfig: com.google.protobuf.struct.Struct = config.getOrElse(com.google.protobuf.struct.Struct.defaultInstance)
    def clearConfig: StartDebuggingRequest = copy(config = _root_.scala.None)
    def withConfig(__v: com.google.protobuf.struct.Struct): StartDebuggingRequest = copy(config = Option(__v))
    def withMessage(__v: _root_.scala.Predef.String): StartDebuggingRequest = copy(message = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => config.orNull
        case 2 => {
          val __t = message
          if (__t != "") __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => config.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 2 => _root_.scalapb.descriptors.PString(message)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.engine.StartDebuggingRequest.type = pulumirpc.engine.StartDebuggingRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.StartDebuggingRequest])
}

object StartDebuggingRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.engine.StartDebuggingRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.engine.StartDebuggingRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.engine.StartDebuggingRequest = {
    var __config: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None
    var __message: _root_.scala.Predef.String = ""
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __config = _root_.scala.Option(__config.fold(_root_.scalapb.LiteParser.readMessage[com.google.protobuf.struct.Struct](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 18 =>
          __message = _input__.readStringRequireUtf8()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.engine.StartDebuggingRequest(
        config = __config,
        message = __message,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.engine.StartDebuggingRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.engine.StartDebuggingRequest(
        config = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[_root_.scala.Option[com.google.protobuf.struct.Struct]]),
        message = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.engine.EngineProto.javaDescriptor.getMessageTypes().get(5)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.engine.EngineProto.scalaDescriptor.messages(5)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = com.google.protobuf.struct.Struct
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.engine.StartDebuggingRequest(
    config = _root_.scala.None,
    message = ""
  )
  implicit class StartDebuggingRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.engine.StartDebuggingRequest]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.engine.StartDebuggingRequest](_l) {
    def config: _root_.scalapb.lenses.Lens[UpperPB, com.google.protobuf.struct.Struct] = field(_.getConfig)((c_, f_) => c_.copy(config = _root_.scala.Option(f_)))
    def optionalConfig: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[com.google.protobuf.struct.Struct]] = field(_.config)((c_, f_) => c_.copy(config = f_))
    def message: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.message)((c_, f_) => c_.copy(message = f_))
  }
  final val CONFIG_FIELD_NUMBER = 1
  final val MESSAGE_FIELD_NUMBER = 2
  def of(
    config: _root_.scala.Option[com.google.protobuf.struct.Struct],
    message: _root_.scala.Predef.String
  ): _root_.pulumirpc.engine.StartDebuggingRequest = _root_.pulumirpc.engine.StartDebuggingRequest(
    config,
    message
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.StartDebuggingRequest])
}