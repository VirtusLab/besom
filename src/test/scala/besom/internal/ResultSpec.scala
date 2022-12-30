package besom.internal

import scala.concurrent.*, ExecutionContext.Implicits.global, duration.*
import scala.util.Try

class ResultSpec extends munit.FunSuite:

  def run[A](result: Result[A]): A       = Await.result(result.run(using FutureRuntime()), 1.second)
  def sleep(napTime: Long): Result[Unit] = Result.defer(Thread.sleep(napTime))

  test("left identity") {
    val a                        = 23
    val f: Int => Result[String] = i => Result(s"$i")

    val lhs = run(Result.pure(a).flatMap(f))
    val rhs = run(f(a))

    assert(lhs == rhs)
  }

  test("right identity") {
    val m   = Result.pure(23)
    val lhs = run(m.flatMap(Result(_)))
    val rhs = run(m)

    assert(lhs == rhs)
  }

  test("associativity") {
    val m                        = Result(23)
    val f: Int => Result[String] = i => Result(s"$i")
    val g: String => Result[Int] = s => Result.evalTry(Try(s.toInt))

    val lhs = run(m.flatMap(f).flatMap(g))
    val rhs = run(m.flatMap(s => f(s).flatMap(g)))

    assert(lhs == rhs)
  }

  // flaky
  // test("forking") {
  //   def getThreadId = Thread.currentThread().getId()
  //   def forked(ref: Ref[F, Set[Long]], done: Promise[F, Unit]): F[Unit] =
  //     for
  //       forkedThreadId <- F.eval(getThreadId).tap(id => F.eval(println(s"forked thread $id")))
  //       _              <- ref.update(s => s + forkedThreadId)
  //       _              <- done.fulfill(())
  //     yield ()

  //   val program = Ref[F, Set[Long]](Set.empty[Long]).flatMap { ref =>
  //     for
  //       mainThreadId <- F.eval(getThreadId).tap(id => F.eval(println(s"main thread $id")))
  //       _            <- ref.update(s => s + mainThreadId)
  //       promise      <- Promise[F, Unit]
  //       _            <- forked(ref, promise).fork
  //       _            <- promise.get
  //       s            <- ref.get
  //     yield s
  //   }

  //   val set          = run(program).get
  //   val expectedSize = 2
  //   val finalSize    = set.size
  //   assert(finalSize == expectedSize, s"Got $finalSize when expected $expectedSize")
  // }

  // this would hang if tasks didn't go through some kind of trampoline
  // there's no easy way to make this test fail, sadly
  test("inter-locking forking") {
    def interlock(p1: Result.Promise[Unit], p2: Result.Promise[Unit]): Result[Unit] =
      for
        _ <- Result(println(s"fulfilling $p1 and waiting for $p2 on thread ${Thread.currentThread()}"))
        _ <- p1.fulfill(())
        _ <- p2.get
      yield ()

    val program =
      for
        p1 <- Result.Promise[Unit]
        p2 <- Result.Promise[Unit]
        _  <- Result(println(s"Promise1: $p1, Promise2: $p2"))
        _  <- Result(println(s"Going to interlock on thread ${Thread.currentThread()}"))
        _  <- interlock(p1, p2).fork
        _  <- interlock(p2, p1).fork
      // _  <- fib1.join
      // _  <- fib2.join
      yield ()

    println(s"Constructed program")

    run(program)
  }

  test("workgroup allows to wait until all tasks complete") {
    def spawnTasks(wg: Result.WorkGroup, ref: Result.Ref[Int]): Result[Unit] =
      Result.sequence {
        (1 to 30).map { idx =>
          val napTime = scala.util.Random.between(10, 20)
          wg.runInWorkGroup(
            sleep(napTime).tap(_ => ref.update(i => i + idx))
          ).fork
        }.toVector
      }.void

    val program =
      for
        wg         <- Result.WorkGroup()
        ref        <- Result.Ref[Int](0)
        _          <- spawnTasks(wg, ref)
        pendingRes <- ref.get
        _          <- wg.waitForAll
        finalRes   <- ref.get
      yield (pendingRes, finalRes)

    val (pending, finalResult) = run(program)

    val expectedResult = (1 to 30).sum

    assert(pending != expectedResult, s"Expected 0 while pending, got $pending")
    assert(finalResult == expectedResult, s"Expected $expectedResult as final result, got $finalResult")
  }
