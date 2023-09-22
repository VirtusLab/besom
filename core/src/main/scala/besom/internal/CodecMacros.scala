package besom.internal

import scala.compiletime.*
object CodecMacros:
  inline def summonLabels[A <: Tuple]: List[String] =
    val constTuple = constValueTuple[A]
    val ev         = summonInline[Tuple.Union[constTuple.type] <:< String]
    ev.substituteCo(constTuple.toList)
  private inline def summonTCList[A <: Tuple, TC[_]] =
    val summoned = summonAll[Tuple.Map[A,TC]]
    summoned.toList

  inline def summonDecoders[A <: Tuple]: List[Decoder[?]]         = summonTCList[A, Decoder].asInstanceOf[List[Decoder[?]]]
  inline def summonEncoders[A <: Tuple]: List[Encoder[?]]         = summonTCList[A, Encoder].asInstanceOf[List[Encoder[?]]]
  inline def summonJsonEncoders[A <: Tuple]: List[JsonEncoder[?]] = summonTCList[A, JsonEncoder].asInstanceOf[List[JsonEncoder[?]]]
