// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.provider

/** @param variables
  *   a map of configuration keys to values.
  * @param args
  *   the input properties for the provider. Only filled in for newer providers.
  * @param acceptSecrets
  *   when true, operations should return secrets as strongly typed.
  * @param acceptResources
  *   when true, operations should return resources as strongly typed values to the provider.
  * @param sendsOldInputs
  *   when true, diff and update will be called with the old outputs and the old inputs.
  * @param sendsOldInputsToDelete
  *   when true, delete will be called with the old outputs and the old inputs.
  */
@SerialVersionUID(0L)
final case class ConfigureRequest(
    variables: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String] = _root_.scala.collection.immutable.Map.empty,
    args: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None,
    acceptSecrets: _root_.scala.Boolean = false,
    acceptResources: _root_.scala.Boolean = false,
    sendsOldInputs: _root_.scala.Boolean = false,
    sendsOldInputsToDelete: _root_.scala.Boolean = false,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[ConfigureRequest] {
    @transient
    private var __serializedSizeMemoized: _root_.scala.Int = 0
    private def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      variables.foreach { __item =>
        val __value = pulumirpc.provider.ConfigureRequest._typemapper_variables.toBase(__item)
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      }
      if (args.isDefined) {
        val __value = args.get
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(__value.serializedSize) + __value.serializedSize
      };
      
      {
        val __value = acceptSecrets
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(3, __value)
        }
      };
      
      {
        val __value = acceptResources
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(4, __value)
        }
      };
      
      {
        val __value = sendsOldInputs
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(5, __value)
        }
      };
      
      {
        val __value = sendsOldInputsToDelete
        if (__value != false) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(6, __value)
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
      variables.foreach { __v =>
        val __m = pulumirpc.provider.ConfigureRequest._typemapper_variables.toBase(__v)
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      args.foreach { __v =>
        val __m = __v
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      {
        val __v = acceptSecrets
        if (__v != false) {
          _output__.writeBool(3, __v)
        }
      };
      {
        val __v = acceptResources
        if (__v != false) {
          _output__.writeBool(4, __v)
        }
      };
      {
        val __v = sendsOldInputs
        if (__v != false) {
          _output__.writeBool(5, __v)
        }
      };
      {
        val __v = sendsOldInputsToDelete
        if (__v != false) {
          _output__.writeBool(6, __v)
        }
      };
      unknownFields.writeTo(_output__)
    }
    def clearVariables = copy(variables = _root_.scala.collection.immutable.Map.empty)
    def addVariables(__vs: (_root_.scala.Predef.String, _root_.scala.Predef.String) *): ConfigureRequest = addAllVariables(__vs)
    def addAllVariables(__vs: Iterable[(_root_.scala.Predef.String, _root_.scala.Predef.String)]): ConfigureRequest = copy(variables = variables ++ __vs)
    def withVariables(__v: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]): ConfigureRequest = copy(variables = __v)
    def getArgs: com.google.protobuf.struct.Struct = args.getOrElse(com.google.protobuf.struct.Struct.defaultInstance)
    def clearArgs: ConfigureRequest = copy(args = _root_.scala.None)
    def withArgs(__v: com.google.protobuf.struct.Struct): ConfigureRequest = copy(args = Option(__v))
    def withAcceptSecrets(__v: _root_.scala.Boolean): ConfigureRequest = copy(acceptSecrets = __v)
    def withAcceptResources(__v: _root_.scala.Boolean): ConfigureRequest = copy(acceptResources = __v)
    def withSendsOldInputs(__v: _root_.scala.Boolean): ConfigureRequest = copy(sendsOldInputs = __v)
    def withSendsOldInputsToDelete(__v: _root_.scala.Boolean): ConfigureRequest = copy(sendsOldInputsToDelete = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => variables.iterator.map(pulumirpc.provider.ConfigureRequest._typemapper_variables.toBase(_)).toSeq
        case 2 => args.orNull
        case 3 => {
          val __t = acceptSecrets
          if (__t != false) __t else null
        }
        case 4 => {
          val __t = acceptResources
          if (__t != false) __t else null
        }
        case 5 => {
          val __t = sendsOldInputs
          if (__t != false) __t else null
        }
        case 6 => {
          val __t = sendsOldInputsToDelete
          if (__t != false) __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PRepeated(variables.iterator.map(pulumirpc.provider.ConfigureRequest._typemapper_variables.toBase(_).toPMessage).toVector)
        case 2 => args.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 3 => _root_.scalapb.descriptors.PBoolean(acceptSecrets)
        case 4 => _root_.scalapb.descriptors.PBoolean(acceptResources)
        case 5 => _root_.scalapb.descriptors.PBoolean(sendsOldInputs)
        case 6 => _root_.scalapb.descriptors.PBoolean(sendsOldInputsToDelete)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.provider.ConfigureRequest.type = pulumirpc.provider.ConfigureRequest
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.ConfigureRequest])
}

