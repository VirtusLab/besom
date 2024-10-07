// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.codegen.hcl

/** DiagnosticSeverity is the severity level of a diagnostic message.
  */
sealed abstract class DiagnosticSeverity(val value: _root_.scala.Int) extends _root_.scalapb.GeneratedEnum {
  type EnumType = pulumirpc.codegen.hcl.DiagnosticSeverity
  type RecognizedType = pulumirpc.codegen.hcl.DiagnosticSeverity.Recognized
  def isDiagInvalid: _root_.scala.Boolean = false
  def isDiagError: _root_.scala.Boolean = false
  def isDiagWarning: _root_.scala.Boolean = false
  def companion: _root_.scalapb.GeneratedEnumCompanion[DiagnosticSeverity] = pulumirpc.codegen.hcl.DiagnosticSeverity
  final def asRecognized: _root_.scala.Option[pulumirpc.codegen.hcl.DiagnosticSeverity.Recognized] = if (isUnrecognized) _root_.scala.None else _root_.scala.Some(this.asInstanceOf[pulumirpc.codegen.hcl.DiagnosticSeverity.Recognized])
}

object DiagnosticSeverity extends _root_.scalapb.GeneratedEnumCompanion[DiagnosticSeverity] {
  sealed trait Recognized extends DiagnosticSeverity
  implicit def enumCompanion: _root_.scalapb.GeneratedEnumCompanion[DiagnosticSeverity] = this
  
  /** DIAG_INVALID is the invalid zero value of DiagnosticSeverity
    */
  @SerialVersionUID(0L)
  case object DIAG_INVALID extends DiagnosticSeverity(0) with DiagnosticSeverity.Recognized {
    val index = 0
    val name = "DIAG_INVALID"
    override def isDiagInvalid: _root_.scala.Boolean = true
  }
  
  /** DIAG_ERROR indicates that the problem reported by a diagnostic prevents
    * further progress in parsing and/or evaluating the subject.
    */
  @SerialVersionUID(0L)
  case object DIAG_ERROR extends DiagnosticSeverity(1) with DiagnosticSeverity.Recognized {
    val index = 1
    val name = "DIAG_ERROR"
    override def isDiagError: _root_.scala.Boolean = true
  }
  
  /** DIAG_WARNING indicates that the problem reported by a diagnostic warrants
    * user attention but does not prevent further progress. It is most
    * commonly used for showing deprecation notices.
    */
  @SerialVersionUID(0L)
  case object DIAG_WARNING extends DiagnosticSeverity(2) with DiagnosticSeverity.Recognized {
    val index = 2
    val name = "DIAG_WARNING"
    override def isDiagWarning: _root_.scala.Boolean = true
  }
  
  @SerialVersionUID(0L)
  final case class Unrecognized(unrecognizedValue: _root_.scala.Int) extends DiagnosticSeverity(unrecognizedValue) with _root_.scalapb.UnrecognizedEnum
  lazy val values: scala.collection.immutable.Seq[ValueType] = scala.collection.immutable.Seq(DIAG_INVALID, DIAG_ERROR, DIAG_WARNING)
  def fromValue(__value: _root_.scala.Int): DiagnosticSeverity = __value match {
    case 0 => DIAG_INVALID
    case 1 => DIAG_ERROR
    case 2 => DIAG_WARNING
    case __other => Unrecognized(__other)
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.EnumDescriptor = pulumirpc.codegen.hcl.HclProto.javaDescriptor.getEnumTypes().get(0)
  def scalaDescriptor: _root_.scalapb.descriptors.EnumDescriptor = pulumirpc.codegen.hcl.HclProto.scalaDescriptor.enums(0)
}