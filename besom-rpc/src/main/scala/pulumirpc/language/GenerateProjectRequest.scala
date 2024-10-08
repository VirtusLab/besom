// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.language

/** @param sourceDirectory
  *   the directory to generate the project from.
  * @param targetDirectory
  *   the directory to generate the project in.
  * @param project
  *   the JSON-encoded pulumi project file.
  * @param strict
  *   if PCL binding should be strict or not.
  * @param loaderTarget
  *   The target of a codegen.LoaderServer to use for loading schemas.
  * @param localDependencies
  *   local dependencies to use instead of using the package system. This is a map of package name to a local
  *   path of a language specific artifact to use for the SDK for that package.
  */
@SerialVersionUID(0L)
final case class GenerateProjectRequest(
    sourceDirectory: _root_.scala.Predef.String = "",
    targetDirectory: _root_.scala.Predef.String = "",
    project: _root_.scala.Predef.String = "",
    strict: _root_.scala.Boolean = false,
    loaderTarget: _root_.scala.Predef.String = "",
    localDependencies: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String] = _root_.scala.collection.immutable.Map.empty,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[GenerateProjectRequest] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = sourceDirectory
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      
      {
        val __value = targetDirectory
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, __value)
        }
      };
      
      {
        val __value = project
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, __value)
        }
      };
      
      {
        val __value = strict
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(4, __value)
        }
      };
      
      {
        val __value = loaderTarget
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, __value)
        }
      };
      localDependencies.foreach { __item =>
        val __value = pulumirpc.language.GenerateProjectRequest._typemapper_localDependencies.toBase(__item)
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
        val __v = sourceDirectory
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      {
        val __v = targetDirectory
        if (!__v.isEmpty) {
          _output__.writeString(2, __v)
        }
      };
      {
        val __v = project
        if (!__v.isEmpty) {
          _output__.writeString(3, __v)
        }
      };
      {
        val __v = strict
        if (__v != false) {
          _output__.writeBool(4, __v)
        }
      };
      {
        val __v = loaderTarget
        if (!__v.isEmpty) {
          _output__.writeString(5, __v)
        }
      };
      localDependencies.foreach { __v =>
        val __m = pulumirpc.language.GenerateProjectRequest._typemapper_localDependencies.toBase(__v)
        _output__.writeTag(6, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      unknownFields.writeTo(_output__)
    }
    def withSourceDirectory(__v: _root_.scala.Predef.String): GenerateProjectRequest = copy(sourceDirectory = __v)
    def withTargetDirectory(__v: _root_.scala.Predef.String): GenerateProjectRequest = copy(targetDirectory = __v)
    def withProject(__v: _root_.scala.Predef.String): GenerateProjectRequest = copy(project = __v)
    def withStrict(__v: _root_.scala.Boolean): GenerateProjectRequest = copy(strict = __v)
    def withLoaderTarget(__v: _root_.scala.Predef.String): GenerateProjectRequest = copy(loaderTarget = __v)
    def clearLocalDependencies = copy(localDependencies = _root_.scala.collection.immutable.Map.empty)
    def addLocalDependencies(__vs: (_root_.scala.Predef.String, _root_.scala.Predef.String) *): GenerateProjectRequest = addAllLocalDependencies(__vs)
    def addAllLocalDependencies(__vs: Iterable[(_root_.scala.Predef.String, _root_.scala.Predef.String)]): GenerateProjectRequest = copy(localDependencies = localDependencies ++ __vs)
    def withLocalDependencies(__v: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]): GenerateProjectRequest = copy(localDependencies = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = sourceDirectory
          if (__t != "") __t else null
        }
        case 2 => {
          val __t = targetDirectory
          if (__t != "") __t else null
        }
        case 3 => {
          val __t = project
          if (__t != "") __t else null
        }
        case 4 => {
          val __t = strict
          if (__t != false) __t else null
        }
        case 5 => {
          val __t = loaderTarget
          if (__t != "") __t else null
        }
        case 6 => localDependencies.iterator.map(pulumirpc.language.GenerateProjectRequest._typemapper_localDependencies.toBase(_)).toSeq
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(sourceDirectory)
        case 2 => _root_.scalapb.descriptors.PString(targetDirectory)
        case 3 => _root_.scalapb.descriptors.PString(project)
        case 4 => _root_.scalapb.descriptors.PBoolean(strict)
        case 5 => _root_.scalapb.descriptors.PString(loaderTarget)
        case 6 => _root_.scalapb.descriptors.PRepeated(localDependencies.iterator.map(pulumirpc.language.GenerateProjectRequest._typemapper_localDependencies.toBase(_).toPMessage).toVector)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.language.GenerateProjectRequest.type = pulumirpc.language.GenerateProjectRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.GenerateProjectRequest])
}

object GenerateProjectRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.language.GenerateProjectRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.language.GenerateProjectRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.language.GenerateProjectRequest = {
    var __sourceDirectory: _root_.scala.Predef.String = ""
    var __targetDirectory: _root_.scala.Predef.String = ""
    var __project: _root_.scala.Predef.String = ""
    var __strict: _root_.scala.Boolean = false
    var __loaderTarget: _root_.scala.Predef.String = ""
    val __localDependencies: _root_.scala.collection.mutable.Builder[(_root_.scala.Predef.String, _root_.scala.Predef.String), _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]] = _root_.scala.collection.immutable.Map.newBuilder[_root_.scala.Predef.String, _root_.scala.Predef.String]
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __sourceDirectory = _input__.readStringRequireUtf8()
        case 18 =>
          __targetDirectory = _input__.readStringRequireUtf8()
        case 26 =>
          __project = _input__.readStringRequireUtf8()
        case 32 =>
          __strict = _input__.readBool()
        case 42 =>
          __loaderTarget = _input__.readStringRequireUtf8()
        case 50 =>
          __localDependencies += pulumirpc.language.GenerateProjectRequest._typemapper_localDependencies.toCustom(_root_.scalapb.LiteParser.readMessage[pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry](_input__))
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.language.GenerateProjectRequest(
        sourceDirectory = __sourceDirectory,
        targetDirectory = __targetDirectory,
        project = __project,
        strict = __strict,
        loaderTarget = __loaderTarget,
        localDependencies = __localDependencies.result(),
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.language.GenerateProjectRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.language.GenerateProjectRequest(
        sourceDirectory = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        targetDirectory = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        project = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        strict = __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).map(_.as[_root_.scala.Boolean]).getOrElse(false),
        loaderTarget = __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        localDependencies = __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).map(_.as[_root_.scala.Seq[pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry]]).getOrElse(_root_.scala.Seq.empty).iterator.map(pulumirpc.language.GenerateProjectRequest._typemapper_localDependencies.toCustom(_)).toMap
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.language.LanguageProto.javaDescriptor.getMessageTypes().get(19)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.language.LanguageProto.scalaDescriptor.messages(19)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
    (__number: @_root_.scala.unchecked) match {
      case 6 => __out = pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]](
      _root_.pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry
    )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.language.GenerateProjectRequest(
    sourceDirectory = "",
    targetDirectory = "",
    project = "",
    strict = false,
    loaderTarget = "",
    localDependencies = _root_.scala.collection.immutable.Map.empty
  )
  @SerialVersionUID(0L)
  final case class LocalDependenciesEntry(
      key: _root_.scala.Predef.String = "",
      value: _root_.scala.Predef.String = "",
      unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
      ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[LocalDependenciesEntry] {
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
      def companion: pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry.type = pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry
      // @@protoc_insertion_point(GeneratedMessage[pulumirpc.GenerateProjectRequest.LocalDependenciesEntry])
  }
  
  object LocalDependenciesEntry extends scalapb.GeneratedMessageCompanion[pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry] = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry = {
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
      pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry(
          key = __key,
          value = __value,
          unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
        pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry(
          key = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          value = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.language.GenerateProjectRequest.javaDescriptor.getNestedTypes().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.language.GenerateProjectRequest.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry(
      key = "",
      value = ""
    )
    implicit class LocalDependenciesEntryLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry](_l) {
      def key: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.key)((c_, f_) => c_.copy(key = f_))
      def value: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.value)((c_, f_) => c_.copy(value = f_))
    }
    final val KEY_FIELD_NUMBER = 1
    final val VALUE_FIELD_NUMBER = 2
    @transient
    implicit val keyValueMapper: _root_.scalapb.TypeMapper[pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)] =
      _root_.scalapb.TypeMapper[pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)](__m => (__m.key, __m.value))(__p => pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry(__p._1, __p._2))
    def of(
      key: _root_.scala.Predef.String,
      value: _root_.scala.Predef.String
    ): _root_.pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry = _root_.pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry(
      key,
      value
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.GenerateProjectRequest.LocalDependenciesEntry])
  }
  
  implicit class GenerateProjectRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.language.GenerateProjectRequest]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.language.GenerateProjectRequest](_l) {
    def sourceDirectory: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.sourceDirectory)((c_, f_) => c_.copy(sourceDirectory = f_))
    def targetDirectory: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.targetDirectory)((c_, f_) => c_.copy(targetDirectory = f_))
    def project: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.project)((c_, f_) => c_.copy(project = f_))
    def strict: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.strict)((c_, f_) => c_.copy(strict = f_))
    def loaderTarget: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.loaderTarget)((c_, f_) => c_.copy(loaderTarget = f_))
    def localDependencies: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]] = field(_.localDependencies)((c_, f_) => c_.copy(localDependencies = f_))
  }
  final val SOURCE_DIRECTORY_FIELD_NUMBER = 1
  final val TARGET_DIRECTORY_FIELD_NUMBER = 2
  final val PROJECT_FIELD_NUMBER = 3
  final val STRICT_FIELD_NUMBER = 4
  final val LOADER_TARGET_FIELD_NUMBER = 5
  final val LOCAL_DEPENDENCIES_FIELD_NUMBER = 6
  @transient
  private[language] val _typemapper_localDependencies: _root_.scalapb.TypeMapper[pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)] = implicitly[_root_.scalapb.TypeMapper[pulumirpc.language.GenerateProjectRequest.LocalDependenciesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)]]
  def of(
    sourceDirectory: _root_.scala.Predef.String,
    targetDirectory: _root_.scala.Predef.String,
    project: _root_.scala.Predef.String,
    strict: _root_.scala.Boolean,
    loaderTarget: _root_.scala.Predef.String,
    localDependencies: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]
  ): _root_.pulumirpc.language.GenerateProjectRequest = _root_.pulumirpc.language.GenerateProjectRequest(
    sourceDirectory,
    targetDirectory,
    project,
    strict,
    loaderTarget,
    localDependencies
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.GenerateProjectRequest])
}
