package besom.util

enum Validated[+E, +A]:
  case Valid(a: A) extends Validated[E, A]
  case Invalid(e: NonEmptyVector[E]) extends Validated[E, A]

  def zip[EE >: E, B](vb: Validated[EE, B])(using z: Zippable[A, B]): Validated[EE, z.Out] =
    (this, vb) match
      case (Valid(a), Valid(b))       => Valid(z.zip(a, b))
      case (Invalid(e1), Invalid(e2)) => Invalid(e1 append e2)
      case (i @ Invalid(_), _)        => i.asInstanceOf[Validated[EE, z.Out]]
      case (_, i @ Invalid(_))        => i.asInstanceOf[Validated[EE, z.Out]]

  def map[B](f: A => B): Validated[E, B] =
    this match
      case Valid(a)         => Valid(f(a))
      case i: Invalid[_, _] => i.asInstanceOf[Validated[E, B]]

  def flatMap[EE >: E, B](f: A => Validated[EE, B]): Validated[EE, B] =
    this match
      case Valid(a)         => f(a)
      case i: Invalid[_, _] => i.asInstanceOf[Validated[EE, B]]

object Validated:
  def valid[A](a: A): Validated[Nothing, A]           = Valid(a)
  def invalid[E](e: E, es: E*): Validated[E, Nothing] = Invalid(NonEmptyVector(e, es: _*))

final case class NonEmptyVector[+A](head: A, tail: Vector[A]):
  def append[B >: A](other: NonEmptyVector[B]): NonEmptyVector[B] =
    NonEmptyVector(head, tail ++ other.toVector)
  def toVector: Vector[A] = head +: tail

object NonEmptyVector:
  def apply[A](head: A, tail: A*): NonEmptyVector[A] = NonEmptyVector(head, tail.toVector)
