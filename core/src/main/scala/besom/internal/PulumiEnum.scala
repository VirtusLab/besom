package besom.internal

sealed trait PulumiEnum:
  def name: String

trait StringEnum extends PulumiEnum:
  def value: String

trait EnumCompanion[E <: PulumiEnum](enumName: String):
  def allInstances: Seq[E]
  private lazy val namesToInstances: Map[String, E] = allInstances.map(instance => instance.name -> instance).toMap

  given Encoder[E] = summon[Encoder[String]].contramap(instance => instance.name)
  given Decoder[E] = summon[Decoder[String]].emap { (name, label) =>
    namesToInstances.get(name).toRight(DecodingError(s"$label: `${name}` is not a valid name of `${enumName}`"))
  }
