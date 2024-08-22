// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.language

/** @param directory
  *   the directory to generate the package in.
  * @param schema
  *   the JSON-encoded schema.
  * @param extraFiles
  *   extra files to copy to the package output.
  * @param loaderTarget
  *   The target of a codegen.LoaderServer to use for loading schemas.
  * @param localDependencies
  *   local dependencies to use instead of using the package system. This is a map of package name to a local
  *   path of a language specific artifact to use for the SDK for that package.
  */
@SerialVersionUID(0L)
final case class GeneratePackageRequest(
    directory: _root_.scala.Predef.String = "",
    schema: _root_.scala.Predef.String = "",
    extraFiles: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString] = _root_.scala.collection.immutable.Map.empty,
    loaderTarget: _root_.scala.Predef.String = "",
    localDependencies: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String] = _root_.scala.collection.immutable.Map.empty,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[GeneratePackageRequest] {
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = directory
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      
      {
        val __value = schema
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, __value)
        }
      };
      extraFiles.foreach { __item =>
        val __value = pulumirpc.language.GeneratePackageRequest._typemapper_extraFiles.toBase(__item)
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
      
      {
        val __value = loaderTarget
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, __value)
        }
      };
      localDependencies.foreach { __item =>
        val __value = pulumirpc.language.GeneratePackageRequest._typemapper_localDependencies.toBase(__item)
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
      {
        val __v = directory
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      {
        val __v = schema
        if (!__v.isEmpty) {
          _output__.writeString(2, __v)
        }
      };
      extraFiles.foreach { __v =>
        val __m = pulumirpc.language.GeneratePackageRequest._typemapper_extraFiles.toBase(__v)
        _output__.writeTag(3, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      {
        val __v = loaderTarget
        if (!__v.isEmpty) {
          _output__.writeString(4, __v)
        }
      };
      localDependencies.foreach { __v =>
        val __m = pulumirpc.language.GeneratePackageRequest._typemapper_localDependencies.toBase(__v)
        _output__.writeTag(5, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      unknownFields.writeTo(_output__)
    }
    def withDirectory(__v: _root_.scala.Predef.String): GeneratePackageRequest = copy(directory = __v)
    def withSchema(__v: _root_.scala.Predef.String): GeneratePackageRequest = copy(schema = __v)
    def clearExtraFiles = copy(extraFiles = _root_.scala.collection.immutable.Map.empty)
    def addExtraFiles(__vs: (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString) *): GeneratePackageRequest = addAllExtraFiles(__vs)
    def addAllExtraFiles(__vs: Iterable[(_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)]): GeneratePackageRequest = copy(extraFiles = extraFiles ++ __vs)
    def withExtraFiles(__v: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString]): GeneratePackageRequest = copy(extraFiles = __v)
    def withLoaderTarget(__v: _root_.scala.Predef.String): GeneratePackageRequest = copy(loaderTarget = __v)
    def clearLocalDependencies = copy(localDependencies = _root_.scala.collection.immutable.Map.empty)
    def addLocalDependencies(__vs: (_root_.scala.Predef.String, _root_.scala.Predef.String) *): GeneratePackageRequest = addAllLocalDependencies(__vs)
    def addAllLocalDependencies(__vs: Iterable[(_root_.scala.Predef.String, _root_.scala.Predef.String)]): GeneratePackageRequest = copy(localDependencies = localDependencies ++ __vs)
    def withLocalDependencies(__v: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]): GeneratePackageRequest = copy(localDependencies = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = directory
          if (__t != "") __t else null
        }
        case 2 => {
          val __t = schema
          if (__t != "") __t else null
        }
        case 3 => extraFiles.iterator.map(pulumirpc.language.GeneratePackageRequest._typemapper_extraFiles.toBase(_)).toSeq
        case 4 => {
          val __t = loaderTarget
          if (__t != "") __t else null
        }
        case 5 => localDependencies.iterator.map(pulumirpc.language.GeneratePackageRequest._typemapper_localDependencies.toBase(_)).toSeq
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(directory)
        case 2 => _root_.scalapb.descriptors.PString(schema)
        case 3 => _root_.scalapb.descriptors.PRepeated(extraFiles.iterator.map(pulumirpc.language.GeneratePackageRequest._typemapper_extraFiles.toBase(_).toPMessage).toVector)
        case 4 => _root_.scalapb.descriptors.PString(loaderTarget)
        case 5 => _root_.scalapb.descriptors.PRepeated(localDependencies.iterator.map(pulumirpc.language.GeneratePackageRequest._typemapper_localDependencies.toBase(_).toPMessage).toVector)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.language.GeneratePackageRequest.type = pulumirpc.language.GeneratePackageRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.GeneratePackageRequest])
}

object GeneratePackageRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.language.GeneratePackageRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.language.GeneratePackageRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.language.GeneratePackageRequest = {
    var __directory: _root_.scala.Predef.String = ""
    var __schema: _root_.scala.Predef.String = ""
    val __extraFiles: _root_.scala.collection.mutable.Builder[(_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString), _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString]] = _root_.scala.collection.immutable.Map.newBuilder[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString]
    var __loaderTarget: _root_.scala.Predef.String = ""
    val __localDependencies: _root_.scala.collection.mutable.Builder[(_root_.scala.Predef.String, _root_.scala.Predef.String), _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]] = _root_.scala.collection.immutable.Map.newBuilder[_root_.scala.Predef.String, _root_.scala.Predef.String]
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __directory = _input__.readStringRequireUtf8()
        case 18 =>
          __schema = _input__.readStringRequireUtf8()
        case 26 =>
          __extraFiles += pulumirpc.language.GeneratePackageRequest._typemapper_extraFiles.toCustom(_root_.scalapb.LiteParser.readMessage[pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry](_input__))
        case 34 =>
          __loaderTarget = _input__.readStringRequireUtf8()
        case 42 =>
          __localDependencies += pulumirpc.language.GeneratePackageRequest._typemapper_localDependencies.toCustom(_root_.scalapb.LiteParser.readMessage[pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry](_input__))
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.language.GeneratePackageRequest(
        directory = __directory,
        schema = __schema,
        extraFiles = __extraFiles.result(),
        loaderTarget = __loaderTarget,
        localDependencies = __localDependencies.result(),
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.language.GeneratePackageRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.language.GeneratePackageRequest(
        directory = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        schema = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        extraFiles = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Seq[pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry]]).getOrElse(_root_.scala.Seq.empty).iterator.map(pulumirpc.language.GeneratePackageRequest._typemapper_extraFiles.toCustom(_)).toMap,
        loaderTarget = __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        localDependencies = __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).map(_.as[_root_.scala.Seq[pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry]]).getOrElse(_root_.scala.Seq.empty).iterator.map(pulumirpc.language.GeneratePackageRequest._typemapper_localDependencies.toCustom(_)).toMap
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = LanguageProto.javaDescriptor.getMessageTypes().get(17)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = LanguageProto.scalaDescriptor.messages(17)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 3 => __out = pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry
      case 5 => __out = pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]](
      _root_.pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry,
      _root_.pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry
    )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.language.GeneratePackageRequest(
    directory = "",
    schema = "",
    extraFiles = _root_.scala.collection.immutable.Map.empty,
    loaderTarget = "",
    localDependencies = _root_.scala.collection.immutable.Map.empty
  )
  @SerialVersionUID(0L)
  final case class ExtraFilesEntry(
      key: _root_.scala.Predef.String = "",
      value: _root_.com.google.protobuf.ByteString = _root_.com.google.protobuf.ByteString.EMPTY,
      unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
      ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[ExtraFilesEntry] {
      @transient
      private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
      private[this] def __computeSerializedSize(): _root_.scala.Int = {
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
      def withKey(__v: _root_.scala.Predef.String): ExtraFilesEntry = copy(key = __v)
      def withValue(__v: _root_.com.google.protobuf.ByteString): ExtraFilesEntry = copy(value = __v)
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
      def companion: pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry.type = pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry
      // @@protoc_insertion_point(GeneratedMessage[pulumirpc.GeneratePackageRequest.ExtraFilesEntry])
  }
  
  object ExtraFilesEntry extends scalapb.GeneratedMessageCompanion[pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry] = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry = {
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
      pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry(
          key = __key,
          value = __value,
          unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
        pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry(
          key = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          value = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.com.google.protobuf.ByteString]).getOrElse(_root_.com.google.protobuf.ByteString.EMPTY)
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.language.GeneratePackageRequest.javaDescriptor.getNestedTypes().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.language.GeneratePackageRequest.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry(
      key = "",
      value = _root_.com.google.protobuf.ByteString.EMPTY
    )
    implicit class ExtraFilesEntryLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry](_l) {
      def key: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.key)((c_, f_) => c_.copy(key = f_))
      def value: _root_.scalapb.lenses.Lens[UpperPB, _root_.com.google.protobuf.ByteString] = field(_.value)((c_, f_) => c_.copy(value = f_))
    }
    final val KEY_FIELD_NUMBER = 1
    final val VALUE_FIELD_NUMBER = 2
    @transient
    implicit val keyValueMapper: _root_.scalapb.TypeMapper[pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry, (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)] =
      _root_.scalapb.TypeMapper[pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry, (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)](__m => (__m.key, __m.value))(__p => pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry(__p._1, __p._2))
    def of(
      key: _root_.scala.Predef.String,
      value: _root_.com.google.protobuf.ByteString
    ): _root_.pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry = _root_.pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry(
      key,
      value
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.GeneratePackageRequest.ExtraFilesEntry])
  }
  
  @SerialVersionUID(0L)
  final case class LocalDependenciesEntry(
      key: _root_.scala.Predef.String = "",
      value: _root_.scala.Predef.String = "",
      unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
      ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[LocalDependenciesEntry] {
      @transient
      private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
      private[this] def __computeSerializedSize(): _root_.scala.Int = {
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
        {
          val __v = key
          if (!__v.isEmpty) {
            _output__.writeString(1, __v)
          }
        };
        {
          val __v = value
          if (!__v.isEmpty) {
            _output__.writeString(2, __v)
          }
        };
        unknownFields.writeTo(_output__)
      }
      def withKey(__v: _root_.scala.Predef.String): LocalDependenciesEntry = copy(key = __v)
      def withValue(__v: _root_.scala.Predef.String): LocalDependenciesEntry = copy(value = __v)
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
            if (__t != "") __t else null
          }
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 => _root_.scalapb.descriptors.PString(key)
          case 2 => _root_.scalapb.descriptors.PString(value)
        }
      }
      def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
      def companion: pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry.type = pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry
      // @@protoc_insertion_point(GeneratedMessage[pulumirpc.GeneratePackageRequest.LocalDependenciesEntry])
  }
  
  object LocalDependenciesEntry extends scalapb.GeneratedMessageCompanion[pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry] = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry = {
      var __key: _root_.scala.Predef.String = ""
      var __value: _root_.scala.Predef.String = ""
      var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __key = _input__.readStringRequireUtf8()
          case 18 =>
            __value = _input__.readStringRequireUtf8()
          case tag =>
            if (_unknownFields__ == null) {
              _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
            }
            _unknownFields__.parseField(tag, _input__)
        }
      }
      pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry(
          key = __key,
          value = __value,
          unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
        pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry(
          key = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          value = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.language.GeneratePackageRequest.javaDescriptor.getNestedTypes().get(1)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.language.GeneratePackageRequest.scalaDescriptor.nestedMessages(1)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry(
      key = "",
      value = ""
    )
    implicit class LocalDependenciesEntryLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry](_l) {
      def key: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.key)((c_, f_) => c_.copy(key = f_))
      def value: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.value)((c_, f_) => c_.copy(value = f_))
    }
    final val KEY_FIELD_NUMBER = 1
    final val VALUE_FIELD_NUMBER = 2
    @transient
    implicit val keyValueMapper: _root_.scalapb.TypeMapper[pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)] =
      _root_.scalapb.TypeMapper[pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)](__m => (__m.key, __m.value))(__p => pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry(__p._1, __p._2))
    def of(
      key: _root_.scala.Predef.String,
      value: _root_.scala.Predef.String
    ): _root_.pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry = _root_.pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry(
      key,
      value
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.GeneratePackageRequest.LocalDependenciesEntry])
  }
  
  implicit class GeneratePackageRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.language.GeneratePackageRequest]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, pulumirpc.language.GeneratePackageRequest](_l) {
    def directory: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.directory)((c_, f_) => c_.copy(directory = f_))
    def schema: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.schema)((c_, f_) => c_.copy(schema = f_))
    def extraFiles: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString]] = field(_.extraFiles)((c_, f_) => c_.copy(extraFiles = f_))
    def loaderTarget: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.loaderTarget)((c_, f_) => c_.copy(loaderTarget = f_))
    def localDependencies: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]] = field(_.localDependencies)((c_, f_) => c_.copy(localDependencies = f_))
  }
  final val DIRECTORY_FIELD_NUMBER = 1
  final val SCHEMA_FIELD_NUMBER = 2
  final val EXTRA_FILES_FIELD_NUMBER = 3
  final val LOADER_TARGET_FIELD_NUMBER = 4
  final val LOCAL_DEPENDENCIES_FIELD_NUMBER = 5
  @transient
  private[language] val _typemapper_extraFiles: _root_.scalapb.TypeMapper[pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry, (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)] = implicitly[_root_.scalapb.TypeMapper[pulumirpc.language.GeneratePackageRequest.ExtraFilesEntry, (_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString)]]
  @transient
  private[language] val _typemapper_localDependencies: _root_.scalapb.TypeMapper[pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)] = implicitly[_root_.scalapb.TypeMapper[pulumirpc.language.GeneratePackageRequest.LocalDependenciesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)]]
  def of(
    directory: _root_.scala.Predef.String,
    schema: _root_.scala.Predef.String,
    extraFiles: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.com.google.protobuf.ByteString],
    loaderTarget: _root_.scala.Predef.String,
    localDependencies: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]
  ): _root_.pulumirpc.language.GeneratePackageRequest = _root_.pulumirpc.language.GeneratePackageRequest(
    directory,
    schema,
    extraFiles,
    loaderTarget,
    localDependencies
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.GeneratePackageRequest])
}