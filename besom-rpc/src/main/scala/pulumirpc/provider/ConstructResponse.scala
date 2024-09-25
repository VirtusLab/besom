// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.provider

/** @param urn
  *   the URN of the component resource.
  * @param state
  *   any properties that were computed during construction.
  * @param stateDependencies
  *   a map from property keys to the dependencies of the property.
  */
@SerialVersionUID(0L)
final case class ConstructResponse(
    urn: _root_.scala.Predef.String = "",
    state: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
    stateDependencies: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies] = _root_.scala.collection.immutable.Map.empty,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[ConstructResponse] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      
      {
        val __value = urn
        if (!__value.isEmpty) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
        }
      };
      if (state.isDefined) {
        val __value = state.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      stateDependencies.foreach { __item =>
        val __value = pulumirpc.provider.ConstructResponse._typemapper_stateDependencies.toBase(__item)
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
        val __v = urn
        if (!__v.isEmpty) {
          _output__.writeString(1, __v)
        }
      };
      state.foreach { __v =>
        val __m = __v
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      stateDependencies.foreach { __v =>
        val __m = pulumirpc.provider.ConstructResponse._typemapper_stateDependencies.toBase(__v)
        _output__.writeTag(3, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      unknownFields.writeTo(_output__)
    }
    def withUrn(__v: _root_.scala.Predef.String): ConstructResponse = copy(urn = __v)
    def getState: com.google.protobuf.struct.Struct = state.getOrElse(com.google.protobuf.struct.Struct.defaultInstance)
    def clearState: ConstructResponse = copy(state = _root_.scala.None)
    def withState(__v: com.google.protobuf.struct.Struct): ConstructResponse = copy(state = Option(__v))
    def clearStateDependencies = copy(stateDependencies = _root_.scala.collection.immutable.Map.empty)
    def addStateDependencies(__vs: (_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies) *): ConstructResponse = addAllStateDependencies(__vs)
    def addAllStateDependencies(__vs: Iterable[(_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies)]): ConstructResponse = copy(stateDependencies = stateDependencies ++ __vs)
    def withStateDependencies(__v: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies]): ConstructResponse = copy(stateDependencies = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => {
          val __t = urn
          if (__t != "") __t else null
        }
        case 2 => state.orNull
        case 3 => stateDependencies.iterator.map(pulumirpc.provider.ConstructResponse._typemapper_stateDependencies.toBase(_)).toSeq
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PString(urn)
        case 2 => state.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 3 => _root_.scalapb.descriptors.PRepeated(stateDependencies.iterator.map(pulumirpc.provider.ConstructResponse._typemapper_stateDependencies.toBase(_).toPMessage).toVector)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.provider.ConstructResponse.type = pulumirpc.provider.ConstructResponse
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.ConstructResponse])
}

