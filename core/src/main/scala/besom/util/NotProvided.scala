package besom.util

sealed trait NotProvided
case object NotProvided extends NotProvided

opaque type NotProvidedOr[A] >: A | NotProvided = A | NotProvided

// we do not want to infect every type so we need this
object NotProvidedOr:
  given notProvidedOrOps: {} with // keep the scope clean, no free function
    extension [A](npo: NotProvidedOr[A])
      def asOption: Option[A] =
        npo match
          case NotProvided     => None
          case a: A @unchecked => Some(a)
      def asEither: Either[NotProvided, A] =
        npo match
          case NotProvided     => Left(NotProvided)
          case a: A @unchecked => Right(a)
      def asSeq: Seq[A] =
        npo match
          case NotProvided     => Seq.empty
          case a: A @unchecked => Seq(a)

      def map[B](f: A => B): Option[B]                        = asOption.map[B](f)
      def flatMap[B](f: A => Option[B]): Option[B]            = asOption.flatMap[B](f)
      def filter(p: A => Boolean): Option[A]                  = asOption.filter(p)
      def fold[B](ifNotProvided: => B)(f: A => B): B          = asOption.fold(ifNotProvided)(f)
      def bimap[B](ifNotProvided: => B)(f: A => B): Option[B] = asOption.map(f).orElse(Some(ifNotProvided))
