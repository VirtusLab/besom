package besom.cfg

// TODO do not use Option[T], use something with a proper error channel and missing value channel
// TODO rationale: if a value is provided but it's not valid (e.g. empty string for an Int), we want to know
// TODO if a value is missing, but the type is optional in configuration, that's fine
trait FromEnv[A]:
  def decode(env: Map[String, String], path: String): Option[A]

object FromEnv:

  import scala.deriving.*
  import scala.compiletime.{erasedValue, summonInline}

  inline def summonAllInstances[T <: Tuple]: List[FromEnv[?]] =
    inline erasedValue[T] match
      case _: (t *: ts)  => summonInline[FromEnv[t]] :: summonAllInstances[ts]
      case _: EmptyTuple => Nil

  inline def summonLabels[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts) =>
        summonInline[ValueOf[t]].value.asInstanceOf[String] :: summonLabels[ts]

  given [A: FromEnv]: FromEnv[Option[A]] with
    def decode(env: Map[String, String], path: String): Option[Option[A]] =
      Some(summon[FromEnv[A]].decode(env, path))

  given FromEnv[Int] with
    def decode(env: Map[String, String], path: String): Option[Int] =
      env.get(path).flatMap(s => scala.util.Try(s.toInt).toOption)

  given FromEnv[Long] with
    def decode(env: Map[String, String], path: String): Option[Long] =
      env.get(path).flatMap(s => scala.util.Try(s.toLong).toOption)

  given FromEnv[String] with
    def decode(env: Map[String, String], path: String): Option[String] =
      env.get(path)

  given FromEnv[Double] with
    def decode(env: Map[String, String], path: String): Option[Double] =
      env.get(path).flatMap(s => scala.util.Try(s.toDouble).toOption)

  given FromEnv[Float] with
    def decode(env: Map[String, String], path: String): Option[Float] =
      env.get(path).flatMap(s => scala.util.Try(s.toFloat).toOption)

  given FromEnv[Boolean] with
    def decode(env: Map[String, String], path: String): Option[Boolean] =
      env.get(path).flatMap(s => scala.util.Try(s.toBoolean).toOption)

  given [A: FromEnv]: FromEnv[List[A]] with
    def decode(env: Map[String, String], path: String): Option[List[A]] =
      Iterator.from(0).map(i => summon[FromEnv[A]].decode(env, s"$path.$i")).takeWhile(_.isDefined).toList.sequence

  given [A: FromEnv]: FromEnv[Vector[A]] with
    def decode(env: Map[String, String], path: String): Option[Vector[A]] =
      Iterator.from(0).map(i => summon[FromEnv[A]].decode(env, s"$path.$i")).takeWhile(_.isDefined).toVector.sequence

  inline given derived[A](using m: Mirror.ProductOf[A]): FromEnv[A] = new FromEnv[A]:
    def decode(env: Map[String, String], path: String): Option[A] =
      val elemDecoders = summonAllInstances[m.MirroredElemTypes]
      val labels = summonLabels[m.MirroredElemLabels]

      val elemValues = elemDecoders.zip(labels).map { case (decoder, label) =>
        // handle top-level gracefully (empty path)
        decoder.asInstanceOf[FromEnv[Any]].decode(env, if path.isBlank() then label else s"$path.$label")
      }

      if elemValues.forall(_.isDefined) then Some(m.fromProduct(Tuple.fromArray(elemValues.flatten.toArray)))
      else None

  // Helper to sequence a List[Option[A]] into Option[List[A]]
  extension [A](xs: List[Option[A]])
    def sequence: Option[List[A]] = xs.foldRight(Option(List.empty[A])) {
      case (Some(a), Some(acc)) => Some(a :: acc)
      case _                    => None
    }

  // Helper to sequence a Vector[Option[A]] into Option[Vector[A]]
  extension [A](xs: Vector[Option[A]])
    def sequence: Option[Vector[A]] = xs.foldLeft(Option(Vector.empty[A])) {
      case (Some(acc), Some(a)) => Some(acc :+ a)
      case _                    => None
    }
