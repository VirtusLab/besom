package besom.util

import scala.reflect.Typeable

type ->[A, B] = (A, B)

object -> {
  def unapply[A, B](t: (A, B)): Some[(A, B)] = Some(t)
}
