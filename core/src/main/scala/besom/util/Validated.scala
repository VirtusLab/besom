package besom.util

import besom.internal.Zippable

enum Validated[+E, +A]:
  case Invalid[E](errors: Vector[E]) extends Validated[E, Nothing]
  case Valid[A](value: A) extends Validated[Nothing, A]

  def isValid: Boolean = this match
    case Valid(_) => true
    case _        => false

  def isInvalid: Boolean = !isValid

  def map[B](f: A => B): Validated[E, B] = this match
    case Valid(value) => Valid(f(value))
    case Invalid(err) => Invalid(err)

  def flatMap[EE >: E, B](f: A => Validated[EE, B]): Validated[EE, B] = this match
    case Valid(value) => f(value)
    case Invalid(err) => Invalid(err)

  def zip[EE >: E, B](that: Validated[EE, B])(using z: Zippable[A, B]): Validated[EE, z.Out] =
    (this, that) match
      case (Valid(a), Valid(b)) => Valid(z.zip(a, b))
      case (Invalid(err1), Invalid(err2)) => Invalid(err1 ++ err2)
      case (Invalid(err), _) => Invalid(err)
      case (_, Invalid(err)) => Invalid(err)

  def zipWith[EE >: E, B, C](that: Validated[EE, B])(f: (A, B) => C): Validated[EE, C] =
    zip(that).map(f.tupled)

  def filterOrError[EE >: E](p: A => Boolean, error: => EE): Validated[EE, A] = this match
    case Valid(value) if p(value) => Valid(value)
    case Valid(_) => Invalid(Vector(error))
    case Invalid(err) => Invalid(err)

  def toEither: Either[Vector[E], A] = this match
    case Valid(value) => Right(value)
    case Invalid(err) => Left(err)

  def toOption: Option[A] = this match
    case Valid(value) => Some(value)
    case Invalid(_) => None

object Validated:
  extension [E, A](xs: List[Validated[E, A]])
    def sequence: Validated[E, List[A]] =
      xs.foldLeft(Valid(List.empty[A]): Validated[E, List[A]]) { case (acc, x) =>
        acc.zipWith(x)(_ :+ _)
      }
    
  extension [E, A](xs: List[A])
    def traverse[B](f: A => Validated[E, B]): Validated[E, List[B]] =
      xs.map(f).sequence
