package besom.internal

trait Zippable[-A, -B]:
  type Out
  def zip(left: A, right: B): Out

object Zippable extends ZippableLowPrio:
  given append[A <: Tuple, B]: (Zippable[A, B] { type Out = Tuple.Append[A, B] }) = new Zippable[A, B] {
    type Out = Tuple.Append[A, B]
    def zip(left: A, right: B): Out = left :* right
  }
  // TODO requires backport of https://github.com/scala/scala3/pull/20092 to 3.3.x LTS branch
  // (left, right) => left :* right

trait ZippableLowPrio:
  given pair[A, B]: (Zippable[A, B] { type Out = (A, B) }) = new Zippable[A, B] {
    type Out = (A, B)
    def zip(left: A, right: B): Out = (left, right)
  }
  // TODO requires backport of https://github.com/scala/scala3/pull/20092 to 3.3.x LTS branch
  // (left, right) => (left, right)
