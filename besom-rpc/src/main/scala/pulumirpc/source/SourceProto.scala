// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.source

object SourceProto extends _root_.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.scalapb.GeneratedFileObject] = Seq.empty
  lazy val messagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]](
      pulumirpc.source.SourcePosition
    )
  private lazy val ProtoBytes: _root_.scala.Array[Byte] =
      scalapb.Encoding.fromBase64(scala.collection.immutable.Seq(
  """ChNwdWx1bWkvc291cmNlLnByb3RvEglwdWx1bWlycGMicAoOU291cmNlUG9zaXRpb24SGgoDdXJpGAEgASgJQgjiPwUSA3Vya
  VIDdXJpEh0KBGxpbmUYAiABKAVCCeI/BhIEbGluZVIEbGluZRIjCgZjb2x1bW4YAyABKAVCC+I/CBIGY29sdW1uUgZjb2x1bW5CN
  FoyZ2l0aHViLmNvbS9wdWx1bWkvcHVsdW1pL3Nkay92My9wcm90by9nbztwdWx1bWlycGNiBnByb3RvMw=="""
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