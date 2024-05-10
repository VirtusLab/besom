// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.resource

sealed abstract class Result(val value: _root_.scala.Int) extends _root_.scalapb.GeneratedEnum {
  type EnumType = pulumirpc.resource.Result
  type RecognizedType = pulumirpc.resource.Result.Recognized
  def isSuccess: _root_.scala.Boolean = false
  def isFail: _root_.scala.Boolean = false
  def isSkip: _root_.scala.Boolean = false
  def companion: _root_.scalapb.GeneratedEnumCompanion[Result] = pulumirpc.resource.Result
  final def asRecognized: _root_.scala.Option[pulumirpc.resource.Result.Recognized] = if (isUnrecognized) _root_.scala.None else _root_.scala.Some(this.asInstanceOf[pulumirpc.resource.Result.Recognized])
}

object Result extends _root_.scalapb.GeneratedEnumCompanion[Result] {
  sealed trait Recognized extends Result
  implicit def enumCompanion: _root_.scalapb.GeneratedEnumCompanion[Result] = this
  
  @SerialVersionUID(0L)
  case object SUCCESS extends Result(0) with Result.Recognized {
    val index = 0
    val name = "SUCCESS"
    override def isSuccess: _root_.scala.Boolean = true
  }
  
  @SerialVersionUID(0L)
  case object FAIL extends Result(1) with Result.Recognized {
    val index = 1
    val name = "FAIL"
    override def isFail: _root_.scala.Boolean = true
  }
  
  @SerialVersionUID(0L)
  case object SKIP extends Result(2) with Result.Recognized {
    val index = 2
    val name = "SKIP"
    override def isSkip: _root_.scala.Boolean = true
  }
  
  @SerialVersionUID(0L)
  final case class Unrecognized(unrecognizedValue: _root_.scala.Int) extends Result(unrecognizedValue) with _root_.scalapb.UnrecognizedEnum
  lazy val values: scala.collection.immutable.Seq[ValueType] = scala.collection.immutable.Seq(SUCCESS, FAIL, SKIP)
  def fromValue(__value: _root_.scala.Int): Result = __value match {
    case 0 => SUCCESS
    case 1 => FAIL
    case 2 => SKIP
    case __other => Unrecognized(__other)
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.EnumDescriptor = ResourceProto.javaDescriptor.getEnumTypes().get(0)
  def scalaDescriptor: _root_.scalapb.descriptors.EnumDescriptor = ResourceProto.scalaDescriptor.enums(0)
}