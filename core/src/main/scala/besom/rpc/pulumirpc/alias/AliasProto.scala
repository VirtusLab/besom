// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.alias

object AliasProto extends _root_.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.scalapb.GeneratedFileObject] = Seq.empty
  lazy val messagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]](
      pulumirpc.alias.Alias
    )
  private lazy val ProtoBytes: _root_.scala.Array[Byte] =
      scalapb.Encoding.fromBase64(scala.collection.immutable.Seq(
  """ChJwdWx1bWkvYWxpYXMucHJvdG8SCXB1bHVtaXJwYyLeAgoFQWxpYXMSHAoDdXJuGAEgASgJQgjiPwUSA3VybkgAUgN1cm4SN
  goEc3BlYxgCIAEoCzIVLnB1bHVtaXJwYy5BbGlhcy5TcGVjQgniPwYSBHNwZWNIAFIEc3BlYxr1AQoEU3BlYxIdCgRuYW1lGAEgA
  SgJQgniPwYSBG5hbWVSBG5hbWUSHQoEdHlwZRgCIAEoCUIJ4j8GEgR0eXBlUgR0eXBlEiAKBXN0YWNrGAMgASgJQgriPwcSBXN0Y
  WNrUgVzdGFjaxImCgdwcm9qZWN0GAQgASgJQgziPwkSB3Byb2plY3RSB3Byb2plY3QSLgoJcGFyZW50VXJuGAUgASgJQg7iPwsSC
  XBhcmVudFVybkgAUglwYXJlbnRVcm4SKwoIbm9QYXJlbnQYBiABKAhCDeI/ChIIbm9QYXJlbnRIAFIIbm9QYXJlbnRCCAoGcGFyZ
  W50QgcKBWFsaWFzQjRaMmdpdGh1Yi5jb20vcHVsdW1pL3B1bHVtaS9zZGsvdjMvcHJvdG8vZ287cHVsdW1pcnBjYgZwcm90bzM="""
      ).mkString)
  lazy val scalaDescriptor: _root_.scalapb.descriptors.FileDescriptor = {
    val scalaProto = com.google.protobuf.descriptor.FileDescriptorProto.parseFrom(ProtoBytes)
    _root_.scalapb.descriptors.FileDescriptor.buildFrom(scalaProto, dependencies.map(_.scalaDescriptor))
  }
  lazy val javaDescriptor: com.google.protobuf.Descriptors.FileDescriptor = {
    val javaProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.parseFrom(ProtoBytes)
    com.google.protobuf.Descriptors.FileDescriptor.buildFrom(javaProto, _root_.scala.Array(
    ))
  }
  @deprecated("Use javaDescriptor instead. In a future version this will refer to scalaDescriptor.", "ScalaPB 0.5.47")
  def descriptor: com.google.protobuf.Descriptors.FileDescriptor = javaDescriptor
}