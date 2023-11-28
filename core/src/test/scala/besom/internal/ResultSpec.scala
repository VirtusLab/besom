package besom.internal

import scala.util.Try
import RunResult.*

trait ResultSpec[F[+_]: RunResult] extends munit.FunSuite:

  test("left identity") {
    val a                        = 23
    val f: Int => Result[String] = i => Result(s"$i")

    val lhs = Result.pure(a).flatMap(f).unsafeRunSync()
    val rhs = f(a).unsafeRunSync()

    assert(lhs == rhs)
  }

  test("right identity") {
    val m   = Result.pure(23)
    val lhs = m.flatMap(Result(_)).unsafeRunSync()
    val rhs = m.unsafeRunSync()

    assert(lhs == rhs)
  }

  test("associativity") {
    val m                        = Result(23)
    val f: Int => Result[String] = i => Result(s"$i")
    val g: String => Result[Int] = s => Result.evalTry(Try(s.toInt))

    val lhs = m.flatMap(f).flatMap(g).unsafeRunSync()
    val rhs = m.flatMap(s => f(s).flatMap(g)).unsafeRunSync()

    assert(lhs == rhs)
  }

  // flaky
  // test("forking") {
  //   def getThreadId = Thread.currentThread().getId()
  //   def forked(ref: Ref[Set[Long]], done: Promise[Unit]): Result[Unit] =
  //     for
  //       forkedThreadId <- Result.defer(getThreadId).tap(id => Result.defer(println(s"forked thread $id")))
  //       _              <- ref.update(s => s + forkedThreadId)
  //       _              <- done.fulfill(())
  //     yield ()

  //   val program = Ref[Set[Long]](Set.empty[Long]).flatMap { ref =>
  //     for
  //       mainThreadId <- Result.defer(getThreadId).tap(id => Result.defer(println(s"main thread $id")))
  //       _            <- ref.update(s => s + mainThreadId)
  //       promise      <- Promise[Unit]
  //       _            <- forked(ref, promise).fork
  //       _            <- promise.get
  //       s            <- ref.get
  //     yield s
  //   }

  //   val set          = run(program)
  //   val expectedSize = 2
  //   val finalSize    = set.size
  //   assert(finalSize == expectedSize, s"Got $finalSize when expected $expectedSize")
  // }

  // this hangs if tasks didn't go through some kind of trampoline
  // there's no easy way to make this test fail, sadly
  test("inter-locking forking") {
    def interlock(p1: Promise[Unit], p2: Promise[Unit]): Result[Unit] =
      for
        _ <- p1.fulfill(())
        _ <- p2.get
      yield ()

    val program =
      for
        p1   <- Promise[Unit]()
        p2   <- Promise[Unit]()
        fib1 <- interlock(p1, p2).fork
        fib2 <- interlock(p2, p1).fork
        _    <- fib1.join
        _    <- fib2.join
      yield ()

    program.unsafeRunSync()
  }

  // this is flaky because it's possible that the forked threads will finish before we get to wg.waitForAll
  // inverse of the above is also true, forked jobs can be scheduled after we get to wg.waitForAll
  // and then we finish before they start, correct solution would be for WorkGroup to acquire on calling thread
  // and only after acquiring, fork the passed in task. sadly, that's not how runInWorkGroup is implemented.
  test("workgroup allows to wait until all tasks complete") {
    def spawnTasks(wg: WorkGroup, ref: Ref[Int]): Result[Unit] =
      Result.sequence {
        (1 to 30).map { idx =>
          val napTime = scala.util.Random.between(50, 100)
          wg.runInWorkGroup {
            Result.sleep(napTime).tap(_ => ref.update(i => i + idx))
          }.fork
        }.toVector
      }.void

    val program =
      for
        wg         <- WorkGroup()
        ref        <- Ref[Int](0)
        _          <- spawnTasks(wg, ref)
        pendingRes <- ref.get
        _          <- wg.waitForAll
        finalRes   <- ref.get
      yield (pendingRes, finalRes)

    val (pending, finalResult) = program.unsafeRunSync()

    val expectedResult = (1 to 30).sum

    assert(pending != expectedResult, s"Expected 0 while pending, got $pending")
    assert(finalResult == expectedResult, s"Expected $expectedResult as final result, got $finalResult")
  }

  // more of an interactive test
  // test("sleep works as intended") {
  //   val program =
  //     Result
  //       .defer(println("before sleep"))
  //       .flatMap(_ => Result.sleep(3000L))
  //       .tap(_ => Result.defer(println("after sleep")))

  //   run(program)
  // }

  test("bracket works in correct order") {
    val program =
      for
        ref <- Ref[String]("")
        append = (s: String) => ref.update(_ + s)
        _ <- Result.bracket(append("1a"))(_ => append("1r")) { _ =>
          Result.bracket(append("2a"))(_ => append("2r")) { _ =>
            Result.bracket(append("3a"))(_ => append("3r"))(_ => append("use"))
          }
        }
        res <- ref.get
      yield res

    val lhs = program.unsafeRunSync()

    assertEquals(lhs, "1a2a3ause3r2r1r")
  }

  test("bracket works in case of failure") {
    val program =
      for
        ref <- Ref[String]("")
        append = (s: String) => ref.update(_ + s)
        _ <- Result
          .bracket(append("a"))(_ => append("r")) { _ =>
            Result.fail(new RuntimeException("Oopsy daisy"))
          }
          .either
        res <- ref.get
      yield res

    val lhs = program.unsafeRunSync()

    assertEquals(lhs, "ar")
  }

  test("scoped and resource works with a single resource") {
    val program =
      for
        ref <- Ref[String]("")
        append = (s: String) => ref.update(_ + s)
        _ <- Result.scoped {
          for
            _ <- Result.resource(append("a"))(_ => append("r"))
            _ <- append("use")
          yield ()
        }
        res <- ref.get
      yield res

    val lhs = program.unsafeRunSync()

    assertEquals(lhs, "auser")
  }

  test("scoped and resource works in correct order") {
    val program =
      for
        ref <- Ref[String]("")
        append = (s: String) => ref.update(_ + s)
        _ <- Result.scoped {
          for
            _ <- Result.resource(append("1a"))(_ => append("1r"))
            _ <- Result.resource(append("2a"))(_ => append("2r"))
            _ <- Result.resource(append("3a"))(_ => append("3r"))
            _ <- append("use")
          yield ()
        }
        res <- ref.get
      yield res

    val lhs = program.unsafeRunSync()

    assertEquals(lhs, "1a2a3ause3r2r1r")
  }

  test("scoped and resource works in case of failure") {
    val program =
      for
        ref <- Ref[String]("")
        append = (s: String) => ref.update(_ + s)
        _ <- Result.scoped {
          for
            _ <- Result.resource(append("a"))(_ => append("r"))
            _ <- Result.fail(new RuntimeException("Oopsy daisy"))
          yield ()
        }.either
        res <- ref.get
      yield res

    val lhs = program.unsafeRunSync()

    assertEquals(lhs, "ar")
  }
end ResultSpec

  test("multiple evaluations of sequence work correctly") {
    val seq        = Result.sequence(List(Result("value"), Result("value2")))
    val firstEval  = seq.unsafeRunSync()
    val secondEval = seq.unsafeRunSync()

    assertEquals(firstEval, secondEval)
  }

end ResultSpec

// TODO test laziness of operators (for Future mostly) somehow
// TODO zip should be probably parallelised
// TODO test cancellation doesn't break anything for product, fork etc
// TODO test that forking never swallows errors (ZIO.die / fiber failure is caught mostly)
// TODO
// TODO
// TODO
// TODO
