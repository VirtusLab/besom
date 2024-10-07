// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.language

/** @param stdout
  *   a line of stdout text.
  * @param stderr
  *   a line of stderr text.
  */
@SerialVersionUID(0L)
final case class InstallDependenciesResponse(
    stdout: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY,
    stderr: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[InstallDependenciesResponse] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = stdout
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBytesSize(1, __value)
        }
      };
      
      {
        val __value = stderr
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
        val __v = stdout
        if (!__v.isEmpty) {
          _output__.writeBytes(1, __v)
        }
      };
      {
        val __v = stderr
        if (!__v.isEmpty) {
          _output__.writeBytes(2, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def withStdout(__v: _root_.com.google.protobuf.ByteString): InstallDependenciesResponse = copy(stdout = __v)
    def withStderr(__v: _root_.com.google.protobuf.ByteString): InstallDependenciesResponse = copy(stderr = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = stdout
          if (__t != _root_.com.google.protobuf.ByteString.EMPTY) __t else null
        }
        case 2 => {
          val __t = stderr
          if (__t != _root_.com.google.protobuf.ByteString.EMPTY) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PByteString(stdout)
        case 2 => _root_.scalapb.descriptors.PByteString(stderr)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.language.InstallDependenciesResponse.type = pulumirpc.language.InstallDependenciesResponse
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.InstallDependenciesResponse])
}

object InstallDependenciesResponse extends scalapb.GeneratedMessageCompanion[pulumirpc.language.InstallDependenciesResponse] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.language.InstallDependenciesResponse] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.language.InstallDependenciesResponse = {
    var __stdout: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY
    var __stderr: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __stdout = _input__.readBytes()
        case 18 =>
          __stderr = _input__.readBytes()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.language.InstallDependenciesResponse(
        stdout = __stdout,
        stderr = __stderr,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.language.InstallDependenciesResponse] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.language.InstallDependenciesResponse(
        stdout = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.com.google.protobuf.ByteString]).getOrElse(_root_.com.google.protobuf.ByteString.EMPTY),
        stderr = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.com.google.protobuf.ByteString]).getOrElse(_root_.com.google.protobuf.ByteString.EMPTY)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.language.LanguageProto.javaDescriptor.getMessageTypes().get(11)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.language.LanguageProto.scalaDescriptor.messages(11)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = throw new MatchError(__number)
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.language.InstallDependenciesResponse(
    stdout = _root_.com.google.protobuf.ByteString.EMPTY,
    stderr = _root_.com.google.protobuf.ByteString.EMPTY
  )
  implicit class InstallDependenciesResponseLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.language.InstallDependenciesResponse]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.language.InstallDependenciesResponse](_l) {
    def stdout: _root_.scalapb.lenses.Lens[UpperPB, _root_.com.google.protobuf.ByteString] = field(_.stdout)((c_, f_) => c_.copy(stdout = f_))
    def stderr: _root_.scalapb.lenses.Lens[UpperPB, _root_.com.google.protobuf.ByteString] = field(_.stderr)((c_, f_) => c_.copy(stderr = f_))
  }
  final val STDOUT_FIELD_NUMBER = 1
  final val STDERR_FIELD_NUMBER = 2
  def of(
    stdout: _root_.com.google.protobuf.ByteString,
    stderr: _root_.com.google.protobuf.ByteString
  ): _root_.pulumirpc.language.InstallDependenciesResponse = _root_.pulumirpc.language.InstallDependenciesResponse(
    stdout,
    stderr
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.InstallDependenciesResponse])
}
