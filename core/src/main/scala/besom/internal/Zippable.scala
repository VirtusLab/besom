package besom.internal

import scala.util.Try
import scala.concurrent.Future

trait Zippable[-A, -B]:
  type Out
  def zip(left: A, right: B): Out

object Zippable extends ZippableLowPrio:
  given append[A <: Tuple, B]: (Zippable[A, B] { type Out = Tuple.Append[A, B] }) =
    (left, right) => left :* right

trait ZippableLowPrio:
  given pair[A, B]: (Zippable[A, B] { type Out = (A, B) }) =
    (left, right) => (left, right)
