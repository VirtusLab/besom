// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!

package pulumirpc.codegen.hcl

object HclProto extends _root_.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.scalapb.GeneratedFileObject] = Seq.empty
  lazy val messagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[? <: _root_.scalapb.GeneratedMessage]](
      pulumirpc.codegen.hcl.Pos,
      pulumirpc.codegen.hcl.Range,
      pulumirpc.codegen.hcl.Diagnostic
    )
  private lazy val ProtoBytes: _root_.scala.Array[Byte] =
      scalapb.Encoding.fromBase64(scala.collection.immutable.Seq(
  """ChhwdWx1bWkvY29kZWdlbi9oY2wucHJvdG8SEXB1bHVtaXJwYy5jb2RlZ2VuImgKA1BvcxIdCgRsaW5lGAEgASgDQgniPwYSB
  GxpbmVSBGxpbmUSIwoGY29sdW1uGAIgASgDQgviPwgSBmNvbHVtblIGY29sdW1uEh0KBGJ5dGUYAyABKANCCeI/BhIEYnl0ZVIEY
  nl0ZSKgAQoFUmFuZ2USKQoIZmlsZW5hbWUYASABKAlCDeI/ChIIZmlsZW5hbWVSCGZpbGVuYW1lEjgKBXN0YXJ0GAIgASgLMhYuc
  HVsdW1pcnBjLmNvZGVnZW4uUG9zQgriPwcSBXN0YXJ0UgVzdGFydBIyCgNlbmQYAyABKAsyFi5wdWx1bWlycGMuY29kZWdlbi5Qb
  3NCCOI/BRIDZW5kUgNlbmQirwIKCkRpYWdub3N0aWMSUAoIc2V2ZXJpdHkYASABKA4yJS5wdWx1bWlycGMuY29kZWdlbi5EaWFnb
  m9zdGljU2V2ZXJpdHlCDeI/ChIIc2V2ZXJpdHlSCHNldmVyaXR5EiYKB3N1bW1hcnkYAiABKAlCDOI/CRIHc3VtbWFyeVIHc3Vtb
  WFyeRIjCgZkZXRhaWwYAyABKAlCC+I/CBIGZGV0YWlsUgZkZXRhaWwSQAoHc3ViamVjdBgEIAEoCzIYLnB1bHVtaXJwYy5jb2RlZ
  2VuLlJhbmdlQgziPwkSB3N1YmplY3RSB3N1YmplY3QSQAoHY29udGV4dBgFIAEoCzIYLnB1bHVtaXJwYy5jb2RlZ2VuLlJhbmdlQ
  gziPwkSB2NvbnRleHRSB2NvbnRleHQqfwoSRGlhZ25vc3RpY1NldmVyaXR5EiMKDERJQUdfSU5WQUxJRBAAGhHiPw4SDERJQUdfS
  U5WQUxJRBIfCgpESUFHX0VSUk9SEAEaD+I/DBIKRElBR19FUlJPUhIjCgxESUFHX1dBUk5JTkcQAhoR4j8OEgxESUFHX1dBUk5JT
  kdCMlowZ2l0aHViLmNvbS9wdWx1bWkvcHVsdW1pL3Nkay92My9wcm90by9nby9jb2RlZ2VuYgZwcm90bzM="""
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