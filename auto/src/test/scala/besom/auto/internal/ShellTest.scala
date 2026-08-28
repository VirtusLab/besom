package besom.auto.internal

import besom.auto.internal.shell.*

class ShellTest extends munit.FunSuite:
  test("ShellOptions.from") {
    val options = ShellOptions.from(
      ShellOption.Cwd(os.pwd / "test"),
      ShellOption.Env("FOO" -> "BAR"),
      ShellOption.Env("FOO2" -> "BAR2"),
      ShellOption.Stdin("input"),
      ShellOption.Stdout(os.ProcessOutput.Readlines(_ => ())),
      ShellOption.Stderr(os.ProcessOutput.Readlines(_ => ())),
      ShellOption.Timeout(1000),
      ShellOption.MergeErrIntoOut,
      ShellOption.Check,
      ShellOption.DontPropagateEnv
    )

    assertEquals(options.cwd, os.pwd / "test")
    assertEquals(options.env, Map("FOO" -> "BAR", "FOO2" -> "BAR2"))
    assertEquals(options.stdin.getClass.getTypeName, "os.ProcessInput$SourceInput")
    assertEquals(options.stdout.getClass.getTypeName, "os.ProcessOutput$Readlines")
    assertEquals(options.stderr.getClass.getTypeName, "os.ProcessOutput$Readlines")
    assertEquals(options.timeout, 1000L)
    assertEquals(options.mergeErrIntoOut, true)
    assertEquals(options.check, true)
    assertEquals(options.propagateEnv, false)
    assertEquals(options.onStart, None)
  }

  test("ShellOptions.from OnStart") {
    val handler: ChildProcess => Unit = _ => ()
    assertEquals(ShellOptions.from(ShellOption.OnStart(handler)).onStart, Some(handler))
  }

  test("out/err are collected separately") {
    val res = shell("sh", "-c", "echo out; echo err 1>&2")().getOrElse(fail("command failed"))
    assertEquals(res.exitCode, 0)
    assertEquals(res.out.trim, "out")
    assertEquals(res.err.trim, "err")
  }

  test("mergeErrIntoOut folds stderr into stdout") {
    val res = shell("sh", "-c", "echo err 1>&2")(ShellOption.MergeErrIntoOut).getOrElse(fail("command failed"))
    assertEquals(res.out.trim, "err")
    assertEquals(res.err.trim, "")
  }

  test("a caller supplied stdout wins over collection") {
    val lines = scala.collection.mutable.ListBuffer.empty[String]
    val res = shell("sh", "-c", "echo out")(
      ShellOption.Stdout(os.ProcessOutput.Readlines(l => lines.synchronized(lines += l)))
    ).getOrElse(fail("command failed"))

    assertEquals(lines.synchronized(lines.toList), List("out"))
    assertEquals(res.out, "") // nothing was collected, the caller took it
  }

  test("a non-zero exit code is reported as a ShellAutoError") {
    val err = shell("sh", "-c", "echo boom 1>&2; exit 3")().left.getOrElse(fail("expected a failure"))
    assertEquals(err.exitCode, 3)
    assertEquals(err.stderr.trim, "boom")
  }

  test("Check throws instead of returning a Left") {
    intercept[os.SubprocessException] {
      shell("sh", "-c", "exit 3")(ShellOption.Check)
    }
  }

  test("OnStart hands over a live process handle") {
    @volatile var handle: Option[ChildProcess] = None

    val res = shell("sh", "-c", "echo hello")(ShellOption.OnStart(p => handle = Some(p)))
      .getOrElse(fail("command failed"))

    assertEquals(res.out.trim, "hello")
    val p = handle.getOrElse(fail("OnStart was never invoked"))
    assert(p.pid > 0, s"expected a real pid, got ${p.pid}")
    assert(!p.isAlive, "the process should be done by the time the call returned")
  }

  test("OnStart handle can interrupt a running process") {
    @volatile var handle: Option[ChildProcess] = None
    val started                                = new java.util.concurrent.CountDownLatch(1)

    val runner = new Thread(() =>
      shell("sh", "-c", "sleep 30")(ShellOption.OnStart { p =>
        handle = Some(p)
        started.countDown()
      })
      ()
    )
    runner.setDaemon(true)
    runner.start()

    assert(started.await(10, java.util.concurrent.TimeUnit.SECONDS), "the process never started")
    val p = handle.getOrElse(fail("OnStart was never invoked"))
    assert(p.isAlive, "the process should still be running")

    p.interrupt()
    runner.join(10000)
    assert(!runner.isAlive, "the call did not return after the process was interrupted")
    assert(!p.isAlive, "the process should be gone")
  }
end ShellTest
