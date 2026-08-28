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

  test("repeated options resolve the same way as the pre-existing ones") {
    val other: EngineEvent => Unit = _ => ()
    val opts = UpOptions.from(
      UpOption.OnEvent(onEvent),
      UpOption.OnEvent(other),
      UpOption.Message("first"),
      UpOption.Message("second")
    )
    assertEquals(opts.onEvent, if opts.message == "first" then Some(onEvent) else Some(other))
  }

end OptionsTest
