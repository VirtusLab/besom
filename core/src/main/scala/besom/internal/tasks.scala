package besom.internal

trait TaskTracker:
  private[besom] def registerTask[A](fa: => Result[A]): Result[A]
  private[besom] def waitForAllTasks: Result[Unit]
  private[besom] def fail(e: Throwable): Result[Unit]

object TaskTracker:
  def apply(): Result[TaskTracker] =
    for
      promise   <- Promise[Throwable]()
      workgroup <- WorkGroup()
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
