// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.language

/** RunResponse is the response back from the interpreter/source back to the monitor.
  *
  * @param error
  *   An unhandled error if any occurred.
  * @param bail
  *   An error happened.  And it was reported to the user.  Work should stop immediately
  *   with nothing further to print to the user.  This corresponds to a "result.Bail()"
  *   value in the 'go' layer.
  */
@SerialVersionUID(0L)
final case class RunResponse(
    error: _root_.scala.Predef.String = "",
    bail: _root_.scala.Boolean = false,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[RunResponse] {
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = error
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      
      {
        val __value = bail
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(2, __value)
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
        val __v = error
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      {
        val __v = bail
        if (__v != false) {
          _output__.writeBool(2, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def withError(__v: _root_.scala.Predef.String): RunResponse = copy(error = __v)
    def withBail(__v: _root_.scala.Boolean): RunResponse = copy(bail = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = error
          if (__t != "") __t else null
        }
        case 2 => {
          val __t = bail
          if (__t != false) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(error)
        case 2 => _root_.scalapb.descriptors.PBoolean(bail)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.language.RunResponse.type = pulumirpc.language.RunResponse
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.RunResponse])
}

object RunResponse extends scalapb.GeneratedMessageCompanion[pulumirpc.language.RunResponse] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.language.RunResponse] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.language.RunResponse = {
    var __error: _root_.scala.Predef.String = ""
    var __bail: _root_.scala.Boolean = false
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __error = _input__.readStringRequireUtf8()
        case 16 =>
          __bail = _input__.readBool()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.language.RunResponse(
        error = __error,
        bail = __bail,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.language.RunResponse] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.language.RunResponse(
        error = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        bail = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Boolean]).getOrElse(false)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = LanguageProto.javaDescriptor.getMessageTypes().get(3)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = LanguageProto.scalaDescriptor.messages(3)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.language.RunResponse(
    error = "",
    bail = false
  )
  implicit class RunResponseLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.language.RunResponse]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, pulumirpc.language.RunResponse](_l) {
    def error: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.error)((c_, f_) => c_.copy(error = f_))
    def bail: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.bail)((c_, f_) => c_.copy(bail = f_))
  }
  final val ERROR_FIELD_NUMBER = 1
  final val BAIL_FIELD_NUMBER = 2
  def of(
    error: _root_.scala.Predef.String,
    bail: _root_.scala.Boolean
  ): _root_.pulumirpc.language.RunResponse = _root_.pulumirpc.language.RunResponse(
    error,
    bail
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.RunResponse])
}
