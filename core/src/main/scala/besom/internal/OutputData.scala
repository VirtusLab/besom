package besom.internal

import scala.collection.BuildFrom

enum OutputData[+A]:
  case Unknown(resources: Set[Resource], isSecret: Boolean) extends OutputData[Nothing]
  case Known(resources: Set[Resource], isSecret: Boolean, value: Option[A])
  private[internal] case Broken(errors: List[DecodingError]) extends OutputData[Nothing]

  def secret: Boolean =
    this match
      case Unknown(resources, isSecret)      => isSecret
      case Known(resources, isSecret, value) => isSecret
      case Broken(error)                     => false

  def getResources: Set[Resource] =
    this match
      case Unknown(resources, isSecret)      => resources
      case Known(resources, isSecret, value) => resources
      case Broken(error)                     => Set.empty

  def map[B](f: A => B): OutputData[B] =
    this match
      case u @ Unknown(_, _)                 => u
      case Known(resources, isSecret, value) => Known(resources, isSecret, value.map(f))
      case b: Broken                         => b

  def flatMap[B](f: A => OutputData[B]): OutputData[B] =
    this match
      case u @ Unknown(_, _) => u
      case k @ Known(resources, isSecret, value) =>
        value match
          case None        => k.asInstanceOf[OutputData[B]]
          case Some(value) => combine(f(value), (_, r) => r)
      case b: Broken => b

  def flatten[B](using A <:< OutputData[B]): OutputData[B] = flatMap(identity)

  def optional: OutputData[Option[A]] =
    this match
      case u @ Unknown(_, _)                 => u.asInstanceOf[OutputData[Option[A]]]
      case Known(resources, isSecret, value) => Known(resources, isSecret, Some(value))
      case b: Broken                         => b.asInstanceOf[OutputData[Option[A]]]

  def orElse[B >: A](that: => OutputData[B]): OutputData[B] =
    this match
      // TODO this is quite a quirky idea - it changes the semantics between preview and deployment, probably not a good idea
      case Unknown(resources, isSecret) => combine(that, (_, r) => r)
      case k @ Known(_, _, _)           => k // TODO: this ignores the fact that Known can be empty
      case b: Broken                    => combine(that, (_, r) => r) // the lambda will not be used

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

      case (Broken(errors), Broken(otherErrors)) =>
        Broken(errors ++ otherErrors)
      case (_: Broken, other) =>
        other.asInstanceOf[OutputData[C]]
      case (ths, _: Broken) =>
        ths.asInstanceOf[OutputData[C]]
      case (_, _) =>
        Unknown(combinedResources, combinedSecret)

  def withDependencies(resources: Set[Resource]): OutputData[A] =
    resources.foldLeft(this)((acc, curr) => acc.withDependency(curr))

  def withDependency(resource: Resource): OutputData[A] =
    this match
      case Unknown(resources, isSecret)      => Unknown(resources + resource, isSecret)
      case Known(resources, isSecret, value) => Known(resources + resource, isSecret, value)
      case Broken(errors)                    => Broken(errors)

  def withIsSecret(isSecret: Boolean): OutputData[A] =
    this match
      case Unknown(resources, _)      => Unknown(resources, isSecret)
      case Known(resources, _, value) => Known(resources, isSecret, value)
      case Broken(errors)             => Broken(errors)

  def traverseResult[B](f: A => Result[B]): Result[OutputData[B]] =
    this match
      case u @ Unknown(_, _)                       => Result.pure(u)
      case k @ Known(resources, isSecret, None)    => Result.pure(k.asInstanceOf[OutputData[B]])
      case Known(resources, isSecret, Some(value)) => f(value).map(b => Known(resources, isSecret, Some(b)))
      case b: Broken                               => Result.pure(b.asInstanceOf[OutputData[B]]) // should it be fail?

  def isEmpty: Boolean =
    this match
      case Unknown(_, _)         => true
      case Known(_, _, optValue) => optValue.isEmpty
      case Broken(_)             => true

  private[internal] def getValue: Option[A] =
    this match
      case Unknown(_, _)         => None
      case Known(_, _, optValue) => optValue
      case Broken(_)             => None

  private[internal] def getValueOrElse[B >: A](default: => B): B =
    this match
      case Unknown(_, _)         => default
      case Known(_, _, optValue) => optValue.getOrElse(default)
      case Broken(_)             => default

object OutputData:
  def unknown(isSecret: Boolean = false): OutputData[Nothing] = Unknown(Set.empty, isSecret)

  private[internal] def broken(errors: List[DecodingError]): OutputData[Nothing] = Broken(errors)

  def apply[A](resources: Set[Resource], value: Option[A], isSecret: Boolean): OutputData[A] =
    Known(resources, isSecret, value)

  def apply[A](value: A, resources: Set[Resource] = Set.empty, isSecret: Boolean = false): OutputData[A] =
    apply(resources, Some(value), isSecret)

  def empty[A](resources: Set[Resource] = Set.empty, isSecret: Boolean = false): OutputData[A] =
    Known(resources, isSecret, None)

  def traverseResult[A](using ctx: Context)(value: => Result[A]): Result[OutputData[A]] =
    value.map(OutputData.apply(_))

  def sequence[A, CC[X] <: IterableOnce[X], To](
    coll: CC[OutputData[A]]
  )(using bf: BuildFrom[CC[OutputData[A]], A, To]): OutputData[To] =
    coll.iterator
      .foldLeft(OutputData(bf.newBuilder(coll))) { (acc, curr) =>
        acc.zip(curr).map { case (b, r) => b += r }
      }
      .map(_.result())
