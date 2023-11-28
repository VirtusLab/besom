package besom.util

opaque type NonEmptySet[A] <: Set[A] = Set[A]

object NonEmptySet:
  def apply[A](elems: Iterable[A]): Option[NonEmptySet[A]] =
    if elems.isEmpty then None else Some(Set.from(elems))

  def apply[A](head: A, tail: A*): NonEmptySet[A] =
    Set(head) ++ Set(tail: _*)
