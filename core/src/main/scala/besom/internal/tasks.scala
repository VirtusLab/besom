package besom.internal

import besom.internal.logging.BesomLogger

trait TaskTracker:
  private[besom] def registerTask[A](fa: => Result[A]): Result[A]
  private[besom] def waitForAllTasks: Result[Unit]

object TaskTracker:
  def apply(): Result[TaskTracker] = WorkGroup().map { workgroup =>
    new TaskTracker:
      override private[besom] def registerTask[A](fa: => Result[A]): Result[A] =
        workgroup.runInWorkGroup(fa)
      override private[besom] def waitForAllTasks: Result[Unit] = workgroup.waitForAll *> workgroup.reset
  }
