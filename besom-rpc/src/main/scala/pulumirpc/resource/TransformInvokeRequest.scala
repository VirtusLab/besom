// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.resource

/** TransformInvokeRequest is the request object for the TransformInvoke RPC.
  *
  * @param token
  *   the token for the invoke request.
  * @param args
  *   the input args of the resource.
  * @param options
  *   the options for the resource.
  */
@SerialVersionUID(0L)
final case class TransformInvokeRequest(
    token: _root_.scala.Predef.String = "",
    args: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
    options: _root_.scala.Option[pulumirpc.resource.TransformInvokeOptions] = _root_.scala.None,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[TransformInvokeRequest] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = token
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      if (args.isDefined) {
        val __value = args.get
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
        val __v = token
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      args.foreach { __v =>
        val __m = __v
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      options.foreach { __v =>
        val __m = __v
        _output__.writeTag(3, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      unknownFields.writeTo(_output__)
    }
    def withToken(__v: _root_.scala.Predef.String): TransformInvokeRequest = copy(token = __v)
    def getArgs: com.google.protobuf.struct.Struct = args.getOrElse(com.google.protobuf.struct.Struct.defaultInstance)
    def clearArgs: TransformInvokeRequest = copy(args = _root_.scala.None)
    def withArgs(__v: com.google.protobuf.struct.Struct): TransformInvokeRequest = copy(args = Option(__v))
    def getOptions: pulumirpc.resource.TransformInvokeOptions = options.getOrElse(pulumirpc.resource.TransformInvokeOptions.defaultInstance)
    def clearOptions: TransformInvokeRequest = copy(options = _root_.scala.None)
    def withOptions(__v: pulumirpc.resource.TransformInvokeOptions): TransformInvokeRequest = copy(options = Option(__v))
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = token
          if (__t != "") __t else null
        }
        case 2 => args.orNull
        case 3 => options.orNull
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(token)
        case 2 => args.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 3 => options.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.resource.TransformInvokeRequest.type = pulumirpc.resource.TransformInvokeRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.TransformInvokeRequest])
}

object TransformInvokeRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.resource.TransformInvokeRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.resource.TransformInvokeRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.resource.TransformInvokeRequest = {
    var __token: _root_.scala.Predef.String = ""
    var __args: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None
    var __options: _root_.scala.Option[pulumirpc.resource.TransformInvokeOptions] = _root_.scala.None
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __token = _input__.readStringRequireUtf8()
        case 18 =>
          __args = _root_.scala.Option(__args.fold(_root_.scalapb.LiteParser.readMessage[com.google.protobuf.struct.Struct](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 26 =>
          __options = _root_.scala.Option(__options.fold(_root_.scalapb.LiteParser.readMessage[pulumirpc.resource.TransformInvokeOptions](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.resource.TransformInvokeRequest(
        token = __token,
        args = __args,
        options = __options,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.resource.TransformInvokeRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.resource.TransformInvokeRequest(
        token = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        args = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[_root_.scala.Option[com.google.protobuf.struct.Struct]]),
        options = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[_root_.scala.Option[pulumirpc.resource.TransformInvokeOptions]])
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.resource.ResourceProto.javaDescriptor.getMessageTypes().get(12)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.resource.ResourceProto.scalaDescriptor.messages(12)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
    (__number: @_root_.scala.unchecked) match {
      case 2 => __out = com.google.protobuf.struct.Struct
      case 3 => __out = pulumirpc.resource.TransformInvokeOptions
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.resource.TransformInvokeRequest(
    token = "",
    args = _root_.scala.None,
    options = _root_.scala.None
  )
  implicit class TransformInvokeRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.resource.TransformInvokeRequest]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.resource.TransformInvokeRequest](_l) {
    def token: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.token)((c_, f_) => c_.copy(token = f_))
    def args: _root_.scalapb.lenses.Lens[UpperPB, com.google.protobuf.struct.Struct] = field(_.getArgs)((c_, f_) => c_.copy(args = _root_.scala.Option(f_)))
    def optionalArgs: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[com.google.protobuf.struct.Struct]] = field(_.args)((c_, f_) => c_.copy(args = f_))
    def options: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.resource.TransformInvokeOptions] = field(_.getOptions)((c_, f_) => c_.copy(options = _root_.scala.Option(f_)))
    def optionalOptions: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[pulumirpc.resource.TransformInvokeOptions]] = field(_.options)((c_, f_) => c_.copy(options = f_))
  }
  final val TOKEN_FIELD_NUMBER = 1
  final val ARGS_FIELD_NUMBER = 2
  final val OPTIONS_FIELD_NUMBER = 3
  def of(
    token: _root_.scala.Predef.String,
    args: _root_.scala.Option[com.google.protobuf.struct.Struct],
    options: _root_.scala.Option[pulumirpc.resource.TransformInvokeOptions]
  ): _root_.pulumirpc.resource.TransformInvokeRequest = _root_.pulumirpc.resource.TransformInvokeRequest(
    token,
    args,
    options
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.TransformInvokeRequest])
}