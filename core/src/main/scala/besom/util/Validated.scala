package besom.util

import besom.internal.Zippable

enum Validated[+E, +A]:
  case Valid(a: A) extends Validated[E, A]
  case Invalid(e: NonEmptyVector[E]) extends Validated[E, A]

  def zip[EE >: E, B](vb: Validated[EE, B])(using z: Zippable[A, B]): Validated[EE, z.Out] =
    this.zipWith(vb)(z.zip)

  def zipWith[EE >: E, B, C](vb: Validated[EE, B])(f: (A, B) => C): Validated[EE, C] =
    (this, vb) match
      case (Valid(a), Valid(b))       => Valid(f(a, b))
      case (Invalid(e1), Invalid(e2)) => Invalid(e1 append e2)
      case (i @ Invalid(_), _)        => i.asInstanceOf[Validated[EE, C]]
      case (_, i @ Invalid(_))        => i.asInstanceOf[Validated[EE, C]]

  def map[B](f: A => B): Validated[E, B] =
    this match
      case Valid(a)     => Valid(f(a))
      case i@Invalid(_) => i.asInstanceOf[Validated[E, B]]

  def flatMap[EE >: E, B](f: A => Validated[EE, B]): Validated[EE, B] =
    this match
      case Valid(a)     => f(a)
      case i@Invalid(_) => i.asInstanceOf[Validated[EE, B]]

  def filterOrError[EE >: E](p: A => Boolean)(e: EE): Validated[EE, A] =
    this match
      case Valid(a) if p(a) => Valid(a)
      case _                => Validated.invalid(e)

  def getOrElse[B >: A](default: => B): B =
    this match
      case Valid(a)   => a
      case Invalid(_) => default

  def orElse[EE >: E, B >: A](that: => Validated[EE, B]): Validated[EE, B] =
    this match
      case Valid(a)   => Valid(a)
      case Invalid(_) => that

  def lmap[EE](f: E => EE): Validated[EE, A] =
    this match
      case Valid(a)   => Valid(a)
      case Invalid(e) => Invalid(e.map(f))

extension [A](a: A)
  def valid: Validated[Nothing, A] = Validated.valid(a)

extension [E](e: E)
  def invalid: Validated[E, Nothing] = Validated.invalid(e)

extension [E, A](e: Either[E, A])
  def toValidated: Validated[E, A] =
    e.fold(_.invalid, _.valid)

extension [A](a: Option[A])
  def toValidatedOrError[E](e: => E): Validated[E, A] =
    a.fold(e.invalid)(_.valid)

extension [A](vec: Vector[A])
  def traverseV[E, B](f: A => Validated[E, B]): Validated[E, Vector[B]] =
    vec.foldLeft[Validated[E, Vector[B]]](Vector.empty[B].valid) { (acc, a) =>
      acc.zipWith(f(a))(_ :+ _)
    }

extension [E, A](vec: Vector[Validated[E, A]])
  def sequenceV: Validated[E, Vector[A]] =
    vec.traverseV(identity)

object Validated:
  def valid[A](a: A): Validated[Nothing, A]           = Valid(a)
  def invalid[E](e: E, es: E*): Validated[E, Nothing] = Invalid(NonEmptyVector(e, es: _*))

final case class NonEmptyVector[+A](head: A, tail: Vector[A]):
  def append[B >: A](other: NonEmptyVector[B]): NonEmptyVector[B] =
    NonEmptyVector(head, tail ++ other.toVector)
  def toVector: Vector[A] = head +: tail
  def map[B](f: A => B): NonEmptyVector[B] =
    NonEmptyVector(f(head), tail.map(f))

object NonEmptyVector:
  def apply[A](head: A, tail: A*): NonEmptyVector[A] = NonEmptyVector(head, tail.toVector)
