package besom.internal

import java.util.concurrent.ConcurrentHashMap

/** A memoization utility that caches the results of a side-effecting computation based on the type and name of the resource for which said
  * computation is being performed. This is necessary in order to prevent runtime crashes caused by multiple evaluations of resource
  * constructors that perform gRPC calls to the Pulumi engine. Pulumi engine is not idempotent on register resource, register resource
  * outputs and read resource calls (at least for these calls we do know it will crash if called multiple times).
  *
  * We want to memoize as little as possible because:
  *
  * a) memoizing too much can lead to surprising behavior for end user (e.g. for calls that can return different results on each invocation)
  * b) we don't have a sane way to use WeakConcurrentMap here because we can't control the lifecycle of the keys due to the lazy nature of
  * Result monad, on each evaluation of the resource constructor referencing effect keys will be new instances of String and therefore not
  * stable identifiers for use in WeakConcurrentMap. This in turn means this map WILL grow indefinitely and we can't do anything about it.
  * It will get deallocated when Context that holds it is deallocated so effectively it's bounded by the lifetime of infrastructural
  * program.
  *
  * This memoization utility uses ConcurrentHashMap to handle concurrent access to the cache on per-key level (concurrent access *does
  * indeed happen* due to the nature of Pulumi SDKs that are parallel by default; quick example - Output referring to a resource constructor
  * is passed to args of two other resource constructors, these resource constructors are evaluated in parallel on different threads to
  * resolve their dependencies for serialization).
  *
  * Mechanics of memoization are quite simple: we put a Promise into the map for a given key using atomic putIfAbsent operation. If the key
  * is not present in the map, we evaluate the effect and fulfill the promise with the result of the effect. If the key is present in the
  * map, we return the existing promise and wait for it to be fulfilled by the effect. This way we ensure that the effect is evaluated only
  * once for a given key.
  *
  * @param chm
  *   ConcurrentHashMap using tuple of type and name as a key and Promise as a value.
  */
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
