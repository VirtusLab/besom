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
      case Valid(a)       => Valid(f(a))
      case i @ Invalid(_) => i.asInstanceOf[Validated[E, B]]

  def flatMap[EE >: E, B](f: A => Validated[EE, B]): Validated[EE, B] =
    this match
      case Valid(a)       => f(a)
      case i @ Invalid(_) => i.asInstanceOf[Validated[EE, B]]

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
end Validated

extension [A](a: A)
  def valid: Validated[Nothing, A]                       = Validated.valid(a)
  def validResult: Validated.ValidatedResult[Nothing, A] = Validated.ValidatedResult.valid(a)

extension [E](e: E)
  def invalid: Validated[E, Nothing]                       = Validated.invalid(e)
  def invalidResult: Validated.ValidatedResult[E, Nothing] = Validated.ValidatedResult.invalid(e)

extension [E, A](e: Either[E, A])
  def toValidated: Validated[E, A] =
    e.fold(_.invalid, _.valid)
  def toValidatedResult: Validated.ValidatedResult[E, A] =
    e.fold(_.invalidResult, _.validResult)

extension [A](a: Option[A])
  def toValidatedOrError[E](e: => E): Validated[E, A] =
    a.fold(e.invalid)(_.valid)
  def toValidatedResultOrError[E](e: => E): Validated.ValidatedResult[E, A] =
    a.fold[Validated.ValidatedResult[E, A]](e.invalidResult)(_.validResult)

extension [A](vec: Vector[A])
  def traverseV[E, B](f: A => Validated[E, B]): Validated[E, Vector[B]] =
    vec.foldLeft[Validated[E, Vector[B]]](Vector.empty[B].valid) { (acc, a) =>
      acc.zipWith(f(a))(_ :+ _)
    }
  def traverseVR[E, B](f: A => Validated.ValidatedResult[E, B]): Validated.ValidatedResult[E, Vector[B]] =
    vec.foldLeft[Validated.ValidatedResult[E, Vector[B]]](Vector.empty[B].validResult) { (acc, a) =>
      acc.zipWith(f(a))(_ :+ _)
    }

extension [E, A](vec: Vector[Validated[E, A]])
  def sequenceV: Validated[E, Vector[A]] =
    vec.traverseV(identity)

object Validated:
  def valid[A](a: A): Validated[Nothing, A]           = Valid(a)
  def invalid[E](e: E, es: E*): Validated[E, Nothing] = Invalid(NonEmptyVector(e, es: _*))

  import besom.internal.Result

  import Validated.*

  opaque type ValidatedResult[+E, +A] = Result[Validated[E, A]]

  extension [E, A](resultOfValidated: Result[Validated[E, A]]) def asValidatedResult: ValidatedResult[E, A] = resultOfValidated

  extension [E, A](result: ValidatedResult[E, A])
    def zip[EE >: E, B](vb: ValidatedResult[EE, B])(using z: Zippable[A, B]): ValidatedResult[EE, z.Out] =
      result.zipWith(vb)(z.zip)

    def zipWith[EE >: E, B, C](vb: ValidatedResult[EE, B])(f: (A, B) => C): ValidatedResult[EE, C] =
      for
        va <- result
        vb <- vb
      yield va.zipWith(vb)(f)

    def map[B](f: A => B): ValidatedResult[E, B] =
      result.map(_.map(f))

    def flatMap[EE >: E, B](f: A => ValidatedResult[EE, B]): ValidatedResult[EE, B] =
      result.flatMap { va =>
        va match
          case Valid(a)       => f(a)
          case i @ Invalid(_) => Result.pure(i.asInstanceOf[Validated[EE, B]])
      }

    def filterOrError[EE >: E](p: A => Boolean)(e: EE): ValidatedResult[EE, A] =
      result.map(_.filterOrError(p)(e))

    def getOrElse[B >: A](default: => B): Result[B] =
      result.map(_.getOrElse(default))

    def tapBoth(f: NonEmptyVector[E] => Result[Unit], g: A => Result[Unit]): ValidatedResult[E, A] =
      result.flatMap { v =>
        v match
          case Invalid(e) => f(e).map(_ => v)
          case Valid(a)   => g(a).map(_ => v)
      }

    def orElse[EE >: E, B >: A](that: => ValidatedResult[EE, B]): ValidatedResult[EE, B] =
      result.flatMap { va =>
        va match
          case v @ Valid(_)   => Result.pure(v)
          case i @ Invalid(_) => that.map(_.orElse(i))
      }

    def lmap[EE](f: E => EE): ValidatedResult[EE, A] =
      result.map(_.lmap(f))

    def asResult: Result[Validated[E, A]] = result

  end extension

  object ValidatedResult:
    def apply[E, A](result: Result[Validated[E, A]]): ValidatedResult[E, A] = result

    def apply[E, A](validated: Validated[E, A]): ValidatedResult[E, A] = Result.pure(validated).asValidatedResult

    def valid[E, A](a: A): ValidatedResult[E, A] = Result.pure(Validated.valid(a))

    def invalid[E, A](e: E, es: E*): ValidatedResult[E, A] = Result.pure(Validated.invalid(e, es: _*))

    class TransparencyZone[E, A](private val result: Result[Validated[E, A]]):
      def in(zone: Result[Validated[E, A]] => Result[Validated[E, A]]): ValidatedResult[E, A] =
        zone(result).asValidatedResult

    def transparent[E, A](result: ValidatedResult[E, A]): TransparencyZone[E, A] =
      new TransparencyZone(result.asResult)

  end ValidatedResult
end Validated

final case class NonEmptyVector[+A](head: A, tail: Vector[A]):
  def append[B >: A](other: NonEmptyVector[B]): NonEmptyVector[B] =
    NonEmptyVector(head, tail ++ other.toVector)
  def toVector: Vector[A] = head +: tail
  def map[B](f: A => B): NonEmptyVector[B] =
    NonEmptyVector(f(head), tail.map(f))
  def foreach(f: A => Unit): Unit =
    f(head)
    tail.foreach(f)
  def size: Int = 1 + tail.size
  def mkString(start: String, sep: String, end: String): String =
    toVector.mkString(start, sep, end)

object NonEmptyVector:
  def apply[A](head: A, tail: A*): NonEmptyVector[A] = NonEmptyVector(head, tail.toVector)
