// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.language

/** @param diagnostics
  *   any diagnostics from code generation.
  * @param source
  *   the generated program source code.
  */
@SerialVersionUID(0L)
final case class GenerateProgramResponse(
    diagnostics: _root_.scala.Seq[pulumirpc.codegen.hcl.Diagnostic] = _root_.scala.Seq.empty,
    source: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString] = _root_.scala.collection.immutable.Map.empty,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[GenerateProgramResponse] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      diagnostics.foreach { __item =>
        val __value = __item
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
      source.foreach { __item =>
        val __value = pulumirpc.language.GenerateProgramResponse._typemapper_source.toBase(__item)
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
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
      diagnostics.foreach { __v =>
        val __m = __v
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      source.foreach { __v =>
        val __m = pulumirpc.language.GenerateProgramResponse._typemapper_source.toBase(__v)
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      unknownFields.writeTo(_output__)
    }
    def clearDiagnostics = copy(diagnostics = _root_.scala.Seq.empty)
    def addDiagnostics(__vs: pulumirpc.codegen.hcl.Diagnostic *): GenerateProgramResponse = addAllDiagnostics(__vs)
    def addAllDiagnostics(__vs: Iterable[pulumirpc.codegen.hcl.Diagnostic]): GenerateProgramResponse = copy(diagnostics = diagnostics ++ __vs)
    def withDiagnostics(__v: _root_.scala.Seq[pulumirpc.codegen.hcl.Diagnostic]): GenerateProgramResponse = copy(diagnostics = __v)
    def clearSource = copy(source = _root_.scala.collection.immutable.Map.empty)
    def addSource(__vs: (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString) *): GenerateProgramResponse = addAllSource(__vs)
    def addAllSource(__vs: Iterable[(_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)]): GenerateProgramResponse = copy(source = source ++ __vs)
    def withSource(__v: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString]): GenerateProgramResponse = copy(source = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => diagnostics
        case 2 => source.iterator.map(pulumirpc.language.GenerateProgramResponse._typemapper_source.toBase(_)).toSeq
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PRepeated(diagnostics.iterator.map(_.toPMessage).toVector)
        case 2 => _root_.scalapb.descriptors.PRepeated(source.iterator.map(pulumirpc.language.GenerateProgramResponse._typemapper_source.toBase(_).toPMessage).toVector)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.language.GenerateProgramResponse.type = pulumirpc.language.GenerateProgramResponse
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.GenerateProgramResponse])
}

