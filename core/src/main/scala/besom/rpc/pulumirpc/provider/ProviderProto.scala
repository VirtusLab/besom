// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package pulumirpc.provider

object ProviderProto extends _root_.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.scalapb.GeneratedFileObject] = Seq(
    pulumirpc.plugin.PluginProto,
    com.google.protobuf.empty.EmptyProto,
    com.google.protobuf.struct.StructProto,
    pulumirpc.source.SourceProto
  )
  lazy val messagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]] =
    Seq[_root_.scalapb.GeneratedMessageCompanion[_ <: _root_.scalapb.GeneratedMessage]](
      pulumirpc.provider.GetSchemaRequest,
      pulumirpc.provider.GetSchemaResponse,
      pulumirpc.provider.ConfigureRequest,
      pulumirpc.provider.ConfigureResponse,
      pulumirpc.provider.ConfigureErrorMissingKeys,
      pulumirpc.provider.InvokeRequest,
      pulumirpc.provider.InvokeResponse,
      pulumirpc.provider.CallRequest,
      pulumirpc.provider.CallResponse,
      pulumirpc.provider.CheckRequest,
      pulumirpc.provider.CheckResponse,
      pulumirpc.provider.CheckFailure,
      pulumirpc.provider.DiffRequest,
      pulumirpc.provider.PropertyDiff,
      pulumirpc.provider.DiffResponse,
      pulumirpc.provider.CreateRequest,
      pulumirpc.provider.CreateResponse,
      pulumirpc.provider.ReadRequest,
      pulumirpc.provider.ReadResponse,
      pulumirpc.provider.UpdateRequest,
      pulumirpc.provider.UpdateResponse,
      pulumirpc.provider.DeleteRequest,
      pulumirpc.provider.ConstructRequest,
      pulumirpc.provider.ConstructResponse,
      pulumirpc.provider.ErrorResourceInitFailed,
      pulumirpc.provider.GetMappingRequest,
      pulumirpc.provider.GetMappingResponse,
      pulumirpc.provider.GetMappingsRequest,
      pulumirpc.provider.GetMappingsResponse
    )
  private lazy val ProtoBytes: _root_.scala.Array[Byte] =
      scalapb.Encoding.fromBase64(scala.collection.immutable.Seq(
  """ChVwdWx1bWkvcHJvdmlkZXIucHJvdG8SCXB1bHVtaXJwYxoTcHVsdW1pL3BsdWdpbi5wcm90bxobZ29vZ2xlL3Byb3RvYnVmL
  2VtcHR5LnByb3RvGhxnb29nbGUvcHJvdG9idWYvc3RydWN0LnByb3RvGhNwdWx1bWkvc291cmNlLnByb3RvIjoKEEdldFNjaGVtY
  VJlcXVlc3QSJgoHdmVyc2lvbhgBIAEoBUIM4j8JEgd2ZXJzaW9uUgd2ZXJzaW9uIjgKEUdldFNjaGVtYVJlc3BvbnNlEiMKBnNja
  GVtYRgBIAEoCUIL4j8IEgZzY2hlbWFSBnNjaGVtYSKxAwoQQ29uZmlndXJlUmVxdWVzdBJYCgl2YXJpYWJsZXMYASADKAsyKi5wd
  Wx1bWlycGMuQ29uZmlndXJlUmVxdWVzdC5WYXJpYWJsZXNFbnRyeUIO4j8LEgl2YXJpYWJsZXNSCXZhcmlhYmxlcxI2CgRhcmdzG
  AIgASgLMhcuZ29vZ2xlLnByb3RvYnVmLlN0cnVjdEIJ4j8GEgRhcmdzUgRhcmdzEjgKDWFjY2VwdFNlY3JldHMYAyABKAhCEuI/D
  xINYWNjZXB0U2VjcmV0c1INYWNjZXB0U2VjcmV0cxI+Cg9hY2NlcHRSZXNvdXJjZXMYBCABKAhCFOI/ERIPYWNjZXB0UmVzb3VyY
  2VzUg9hY2NlcHRSZXNvdXJjZXMSPQoQc2VuZHNfb2xkX2lucHV0cxgFIAEoCEIT4j8QEg5zZW5kc09sZElucHV0c1IOc2VuZHNPb
  GRJbnB1dHMaUgoOVmFyaWFibGVzRW50cnkSGgoDa2V5GAEgASgJQgjiPwUSA2tleVIDa2V5EiAKBXZhbHVlGAIgASgJQgriPwcSB
  XZhbHVlUgV2YWx1ZToCOAEihwIKEUNvbmZpZ3VyZVJlc3BvbnNlEjgKDWFjY2VwdFNlY3JldHMYASABKAhCEuI/DxINYWNjZXB0U
  2VjcmV0c1INYWNjZXB0U2VjcmV0cxI+Cg9zdXBwb3J0c1ByZXZpZXcYAiABKAhCFOI/ERIPc3VwcG9ydHNQcmV2aWV3Ug9zdXBwb
  3J0c1ByZXZpZXcSPgoPYWNjZXB0UmVzb3VyY2VzGAMgASgIQhTiPxESD2FjY2VwdFJlc291cmNlc1IPYWNjZXB0UmVzb3VyY2VzE
  jgKDWFjY2VwdE91dHB1dHMYBCABKAhCEuI/DxINYWNjZXB0T3V0cHV0c1INYWNjZXB0T3V0cHV0cyLhAQoZQ29uZmlndXJlRXJyb
  3JNaXNzaW5nS2V5cxJjCgttaXNzaW5nS2V5cxgBIAMoCzIvLnB1bHVtaXJwYy5Db25maWd1cmVFcnJvck1pc3NpbmdLZXlzLk1pc
  3NpbmdLZXlCEOI/DRILbWlzc2luZ0tleXNSC21pc3NpbmdLZXlzGl8KCk1pc3NpbmdLZXkSHQoEbmFtZRgBIAEoCUIJ4j8GEgRuY
  W1lUgRuYW1lEjIKC2Rlc2NyaXB0aW9uGAIgASgJQhDiPw0SC2Rlc2NyaXB0aW9uUgtkZXNjcmlwdGlvbiKgAQoNSW52b2tlUmVxd
  WVzdBIaCgN0b2sYASABKAlCCOI/BRIDdG9rUgN0b2sSNgoEYXJncxgCIAEoCzIXLmdvb2dsZS5wcm90b2J1Zi5TdHJ1Y3RCCeI/B
  hIEYXJnc1IEYXJnc0oECAMQB1IIcHJvdmlkZXJSB3ZlcnNpb25SD2FjY2VwdFJlc291cmNlc1IRcGx1Z2luRG93bmxvYWRVUkwik
  gEKDkludm9rZVJlc3BvbnNlEjwKBnJldHVybhgBIAEoCzIXLmdvb2dsZS5wcm90b2J1Zi5TdHJ1Y3RCC+I/CBIGcmV0dXJuUgZyZ
  XR1cm4SQgoIZmFpbHVyZXMYAiADKAsyFy5wdWx1bWlycGMuQ2hlY2tGYWlsdXJlQg3iPwoSCGZhaWx1cmVzUghmYWlsdXJlcyKzC
  goLQ2FsbFJlcXVlc3QSGgoDdG9rGAEgASgJQgjiPwUSA3Rva1IDdG9rEjYKBGFyZ3MYAiABKAsyFy5nb29nbGUucHJvdG9idWYuU
  3RydWN0QgniPwYSBGFyZ3NSBGFyZ3MSawoPYXJnRGVwZW5kZW5jaWVzGAMgAygLMisucHVsdW1pcnBjLkNhbGxSZXF1ZXN0LkFyZ
  0RlcGVuZGVuY2llc0VudHJ5QhTiPxESD2FyZ0RlcGVuZGVuY2llc1IPYXJnRGVwZW5kZW5jaWVzEikKCHByb3ZpZGVyGAQgASgJQ
  g3iPwoSCHByb3ZpZGVyUghwcm92aWRlchImCgd2ZXJzaW9uGAUgASgJQgziPwkSB3ZlcnNpb25SB3ZlcnNpb24SRAoRcGx1Z2luR
  G93bmxvYWRVUkwYDSABKAlCFuI/ExIRcGx1Z2luRG93bmxvYWRVUkxSEXBsdWdpbkRvd25sb2FkVVJMEmsKD3BsdWdpbkNoZWNrc
  3VtcxgQIAMoCzIrLnB1bHVtaXJwYy5DYWxsUmVxdWVzdC5QbHVnaW5DaGVja3N1bXNFbnRyeUIU4j8REg9wbHVnaW5DaGVja3N1b
  XNSD3BsdWdpbkNoZWNrc3VtcxImCgdwcm9qZWN0GAYgASgJQgziPwkSB3Byb2plY3RSB3Byb2plY3QSIAoFc3RhY2sYByABKAlCC
  uI/BxIFc3RhY2tSBXN0YWNrEkcKBmNvbmZpZxgIIAMoCzIiLnB1bHVtaXJwYy5DYWxsUmVxdWVzdC5Db25maWdFbnRyeUIL4j8IE
  gZjb25maWdSBmNvbmZpZxJBChBjb25maWdTZWNyZXRLZXlzGAkgAygJQhXiPxISEGNvbmZpZ1NlY3JldEtleXNSEGNvbmZpZ1NlY
  3JldEtleXMSIwoGZHJ5UnVuGAogASgIQgviPwgSBmRyeVJ1blIGZHJ5UnVuEikKCHBhcmFsbGVsGAsgASgFQg3iPwoSCHBhcmFsb
  GVsUghwYXJhbGxlbBI+Cg9tb25pdG9yRW5kcG9pbnQYDCABKAlCFOI/ERIPbW9uaXRvckVuZHBvaW50Ug9tb25pdG9yRW5kcG9pb
  nQSNQoMb3JnYW5pemF0aW9uGA4gASgJQhHiPw4SDG9yZ2FuaXphdGlvblIMb3JnYW5pemF0aW9uElYKDnNvdXJjZVBvc2l0aW9uG
  A8gASgLMhkucHVsdW1pcnBjLlNvdXJjZVBvc2l0aW9uQhPiPxASDnNvdXJjZVBvc2l0aW9uUg5zb3VyY2VQb3NpdGlvbho1ChRBc
  md1bWVudERlcGVuZGVuY2llcxIdCgR1cm5zGAEgAygJQgniPwYSBHVybnNSBHVybnMahQEKFEFyZ0RlcGVuZGVuY2llc0VudHJ5E
  hoKA2tleRgBIAEoCUII4j8FEgNrZXlSA2tleRJNCgV2YWx1ZRgCIAEoCzIrLnB1bHVtaXJwYy5DYWxsUmVxdWVzdC5Bcmd1bWVud
  ERlcGVuZGVuY2llc0IK4j8HEgV2YWx1ZVIFdmFsdWU6AjgBGlgKFFBsdWdpbkNoZWNrc3Vtc0VudHJ5EhoKA2tleRgBIAEoCUII4
  j8FEgNrZXlSA2tleRIgCgV2YWx1ZRgCIAEoDEIK4j8HEgV2YWx1ZVIFdmFsdWU6AjgBGk8KC0NvbmZpZ0VudHJ5EhoKA2tleRgBI
  AEoCUII4j8FEgNrZXlSA2tleRIgCgV2YWx1ZRgCIAEoCUIK4j8HEgV2YWx1ZVIFdmFsdWU6AjgBIskDCgxDYWxsUmVzcG9uc2USP
  AoGcmV0dXJuGAEgASgLMhcuZ29vZ2xlLnByb3RvYnVmLlN0cnVjdEIL4j8IEgZyZXR1cm5SBnJldHVybhJ4ChJyZXR1cm5EZXBlb
  mRlbmNpZXMYAiADKAsyLy5wdWx1bWlycGMuQ2FsbFJlc3BvbnNlLlJldHVybkRlcGVuZGVuY2llc0VudHJ5QhfiPxQSEnJldHVyb
  kRlcGVuZGVuY2llc1IScmV0dXJuRGVwZW5kZW5jaWVzEkIKCGZhaWx1cmVzGAMgAygLMhcucHVsdW1pcnBjLkNoZWNrRmFpbHVyZ
  UIN4j8KEghmYWlsdXJlc1IIZmFpbHVyZXMaMwoSUmV0dXJuRGVwZW5kZW5jaWVzEh0KBHVybnMYASADKAlCCeI/BhIEdXJuc1IEd
  XJucxqHAQoXUmV0dXJuRGVwZW5kZW5jaWVzRW50cnkSGgoDa2V5GAEgASgJQgjiPwUSA2tleVIDa2V5EkwKBXZhbHVlGAIgASgLM
  ioucHVsdW1pcnBjLkNhbGxSZXNwb25zZS5SZXR1cm5EZXBlbmRlbmNpZXNCCuI/BxIFdmFsdWVSBXZhbHVlOgI4ASLhAQoMQ2hlY
  2tSZXF1ZXN0EhoKA3VybhgBIAEoCUII4j8FEgN1cm5SA3VybhI2CgRvbGRzGAIgASgLMhcuZ29vZ2xlLnByb3RvYnVmLlN0cnVjd
  EIJ4j8GEgRvbGRzUgRvbGRzEjYKBG5ld3MYAyABKAsyFy5nb29nbGUucHJvdG9idWYuU3RydWN0QgniPwYSBG5ld3NSBG5ld3MSL
  woKcmFuZG9tU2VlZBgFIAEoDEIP4j8MEgpyYW5kb21TZWVkUgpyYW5kb21TZWVkSgQIBBAFUg5zZXF1ZW5jZU51bWJlciKRAQoNQ
  2hlY2tSZXNwb25zZRI8CgZpbnB1dHMYASABKAsyFy5nb29nbGUucHJvdG9idWYuU3RydWN0QgviPwgSBmlucHV0c1IGaW5wdXRzE
  kIKCGZhaWx1cmVzGAIgAygLMhcucHVsdW1pcnBjLkNoZWNrRmFpbHVyZUIN4j8KEghmYWlsdXJlc1IIZmFpbHVyZXMiXgoMQ2hlY
  2tGYWlsdXJlEikKCHByb3BlcnR5GAEgASgJQg3iPwoSCHByb3BlcnR5Ughwcm9wZXJ0eRIjCgZyZWFzb24YAiABKAlCC+I/CBIGc
  mVhc29uUgZyZWFzb24itAIKC0RpZmZSZXF1ZXN0EhcKAmlkGAEgASgJQgfiPwQSAmlkUgJpZBIaCgN1cm4YAiABKAlCCOI/BRIDd
  XJuUgN1cm4SNgoEb2xkcxgDIAEoCzIXLmdvb2dsZS5wcm90b2J1Zi5TdHJ1Y3RCCeI/BhIEb2xkc1IEb2xkcxI2CgRuZXdzGAQgA
  SgLMhcuZ29vZ2xlLnByb3RvYnVmLlN0cnVjdEIJ4j8GEgRuZXdzUgRuZXdzEjgKDWlnbm9yZUNoYW5nZXMYBSADKAlCEuI/DxINa
  Wdub3JlQ2hhbmdlc1INaWdub3JlQ2hhbmdlcxJGCgpvbGRfaW5wdXRzGAYgASgLMhcuZ29vZ2xlLnByb3RvYnVmLlN0cnVjdEIO4
  j8LEglvbGRJbnB1dHNSCW9sZElucHV0cyK8AgoMUHJvcGVydHlEaWZmEjsKBGtpbmQYASABKA4yHC5wdWx1bWlycGMuUHJvcGVyd
  HlEaWZmLktpbmRCCeI/BhIEa2luZFIEa2luZBIsCglpbnB1dERpZmYYAiABKAhCDuI/CxIJaW5wdXREaWZmUglpbnB1dERpZmYiw
  AEKBEtpbmQSEQoDQUREEAAaCOI/BRIDQUREEiEKC0FERF9SRVBMQUNFEAEaEOI/DRILQUREX1JFUExBQ0USFwoGREVMRVRFEAIaC
  +I/CBIGREVMRVRFEicKDkRFTEVURV9SRVBMQUNFEAMaE+I/EBIOREVMRVRFX1JFUExBQ0USFwoGVVBEQVRFEAQaC+I/CBIGVVBEQ
  VRFEicKDlVQREFURV9SRVBMQUNFEAUaE+I/EBIOVVBEQVRFX1JFUExBQ0UioAUKDERpZmZSZXNwb25zZRIpCghyZXBsYWNlcxgBI
  AMoCUIN4j8KEghyZXBsYWNlc1IIcmVwbGFjZXMSJgoHc3RhYmxlcxgCIAMoCUIM4j8JEgdzdGFibGVzUgdzdGFibGVzEkoKE2Rlb
  GV0ZUJlZm9yZVJlcGxhY2UYAyABKAhCGOI/FRITZGVsZXRlQmVmb3JlUmVwbGFjZVITZGVsZXRlQmVmb3JlUmVwbGFjZRJLCgdja
  GFuZ2VzGAQgASgOMiMucHVsdW1pcnBjLkRpZmZSZXNwb25zZS5EaWZmQ2hhbmdlc0IM4j8JEgdjaGFuZ2VzUgdjaGFuZ2VzEiAKB
  WRpZmZzGAUgAygJQgriPwcSBWRpZmZzUgVkaWZmcxJgCgxkZXRhaWxlZERpZmYYBiADKAsyKS5wdWx1bWlycGMuRGlmZlJlc3Bvb
  nNlLkRldGFpbGVkRGlmZkVudHJ5QhHiPw4SDGRldGFpbGVkRGlmZlIMZGV0YWlsZWREaWZmEj4KD2hhc0RldGFpbGVkRGlmZhgHI
  AEoCEIU4j8REg9oYXNEZXRhaWxlZERpZmZSD2hhc0RldGFpbGVkRGlmZhpuChFEZXRhaWxlZERpZmZFbnRyeRIaCgNrZXkYASABK
  AlCCOI/BRIDa2V5UgNrZXkSOQoFdmFsdWUYAiABKAsyFy5wdWx1bWlycGMuUHJvcGVydHlEaWZmQgriPwcSBXZhbHVlUgV2YWx1Z
  ToCOAEicAoLRGlmZkNoYW5nZXMSIwoMRElGRl9VTktOT1dOEAAaEeI/DhIMRElGRl9VTktOT1dOEh0KCURJRkZfTk9ORRABGg7iP
  wsSCURJRkZfTk9ORRIdCglESUZGX1NPTUUQAhoO4j8LEglESUZGX1NPTUUixQEKDUNyZWF0ZVJlcXVlc3QSGgoDdXJuGAEgASgJQ
  gjiPwUSA3VyblIDdXJuEkgKCnByb3BlcnRpZXMYAiABKAsyFy5nb29nbGUucHJvdG9idWYuU3RydWN0Qg/iPwwSCnByb3BlcnRpZ
  XNSCnByb3BlcnRpZXMSJgoHdGltZW91dBgDIAEoAUIM4j8JEgd0aW1lb3V0Ugd0aW1lb3V0EiYKB3ByZXZpZXcYBCABKAhCDOI/C
  RIHcHJldmlld1IHcHJldmlldyJzCg5DcmVhdGVSZXNwb25zZRIXCgJpZBgBIAEoCUIH4j8EEgJpZFICaWQSSAoKcHJvcGVydGllc
  xgCIAEoCzIXLmdvb2dsZS5wcm90b2J1Zi5TdHJ1Y3RCD+I/DBIKcHJvcGVydGllc1IKcHJvcGVydGllcyLKAQoLUmVhZFJlcXVlc
  3QSFwoCaWQYASABKAlCB+I/BBICaWRSAmlkEhoKA3VybhgCIAEoCUII4j8FEgN1cm5SA3VybhJICgpwcm9wZXJ0aWVzGAMgASgLM
  hcuZ29vZ2xlLnByb3RvYnVmLlN0cnVjdEIP4j8MEgpwcm9wZXJ0aWVzUgpwcm9wZXJ0aWVzEjwKBmlucHV0cxgEIAEoCzIXLmdvb
  2dsZS5wcm90b2J1Zi5TdHJ1Y3RCC+I/CBIGaW5wdXRzUgZpbnB1dHMirwEKDFJlYWRSZXNwb25zZRIXCgJpZBgBIAEoCUIH4j8EE
  gJpZFICaWQSSAoKcHJvcGVydGllcxgCIAEoCzIXLmdvb2dsZS5wcm90b2J1Zi5TdHJ1Y3RCD+I/DBIKcHJvcGVydGllc1IKcHJvc
  GVydGllcxI8CgZpbnB1dHMYAyABKAsyFy5nb29nbGUucHJvdG9idWYuU3RydWN0QgviPwgSBmlucHV0c1IGaW5wdXRzIoYDCg1Vc
  GRhdGVSZXF1ZXN0EhcKAmlkGAEgASgJQgfiPwQSAmlkUgJpZBIaCgN1cm4YAiABKAlCCOI/BRIDdXJuUgN1cm4SNgoEb2xkcxgDI
  AEoCzIXLmdvb2dsZS5wcm90b2J1Zi5TdHJ1Y3RCCeI/BhIEb2xkc1IEb2xkcxI2CgRuZXdzGAQgASgLMhcuZ29vZ2xlLnByb3RvY
  nVmLlN0cnVjdEIJ4j8GEgRuZXdzUgRuZXdzEiYKB3RpbWVvdXQYBSABKAFCDOI/CRIHdGltZW91dFIHdGltZW91dBI4Cg1pZ25vc
  mVDaGFuZ2VzGAYgAygJQhLiPw8SDWlnbm9yZUNoYW5nZXNSDWlnbm9yZUNoYW5nZXMSJgoHcHJldmlldxgHIAEoCEIM4j8JEgdwc
  mV2aWV3UgdwcmV2aWV3EkYKCm9sZF9pbnB1dHMYCCABKAsyFy5nb29nbGUucHJvdG9idWYuU3RydWN0Qg7iPwsSCW9sZElucHV0c
  1IJb2xkSW5wdXRzIloKDlVwZGF0ZVJlc3BvbnNlEkgKCnByb3BlcnRpZXMYASABKAsyFy5nb29nbGUucHJvdG9idWYuU3RydWN0Q
  g/iPwwSCnByb3BlcnRpZXNSCnByb3BlcnRpZXMitgEKDURlbGV0ZVJlcXVlc3QSFwoCaWQYASABKAlCB+I/BBICaWRSAmlkEhoKA
  3VybhgCIAEoCUII4j8FEgN1cm5SA3VybhJICgpwcm9wZXJ0aWVzGAMgASgLMhcuZ29vZ2xlLnByb3RvYnVmLlN0cnVjdEIP4j8ME
  gpwcm9wZXJ0aWVzUgpwcm9wZXJ0aWVzEiYKB3RpbWVvdXQYBCABKAFCDOI/CRIHdGltZW91dFIHdGltZW91dCKXDwoQQ29uc3Ryd
  WN0UmVxdWVzdBImCgdwcm9qZWN0GAEgASgJQgziPwkSB3Byb2plY3RSB3Byb2plY3QSIAoFc3RhY2sYAiABKAlCCuI/BxIFc3RhY
  2tSBXN0YWNrEkwKBmNvbmZpZxgDIAMoCzInLnB1bHVtaXJwYy5Db25zdHJ1Y3RSZXF1ZXN0LkNvbmZpZ0VudHJ5QgviPwgSBmNvb
  mZpZ1IGY29uZmlnEiMKBmRyeVJ1bhgEIAEoCEIL4j8IEgZkcnlSdW5SBmRyeVJ1bhIpCghwYXJhbGxlbBgFIAEoBUIN4j8KEghwY
  XJhbGxlbFIIcGFyYWxsZWwSPgoPbW9uaXRvckVuZHBvaW50GAYgASgJQhTiPxESD21vbml0b3JFbmRwb2ludFIPbW9uaXRvckVuZ
  HBvaW50Eh0KBHR5cGUYByABKAlCCeI/BhIEdHlwZVIEdHlwZRIdCgRuYW1lGAggASgJQgniPwYSBG5hbWVSBG5hbWUSIwoGcGFyZ
  W50GAkgASgJQgviPwgSBnBhcmVudFIGcGFyZW50EjwKBmlucHV0cxgKIAEoCzIXLmdvb2dsZS5wcm90b2J1Zi5TdHJ1Y3RCC+I/C
  BIGaW5wdXRzUgZpbnB1dHMSeAoRaW5wdXREZXBlbmRlbmNpZXMYCyADKAsyMi5wdWx1bWlycGMuQ29uc3RydWN0UmVxdWVzdC5Jb
  nB1dERlcGVuZGVuY2llc0VudHJ5QhbiPxMSEWlucHV0RGVwZW5kZW5jaWVzUhFpbnB1dERlcGVuZGVuY2llcxJYCglwcm92aWRlc
  nMYDSADKAsyKi5wdWx1bWlycGMuQ29uc3RydWN0UmVxdWVzdC5Qcm92aWRlcnNFbnRyeUIO4j8LEglwcm92aWRlcnNSCXByb3ZpZ
  GVycxI1CgxkZXBlbmRlbmNpZXMYDyADKAlCEeI/DhIMZGVwZW5kZW5jaWVzUgxkZXBlbmRlbmNpZXMSQQoQY29uZmlnU2VjcmV0S
  2V5cxgQIAMoCUIV4j8SEhBjb25maWdTZWNyZXRLZXlzUhBjb25maWdTZWNyZXRLZXlzEjUKDG9yZ2FuaXphdGlvbhgRIAEoCUIR4
  j8OEgxvcmdhbml6YXRpb25SDG9yZ2FuaXphdGlvbhImCgdwcm90ZWN0GAwgASgIQgziPwkSB3Byb3RlY3RSB3Byb3RlY3QSJgoHY
  WxpYXNlcxgOIAMoCUIM4j8JEgdhbGlhc2VzUgdhbGlhc2VzElYKF2FkZGl0aW9uYWxTZWNyZXRPdXRwdXRzGBIgAygJQhziPxkSF
  2FkZGl0aW9uYWxTZWNyZXRPdXRwdXRzUhdhZGRpdGlvbmFsU2VjcmV0T3V0cHV0cxJnCg5jdXN0b21UaW1lb3V0cxgTIAEoCzIqL
  nB1bHVtaXJwYy5Db25zdHJ1Y3RSZXF1ZXN0LkN1c3RvbVRpbWVvdXRzQhPiPxASDmN1c3RvbVRpbWVvdXRzUg5jdXN0b21UaW1lb
  3V0cxIyCgtkZWxldGVkV2l0aBgUIAEoCUIQ4j8NEgtkZWxldGVkV2l0aFILZGVsZXRlZFdpdGgSSgoTZGVsZXRlQmVmb3JlUmVwb
  GFjZRgVIAEoCEIY4j8VEhNkZWxldGVCZWZvcmVSZXBsYWNlUhNkZWxldGVCZWZvcmVSZXBsYWNlEjgKDWlnbm9yZUNoYW5nZXMYF
  iADKAlCEuI/DxINaWdub3JlQ2hhbmdlc1INaWdub3JlQ2hhbmdlcxJBChByZXBsYWNlT25DaGFuZ2VzGBcgAygJQhXiPxISEHJlc
  GxhY2VPbkNoYW5nZXNSEHJlcGxhY2VPbkNoYW5nZXMSOwoOcmV0YWluT25EZWxldGUYGCABKAhCE+I/EBIOcmV0YWluT25EZWxld
  GVSDnJldGFpbk9uRGVsZXRlGjUKFFByb3BlcnR5RGVwZW5kZW5jaWVzEh0KBHVybnMYASADKAlCCeI/BhIEdXJuc1IEdXJucxp/C
  g5DdXN0b21UaW1lb3V0cxIjCgZjcmVhdGUYASABKAlCC+I/CBIGY3JlYXRlUgZjcmVhdGUSIwoGdXBkYXRlGAIgASgJQgviPwgSB
  nVwZGF0ZVIGdXBkYXRlEiMKBmRlbGV0ZRgDIAEoCUIL4j8IEgZkZWxldGVSBmRlbGV0ZRpPCgtDb25maWdFbnRyeRIaCgNrZXkYA
  SABKAlCCOI/BRIDa2V5UgNrZXkSIAoFdmFsdWUYAiABKAlCCuI/BxIFdmFsdWVSBXZhbHVlOgI4ARqMAQoWSW5wdXREZXBlbmRlb
  mNpZXNFbnRyeRIaCgNrZXkYASABKAlCCOI/BRIDa2V5UgNrZXkSUgoFdmFsdWUYAiABKAsyMC5wdWx1bWlycGMuQ29uc3RydWN0U
  mVxdWVzdC5Qcm9wZXJ0eURlcGVuZGVuY2llc0IK4j8HEgV2YWx1ZVIFdmFsdWU6AjgBGlIKDlByb3ZpZGVyc0VudHJ5EhoKA2tle
  RgBIAEoCUII4j8FEgNrZXlSA2tleRIgCgV2YWx1ZRgCIAEoCUIK4j8HEgV2YWx1ZVIFdmFsdWU6AjgBIqwDChFDb25zdHJ1Y3RSZ
  XNwb25zZRIaCgN1cm4YASABKAlCCOI/BRIDdXJuUgN1cm4SOQoFc3RhdGUYAiABKAsyFy5nb29nbGUucHJvdG9idWYuU3RydWN0Q
  griPwcSBXN0YXRlUgVzdGF0ZRJ5ChFzdGF0ZURlcGVuZGVuY2llcxgDIAMoCzIzLnB1bHVtaXJwYy5Db25zdHJ1Y3RSZXNwb25zZ
  S5TdGF0ZURlcGVuZGVuY2llc0VudHJ5QhbiPxMSEXN0YXRlRGVwZW5kZW5jaWVzUhFzdGF0ZURlcGVuZGVuY2llcxo1ChRQcm9wZ
  XJ0eURlcGVuZGVuY2llcxIdCgR1cm5zGAEgAygJQgniPwYSBHVybnNSBHVybnMajQEKFlN0YXRlRGVwZW5kZW5jaWVzRW50cnkSG
  goDa2V5GAEgASgJQgjiPwUSA2tleVIDa2V5ElMKBXZhbHVlGAIgASgLMjEucHVsdW1pcnBjLkNvbnN0cnVjdFJlc3BvbnNlLlByb
  3BlcnR5RGVwZW5kZW5jaWVzQgriPwcSBXZhbHVlUgV2YWx1ZToCOAEi4gEKF0Vycm9yUmVzb3VyY2VJbml0RmFpbGVkEhcKAmlkG
  AEgASgJQgfiPwQSAmlkUgJpZBJICgpwcm9wZXJ0aWVzGAIgASgLMhcuZ29vZ2xlLnByb3RvYnVmLlN0cnVjdEIP4j8MEgpwcm9wZ
  XJ0aWVzUgpwcm9wZXJ0aWVzEiYKB3JlYXNvbnMYAyADKAlCDOI/CRIHcmVhc29uc1IHcmVhc29ucxI8CgZpbnB1dHMYBCABKAsyF
  y5nb29nbGUucHJvdG9idWYuU3RydWN0QgviPwgSBmlucHV0c1IGaW5wdXRzIloKEUdldE1hcHBpbmdSZXF1ZXN0EhoKA2tleRgBI
  AEoCUII4j8FEgNrZXlSA2tleRIpCghwcm92aWRlchgCIAEoCUIN4j8KEghwcm92aWRlclIIcHJvdmlkZXIiXgoSR2V0TWFwcGluZ
  1Jlc3BvbnNlEikKCHByb3ZpZGVyGAEgASgJQg3iPwoSCHByb3ZpZGVyUghwcm92aWRlchIdCgRkYXRhGAIgASgMQgniPwYSBGRhd
  GFSBGRhdGEiMAoSR2V0TWFwcGluZ3NSZXF1ZXN0EhoKA2tleRgBIAEoCUII4j8FEgNrZXlSA2tleSJDChNHZXRNYXBwaW5nc1Jlc
  3BvbnNlEiwKCXByb3ZpZGVycxgBIAMoCUIO4j8LEglwcm92aWRlcnNSCXByb3ZpZGVyczKGCgoQUmVzb3VyY2VQcm92aWRlchJIC
  glHZXRTY2hlbWESGy5wdWx1bWlycGMuR2V0U2NoZW1hUmVxdWVzdBocLnB1bHVtaXJwYy5HZXRTY2hlbWFSZXNwb25zZSIAEkIKC
  0NoZWNrQ29uZmlnEhcucHVsdW1pcnBjLkNoZWNrUmVxdWVzdBoYLnB1bHVtaXJwYy5DaGVja1Jlc3BvbnNlIgASPwoKRGlmZkNvb
  mZpZxIWLnB1bHVtaXJwYy5EaWZmUmVxdWVzdBoXLnB1bHVtaXJwYy5EaWZmUmVzcG9uc2UiABJICglDb25maWd1cmUSGy5wdWx1b
  WlycGMuQ29uZmlndXJlUmVxdWVzdBocLnB1bHVtaXJwYy5Db25maWd1cmVSZXNwb25zZSIAEj8KBkludm9rZRIYLnB1bHVtaXJwY
  y5JbnZva2VSZXF1ZXN0GhkucHVsdW1pcnBjLkludm9rZVJlc3BvbnNlIgASRwoMU3RyZWFtSW52b2tlEhgucHVsdW1pcnBjLklud
  m9rZVJlcXVlc3QaGS5wdWx1bWlycGMuSW52b2tlUmVzcG9uc2UiADABEjkKBENhbGwSFi5wdWx1bWlycGMuQ2FsbFJlcXVlc3QaF
  y5wdWx1bWlycGMuQ2FsbFJlc3BvbnNlIgASPAoFQ2hlY2sSFy5wdWx1bWlycGMuQ2hlY2tSZXF1ZXN0GhgucHVsdW1pcnBjLkNoZ
  WNrUmVzcG9uc2UiABI5CgREaWZmEhYucHVsdW1pcnBjLkRpZmZSZXF1ZXN0GhcucHVsdW1pcnBjLkRpZmZSZXNwb25zZSIAEj8KB
  kNyZWF0ZRIYLnB1bHVtaXJwYy5DcmVhdGVSZXF1ZXN0GhkucHVsdW1pcnBjLkNyZWF0ZVJlc3BvbnNlIgASOQoEUmVhZBIWLnB1b
  HVtaXJwYy5SZWFkUmVxdWVzdBoXLnB1bHVtaXJwYy5SZWFkUmVzcG9uc2UiABI/CgZVcGRhdGUSGC5wdWx1bWlycGMuVXBkYXRlU
  mVxdWVzdBoZLnB1bHVtaXJwYy5VcGRhdGVSZXNwb25zZSIAEjwKBkRlbGV0ZRIYLnB1bHVtaXJwYy5EZWxldGVSZXF1ZXN0GhYuZ
  29vZ2xlLnByb3RvYnVmLkVtcHR5IgASSAoJQ29uc3RydWN0EhsucHVsdW1pcnBjLkNvbnN0cnVjdFJlcXVlc3QaHC5wdWx1bWlyc
  GMuQ29uc3RydWN0UmVzcG9uc2UiABI6CgZDYW5jZWwSFi5nb29nbGUucHJvdG9idWYuRW1wdHkaFi5nb29nbGUucHJvdG9idWYuR
  W1wdHkiABJACg1HZXRQbHVnaW5JbmZvEhYuZ29vZ2xlLnByb3RvYnVmLkVtcHR5GhUucHVsdW1pcnBjLlBsdWdpbkluZm8iABI7C
  gZBdHRhY2gSFy5wdWx1bWlycGMuUGx1Z2luQXR0YWNoGhYuZ29vZ2xlLnByb3RvYnVmLkVtcHR5IgASSwoKR2V0TWFwcGluZxIcL
  nB1bHVtaXJwYy5HZXRNYXBwaW5nUmVxdWVzdBodLnB1bHVtaXJwYy5HZXRNYXBwaW5nUmVzcG9uc2UiABJOCgtHZXRNYXBwaW5nc
  xIdLnB1bHVtaXJwYy5HZXRNYXBwaW5nc1JlcXVlc3QaHi5wdWx1bWlycGMuR2V0TWFwcGluZ3NSZXNwb25zZSIAQjRaMmdpdGh1Y
  i5jb20vcHVsdW1pL3B1bHVtaS9zZGsvdjMvcHJvdG8vZ287cHVsdW1pcnBjYgZwcm90bzM="""
      ).mkString)
  lazy val scalaDescriptor: _root_.scalapb.descriptors.FileDescriptor = {
    val scalaProto = com.google.protobuf.descriptor.FileDescriptorProto.parseFrom(ProtoBytes)
    _root_.scalapb.descriptors.FileDescriptor.buildFrom(scalaProto, dependencies.map(_.scalaDescriptor))
  }
  lazy val javaDescriptor: com.google.protobuf.Descriptors.FileDescriptor = {
    val javaProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.parseFrom(ProtoBytes)
    com.google.protobuf.Descriptors.FileDescriptor.buildFrom(javaProto, _root_.scala.Array(
      pulumirpc.plugin.PluginProto.javaDescriptor,
      com.google.protobuf.empty.EmptyProto.javaDescriptor,
      com.google.protobuf.struct.StructProto.javaDescriptor,
      pulumirpc.source.SourceProto.javaDescriptor
    ))
  }
  @deprecated("Use javaDescriptor instead. In a future version this will refer to scalaDescriptor.", "ScalaPB 0.5.47")
  def descriptor: com.google.protobuf.Descriptors.FileDescriptor = javaDescriptor
}