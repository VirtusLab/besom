package besom.util

sealed trait NotProvided
case object NotProvided extends NotProvided

extension [A](v: A | NotProvided)
  def asOption: Option[A] =
    v match
      case NotProvided     => None
      case a: A @unchecked => Some(a)