object ConfigureRequest extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConfigureRequest] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConfigureRequest] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.ConfigureRequest = {
    val __variables: _root_.scala.collection.mutable.Builder[(_root_.scala.Predef.String, _root_.scala.Predef.String), _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]] = _root_.scala.collection.immutable.Map.newBuilder[_root_.scala.Predef.String, _root_.scala.Predef.String]
    var __args: _root_.scala.Option[com.google.protobuf.struct.Struct] = _root_.scala.None
    var __acceptSecrets: _root_.scala.Boolean = false
    var __acceptResources: _root_.scala.Boolean = false
    var __sendsOldInputs: _root_.scala.Boolean = false
    var __sendsOldInputsToDelete: _root_.scala.Boolean = false
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __variables += pulumirpc.provider.ConfigureRequest._typemapper_variables.toCustom(_root_.scalapb.LiteParser.readMessage[pulumirpc.provider.ConfigureRequest.VariablesEntry](_input__))
        case 18 =>
          __args = _root_.scala.Option(__args.fold(_root_.scalapb.LiteParser.readMessage[com.google.protobuf.struct.Struct](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case 24 =>
          __acceptSecrets = _input__.readBool()
        case 32 =>
          __acceptResources = _input__.readBool()
        case 40 =>
          __sendsOldInputs = _input__.readBool()
        case 48 =>
          __sendsOldInputsToDelete = _input__.readBool()
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.provider.ConfigureRequest(
        variables = __variables.result(),
        args = __args,
        acceptSecrets = __acceptSecrets,
        acceptResources = __acceptResources,
        sendsOldInputs = __sendsOldInputs,
        sendsOldInputsToDelete = __sendsOldInputsToDelete,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.ConfigureRequest] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.provider.ConfigureRequest(
        variables = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Seq[pulumirpc.provider.ConfigureRequest.VariablesEntry]]).getOrElse(_root_.scala.Seq.empty).iterator.map(pulumirpc.provider.ConfigureRequest._typemapper_variables.toCustom(_)).toMap,
        args = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[_root_.scala.Option[com.google.protobuf.struct.Struct]]),
        acceptSecrets = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Boolean]).getOrElse(false),
        acceptResources = __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).map(_.as[_root_.scala.Boolean]).getOrElse(false),
        sendsOldInputs = __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).map(_.as[_root_.scala.Boolean]).getOrElse(false),
        sendsOldInputsToDelete = __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).map(_.as[_root_.scala.Boolean]).getOrElse(false)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ProviderProto.javaDescriptor.getMessageTypes().get(2)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ProviderProto.scalaDescriptor.messages(2)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[?] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = pulumirpc.provider.ConfigureRequest.VariablesEntry
      case 2 => __out = com.google.protobuf.struct.Struct
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]](
      _root_.pulumirpc.provider.ConfigureRequest.VariablesEntry
    )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.provider.ConfigureRequest(
    variables = _root_.scala.collection.immutable.Map.empty,
    args = _root_.scala.None,
    acceptSecrets = false,
    acceptResources = false,
    sendsOldInputs = false,
    sendsOldInputsToDelete = false
  )
  @SerialVersionUID(0L)
  final case class VariablesEntry(
      key: _root_.scala.Predef.String = "",
      value: _root_.scala.Predef.String = "",
      unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
      ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[VariablesEntry] {
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
      def withKey(__v: _root_.scala.Predef.String): VariablesEntry = copy(key = __v)
      def withValue(__v: _root_.scala.Predef.String): VariablesEntry = copy(value = __v)
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
      def companion: pulumirpc.provider.ConfigureRequest.VariablesEntry.type = pulumirpc.provider.ConfigureRequest.VariablesEntry
      // @@protoc_insertion_point(GeneratedMessage[pulumirpc.ConfigureRequest.VariablesEntry])
  }
  
  object VariablesEntry extends scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConfigureRequest.VariablesEntry] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.provider.ConfigureRequest.VariablesEntry] = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.provider.ConfigureRequest.VariablesEntry = {
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
      pulumirpc.provider.ConfigureRequest.VariablesEntry(
          key = __key,
          value = __value,
          unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.provider.ConfigureRequest.VariablesEntry] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
        pulumirpc.provider.ConfigureRequest.VariablesEntry(
          key = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          value = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.provider.ConfigureRequest.javaDescriptor.getNestedTypes().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.provider.ConfigureRequest.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[?] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[?] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = pulumirpc.provider.ConfigureRequest.VariablesEntry(
      key = "",
      value = ""
    )
    implicit class VariablesEntryLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.ConfigureRequest.VariablesEntry]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.provider.ConfigureRequest.VariablesEntry](_l) {
      def key: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.key)((c_, f_) => c_.copy(key = f_))
      def value: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.value)((c_, f_) => c_.copy(value = f_))
    }
    final val KEY_FIELD_NUMBER = 1
    final val VALUE_FIELD_NUMBER = 2
    @transient
    implicit val keyValueMapper: _root_.scalapb.TypeMapper[pulumirpc.provider.ConfigureRequest.VariablesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)] =
      _root_.scalapb.TypeMapper[pulumirpc.provider.ConfigureRequest.VariablesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)](__m => (__m.key, __m.value))(__p => pulumirpc.provider.ConfigureRequest.VariablesEntry(__p._1, __p._2))
    def of(
      key: _root_.scala.Predef.String,
      value: _root_.scala.Predef.String
    ): _root_.pulumirpc.provider.ConfigureRequest.VariablesEntry = _root_.pulumirpc.provider.ConfigureRequest.VariablesEntry(
      key,
      value
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.ConfigureRequest.VariablesEntry])
  }
  
  implicit class ConfigureRequestLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.provider.ConfigureRequest]) extends _root_.scalapb.lenses.MessageLens[UpperPB, pulumirpc.provider.ConfigureRequest](_l) {
    def variables: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String]] = field(_.variables)((c_, f_) => c_.copy(variables = f_))
    def args: _root_.scalapb.lenses.Lens[UpperPB, com.google.protobuf.struct.Struct] = field(_.getArgs)((c_, f_) => c_.copy(args = _root_.scala.Option(f_)))
    def optionalArgs: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Option[com.google.protobuf.struct.Struct]] = field(_.args)((c_, f_) => c_.copy(args = f_))
    def acceptSecrets: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.acceptSecrets)((c_, f_) => c_.copy(acceptSecrets = f_))
    def acceptResources: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.acceptResources)((c_, f_) => c_.copy(acceptResources = f_))
    def sendsOldInputs: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.sendsOldInputs)((c_, f_) => c_.copy(sendsOldInputs = f_))
    def sendsOldInputsToDelete: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.sendsOldInputsToDelete)((c_, f_) => c_.copy(sendsOldInputsToDelete = f_))
  }
  final val VARIABLES_FIELD_NUMBER = 1
  final val ARGS_FIELD_NUMBER = 2
  final val ACCEPTSECRETS_FIELD_NUMBER = 3
  final val ACCEPTRESOURCES_FIELD_NUMBER = 4
  final val SENDS_OLD_INPUTS_FIELD_NUMBER = 5
  final val SENDS_OLD_INPUTS_TO_DELETE_FIELD_NUMBER = 6
  @transient
  private[provider] val _typemapper_variables: _root_.scalapb.TypeMapper[pulumirpc.provider.ConfigureRequest.VariablesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)] = implicitly[_root_.scalapb.TypeMapper[pulumirpc.provider.ConfigureRequest.VariablesEntry, (_root_.scala.Predef.String, _root_.scala.Predef.String)]]
  def of(
    variables: _root_.scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String],
    args: _root_.scala.Option[com.google.protobuf.struct.Struct],
    acceptSecrets: _root_.scala.Boolean,
    acceptResources: _root_.scala.Boolean,
    sendsOldInputs: _root_.scala.Boolean,
    sendsOldInputsToDelete: _root_.scala.Boolean
  ): _root_.pulumirpc.provider.ConfigureRequest = _root_.pulumirpc.provider.ConfigureRequest(
    variables,
    args,
    acceptSecrets,
    acceptResources,
    sendsOldInputs,
    sendsOldInputsToDelete
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.ConfigureRequest])
}