object ConstructResponse extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConstructResponse] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConstructResponse] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.ConstructResponse = {
    var __urn: _root_.scala.Predef.String = ""
    var __state: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None
    val __stateDependencies: _root_.scala.collection.mutable.Builder[(_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies), _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies]] = _root_.scala.collection.immutable.Map.newBuilder[_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies]
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __urn = _input__.readStringRequireUtf8()
        case 18 =>
          __state = _root_.scala.Option(__state.fold(_root_.scalapb.LiteParser.readMessage[com.google.protobuf.struct.Struct](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 26 =>
          __stateDependencies += pulumirpc.provider.ConstructResponse._typemapper_stateDependencies.toCustom(_root_.scalapb.LiteParser.readMessage[pulumirpc.provider.ConstructResponse.StateDependenciesEntry](_input__))
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.provider.ConstructResponse(
        urn = __urn,
        state = __state,
        stateDependencies = __stateDependencies.result(),
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.ConstructResponse] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.provider.ConstructResponse(
        urn = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
        state = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[_root_.scala.Option[com.google.protobuf.struct.Struct]]),
        stateDependencies = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Seq[pulumirpc.provider.ConstructResponse.StateDependenciesEntry]]).getOrElse(_root_.scala.Seq.empty).iterator.map(pulumirpc.provider.ConstructResponse._typemapper_stateDependencies.toCustom(_)).toMap
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ProviderProto.javaDescriptor.getMessageTypes().get(23)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ProviderProto.scalaDescriptor.messages(23)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
    (__number: @_root_.scala.unchecked) match {
      case 2 => __out = com.google.protobuf.struct.Struct
      case 3 => __out = pulumirpc.provider.ConstructResponse.StateDependenciesEntry
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]](
      _root_.pulumirpc.provider.ConstructResponse.PropertyDependencies,
      _root_.pulumirpc.provider.ConstructResponse.StateDependenciesEntry
    )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.provider.ConstructResponse(
    urn = "",
    state = _root_.scala.None,
    stateDependencies = _root_.scala.collection.immutable.Map.empty
  )
  /** PropertyDependencies describes the resources that a particular property depends on.
    *
    * @param urns
    *   A list of URNs this property depends on.
    */
  @SerialVersionUID(0L)
  final case class PropertyDependencies(
      urns: _root_.scala.Seq[_root_.scala.Predef.String] = _root_.scala.Seq.empty,
      unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
      ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[PropertyDependencies] {
      @transient
      private var __serializedSizeMemoized: _root_.scala.Int = 0
      private def __computeSerializedSize(): _root_.scala.Int = {
        var __size = 0
        urns.foreach { __item =>
          val __value = __item
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
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
        urns.foreach { __v =>
          val __m = __v
          _output__.writeString(1, __m)
        };
        unknownFields.writeTo(_output__)
      }
      def clearUrns = copy(urns = _root_.scala.Seq.empty)
      def addUrns(__vs: _root_.scala.Predef.String *): PropertyDependencies = addAllUrns(__vs)
      def addAllUrns(__vs: Iterable[_root_.scala.Predef.String]): PropertyDependencies = copy(urns = urns ++ __vs)
      def withUrns(__v: _root_.scala.Seq[_root_.scala.Predef.String]): PropertyDependencies = copy(urns = __v)
      def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
      def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
      def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 => urns
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 => _root_.scalapb.descriptors.PRepeated(urns.iterator.map(_root_.scalapb.descriptors.PString(_)).toVector)
        }
      }
      def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
      def companion: pulumirpc.provider.ConstructResponse.PropertyDependencies.type = pulumirpc.provider.ConstructResponse.PropertyDependencies
      // @@protoc_insertion_point(GeneratedMessage[pulumirpc.ConstructResponse.PropertyDependencies])
  }
  
  object PropertyDependencies extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConstructResponse.PropertyDependencies] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConstructResponse.PropertyDependencies] = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.ConstructResponse.PropertyDependencies = {
      val __urns: _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String] = new _root_.scala.collection.immutable.VectorBuilder[_root_.scala.Predef.String]
      var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __urns += _input__.readStringRequireUtf8()
          case tag =>
            if (_unknownFields__ == null) {
              _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
            }
            _unknownFields__.parseField(tag, _input__)
        }
      }
      pulumirpc.provider.ConstructResponse.PropertyDependencies(
          urns = __urns.result(),
          unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.ConstructResponse.PropertyDependencies] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
        pulumirpc.provider.ConstructResponse.PropertyDependencies(
          urns = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Seq[_root_.scala.Predef.String]]).getOrElse(_root_.scala.Seq.empty)
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.provider.ConstructResponse.javaDescriptor.getNestedTypes().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.provider.ConstructResponse.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = pulumirpc.provider.ConstructResponse.PropertyDependencies(
      urns = _root_.scala.Seq.empty
    )
    implicit class PropertyDependenciesLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.ConstructResponse.PropertyDependencies]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.provider.ConstructResponse.PropertyDependencies](_l) {
      def urns: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Seq[_root_.scala.Predef.String]] = field(_.urns)((c_, f_) => c_.copy(urns = f_))
    }
    final val URNS_FIELD_NUMBER = 1
    def of(
      urns: _root_.scala.Seq[_root_.scala.Predef.String]
    ): _root_.pulumirpc.provider.ConstructResponse.PropertyDependencies = _root_.pulumirpc.provider.ConstructResponse.PropertyDependencies(
      urns
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.ConstructResponse.PropertyDependencies])
  }
  
  @SerialVersionUID(0L)
  final case class StateDependenciesEntry(
      key: _root_.scala.Predef.String = "",
      value: _root_.scala.Option[pulumirpc.provider.ConstructResponse.PropertyDependencies] = _root_.scala.None,
      unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
      ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[StateDependenciesEntry] {
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
      def withKey(__v: _root_.scala.Predef.String): StateDependenciesEntry = copy(key = __v)
      def getValue: pulumirpc.provider.ConstructResponse.PropertyDependencies = value.getOrElse(pulumirpc.provider.ConstructResponse.PropertyDependencies.defaultInstance)
      def clearValue: StateDependenciesEntry = copy(value = _root_.scala.None)
      def withValue(__v: pulumirpc.provider.ConstructResponse.PropertyDependencies): StateDependenciesEntry = copy(value = Option(__v))
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
      def companion: pulumirpc.provider.ConstructResponse.StateDependenciesEntry.type = pulumirpc.provider.ConstructResponse.StateDependenciesEntry
      // @@protoc_insertion_point(GeneratedMessage[pulumirpc.ConstructResponse.StateDependenciesEntry])
  }
  
  object StateDependenciesEntry extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConstructResponse.StateDependenciesEntry] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConstructResponse.StateDependenciesEntry] = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.ConstructResponse.StateDependenciesEntry = {
      var __key: _root_.scala.Predef.String = ""
      var __value: _root_.scala.Option[pulumirpc.provider.ConstructResponse.PropertyDependencies] = _root_.scala.None
      var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __key = _input__.readStringRequireUtf8()
          case 18 =>
            __value = _root_.scala.Option(__value.fold(_root_.scalapb.LiteParser.readMessage[pulumirpc.provider.ConstructResponse.PropertyDependencies](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
          case tag =>
            if (_unknownFields__ == null) {
              _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
            }
            _unknownFields__.parseField(tag, _input__)
        }
      }
      pulumirpc.provider.ConstructResponse.StateDependenciesEntry(
          key = __key,
          value = __value,
          unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.ConstructResponse.StateDependenciesEntry] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
        pulumirpc.provider.ConstructResponse.StateDependenciesEntry(
          key = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          value = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[_root_.scala.Option[pulumirpc.provider.ConstructResponse.PropertyDependencies]])
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.provider.ConstructResponse.javaDescriptor.getNestedTypes().get(1)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.provider.ConstructResponse.scalaDescriptor.nestedMessages(1)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
      var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
      (__number: @_root_.scala.unchecked) match {
        case 2 => __out = pulumirpc.provider.ConstructResponse.PropertyDependencies
      }
      __out
    }
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = pulumirpc.provider.ConstructResponse.StateDependenciesEntry(
      key = "",
      value = _root_.scala.None
    )
    implicit class StateDependenciesEntryLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.ConstructResponse.StateDependenciesEntry]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.provider.ConstructResponse.StateDependenciesEntry](_l) {
      def key: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.key)((c_, f_) => c_.copy(key = f_))
      def value: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.ConstructResponse.PropertyDependencies] = field(_.getValue)((c_, f_) => c_.copy(value = _root_.scala.Option(f_)))
      def optionalValue: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[pulumirpc.provider.ConstructResponse.PropertyDependencies]] = field(_.value)((c_, f_) => c_.copy(value = f_))
    }
    final val KEY_FIELD_NUMBER = 1
    final val VALUE_FIELD_NUMBER = 2
    @transient
    implicit val keyValueMapper: _root_.scalapb.TypeMapper[pulumirpc.provider.ConstructResponse.StateDependenciesEntry, (_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies)] =
      _root_.scalapb.TypeMapper[pulumirpc.provider.ConstructResponse.StateDependenciesEntry, (_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies)](__m => (__m.key, __m.getValue))(__p => pulumirpc.provider.ConstructResponse.StateDependenciesEntry(__p._1, Some(__p._2)))
    def of(
      key: _root_.scala.Predef.String,
      value: _root_.scala.Option[pulumirpc.provider.ConstructResponse.PropertyDependencies]
    ): _root_.pulumirpc.provider.ConstructResponse.StateDependenciesEntry = _root_.pulumirpc.provider.ConstructResponse.StateDependenciesEntry(
      key,
      value
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.ConstructResponse.StateDependenciesEntry])
  }
  
  implicit class ConstructResponseLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.ConstructResponse]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.provider.ConstructResponse](_l) {
    def urn: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.urn)((c_, f_) => c_.copy(urn = f_))
    def state: _root_.scalapb.lenses.Lens[UpperPB, com.google.protobuf.struct.Struct] = field(_.getState)((c_, f_) => c_.copy(state = _root_.scala.Option(f_)))
    def optionalState: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[com.google.protobuf.struct.Struct]] = field(_.state)((c_, f_) => c_.copy(state = f_))
    def stateDependencies: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies]] = field(_.stateDependencies)((c_, f_) => c_.copy(stateDependencies = f_))
  }
  final val URN_FIELD_NUMBER = 1
  final val STATE_FIELD_NUMBER = 2
  final val STATEDEPENDENCIES_FIELD_NUMBER = 3
  @transient
  private[provider] val _typemapper_stateDependencies: _root_.scalapb.TypeMapper[pulumirpc.provider.ConstructResponse.StateDependenciesEntry, (_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies)] = implicitly[_root_.scalapb.TypeMapper[pulumirpc.provider.ConstructResponse.StateDependenciesEntry, (_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies)]]
  def of(
    urn: _root_.scala.Predef.String,
    state: _root_.scala.Option[com.google.protobuf.struct.Struct],
    stateDependencies: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, pulumirpc.provider.ConstructResponse.PropertyDependencies]
  ): _root_.pulumirpc.provider.ConstructResponse = _root_.pulumirpc.provider.ConstructResponse(
    urn,
    state,
    stateDependencies
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.ConstructResponse])
}
