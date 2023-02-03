package besom.util

import scala.reflect.Typeable

// TODO this should not be here
enum Protocol:
  case TCP
  case UDP

// TODO probably not necessary
def dispatchUnion[A: Typeable, B: Typeable](ab: A | B): Either[A, B] = ab match
  case a: A => Left(a)
  case b: B => Right(b)

type ->[A, B] = (A, B)

object -> {
  def unapply[A, B](t: (A, B)): Some[(A, B)] = Some(t)
}
