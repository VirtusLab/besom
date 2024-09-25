// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.provider

/** @param replaces
  *   if this update requires a replacement, the set of properties triggering it.
  * @param stables
  *   an optional list of properties that will not ever change.
  * @param deleteBeforeReplace
  *   if true, this resource must be deleted before replacing it.
  * @param changes
  *   if true, this diff represents an actual difference and thus requires an update.
  * @param diffs
  *   a list of the properties that changed.
  * @param detailedDiff
  *   detailedDiff is an optional field that contains map from each changed property to the type of the change.
  *  
  *   The keys of this map are property paths. These paths are essentially Javascript property access expressions
  *   in which all elements are literals, and obey the following EBNF-ish grammar:
  *  
  *     propertyName := [a-zA-Z_$] { [a-zA-Z0-9_$] }
  *     quotedPropertyName := '"' ( '&92;' '"' | [^"] ) { ( '&92;' '"' | [^"] ) } '"'
  *     arrayIndex := { [0-9] }
  *  
  *     propertyIndex := '[' ( quotedPropertyName | arrayIndex ) ']'
  *     rootProperty := ( propertyName | propertyIndex )
  *     propertyAccessor := ( ( '.' propertyName ) |  propertyIndex )
  *     path := rootProperty { propertyAccessor }
  *  
  *   Examples of valid keys:
  *   - root
  *   - root.nested
  *   - root["nested"]
  *   - root.double.nest
  *   - root["double"].nest
  *   - root["double"]["nest"]
  *   - root.array[0]
  *   - root.array[100]
  *   - root.array[0].nested
  *   - root.array[0][1].nested
  *   - root.nested.array[0].double[1]
  *   - root["key with &92;"escaped&92;" quotes"]
  *   - root["key with a ."]
  *   - ["root key with &92;"escaped&92;" quotes"].nested
  *   - ["root key with a ."][100]
  *   a detailed diff appropriate for display.
  * @param hasDetailedDiff
  *   true if this response contains a detailed diff.
  */
