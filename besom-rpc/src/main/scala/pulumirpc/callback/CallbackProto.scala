// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.callback

object CallbackProto extends _root_.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.scalapb.GeneratedFileObject] = Seq.empty
  lazy val messagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]](
      pulumirpc.callback.Callback,
      pulumirpc.callback.CallbackInvokeRequest,
      pulumirpc.callback.CallbackInvokeResponse
    )
  private lazy val ProtoBytes: _root_.scala.Array[Byte] =
      scalapb.Encoding.fromBase64(scala.collection.immutable.Seq(
  """ChVwdWx1bWkvY2FsbGJhY2sucHJvdG8SCXB1bHVtaXJwYyJRCghDYWxsYmFjaxIjCgZ0YXJnZXQYASABKAlCC+I/CBIGdGFyZ
  2V0UgZ0YXJnZXQSIAoFdG9rZW4YAiABKAlCCuI/BxIFdG9rZW5SBXRva2VuImEKFUNhbGxiYWNrSW52b2tlUmVxdWVzdBIgCgV0b
  2tlbhgBIAEoCUIK4j8HEgV0b2tlblIFdG9rZW4SJgoHcmVxdWVzdBgCIAEoDEIM4j8JEgdyZXF1ZXN0UgdyZXF1ZXN0IkMKFkNhb
  GxiYWNrSW52b2tlUmVzcG9uc2USKQoIcmVzcG9uc2UYASABKAxCDeI/ChIIcmVzcG9uc2VSCHJlc3BvbnNlMlwKCUNhbGxiYWNrc
  xJPCgZJbnZva2USIC5wdWx1bWlycGMuQ2FsbGJhY2tJbnZva2VSZXF1ZXN0GiEucHVsdW1pcnBjLkNhbGxiYWNrSW52b2tlUmVzc
  G9uc2UiAEI0WjJnaXRodWIuY29tL3B1bHVtaS9wdWx1bWkvc2RrL3YzL3Byb3RvL2dvO3B1bHVtaXJwY2IGcHJvdG8z"""
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