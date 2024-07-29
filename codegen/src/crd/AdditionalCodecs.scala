package besom.codegen.crd

import scala.meta.*
import scala.meta.dialects.Scala33
import besom.codegen.scalameta.interpolator.*
import besom.codegen.scalameta.ref

enum AdditionalCodecs(val name: Type, val codecs: Seq[Stat]):
  case LocalDateTime
      extends AdditionalCodecs(
        Type.Select(ref("java", "time"), Type.Name("LocalDateTime")),
        Seq(
          m"""
       |  given besom.Encoder[java.time.LocalDateTime] with
       |    def encode(t: java.time.LocalDateTime)(using besom.Context): besom.internal.Result[(besom.internal.Metadata, com.google.protobuf.struct.Value)] =
       |      besom.internal.Encoder.stringEncoder.encode(t.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME))
       |""",
          m"""
       |  given besom.Decoder[java.time.LocalDateTime] with
       |    def mapping(v: com.google.protobuf.struct.Value, label: besom.types.Label): besom.util.Validated[besom.internal.DecodingError, java.time.LocalDateTime] =
       |      besom.internal.Decoder.stringDecoder.mapping(v, label).flatMap(str =>
       |        scala.util.Try(java.time.LocalDateTime.parse(v.getStringValue, java.time.format.DateTimeFormatter.ISO_DATE_TIME)) match
       |          case scala.util.Success(value) =>
       |            besom.util.Validated.valid(value)
       |          case scala.util.Failure(_) =>
       |            besom.util.Validated.invalid(besom.internal.Decoder.error(s"$$label: Expected a LocalDateTime, got: '$${v.kind}'", label))
       |      )
       |"""
        ).map(_.stripMargin.parse[Stat].get)
      )

  case LocalDate
      extends AdditionalCodecs(
        Type.Select(ref("java", "time"), Type.Name("LocalDate")),
        Seq(
          m"""
       |  given besom.Encoder[java.time.LocalDate] with
       |    def encode(t: java.time.LocalDate)(using besom.Context): besom.internal.Result[(besom.internal.Metadata, com.google.protobuf.struct.Value)] =
       |      besom.internal.Encoder.stringEncoder.encode(t.format(java.time.format.DateTimeFormatter.ISO_DATE))
       |""",
          m"""
       |  given besom.Decoder[java.time.LocalDate] with
       |    def mapping(v: com.google.protobuf.struct.Value, label: besom.types.Label): besom.util.Validated[besom.internal.DecodingError, java.time.LocalDate] =
       |      besom.internal.Decoder.stringDecoder.mapping(v, label).flatMap(str =>
       |        scala.util.Try(java.time.LocalDate.parse(v.getStringValue, java.time.format.DateTimeFormatter.ISO_DATE)) match
       |          case scala.util.Success(value) =>
       |            besom.util.Validated.valid(value)
       |          case scala.util.Failure(_) =>
       |            besom.util.Validated.invalid(besom.internal.Decoder.error(s"$$label: Expected a LocalDate, got: '$${v.kind}'", label))
       |     )
       |"""
        ).map(_.stripMargin.parse[Stat].get)
      )
end AdditionalCodecs

object AdditionalCodecs:
  private val nameToValuesMap: Map[String, AdditionalCodecs] =
    AdditionalCodecs.values.map(c => c.name.syntax -> c).toMap

  def getCodec(name: Type): Option[AdditionalCodecs] =
    nameToValuesMap.get(name.syntax)

  private def enumEncoder(enumName: String): Stat =
    m"""
    |  given besom.Encoder[$enumName] with
    |    def encode(e: $enumName)(using besom.Context): besom.internal.Result[(besom.internal.Metadata, com.google.protobuf.struct.Value)] =
    |      besom.internal.Encoder.stringEncoder.encode(e.toString)
    |""".stripMargin.parse[Stat].get

  private def enumDecoder(enumName: String): Stat =
    m"""
    |  given besom.Decoder[$enumName] with
    |    def mapping(v: com.google.protobuf.struct.Value, label: besom.types.Label): besom.util.Validated[besom.internal.DecodingError, $enumName] =
    |      besom.internal.Decoder.stringDecoder.mapping(v, label).flatMap(str =>
    |        scala.util.Try($enumName.valueOf(str)) match
    |          case scala.util.Success(value) =>
    |            besom.util.Validated.valid(value)
    |          case scala.util.Failure(_) =>
    |            besom.util.Validated.invalid(besom.internal.Decoder.error(s"$$label: Expected a $enumName enum, got: '$${v.kind}'", label))
    |      )
    |""".stripMargin.parse[Stat].get

  def enumCodecs(enumName: String): Seq[Stat] = Seq(enumEncoder(enumName), enumDecoder(enumName))
end AdditionalCodecs
