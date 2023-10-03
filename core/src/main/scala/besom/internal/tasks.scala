package besom.internal

import besom.internal.logging.{LocalBesomLogger => logger, BesomLogger}

trait TaskTracker:
  private[besom] def registerTask[A](fa: => Result[A]): Result[A]
  private[besom] def waitForAllTasks: Result[Unit]
  private[besom] def fail(e: Throwable): Result[Unit]

object TaskTracker:
  def apply(): Result[TaskTracker] =
    for
      promise   <- Promise[Throwable]()
      workgroup <- WorkGroup()
      _ <- {
        Result.sleep(10000) *>
        Result.forever {
          Result.sleep(5000) *> {
            logger.debug(workgroup.promises.map(_.toString).mkString("\n") ++ "\n")
          }
        }
      }.fork
    yield new TaskTracker:
      override private[besom] def registerTask[A](fa: => Result[A]): Result[A] =
        promise.isCompleted.flatMap { globalFailure =>
          if globalFailure then promise.get.flatMap(Result.fail(_)) else workgroup.runInWorkGroup(fa)
        }
      override private[besom] def waitForAllTasks: Result[Unit] =
        promise.isCompleted.flatMap { globalFailure =>
          if globalFailure then promise.get.flatMap(Result.fail(_)) else workgroup.waitForAll *> workgroup.reset
        }

      override private[besom] def fail(e: Throwable): Result[Unit] = promise.fail(e)