object GenerateProgramResponse extends scalapb.GeneratedMessageCompanion[pulumirpc.language.GenerateProgramResponse] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.language.GenerateProgramResponse] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.language.GenerateProgramResponse = {
    val __diagnostics: _root_.scala.collection.immutable.VectorBuilder[pulumirpc.codegen.hcl.Diagnostic] = new _root_.scala.collection.immutable.VectorBuilder[pulumirpc.codegen.hcl.Diagnostic]
    val __source: _root_.scala.collection.mutable.Builder[(_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString), _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString]] = _root_.scala.collection.immutable.Map.newBuilder[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString]
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __diagnostics += _root_.scalapb.LiteParser.readMessage[pulumirpc.codegen.hcl.Diagnostic](_input__)
        case 18 =>
          __source += pulumirpc.language.GenerateProgramResponse._typemapper_source.toCustom(_root_.scalapb.LiteParser.readMessage[pulumirpc.language.GenerateProgramResponse.SourceEntry](_input__))
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.language.GenerateProgramResponse(
        diagnostics = __diagnostics.result(),
        source = __source.result(),
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.language.GenerateProgramResponse] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.language.GenerateProgramResponse(
        diagnostics = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Seq[pulumirpc.codegen.hcl.Diagnostic]]).getOrElse(_root_.scala.Seq.empty),
        source = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Seq[pulumirpc.language.GenerateProgramResponse.SourceEntry]]).getOrElse(_root_.scala.Seq.empty).iterator.map(pulumirpc.language.GenerateProgramResponse._typemapper_source.toCustom(_)).toMap
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.language.LanguageProto.javaDescriptor.getMessageTypes().get(18)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.language.LanguageProto.scalaDescriptor.messages(18)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = pulumirpc.codegen.hcl.Diagnostic
      case 2 => __out = pulumirpc.language.GenerateProgramResponse.SourceEntry
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]](
      _root_.pulumirpc.language.GenerateProgramResponse.SourceEntry
    )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.language.GenerateProgramResponse(
    diagnostics = _root_.scala.Seq.empty,
    source = _root_.scala.collection.immutable.Map.empty
  )
  @SerialVersionUID(0L)
  final case class SourceEntry(
      key: _root_.scala.Predef.String = "",
      value: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY,
      unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
      ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[SourceEntry] {
      @transient
      private var __serializedSizeMemoized: _root_.scala.Int = 0
      private def __computeSerializedSize(): _root_.scala.Int = {
        var __size = 0
        
        {
          val __value = key
          if (!__value.isEmpty) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
          }
        };
        
        {
          val __value = value
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
          val __v = key
          if (!__v.isEmpty) {
            _output__.writeString(1, __v)
          }
        };
        {
          val __v = value
          if (!__v.isEmpty) {
            _output__.writeBytes(2, __v)
          }
        };
        unknownFields.writeTo(_output__)
      }
      def withKey(__v: _root_.scala.Predef.String): SourceEntry = copy(key = __v)
      def withValue(__v: _root_.com.google.protobuf.ByteString): SourceEntry = copy(value = __v)
      def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
      def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
      def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 => {
            val __t = key
            if (__t != "") __t else null
          }
          case 2 => {
            val __t = value
            if (__t != _root_.com.google.protobuf.ByteString.EMPTY) __t else null
          }
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 => _root_.scalapb.descriptors.PString(key)
          case 2 => _root_.scalapb.descriptors.PByteString(value)
        }
      }
      def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
      def companion: pulumirpc.language.GenerateProgramResponse.SourceEntry.type = pulumirpc.language.GenerateProgramResponse.SourceEntry
      // @@protoc_insertion_point(GeneratedMessage[pulumirpc.GenerateProgramResponse.SourceEntry])
  }
  
  object SourceEntry extends scalapb.GeneratedMessageCompanion[pulumirpc.language.GenerateProgramResponse.SourceEntry] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.language.GenerateProgramResponse.SourceEntry] = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.language.GenerateProgramResponse.SourceEntry = {
      var __key: _root_.scala.Predef.String = ""
      var __value: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY
      var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __key = _input__.readStringRequireUtf8()
          case 18 =>
            __value = _input__.readBytes()
          case tag =>
            if (_unknownFields__ == null) {
              _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
            }
            _unknownFields__.parseField(tag, _input__)
        }
      }
      pulumirpc.language.GenerateProgramResponse.SourceEntry(
          key = __key,
          value = __value,
          unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.language.GenerateProgramResponse.SourceEntry] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
        pulumirpc.language.GenerateProgramResponse.SourceEntry(
          key = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          value = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.com.google.protobuf.ByteString]).getOrElse(_root_.com.google.protobuf.ByteString.EMPTY)
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.language.GenerateProgramResponse.javaDescriptor.getNestedTypes().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.language.GenerateProgramResponse.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = pulumirpc.language.GenerateProgramResponse.SourceEntry(
      key = "",
      value = _root_.com.google.protobuf.ByteString.EMPTY
    )
    implicit class SourceEntryLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.language.GenerateProgramResponse.SourceEntry]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.language.GenerateProgramResponse.SourceEntry](_l) {
      def key: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.key)((c_, f_) => c_.copy(key = f_))
      def value: _root_.scalapb.lenses.Lens[UpperPB, _root_.com.google.protobuf.ByteString] = field(_.value)((c_, f_) => c_.copy(value = f_))
    }
    final val KEY_FIELD_NUMBER = 1
    final val VALUE_FIELD_NUMBER = 2
    @transient
    implicit val keyValueMapper: _root_.scalapb.TypeMapper[pulumirpc.language.GenerateProgramResponse.SourceEntry, (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)] =
      _root_.scalapb.TypeMapper[pulumirpc.language.GenerateProgramResponse.SourceEntry, (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)](__m => (__m.key, __m.value))(__p => pulumirpc.language.GenerateProgramResponse.SourceEntry(__p._1, __p._2))
    def of(
      key: _root_.scala.Predef.String,
      value: _root_.com.google.protobuf.ByteString
    ): _root_.pulumirpc.language.GenerateProgramResponse.SourceEntry = _root_.pulumirpc.language.GenerateProgramResponse.SourceEntry(
      key,
      value
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.GenerateProgramResponse.SourceEntry])
  }
  
  implicit class GenerateProgramResponseLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.language.GenerateProgramResponse]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.language.GenerateProgramResponse](_l) {
    def diagnostics: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Seq[pulumirpc.codegen.hcl.Diagnostic]] = field(_.diagnostics)((c_, f_) => c_.copy(diagnostics = f_))
    def source: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString]] = field(_.source)((c_, f_) => c_.copy(source = f_))
  }
  final val DIAGNOSTICS_FIELD_NUMBER = 1
  final val SOURCE_FIELD_NUMBER = 2
  @transient
  private[language] val _typemapper_source: _root_.scalapb.TypeMapper[pulumirpc.language.GenerateProgramResponse.SourceEntry, (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)] = implicitly[_root_.scalapb.TypeMapper[pulumirpc.language.GenerateProgramResponse.SourceEntry, (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)]]
  def of(
    diagnostics: _root_.scala.Seq[pulumirpc.codegen.hcl.Diagnostic],
    source: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString]
  ): _root_.pulumirpc.language.GenerateProgramResponse = _root_.pulumirpc.language.GenerateProgramResponse(
    diagnostics,
    source
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.GenerateProgramResponse])
}
