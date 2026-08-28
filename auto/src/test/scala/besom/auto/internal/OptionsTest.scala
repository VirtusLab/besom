package besom.auto.internal

class OptionsTest extends munit.FunSuite:

  private val onEvent: EngineEvent => Unit  = _ => ()
  private val onStart: ChildProcess => Unit = _ => ()
  private val logPath                       = os.pwd / "eventlog.txt"

  test("PreviewOptions.from event and process options") {
    val opts = PreviewOptions.from(
      PreviewOption.OnEvent(onEvent),
      PreviewOption.EventLog(logPath),
      PreviewOption.OnProcessStart(onStart)
    )
    assertEquals(opts.onEvent, Some(onEvent))
    assertEquals(opts.eventLog, logPath)
    assertEquals(opts.onProcessStart, Some(onStart))

    val defaults = PreviewOptions.from()
    assertEquals(defaults.onEvent, None)
    assertEquals(defaults.eventLog.asOption, None)
    assertEquals(defaults.onProcessStart, None)
  }

  test("UpOptions.from event and process options") {
    val opts = UpOptions.from(
      UpOption.OnEvent(onEvent),
      UpOption.EventLog(logPath),
      UpOption.OnProcessStart(onStart)
    )
    assertEquals(opts.onEvent, Some(onEvent))
    assertEquals(opts.eventLog, logPath)
    assertEquals(opts.onProcessStart, Some(onStart))

    val defaults = UpOptions.from()
    assertEquals(defaults.onEvent, None)
    assertEquals(defaults.eventLog.asOption, None)
    assertEquals(defaults.onProcessStart, None)
  }

  test("RefreshOptions.from event and process options") {
    val opts = RefreshOptions.from(
      RefreshOption.OnEvent(onEvent),
      RefreshOption.EventLog(logPath),
      RefreshOption.OnProcessStart(onStart)
    )
    assertEquals(opts.onEvent, Some(onEvent))
    assertEquals(opts.eventLog, logPath)
    assertEquals(opts.onProcessStart, Some(onStart))

    val defaults = RefreshOptions.from()
    assertEquals(defaults.onEvent, None)
    assertEquals(defaults.eventLog.asOption, None)
    assertEquals(defaults.onProcessStart, None)
  }

  test("DestroyOptions.from event and process options") {
    val opts = DestroyOptions.from(
      DestroyOption.OnEvent(onEvent),
      DestroyOption.EventLog(logPath),
      DestroyOption.OnProcessStart(onStart)
    )
    assertEquals(opts.onEvent, Some(onEvent))
    assertEquals(opts.eventLog, logPath)
    assertEquals(opts.onProcessStart, Some(onStart))

    val defaults = DestroyOptions.from()
    assertEquals(defaults.onEvent, None)
    assertEquals(defaults.eventLog.asOption, None)
    assertEquals(defaults.onProcessStart, None)
  }

  test("ShellOptions.from - the last occurrence of an option wins") {
    val opts = shell.ShellOptions.from(
      shell.ShellOption.Cwd(os.pwd / "first"),
      shell.ShellOption.Cwd(os.pwd / "second"),
      shell.ShellOption.Timeout(1),
      shell.ShellOption.Timeout(2)
    )
    assertEquals(opts.cwd.asOption, Some(os.pwd / "second"))
    assertEquals(opts.timeout, 2L)
  }

  test("ShellOptions.from - Env accumulates, a repeated key resolves to the last occurrence") {
    val opts = shell.ShellOptions.from(
      shell.ShellOption.Env("A" -> "1", "SHARED" -> "first"),
      shell.ShellOption.Env("B" -> "2", "SHARED" -> "second")
    )
    assertEquals(opts.env, Map("A" -> "1", "B" -> "2", "SHARED" -> "second"))
  }

  test("LoginOptions.from - the last occurrence of an option wins") {
    val opts = LoginOptions.from(
      LoginOption.Cloud("first"),
      LoginOption.Cloud("second"),
      LoginOption.DefaultOrg("orgA"),
      LoginOption.DefaultOrg("orgB")
    )
    assertEquals(opts.cloud.asOption, Some("second"))
    assertEquals(opts.defaultOrg.asOption, Some("orgB"))
  }

  test("LogoutOptions.from - the last occurrence of an option wins") {
    val opts = LogoutOptions.from(
      LogoutOption.Cloud("first"),
      LogoutOption.Cloud("second"),
      LogoutOption.PulumiHome(os.pwd / "first"),
      LogoutOption.PulumiHome(os.pwd / "second")
    )
    assertEquals(opts.cloud.asOption, Some("second"))
    assertEquals(opts.pulumiHome.asOption, Some(os.pwd / "second"))
  }

  test("LocalWorkspaceOptions.from - the last occurrence of an option wins") {
    val opts = LocalWorkspaceOptions.from(
      LocalWorkspaceOption.WorkDir(os.pwd / "first"),
      LocalWorkspaceOption.WorkDir(os.pwd / "second"),
      LocalWorkspaceOption.SecretsProvider("first"),
      LocalWorkspaceOption.SecretsProvider("second")
    )
    assertEquals(opts.workDir.asOption, Some(os.pwd / "second"))
    assertEquals(opts.secretsProvider.asOption, Some("second"))
  }

  test("LocalWorkspaceOptions.from - EnvVars accumulate, a repeated key resolves to the last occurrence") {
    val opts = LocalWorkspaceOptions.from(
      LocalWorkspaceOption.EnvVars("A" -> "1", "SHARED" -> "first"),
      LocalWorkspaceOption.EnvVars("B" -> "2", "SHARED" -> "second")
    )
    assertEquals(opts.envVars, Map("A" -> "1", "B" -> "2", "SHARED" -> "second"))
  }

  test("PreviewOptions.from - the last occurrence of an option wins") {
    val other: EngineEvent => Unit = _ => ()
    val opts = PreviewOptions.from(
      PreviewOption.OnEvent(onEvent),
      PreviewOption.OnEvent(other),
      PreviewOption.Message("first"),
      PreviewOption.Message("second"),
      PreviewOption.Parallel(1),
      PreviewOption.Parallel(2)
    )
    assertEquals(opts.onEvent, Some(other))
    assertEquals(opts.message.asOption, Some("second"))
    assertEquals(opts.parallel.asOption, Some(2))
  }

  test("UpOptions.from - the last occurrence of an option wins") {
    val other: EngineEvent => Unit = _ => ()
    val opts = UpOptions.from(
      UpOption.OnEvent(onEvent),
      UpOption.OnEvent(other),
      UpOption.Message("first"),
      UpOption.Message("second"),
      UpOption.Parallel(1),
      UpOption.Parallel(2)
    )
    assertEquals(opts.onEvent, Some(other))
    assertEquals(opts.message.asOption, Some("second"))
    assertEquals(opts.parallel.asOption, Some(2))
  }

  test("RefreshOptions.from - the last occurrence of an option wins") {
    val other: EngineEvent => Unit = _ => ()
    val opts = RefreshOptions.from(
      RefreshOption.OnEvent(onEvent),
      RefreshOption.OnEvent(other),
      RefreshOption.Message("first"),
      RefreshOption.Message("second"),
      RefreshOption.Parallel(1),
      RefreshOption.Parallel(2)
    )
    assertEquals(opts.onEvent, Some(other))
    assertEquals(opts.message.asOption, Some("second"))
    assertEquals(opts.parallel.asOption, Some(2))
  }

  test("DestroyOptions.from - the last occurrence of an option wins") {
    val other: EngineEvent => Unit = _ => ()
    val opts = DestroyOptions.from(
      DestroyOption.OnEvent(onEvent),
      DestroyOption.OnEvent(other),
      DestroyOption.Message("first"),
      DestroyOption.Message("second"),
      DestroyOption.Parallel(1),
      DestroyOption.Parallel(2)
    )
    assertEquals(opts.onEvent, Some(other))
    assertEquals(opts.message.asOption, Some("second"))
    assertEquals(opts.parallel.asOption, Some(2))
  }

  test("HistoryOptions.from - a repeated flag is idempotent") {
    assertEquals(HistoryOptions.from(HistoryOption.ShowSecrets, HistoryOption.ShowSecrets).showSecrets, true)
    assertEquals(HistoryOptions.from().showSecrets, false)
  }

  test("a base list of options can be overridden by appending to it") {
    val base = Seq(UpOption.Message("nightly"), UpOption.Parallel(8))
    val opts = UpOptions.from(base :+ UpOption.Parallel(1)*)
    assertEquals(opts.message.asOption, Some("nightly"))
    assertEquals(opts.parallel.asOption, Some(1))
  }

end OptionsTest
