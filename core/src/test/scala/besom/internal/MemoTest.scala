package besom.internal

import besom.internal.RunResult.{*, given}

class MemoTest extends munit.FunSuite:

  test("memoization leads to just one evaluation of the memoized effect") {
    val atomicInt = new java.util.concurrent.atomic.AtomicInteger(0)

    val effect = Result(atomicInt.incrementAndGet())

    val memo = Memo().unsafeRunSync()

    val memoizedEffect = memo.memoize("test", "test", effect)

    val program =
      for
        _ <- memoizedEffect
        _ <- memoizedEffect
        _ <- memoizedEffect
      yield ()

    program.unsafeRunSync()

    assertEquals(atomicInt.get(), 1)
  }

  test("memoization is thread safe (multiple threads trying to evaluate memoized effect do not lead to multiple evaluations)") {
    val atomicInt = new java.util.concurrent.atomic.AtomicInteger(0)

    val effect = Result(atomicInt.incrementAndGet())

    val memo = Memo().unsafeRunSync()

    val memoizedEffect = memo.memoize("test", "test", effect)

    val program =
      for
        fibs <- Result.sequence((1 to 100).map(_ => memoizedEffect.fork).toList)
        _    <- Result.sequence(fibs.map(_.join))
      yield ()

    program.unsafeRunSync()

    assertEquals(atomicInt.get(), 1)
  }

  test("memoization works for errors too") {
    val effect = Result.unit.flatMap { _ => Result.fail(new RuntimeException("oh no")) }

    val memo = Memo().unsafeRunSync()

    val memoizedEffect = memo.memoize("test", "test", effect)

    val setOfErrors = scala.collection.mutable.Set.empty[Throwable]

    val program =
      for
        _ <- memoizedEffect.either.map(_.left.foreach(setOfErrors.add))
        _ <- memoizedEffect.either.map(_.left.foreach(setOfErrors.add))
        _ <- memoizedEffect.either.map(_.left.foreach(setOfErrors.add))
      yield ()

    program.unsafeRunSync()

    assertEquals(setOfErrors.size, 1)
  }

  test("memoization works in nested flatMap calls") {
    val atomicInt = new java.util.concurrent.atomic.AtomicInteger(0)

    val memo = Memo().unsafeRunSync()

    // if only keys are the same, memoization should work always
    val memoizedEffect = Result(23).flatMap(_ => memo.memoize("test", "test", Result(atomicInt.incrementAndGet())))

    val program =
      for
        _ <- memoizedEffect
        _ <- memoizedEffect
        _ <- memoizedEffect
      yield ()

    program.unsafeRunSync()

    assertEquals(atomicInt.get(), 1)
  }

  test("memoization works in nested flatMap calls executed on different fibers") {
    val atomicInt = new java.util.concurrent.atomic.AtomicInteger(0)

    val memo = Memo().unsafeRunSync()

    // if only keys are the same, memoization should work always
    val memoizedEffect = Result(23).flatMap(_ => memo.memoize("test", "test", Result(atomicInt.incrementAndGet())))

    val program =
      for
        fib1 <- memoizedEffect.fork
        fib2 <- memoizedEffect.fork
        fib3 <- memoizedEffect.fork
        _    <- Result.sequence(List(fib1, fib2, fib3).map(_.join))
      yield ()

    program.unsafeRunSync()

    assertEquals(atomicInt.get(), 1)
  }

  test("memoization should work for different keys in nested flatMap calls") {
    val atomicInt = new java.util.concurrent.atomic.AtomicInteger(0)

    val memo = Memo().unsafeRunSync()

    // if only keys are the same, memoization should work always
    val memoizedEffect = Result(1 to 3).flatMap { range =>
      Result.sequence(range.map(i => memo.memoize(s"test-$i", "test", Result(atomicInt.incrementAndGet()))))
    }

    val program =
      for
        _ <- memoizedEffect
        _ <- memoizedEffect
        _ <- memoizedEffect
      yield ()

    program.unsafeRunSync()

    assertEquals(atomicInt.get(), 3)
  }
end MemoTest
