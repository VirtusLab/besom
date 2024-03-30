package besom.internal

import java.util.concurrent.ConcurrentHashMap

class Memo(private val chm: ConcurrentHashMap[(String, String), Promise[?]]):
  def memoize[A](typ: String, name: String, effect: Result[A]): Result[A] =
    Promise[A]().flatMap { promise =>
      val existing = chm.putIfAbsent((typ, name), promise)
      if existing == null then
        effect
          .flatMap(promise.fulfill)
          .recover(e => promise.fail(e))
          .flatMap(_ => promise.get)
      else existing.get.asInstanceOf[Result[A]]
    }

object Memo:
  def apply(): Result[Memo] = Result.defer {
    val chm = ConcurrentHashMap[(String, String), Promise[?]]()

    new Memo(chm)
  }
