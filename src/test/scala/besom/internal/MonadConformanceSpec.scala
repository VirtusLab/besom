package besom.internal

import scala.util.Try

// trait MonadConformanceSpec[F[+_]](using F: Monad[F]) extends munit.FunSuite, RunEffect[F], MonadHelpers[F]:
//   test("left identity") {
//     val a                   = 23
//     val f: Int => F[String] = i => F.eval(s"$i")

//     val lhs = run(F.eval(a).flatMap(f))
//     val rhs = run(f(a))

//     assert(lhs == rhs)
//   }

//   test("right identity") {
//     val m   = F.eval(23)
//     val lhs = run(m.flatMap(F.eval))
//     val rhs = run(m)

//     assert(lhs == rhs)
//   }

//   test("associativity") {
//     val m                   = F.eval(23)
//     val f: Int => F[String] = i => F.eval(s"$i")
//     val g: String => F[Int] = s => F.evalTry(Try(s.toInt))

//     val lhs = run(m.flatMap(f).flatMap(g))
//     val rhs = run(m.flatMap(s => f(s).flatMap(g)))

//     assert(lhs == rhs)
//   }

//   // flaky
//   // test("forking") {
//   //   def getThreadId = Thread.currentThread().getId()
//   //   def forked(ref: Ref[F, Set[Long]], done: Promise[F, Unit]): F[Unit] =
//   //     for
//   //       forkedThreadId <- F.eval(getThreadId).tap(id => F.eval(println(s"forked thread $id")))
//   //       _              <- ref.update(s => s + forkedThreadId)
//   //       _              <- done.fulfill(())
//   //     yield ()

//   //   val program = Ref[F, Set[Long]](Set.empty[Long]).flatMap { ref =>
//   //     for
//   //       mainThreadId <- F.eval(getThreadId).tap(id => F.eval(println(s"main thread $id")))
//   //       _            <- ref.update(s => s + mainThreadId)
//   //       promise      <- Promise[F, Unit]
//   //       _            <- forked(ref, promise).fork
//   //       _            <- promise.get
//   //       s            <- ref.get
//   //     yield s
//   //   }

//   //   val set          = run(program).get
//   //   val expectedSize = 2
//   //   val finalSize    = set.size
//   //   assert(finalSize == expectedSize, s"Got $finalSize when expected $expectedSize")
//   // }

//   // this would hang if tasks didn't go through some kind of trampoline
//   // there's no easy way to make this test fail, sadly
//   test("inter-locking forking") {
//     def interlock(p1: Promise[F, Unit], p2: Promise[F, Unit]): F[Unit] =
//       for
//         _ <- p1.fulfill(())
//         _ <- p2.get
//       yield ()

//     val program =
//       for
//         p1   <- Promise[F, Unit]
//         p2   <- Promise[F, Unit]
//         fib1 <- interlock(p1, p2).fork
//         fib2 <- interlock(p2, p1).fork
//         _    <- fib1.join
//         _    <- fib2.join
//       yield ()

//     run(program).get
//   }

//   test("workgroup allows to wait until all tasks complete") {
//     def spawnTasks(wg: WorkGroup[F], ref: Ref[F, Int]): F[Unit] =
//       F.sequence {
//         (1 to 30).map { idx =>
//           val napTime = scala.util.Random.between(10, 20)
//           wg.runInWorkGroup(
//             sleep(napTime).tap(_ => ref.update(i => i + idx))
//           ).fork
//         }.toVector
//       }.void

//     val program =
//       for
//         wg         <- WorkGroup[F]
//         ref        <- Ref[F, Int](0)
//         _          <- spawnTasks(wg, ref)
//         pendingRes <- ref.get
//         _          <- wg.waitForAll
//         finalRes   <- ref.get
//       yield (pendingRes, finalRes)

//     val (pending, finalResult) = run(program).get

//     val expectedResult = (1 to 30).sum

//     assert(pending != expectedResult, s"Expected 0 while pending, got $pending")
//     assert(finalResult == expectedResult, s"Expected $expectedResult as final result, got $finalResult")
//   }

// TODO test laziness of operators (for Future mostly) somehow
// TODO zip should be probably parallelised
// TODO test cancellation doesn't break anything for product, fork etc
// TODO test that forking never swallows errors (ZIO.die / fiber failure is caught mostly)
// TODO
// TODO
// TODO
// TODO
// TODO