@SerialVersionUID(0L)
final case class DiffResponse(
    replaces: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
    stables: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
    deleteBeforeReplace: _root_.scala.Boolean = false,
    changes: pulumirpc.provider.DiffResponse.DiffChanges = pulumirpc.provider.DiffResponse.DiffChanges.DIFF_UNKNOWN,
    diffs: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
    detailedDiff: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff] = _root_.scala.collection.immutable.Map.empty,
    hasDetailedDiff: _root_.scala.Boolean = false,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[DiffResponse] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      replaces.foreach { __item =>
        val __value = __item
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
      }
      stables.foreach { __item =>
        val __value = __item
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, __value)
      }
      
      {
        val __value = deleteBeforeReplace
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(3, __value)
        }
      };
      
      {
        val __value = changes.value
        if (__value != 0) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeEnumSize(4, __value)
        }
      };
      diffs.foreach { __item =>
        val __value = __item
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, __value)
      }
      detailedDiff.foreach { __item =>
        val __value = pulumirpc.provider.DiffResponse._typemapper_detailedDiff.toBase(__item)
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
      
      {
        val __value = hasDetailedDiff
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(7, __value)
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
      replaces.foreach { __v =>
        val __m = __v
        _output__.writeString(1, __m)
      };
      stables.foreach { __v =>
        val __m = __v
        _output__.writeString(2, __m)
      };
      {
        val __v = deleteBeforeReplace
        if (__v != false) {
          _output__.writeBool(3, __v)
        }
      };
      {
        val __v = changes.value
        if (__v != 0) {
          _output__.writeEnum(4, __v)
        }
      };
      diffs.foreach { __v =>
        val __m = __v
        _output__.writeString(5, __m)
      };
      detailedDiff.foreach { __v =>
        val __m = pulumirpc.provider.DiffResponse._typemapper_detailedDiff.toBase(__v)
        _output__.writeTag(6, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      {
        val __v = hasDetailedDiff
        if (__v != false) {
          _output__.writeBool(7, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def clearReplaces = copy(replaces = _root_.scala.Seq.empty)
    def addReplaces(__vs: _root_.scala.Predef.String *): DiffResponse = addAllReplaces(__vs)
    def addAllReplaces(__vs: Iterable[_root_.scala.Predef.String]): DiffResponse = copy(replaces = replaces ++ __vs)
    def withReplaces(__v: _root_.scala.Seq[_root_.scala.Predef.String]): DiffResponse = copy(replaces = __v)
    def clearStables = copy(stables = _root_.scala.Seq.empty)
    def addStables(__vs: _root_.scala.Predef.String *): DiffResponse = addAllStables(__vs)
    def addAllStables(__vs: Iterable[_root_.scala.Predef.String]): DiffResponse = copy(stables = stables ++ __vs)
    def withStables(__v: _root_.scala.Seq[_root_.scala.Predef.String]): DiffResponse = copy(stables = __v)
    def withDeleteBeforeReplace(__v: _root_.scala.Boolean): DiffResponse = copy(deleteBeforeReplace = __v)
    def withChanges(__v: pulumirpc.provider.DiffResponse.DiffChanges): DiffResponse = copy(changes = __v)
    def clearDiffs = copy(diffs = _root_.scala.Seq.empty)
    def addDiffs(__vs: _root_.scala.Predef.String *): DiffResponse = addAllDiffs(__vs)
    def addAllDiffs(__vs: Iterable[_root_.scala.Predef.String]): DiffResponse = copy(diffs = diffs ++ __vs)
    def withDiffs(__v: _root_.scala.Seq[_root_.scala.Predef.String]): DiffResponse = copy(diffs = __v)
    def clearDetailedDiff = copy(detailedDiff = _root_.scala.collection.immutable.Map.empty)
    def addDetailedDiff(__vs: (_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff) *): DiffResponse = addAllDetailedDiff(__vs)
    def addAllDetailedDiff(__vs: Iterable[(_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff)]): DiffResponse = copy(detailedDiff = detailedDiff ++ __vs)
    def withDetailedDiff(__v: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff]): DiffResponse = copy(detailedDiff = __v)
    def withHasDetailedDiff(__v: _root_.scala.Boolean): DiffResponse = copy(hasDetailedDiff = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => replaces
        case 2 => stables
        case 3 => {
          val __t = deleteBeforeReplace
          if (__t != false) __t else null
        }
        case 4 => {
          val __t = changes.javaValueDescriptor
          if (__t.getNumber() != 0) __t else null
        }
        case 5 => diffs
        case 6 => detailedDiff.iterator.map(pulumirpc.provider.DiffResponse._typemapper_detailedDiff.toBase(_)).toSeq
        case 7 => {
          val __t = hasDetailedDiff
          if (__t != false) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PRepeated(replaces.iterator.map(_root_.scalapb.descriptors.PString(_)).toVector)
        case 2 => _root_.scalapb.descriptors.PRepeated(stables.iterator.map(_root_.scalapb.descriptors.PString(_)).toVector)
        case 3 => _root_.scalapb.descriptors.PBoolean(deleteBeforeReplace)
        case 4 => _root_.scalapb.descriptors.PEnum(changes.scalaValueDescriptor)
        case 5 => _root_.scalapb.descriptors.PRepeated(diffs.iterator.map(_root_.scalapb.descriptors.PString(_)).toVector)
        case 6 => _root_.scalapb.descriptors.PRepeated(detailedDiff.iterator.map(pulumirpc.provider.DiffResponse._typemapper_detailedDiff.toBase(_).toPMessage).toVector)
        case 7 => _root_.scalapb.descriptors.PBoolean(hasDetailedDiff)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.provider.DiffResponse.type = pulumirpc.provider.DiffResponse
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.DiffResponse])
}

object DiffResponse extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.DiffResponse] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.DiffResponse] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.DiffResponse = {
    val __replaces: _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String] = new _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String]
    val __stables: _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String] = new _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String]
    var __deleteBeforeReplace: _root_.scala.Boolean = false
    var __changes: pulumirpc.provider.DiffResponse.DiffChanges = pulumirpc.provider.DiffResponse.DiffChanges.DIFF_UNKNOWN
    val __diffs: _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String] = new _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String]
    val __detailedDiff: _root_.scala.collection.mutable.Builder[(_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff), _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff]] = _root_.scala.collection.immutable.Map.newBuilder[_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff]
    var __hasDetailedDiff: _root_.scala.Boolean = false
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __replaces += _input__.readStringRequireUtf8()
        case 18 =>
          __stables += _input__.readStringRequireUtf8()
        case 24 =>
          __deleteBeforeReplace = _input__.readBool()
        case 32 =>
          __changes = pulumirpc.provider.DiffResponse.DiffChanges.fromValue(_input__.readEnum())
        case 42 =>
          __diffs += _input__.readStringRequireUtf8()
        case 50 =>
          __detailedDiff += pulumirpc.provider.DiffResponse._typemapper_detailedDiff.toCustom(_root_.scalapb.LiteParser.readMessage[pulumirpc.provider.DiffResponse.DetailedDiffEntry](_input__))
        case 56 =>
          __hasDetailedDiff = _input__.readBool()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.provider.DiffResponse(
        replaces = __replaces.result(),
        stables = __stables.result(),
        deleteBeforeReplace = __deleteBeforeReplace,
        changes = __changes,
        diffs = __diffs.result(),
        detailedDiff = __detailedDiff.result(),
        hasDetailedDiff = __hasDetailedDiff,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.DiffResponse] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.provider.DiffResponse(
        replaces = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Seq[_root_.scala.Predef.String]]).getOrElse(_root_.scala.Seq.empty),
        stables = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Seq[_root_.scala.Predef.String]]).getOrElse(_root_.scala.Seq.empty),
        deleteBeforeReplace = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Boolean]).getOrElse(false),
        changes = pulumirpc.provider.DiffResponse.DiffChanges.fromValue(__fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).map(_.as[_root_.scalapb.descriptors.EnumValueDescriptor]).getOrElse(pulumirpc.provider.DiffResponse.DiffChanges.DIFF_UNKNOWN.scalaValueDescriptor).number),
        diffs = __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).map(_.as[_root_.scala.Seq[_root_.scala.Predef.String]]).getOrElse(_root_.scala.Seq.empty),
        detailedDiff = __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).map(_.as[_root_.scala.Seq[pulumirpc.provider.DiffResponse.DetailedDiffEntry]]).getOrElse(_root_.scala.Seq.empty).iterator.map(pulumirpc.provider.DiffResponse._typemapper_detailedDiff.toCustom(_)).toMap,
        hasDetailedDiff = __fieldsMap.get(scalaDescriptor.findFieldByNumber(7).get).map(_.as[_root_.scala.Boolean]).getOrElse(false)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ProviderProto.javaDescriptor.getMessageTypes().get(14)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ProviderProto.scalaDescriptor.messages(14)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
    (__number: @_root_.scala.unchecked) match {
      case 6 => __out = pulumirpc.provider.DiffResponse.DetailedDiffEntry
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]](
      _root_.pulumirpc.provider.DiffResponse.DetailedDiffEntry
    )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = {
    (__fieldNumber: @_root_.scala.unchecked) match {
      case 4 => pulumirpc.provider.DiffResponse.DiffChanges
    }
  }
  lazy val defaultInstance = pulumirpc.provider.DiffResponse(
    replaces = _root_.scala.Seq.empty,
    stables = _root_.scala.Seq.empty,
    deleteBeforeReplace = false,
    changes = pulumirpc.provider.DiffResponse.DiffChanges.DIFF_UNKNOWN,
    diffs = _root_.scala.Seq.empty,
    detailedDiff = _root_.scala.collection.immutable.Map.empty,
    hasDetailedDiff = false
  )
  sealed abstract class DiffChanges(val value: _root_.scala.Int) extends _root_.scalapb.GeneratedEnum {
    type EnumType = pulumirpc.provider.DiffResponse.DiffChanges
    type RecognizedType = pulumirpc.provider.DiffResponse.DiffChanges.Recognized
    def isDiffUnknown: _root_.scala.Boolean = false
    def isDiffNone: _root_.scala.Boolean = false
    def isDiffSome: _root_.scala.Boolean = false
    def companion: _root_.scalapb.GeneratedEnumCompanion[DiffChanges] = pulumirpc.provider.DiffResponse.DiffChanges
    final def asRecognized: _root_.scala.Option[pulumirpc.provider.DiffResponse.DiffChanges.Recognized] = if (isUnrecognized) _root_.scala.None else _root_.scala.Some(this.asInstanceOf[pulumirpc.provider.DiffResponse.DiffChanges.Recognized])
  }
  
  object DiffChanges extends _root_.scalapb.GeneratedEnumCompanion[DiffChanges] {
    sealed trait Recognized extends DiffChanges
    implicit def enumCompanion: _root_.scalapb.GeneratedEnumCompanion[DiffChanges] = this
    
    /** unknown whether there are changes or not (legacy behavior).
      */
    @SerialVersionUID(0L)
    case object DIFF_UNKNOWN extends DiffChanges(0) with DiffChanges.Recognized {
      val index = 0
      val name = "DIFF_UNKNOWN"
      override def isDiffUnknown: _root_.scala.Boolean = true
    }
    
    /** the diff was performed, and no changes were detected that require an update.
      */
    @SerialVersionUID(0L)
    case object DIFF_NONE extends DiffChanges(1) with DiffChanges.Recognized {
      val index = 1
      val name = "DIFF_NONE"
      override def isDiffNone: _root_.scala.Boolean = true
    }
    
    /** the diff was performed, and changes were detected that require an update or replacement.
      */
    @SerialVersionUID(0L)
    case object DIFF_SOME extends DiffChanges(2) with DiffChanges.Recognized {
      val index = 2
      val name = "DIFF_SOME"
      override def isDiffSome: _root_.scala.Boolean = true
    }
    
    @SerialVersionUID(0L)
    final case class Unrecognized(unrecognizedValue: _root_.scala.Int) extends DiffChanges(unrecognizedValue) with _root_.scalapb.UnrecognizedEnum
    lazy val values: scala.collection.immutable.Seq[ValueType] = scala.collection.immutable.Seq(DIFF_UNKNOWN, DIFF_NONE, DIFF_SOME)
    def fromValue(__value: _root_.scala.Int): DiffChanges = __value match {
      case 0 => DIFF_UNKNOWN
      case 1 => DIFF_NONE
      case 2 => DIFF_SOME
      case __other => Unrecognized(__other)
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.EnumDescriptor = pulumirpc.provider.DiffResponse.javaDescriptor.getEnumTypes().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.EnumDescriptor = pulumirpc.provider.DiffResponse.scalaDescriptor.enums(0)
  }
  @SerialVersionUID(0L)
  final case class DetailedDiffEntry(
      key: _root_.scala.Predef.String = "",
      value: _root_.scala.Option[pulumirpc.provider.PropertyDiff] = _root_.scala.None,
      unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
      ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[DetailedDiffEntry] {
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
        if (value.isDefined) {
          val __value = value.get
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
          val __v = key
          if (!__v.isEmpty) {
            _output__.writeString(1, __v)
          }
        };
        value.foreach { __v =>
          val __m = __v
          _output__.writeTag(2, 2)
          _output__.writeUInt32NoTag(__m.serializedSize)
          __m.writeTo(_output__)
        };
        unknownFields.writeTo(_output__)
      }
      def withKey(__v: _root_.scala.Predef.String): DetailedDiffEntry = copy(key = __v)
      def getValue: pulumirpc.provider.PropertyDiff = value.getOrElse(pulumirpc.provider.PropertyDiff.defaultInstance)
      def clearValue: DetailedDiffEntry = copy(value = _root_.scala.None)
      def withValue(__v: pulumirpc.provider.PropertyDiff): DetailedDiffEntry = copy(value = Option(__v))
      def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
      def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
      def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 => {
            val __t = key
            if (__t != "") __t else null
          }
          case 2 => value.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 => _root_.scalapb.descriptors.PString(key)
          case 2 => value.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
      def companion: pulumirpc.provider.DiffResponse.DetailedDiffEntry.type = pulumirpc.provider.DiffResponse.DetailedDiffEntry
      // @@protoc_insertion_point(GeneratedMessage[pulumirpc.DiffResponse.DetailedDiffEntry])
  }
  
  object DetailedDiffEntry extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.DiffResponse.DetailedDiffEntry] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.DiffResponse.DetailedDiffEntry] = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.DiffResponse.DetailedDiffEntry = {
      var __key: _root_.scala.Predef.String = ""
      var __value: _root_.scala.Option[pulumirpc.provider.PropertyDiff] = _root_.scala.None
      var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __key = _input__.readStringRequireUtf8()
          case 18 =>
            __value = _root_.scala.Option(__value.fold(_root_.scalapb.LiteParser.readMessage[pulumirpc.provider.PropertyDiff](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
          case tag =>
            if (_unknownFields__ == null) {
              _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
            }
            _unknownFields__.parseField(tag, _input__)
        }
      }
      pulumirpc.provider.DiffResponse.DetailedDiffEntry(
          key = __key,
          value = __value,
          unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.DiffResponse.DetailedDiffEntry] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
        pulumirpc.provider.DiffResponse.DetailedDiffEntry(
          key = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          value = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[_root_.scala.Option[pulumirpc.provider.PropertyDiff]])
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.provider.DiffResponse.javaDescriptor.getNestedTypes().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.provider.DiffResponse.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
      var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
      (__number: @_root_.scala.unchecked) match {
        case 2 => __out = pulumirpc.provider.PropertyDiff
      }
      __out
    }
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = pulumirpc.provider.DiffResponse.DetailedDiffEntry(
      key = "",
      value = _root_.scala.None
    )
    implicit class DetailedDiffEntryLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.DiffResponse.DetailedDiffEntry]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.provider.DiffResponse.DetailedDiffEntry](_l) {
      def key: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.key)((c_, f_) => c_.copy(key = f_))
      def value: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.PropertyDiff] = field(_.getValue)((c_, f_) => c_.copy(value = _root_.scala.Option(f_)))
      def optionalValue: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[pulumirpc.provider.PropertyDiff]] = field(_.value)((c_, f_) => c_.copy(value = f_))
    }
    final val KEY_FIELD_NUMBER = 1
    final val VALUE_FIELD_NUMBER = 2
    @transient
    implicit val keyValueMapper: _root_.scalapb.TypeMapper[pulumirpc.provider.DiffResponse.DetailedDiffEntry, (_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff)] =
      _root_.scalapb.TypeMapper[pulumirpc.provider.DiffResponse.DetailedDiffEntry, (_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff)](__m => (__m.key, __m.getValue))(__p => pulumirpc.provider.DiffResponse.DetailedDiffEntry(__p._1, Some(__p._2)))
    def of(
      key: _root_.scala.Predef.String,
      value: _root_.scala.Option[pulumirpc.provider.PropertyDiff]
    ): _root_.pulumirpc.provider.DiffResponse.DetailedDiffEntry = _root_.pulumirpc.provider.DiffResponse.DetailedDiffEntry(
      key,
      value
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.DiffResponse.DetailedDiffEntry])
  }
  
  implicit class DiffResponseLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.DiffResponse]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.provider.DiffResponse](_l) {
    def replaces: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Seq[_root_.scala.Predef.String]] = field(_.replaces)((c_, f_) => c_.copy(replaces = f_))
    def stables: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Seq[_root_.scala.Predef.String]] = field(_.stables)((c_, f_) => c_.copy(stables = f_))
    def deleteBeforeReplace: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.deleteBeforeReplace)((c_, f_) => c_.copy(deleteBeforeReplace = f_))
    def changes: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.DiffResponse.DiffChanges] = field(_.changes)((c_, f_) => c_.copy(changes = f_))
    def diffs: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Seq[_root_.scala.Predef.String]] = field(_.diffs)((c_, f_) => c_.copy(diffs = f_))
    def detailedDiff: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff]] = field(_.detailedDiff)((c_, f_) => c_.copy(detailedDiff = f_))
    def hasDetailedDiff: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.hasDetailedDiff)((c_, f_) => c_.copy(hasDetailedDiff = f_))
  }
  final val REPLACES_FIELD_NUMBER = 1
  final val STABLES_FIELD_NUMBER = 2
  final val DELETEBEFOREREPLACE_FIELD_NUMBER = 3
  final val CHANGES_FIELD_NUMBER = 4
  final val DIFFS_FIELD_NUMBER = 5
  final val DETAILEDDIFF_FIELD_NUMBER = 6
  final val HASDETAILEDDIFF_FIELD_NUMBER = 7
  @transient
  private[provider] val _typemapper_detailedDiff: _root_.scalapb.TypeMapper[pulumirpc.provider.DiffResponse.DetailedDiffEntry, (_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff)] = implicitly[_root_.scalapb.TypeMapper[pulumirpc.provider.DiffResponse.DetailedDiffEntry, (_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff)]]
  def of(
    replaces: _root_.scala.Seq[_root_.scala.Predef.String],
    stables: _root_.scala.Seq[_root_.scala.Predef.String],
    deleteBeforeReplace: _root_.scala.Boolean,
    changes: pulumirpc.provider.DiffResponse.DiffChanges,
    diffs: _root_.scala.Seq[_root_.scala.Predef.String],
    detailedDiff: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.PropertyDiff],
    hasDetailedDiff: _root_.scala.Boolean
  ): _root_.pulumirpc.provider.DiffResponse = _root_.pulumirpc.provider.DiffResponse(
    replaces,
    stables,
    deleteBeforeReplace,
    changes,
    diffs,
    detailedDiff,
    hasDetailedDiff
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.DiffResponse])
}
