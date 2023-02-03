// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.resource

/** Alias is a description of an alias.
  */
@SerialVersionUID(0L)
final case class Alias(
    alias: pulumirpc.resource.Alias.Alias = pulumirpc.resource.Alias.Alias.Empty,
    unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
    ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[Alias] {
    @transient
    private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
    private[this] def __computeSerializedSize(): _root_.scala.Int = {
      var __size = 0
      if (alias.urn.isDefined) {
        val __value = alias.urn.get
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
      };
      if (alias.spec.isDefined) {
        val __value = alias.spec.get
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
      alias.urn.foreach { __v =>
        val __m = __v
        _output__.writeString(1, __m)
      };
      alias.spec.foreach { __v =>
        val __m = __v
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__m.serializedSize)
        __m.writeTo(_output__)
      };
      unknownFields.writeTo(_output__)
    }
    def getUrn: _root_.scala.Predef.String = alias.urn.getOrElse("")
    def withUrn(__v: _root_.scala.Predef.String): Alias = copy(alias = pulumirpc.resource.Alias.Alias.Urn(__v))
    def getSpec: pulumirpc.resource.Alias.Spec = alias.spec.getOrElse(pulumirpc.resource.Alias.Spec.defaultInstance)
    def withSpec(__v: pulumirpc.resource.Alias.Spec): Alias = copy(alias = pulumirpc.resource.Alias.Alias.Spec(__v))
    def clearAlias: Alias = copy(alias = pulumirpc.resource.Alias.Alias.Empty)
    def withAlias(__v: pulumirpc.resource.Alias.Alias): Alias = copy(alias = __v)
    def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
    def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => alias.urn.orNull
        case 2 => alias.spec.orNull
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => alias.urn.map(_root_.scalapb.descriptors.PString(_)).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 2 => alias.spec.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion: pulumirpc.resource.Alias.type = pulumirpc.resource.Alias
    // @@protoc_insertion_point(GeneratedMessage[pulumirpc.Alias])
}

object Alias extends scalapb.GeneratedMessageCompanion[pulumirpc.resource.Alias] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.resource.Alias] = this
  def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.resource.Alias = {
    var __alias: pulumirpc.resource.Alias.Alias = pulumirpc.resource.Alias.Alias.Empty
    var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 => _done__ = true
        case 10 =>
          __alias = pulumirpc.resource.Alias.Alias.Urn(_input__.readStringRequireUtf8())
        case 18 =>
          __alias = pulumirpc.resource.Alias.Alias.Spec(__alias.spec.fold(_root_.scalapb.LiteParser.readMessage[pulumirpc.resource.Alias.Spec](_input__))(_root_.scalapb.LiteParser.readMessage(_input__, _)))
        case tag =>
          if (_unknownFields__ == null) {
            _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
          }
          _unknownFields__.parseField(tag, _input__)
      }
    }
    pulumirpc.resource.Alias(
        alias = __alias,
        unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.resource.Alias] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
      pulumirpc.resource.Alias(
        alias = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[_root_.scala.Option[_root_.scala.Predef.String]]).map(pulumirpc.resource.Alias.Alias.Urn(_))
            .orElse[pulumirpc.resource.Alias.Alias](__fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[_root_.scala.Option[pulumirpc.resource.Alias.Spec]]).map(pulumirpc.resource.Alias.Alias.Spec(_)))
            .getOrElse(pulumirpc.resource.Alias.Alias.Empty)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ResourceProto.javaDescriptor.getMessageTypes().get(2)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ResourceProto.scalaDescriptor.messages(2)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 2 => __out = pulumirpc.resource.Alias.Spec
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]](
      _root_.pulumirpc.resource.Alias.Spec
    )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = pulumirpc.resource.Alias(
    alias = pulumirpc.resource.Alias.Alias.Empty
  )
  sealed trait Alias extends _root_.scalapb.GeneratedOneof {
    def isEmpty: _root_.scala.Boolean = false
    def isDefined: _root_.scala.Boolean = true
    def isUrn: _root_.scala.Boolean = false
    def isSpec: _root_.scala.Boolean = false
    def urn: _root_.scala.Option[_root_.scala.Predef.String] = _root_.scala.None
    def spec: _root_.scala.Option[pulumirpc.resource.Alias.Spec] = _root_.scala.None
  }
  object Alias {
    @SerialVersionUID(0L)
    case object Empty extends pulumirpc.resource.Alias.Alias {
      type ValueType = _root_.scala.Nothing
      override def isEmpty: _root_.scala.Boolean = true
      override def isDefined: _root_.scala.Boolean = false
      override def number: _root_.scala.Int = 0
      override def value: _root_.scala.Nothing = throw new java.util.NoSuchElementException("Empty.value")
    }
  
    @SerialVersionUID(0L)
    final case class Urn(value: _root_.scala.Predef.String) extends pulumirpc.resource.Alias.Alias {
      type ValueType = _root_.scala.Predef.String
      override def isUrn: _root_.scala.Boolean = true
      override def urn: _root_.scala.Option[_root_.scala.Predef.String] = Some(value)
      override def number: _root_.scala.Int = 1
    }
    @SerialVersionUID(0L)
    final case class Spec(value: pulumirpc.resource.Alias.Spec) extends pulumirpc.resource.Alias.Alias {
      type ValueType = pulumirpc.resource.Alias.Spec
      override def isSpec: _root_.scala.Boolean = true
      override def spec: _root_.scala.Option[pulumirpc.resource.Alias.Spec] = Some(value)
      override def number: _root_.scala.Int = 2
    }
  }
  /** @param name
    *   The previous name of the resource. If not set, the current name of the resource is used.
    * @param type
    *   The previous type of the resource. If not set, the current type of the resource is used.
    * @param stack
    *   The previous stack of the resource. If not set, the current stack of the resource is used.
    * @param project
    *   The previous project of the resource. If not set, the current project of the resource is used.
    */
  @SerialVersionUID(0L)
  final case class Spec(
      name: _root_.scala.Predef.String = "",
      `type`: _root_.scala.Predef.String = "",
      stack: _root_.scala.Predef.String = "",
      project: _root_.scala.Predef.String = "",
      parent: pulumirpc.resource.Alias.Spec.Parent = pulumirpc.resource.Alias.Spec.Parent.Empty,
      unknownFields: _root_.scalapb.UnknownFieldSet = _root_.scalapb.UnknownFieldSet.empty
      ) extends scalapb.GeneratedMessage with scalapb.lenses.Updatable[Spec] {
      @transient
      private[this] var __serializedSizeMemoized: _root_.scala.Int = 0
      private[this] def __computeSerializedSize(): _root_.scala.Int = {
        var __size = 0
        
        {
          val __value = name
          if (!__value.isEmpty) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, __value)
          }
        };
        
        {
          val __value = `type`
          if (!__value.isEmpty) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, __value)
          }
        };
        
        {
          val __value = stack
          if (!__value.isEmpty) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, __value)
          }
        };
        
        {
          val __value = project
          if (!__value.isEmpty) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, __value)
          }
        };
        if (parent.parentUrn.isDefined) {
          val __value = parent.parentUrn.get
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, __value)
        };
        if (parent.noParent.isDefined) {
          val __value = parent.noParent.get
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(6, __value)
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
          val __v = name
          if (!__v.isEmpty) {
            _output__.writeString(1, __v)
          }
        };
        {
          val __v = `type`
          if (!__v.isEmpty) {
            _output__.writeString(2, __v)
          }
        };
        {
          val __v = stack
          if (!__v.isEmpty) {
            _output__.writeString(3, __v)
          }
        };
        {
          val __v = project
          if (!__v.isEmpty) {
            _output__.writeString(4, __v)
          }
        };
        parent.parentUrn.foreach { __v =>
          val __m = __v
          _output__.writeString(5, __m)
        };
        parent.noParent.foreach { __v =>
          val __m = __v
          _output__.writeBool(6, __m)
        };
        unknownFields.writeTo(_output__)
      }
      def withName(__v: _root_.scala.Predef.String): Spec = copy(name = __v)
      def withType(__v: _root_.scala.Predef.String): Spec = copy(`type` = __v)
      def withStack(__v: _root_.scala.Predef.String): Spec = copy(stack = __v)
      def withProject(__v: _root_.scala.Predef.String): Spec = copy(project = __v)
      def getParentUrn: _root_.scala.Predef.String = parent.parentUrn.getOrElse("")
      def withParentUrn(__v: _root_.scala.Predef.String): Spec = copy(parent = pulumirpc.resource.Alias.Spec.Parent.ParentUrn(__v))
      def getNoParent: _root_.scala.Boolean = parent.noParent.getOrElse(false)
      def withNoParent(__v: _root_.scala.Boolean): Spec = copy(parent = pulumirpc.resource.Alias.Spec.Parent.NoParent(__v))
      def clearParent: Spec = copy(parent = pulumirpc.resource.Alias.Spec.Parent.Empty)
      def withParent(__v: pulumirpc.resource.Alias.Spec.Parent): Spec = copy(parent = __v)
      def withUnknownFields(__v: _root_.scalapb.UnknownFieldSet) = copy(unknownFields = __v)
      def discardUnknownFields = copy(unknownFields = _root_.scalapb.UnknownFieldSet.empty)
      def getFieldByNumber(__fieldNumber: _root_.scala.Int): _root_.scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 => {
            val __t = name
            if (__t != "") __t else null
          }
          case 2 => {
            val __t = `type`
            if (__t != "") __t else null
          }
          case 3 => {
            val __t = stack
            if (__t != "") __t else null
          }
          case 4 => {
            val __t = project
            if (__t != "") __t else null
          }
          case 5 => parent.parentUrn.orNull
          case 6 => parent.noParent.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        _root_.scala.Predef.require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 => _root_.scalapb.descriptors.PString(name)
          case 2 => _root_.scalapb.descriptors.PString(`type`)
          case 3 => _root_.scalapb.descriptors.PString(stack)
          case 4 => _root_.scalapb.descriptors.PString(project)
          case 5 => parent.parentUrn.map(_root_.scalapb.descriptors.PString(_)).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 6 => parent.noParent.map(_root_.scalapb.descriptors.PBoolean(_)).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
      def companion: pulumirpc.resource.Alias.Spec.type = pulumirpc.resource.Alias.Spec
      // @@protoc_insertion_point(GeneratedMessage[pulumirpc.Alias.Spec])
  }
  
  object Spec extends scalapb.GeneratedMessageCompanion[pulumirpc.resource.Alias.Spec] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[pulumirpc.resource.Alias.Spec] = this
    def parseFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): pulumirpc.resource.Alias.Spec = {
      var __name: _root_.scala.Predef.String = ""
      var __type: _root_.scala.Predef.String = ""
      var __stack: _root_.scala.Predef.String = ""
      var __project: _root_.scala.Predef.String = ""
      var __parent: pulumirpc.resource.Alias.Spec.Parent = pulumirpc.resource.Alias.Spec.Parent.Empty
      var `_unknownFields__`: _root_.scalapb.UnknownFieldSet.Builder = null
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __name = _input__.readStringRequireUtf8()
          case 18 =>
            __type = _input__.readStringRequireUtf8()
          case 26 =>
            __stack = _input__.readStringRequireUtf8()
          case 34 =>
            __project = _input__.readStringRequireUtf8()
          case 42 =>
            __parent = pulumirpc.resource.Alias.Spec.Parent.ParentUrn(_input__.readStringRequireUtf8())
          case 48 =>
            __parent = pulumirpc.resource.Alias.Spec.Parent.NoParent(_input__.readBool())
          case tag =>
            if (_unknownFields__ == null) {
              _unknownFields__ = new _root_.scalapb.UnknownFieldSet.Builder()
            }
            _unknownFields__.parseField(tag, _input__)
        }
      }
      pulumirpc.resource.Alias.Spec(
          name = __name,
          `type` = __type,
          stack = __stack,
          project = __project,
          parent = __parent,
          unknownFields = if (_unknownFields__ == null) _root_.scalapb.UnknownFieldSet.empty else _unknownFields__.result()
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[pulumirpc.resource.Alias.Spec] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        _root_.scala.Predef.require(__fieldsMap.keys.forall(_.containingMessage eq scalaDescriptor), "FieldDescriptor does not match message type.")
        pulumirpc.resource.Alias.Spec(
          name = __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          `type` = __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          stack = __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          project = __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          parent = __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[_root_.scala.Option[_root_.scala.Predef.String]]).map(pulumirpc.resource.Alias.Spec.Parent.ParentUrn(_))
              .orElse[pulumirpc.resource.Alias.Spec.Parent](__fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[_root_.scala.Option[_root_.scala.Boolean]]).map(pulumirpc.resource.Alias.Spec.Parent.NoParent(_)))
              .getOrElse(pulumirpc.resource.Alias.Spec.Parent.Empty)
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = pulumirpc.resource.Alias.javaDescriptor.getNestedTypes().get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = pulumirpc.resource.Alias.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = pulumirpc.resource.Alias.Spec(
      name = "",
      `type` = "",
      stack = "",
      project = "",
      parent = pulumirpc.resource.Alias.Spec.Parent.Empty
    )
    sealed trait Parent extends _root_.scalapb.GeneratedOneof {
      def isEmpty: _root_.scala.Boolean = false
      def isDefined: _root_.scala.Boolean = true
      def isParentUrn: _root_.scala.Boolean = false
      def isNoParent: _root_.scala.Boolean = false
      def parentUrn: _root_.scala.Option[_root_.scala.Predef.String] = _root_.scala.None
      def noParent: _root_.scala.Option[_root_.scala.Boolean] = _root_.scala.None
    }
    object Parent {
      @SerialVersionUID(0L)
      case object Empty extends pulumirpc.resource.Alias.Spec.Parent {
        type ValueType = _root_.scala.Nothing
        override def isEmpty: _root_.scala.Boolean = true
        override def isDefined: _root_.scala.Boolean = false
        override def number: _root_.scala.Int = 0
        override def value: _root_.scala.Nothing = throw new java.util.NoSuchElementException("Empty.value")
      }
    
      @SerialVersionUID(0L)
      final case class ParentUrn(value: _root_.scala.Predef.String) extends pulumirpc.resource.Alias.Spec.Parent {
        type ValueType = _root_.scala.Predef.String
        override def isParentUrn: _root_.scala.Boolean = true
        override def parentUrn: _root_.scala.Option[_root_.scala.Predef.String] = Some(value)
        override def number: _root_.scala.Int = 5
      }
      @SerialVersionUID(0L)
      final case class NoParent(value: _root_.scala.Boolean) extends pulumirpc.resource.Alias.Spec.Parent {
        type ValueType = _root_.scala.Boolean
        override def isNoParent: _root_.scala.Boolean = true
        override def noParent: _root_.scala.Option[_root_.scala.Boolean] = Some(value)
        override def number: _root_.scala.Int = 6
      }
    }
    implicit class SpecLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.resource.Alias.Spec]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, pulumirpc.resource.Alias.Spec](_l) {
      def name: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.name)((c_, f_) => c_.copy(name = f_))
      def `type`: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.`type`)((c_, f_) => c_.copy(`type` = f_))
      def stack: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.stack)((c_, f_) => c_.copy(stack = f_))
      def project: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.project)((c_, f_) => c_.copy(project = f_))
      def parentUrn: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.getParentUrn)((c_, f_) => c_.copy(parent = pulumirpc.resource.Alias.Spec.Parent.ParentUrn(f_)))
      def noParent: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Boolean] = field(_.getNoParent)((c_, f_) => c_.copy(parent = pulumirpc.resource.Alias.Spec.Parent.NoParent(f_)))
      def parent: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.resource.Alias.Spec.Parent] = field(_.parent)((c_, f_) => c_.copy(parent = f_))
    }
    final val NAME_FIELD_NUMBER = 1
    final val TYPE_FIELD_NUMBER = 2
    final val STACK_FIELD_NUMBER = 3
    final val PROJECT_FIELD_NUMBER = 4
    final val PARENTURN_FIELD_NUMBER = 5
    final val NOPARENT_FIELD_NUMBER = 6
    def of(
      name: _root_.scala.Predef.String,
      `type`: _root_.scala.Predef.String,
      stack: _root_.scala.Predef.String,
      project: _root_.scala.Predef.String,
      parent: pulumirpc.resource.Alias.Spec.Parent
    ): _root_.pulumirpc.resource.Alias.Spec = _root_.pulumirpc.resource.Alias.Spec(
      name,
      `type`,
      stack,
      project,
      parent
    )
    // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.Alias.Spec])
  }
  
  implicit class AliasLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.resource.Alias]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, pulumirpc.resource.Alias](_l) {
    def urn: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.getUrn)((c_, f_) => c_.copy(alias = pulumirpc.resource.Alias.Alias.Urn(f_)))
    def spec: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.resource.Alias.Spec] = field(_.getSpec)((c_, f_) => c_.copy(alias = pulumirpc.resource.Alias.Alias.Spec(f_)))
    def alias: _root_.scalapb.lenses.Lens[UpperPB, pulumirpc.resource.Alias.Alias] = field(_.alias)((c_, f_) => c_.copy(alias = f_))
  }
  final val URN_FIELD_NUMBER = 1
  final val SPEC_FIELD_NUMBER = 2
  def of(
    alias: pulumirpc.resource.Alias.Alias
  ): _root_.pulumirpc.resource.Alias = _root_.pulumirpc.resource.Alias(
    alias
  )
  // @@protoc_insertion_point(GeneratedMessageCompanion[pulumirpc.Alias])
}
