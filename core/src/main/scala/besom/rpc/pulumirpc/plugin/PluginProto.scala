// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.plugin

object PluginProto extends _root_.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.scalapb.GeneratedFileObject] = Seq.empty
  lazy val messagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]](
      pulumirpc.plugin.PluginInfo,
      pulumirpc.plugin.PluginDependency,
      pulumirpc.plugin.PluginAttach
    )
  private lazy val ProtoBytes: _root_.scala.Array[Byte] =
      scalapb.Encoding.fromBase64(scala.collection.immutable.Seq(
  """ChNwdWx1bWkvcGx1Z2luLnByb3RvEglwdWx1bWlycGMiNAoKUGx1Z2luSW5mbxImCgd2ZXJzaW9uGAEgASgJQgziPwkSB3Zlc
  nNpb25SB3ZlcnNpb24iywIKEFBsdWdpbkRlcGVuZGVuY3kSHQoEbmFtZRgBIAEoCUIJ4j8GEgRuYW1lUgRuYW1lEh0KBGtpbmQYA
  iABKAlCCeI/BhIEa2luZFIEa2luZBImCgd2ZXJzaW9uGAMgASgJQgziPwkSB3ZlcnNpb25SB3ZlcnNpb24SIwoGc2VydmVyGAQgA
  SgJQgviPwgSBnNlcnZlclIGc2VydmVyElgKCWNoZWNrc3VtcxgFIAMoCzIqLnB1bHVtaXJwYy5QbHVnaW5EZXBlbmRlbmN5LkNoZ
  WNrc3Vtc0VudHJ5Qg7iPwsSCWNoZWNrc3Vtc1IJY2hlY2tzdW1zGlIKDkNoZWNrc3Vtc0VudHJ5EhoKA2tleRgBIAEoCUII4j8FE
  gNrZXlSA2tleRIgCgV2YWx1ZRgCIAEoDEIK4j8HEgV2YWx1ZVIFdmFsdWU6AjgBIjYKDFBsdWdpbkF0dGFjaBImCgdhZGRyZXNzG
  AEgASgJQgziPwkSB2FkZHJlc3NSB2FkZHJlc3NCNFoyZ2l0aHViLmNvbS9wdWx1bWkvcHVsdW1pL3Nkay92My9wcm90by9nbztwd
  Wx1bWlycGNiBnByb3RvMw=="""
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