package besom.cfg.internal

import scala.deriving.*
import scala.compiletime.*

trait ConfiguredType[A]:
  def toFieldType: FieldType

object ConfiguredType:
  inline def summonLabels[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) =>
        summonInline[ValueOf[t]].value.asInstanceOf[String] :: summonLabels[ts]

  inline def summonAllInstances[A <: Tuple]: List[ConfiguredType[_]] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[ConfiguredType[t]] :: summonAllInstances[ts]

  given ConfiguredType[Int] with
    def toFieldType = FieldType.Int

  given ConfiguredType[Long] with
    def toFieldType = FieldType.Long

  given ConfiguredType[Float] with
    def toFieldType = FieldType.Float

  given ConfiguredType[Double] with
    def toFieldType = FieldType.Double

  given ConfiguredType[String] with
    def toFieldType = FieldType.String

  given ConfiguredType[Boolean] with
    def toFieldType = FieldType.Boolean

  given [A: ConfiguredType]: ConfiguredType[List[A]] with
    def toFieldType = FieldType.Array(summon[ConfiguredType[A]].toFieldType)

  // support for Option is not yet implemented completely
  // given [A: ConfiguredType]: ConfiguredType[Option[A]] with
  //   def toFieldType = FieldType.Optional(summon[ConfiguredType[A]].toFieldType)

  def buildConfiguredTypeFor[A](instances: => List[ConfiguredType[_]], labels: => List[String]): ConfiguredType[A] =
    new ConfiguredType[A]:
      def toFieldType = FieldType.Struct(labels.zip(instances.map(_.toFieldType)): _*)

  inline given derived[A <: Product](using m: Mirror.ProductOf[A]): ConfiguredType[A] =
    lazy val elemInstances = summonAllInstances[m.MirroredElemTypes]
    lazy val elemLabels = summonLabels[m.MirroredElemLabels]
    buildConfiguredTypeFor[A](elemInstances, elemLabels)

end ConfiguredType
