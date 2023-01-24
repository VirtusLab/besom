package besom.internal

import scala.collection.BuildFrom

trait Resource

enum OutputData[+A]:
  case Unknown(resources: Set[Resource], isSecret: Boolean) extends OutputData[Nothing]
  case Known(resources: Set[Resource], isSecret: Boolean, value: Option[A])

  def secret: Boolean =
    this match
      case Unknown(resources, isSecret)      => isSecret
      case Known(resources, isSecret, value) => isSecret

  def getResources: Set[Resource] =
    this match
      case Unknown(resources, isSecret)      => resources
      case Known(resources, isSecret, value) => resources

  def map[B](f: A => B): OutputData[B] =
    this match
      case u @ Unknown(_, _)                 => u
      case Known(resources, isSecret, value) => Known(resources, isSecret, value.map(f))

  def flatMap[B](f: A => OutputData[B]): OutputData[B] =
    this match
      case u @ Unknown(_, _) => u
      case k @ Known(resources, isSecret, value) =>
        value match
          case None        => k.asInstanceOf[OutputData[B]]
          case Some(value) => combine(f(value), (_, r) => r)

  def flatten[B](using A <:< OutputData[B]): OutputData[B] = flatMap(identity)

  def optional: OutputData[Option[A]] =
    this match
      case u @ Unknown(_, _)                 => u.asInstanceOf[OutputData[Option[A]]]
      case Known(resources, isSecret, value) => Known(resources, isSecret, Some(value))

  def orElse[B >: A](that: => OutputData[B]): OutputData[B] =
    this match
      case Unknown(resources, isSecret) => combine(that, (_, r) => r)
      case k @ Known(_, _, _)           => k

  def zip[B](that: OutputData[B])(using z: Zippable[A, B]): OutputData[z.Out] = // OutputData[(A, B)]
    combine(that, (a, b) => z.zip(a, b))

  def combine[B, C](that: OutputData[B], combine: (A, B) => C): OutputData[C] =
    val combinedResources = this.getResources ++ that.getResources
    val combinedSecret    = this.secret || that.secret
    (this, that) match
      case (Known(_, _, optThisValue), Known(_, _, optThatValue)) =>
        val combinedValue =
          for
            thisValue <- optThisValue
            thatValue <- optThatValue
          yield combine(thisValue, thatValue)

        Known(combinedResources, combinedSecret, combinedValue)

      case (_, _) =>
        Unknown(combinedResources, combinedSecret)

  def withDependency(resource: Resource): OutputData[A] =
    this match
      case Unknown(resources, isSecret)      => Unknown(resources + resource, isSecret)
      case Known(resources, isSecret, value) => Known(resources + resource, isSecret, value)

  def withIsSecret(isSecret: Boolean): OutputData[A] =
    this match
      case Unknown(resources, _)      => Unknown(resources, isSecret)
      case Known(resources, _, value) => Known(resources, isSecret, value)

  def traverseResult[B](using ctx: Context)(f: A => Result[B]): Result[OutputData[B]] =
    this match
      case u @ Unknown(_, _)                       => Result.pure(u)
      case k @ Known(resources, isSecret, None)    => Result.pure(k.asInstanceOf[OutputData[B]])
      case Known(resources, isSecret, Some(value)) => f(value).map(b => Known(resources, isSecret, Some(b)))

  def isEmpty: Boolean =
    this match
      case Unknown(_, _)         => true
      case Known(_, _, optValue) => optValue.isEmpty

object OutputData:
  def unknown(isSecret: Boolean = false): OutputData[Nothing] = Unknown(Set.empty, isSecret)

  def apply[A](resources: Set[Resource], value: Option[A], isSecret: Boolean): OutputData[A] =
    Known(resources, isSecret, value)

  def apply[A](value: A, resources: Set[Resource] = Set.empty, isSecret: Boolean = false): OutputData[A] =
    apply(resources, Some(value), isSecret)

  def empty[A](resources: Set[Resource] = Set.empty, isSecret: Boolean = false): OutputData[A] =
    Known(resources, isSecret, None)

  def traverseResult[A](using ctx: Context)(value: => Result[A]): Result[OutputData[A]] =
    empty[A]().traverseResult(_ => value)

  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[OutputData[A]]
  )(using bf: BuildFrom[CC[OutputData[A]], A, To]): OutputData[To] =
    coll.iterator
      .foldLeft(OutputData(bf.newBuilder(coll))) { (acc, curr) =>
        acc.zip(curr).map { case (b, r) => b += r }
      }
      .map(_.result())
