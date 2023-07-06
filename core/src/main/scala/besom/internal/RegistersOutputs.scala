package besom.internal

trait RegistersOutputs[A <: ComponentResource & Product]:
  def toMapOfOutputs(a: A): Map[String, (Encoder[?], Output[Any])]

object RegistersOutputs:
  def apply[A <: ComponentResource & Product](using ro: RegistersOutputs[A]): RegistersOutputs[A] = ro
