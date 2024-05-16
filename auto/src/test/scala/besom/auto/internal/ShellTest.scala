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
  }
