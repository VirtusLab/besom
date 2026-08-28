package besom.auto.internal

/** A handle to a `pulumi` process started by besom-auto, handed to callers that opted in with `ShellOption.OnStart` (or the per-operation
  * `OnProcessStart` options) so that they can implement cancellation.
  *
  * The handle is live only for as long as the operation that produced it runs.
  */
final class ChildProcess private[auto] (val underlying: os.SubProcess):

  /** The operating system process id. */
  def pid: Long = underlying.wrapped.pid()

  /** Whether the process is still running. */
  def isAlive: Boolean = underlying.isAlive()

  /** Sends `SIGINT`, which is the only signal `pulumi` treats as "cancel gracefully" - it logs `^C received; cancelling` and unwinds the
    * current step rather than dying mid-operation. Sending it a second time is what `pulumi` itself escalates to immediate termination, so
    * forwarding a terminal's Ctrl-C straight through gives the usual two stage behaviour.
    *
    * The JDK exposes no signal API, so on Unix this shells out to `kill -INT`; on Windows, where there is no equivalent, it falls back to
    * [[terminate]].
    *
    * The signal goes to the process besom-auto spawned, not to its descendants. That is exactly right for `pulumi`, which is always spawned
    * directly and unwinds its own children - but it does mean a caller that interposes a shell (`sh -c "pulumi ..."`) would break
    * cancellation, since the shell would receive the signal, ignore it while waiting on its foreground child, and never pass it on.
    */
  def interrupt(): Unit =
    if isWindows then terminate()
    else
      // best effort - if the process is already gone kill exits non-zero and there is nothing to cancel
      os.proc("kill", "-INT", pid.toString).call(check = false)
      ()

  /** Sends `SIGTERM` and, if the process is still alive after the grace period, `SIGKILL`. Note that `pulumi` does *not* treat `SIGTERM` as
    * a cancellation - use [[interrupt]] for that.
    */
  def terminate(): Unit = underlying.destroy()

  /** Sends `SIGKILL` immediately. The stack is very likely to be left with a pending operation. */
  def kill(): Unit = underlying.destroy(shutdownGracePeriod = 0)

  private def isWindows: Boolean = System.getProperty("os.name", "").toLowerCase.startsWith("windows")

end ChildProcess
