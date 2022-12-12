trait Zippable[-A, -B] {
  type Out
  def zip(left: A, right: B): Out
}

object Zippable extends ZippableLowPrio:
  given append[A <: Tuple, B]: (Zippable[A, B] { type Out = Tuple.Append[A, B] }) =
    (left, right) => left :* right

trait ZippableLowPrio:
  given pair[A, B]: (Zippable[A, B] { type Out = (A, B) }) =
    (left, right) => (left, right)

def zip[A, B](a: A, b: B)(using z: Zippable[A, B]) = z.zip(a, b)

object Test:
  val x1: (Int, Int)           = zip(1, 2)
  val x2: (Int, Int, Int)      = zip((1, 2), 3)
  val x3: (Int, Int, Int, Int) = zip((1, 2, 3), 4)
