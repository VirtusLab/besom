package besom.auto.internal

import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.collection.mutable

class EventLogsTest extends munit.FunSuite:

  private def event(sequence: Int, urn: String = "urn:pulumi:dev::proj::pkg:index:Res::res"): String =
    s"""{"sequence":$sequence,"timestamp":1700000000,"resourcePreEvent":{"metadata":{"op":"create","urn":"$urn","type":"pkg:index:Res","provider":"","old":null,"new":null,"detailedDiff":null}}}"""

  private val summaryLine =
    """{"sequence":99,"timestamp":1700000000,"summaryEvent":{"maybeCorrupt":false,"durationSeconds":3,"resourceChanges":{"create":1},"PolicyPacks":{}}}"""

  // ── parse ─────────────────────────────────────────────────────────────

  test("parse collects undecodable lines instead of failing the whole log") {
    val path = os.temp.dir() / "eventlog.txt"
    os.write(
      path,
      Seq(
        event(1), // 1: fine
        "not json at all", // 2: garbage
        event(2), // 3: fine
        "", // 4: blank, skipped entirely
        """{"sequence":3,"timestamp":""", // 5: truncated, as a killed process would leave it
        summaryLine // 6: fine
      ).mkString("\n")
    )

    val parsed = EventLogs.parse(path).getOrElse(fail("parse should not fail on undecodable lines"))

    assertEquals(parsed.events.map(_.sequence), List(1, 2, 99))
    assertEquals(parsed.parseErrors.map(_.lineNumber), List(2, 5))
    assertEquals(parsed.parseErrors.map(_.line), List("not json at all", """{"sequence":3,"timestamp":"""))
  }

  test("parse fails only when the file cannot be read") {
    val missing = os.temp.dir() / "nope.txt"
    assert(EventLogs.parse(missing).isLeft)
  }

  test("summary mentions the parse error count when the summary line went missing") {
    val path = os.temp.dir() / "eventlog.txt"
    os.write(path, Seq(event(1), "garbage").mkString("\n"))

    val parsed = EventLogs.parse(path).getOrElse(fail("parse failed"))
    val err    = EventLogs.summary(parsed.events, parsed.parseErrors).left.getOrElse(fail("expected a missing summary"))

    assert(err.getMessage.contains("1 event log line(s) failed to parse"), s"got: ${err.getMessage}")
    // and stays quiet when there is nothing to blame
    assert(!EventLogs.summary(parsed.events).left.getOrElse(fail("expected a missing summary")).getMessage.contains("failed to parse"))
  }

  test("summary finds the summary event") {
    val path = os.temp.dir() / "eventlog.txt"
    os.write(path, Seq(event(1), summaryLine).mkString("\n"))

    val parsed  = EventLogs.parse(path).getOrElse(fail("parse failed"))
    val summary = EventLogs.summary(parsed.events, parsed.parseErrors).getOrElse(fail("expected a summary"))
    assertEquals(summary.resourceChanges, Map[OpType, Int](OpType.Create -> 1))
    assertEquals(EventLogs.resourcePreEvents(parsed.events).size, 1)
  }

  // ── around ────────────────────────────────────────────────────────────

  test("around runs the body unchanged when there is no handler") {
    val path = os.temp.dir() / "eventlog.txt"
    os.write(path, "")
    assertEquals(EventLogs.around(path, None)(Right("done")), Right("done"))
  }

  test("around fails without running the body when the log cannot be opened") {
    var ran = false
    val res = EventLogs.around(os.temp.dir() / "missing.txt", Some(_ => ())) {
      ran = true
      Right(())
    }
    assert(res.isLeft, s"expected a Left, got: $res")
    assert(!ran, "the body must not run when the follower could not be opened")
  }

  test("around delivers events live and drains what was written just before the body returned") {
    val path = os.temp.dir() / "eventlog.txt"
    os.write(path, "")

    val received  = mutable.ListBuffer.empty[Int]
    val sawFirst  = new CountDownLatch(1)
    val sawSecond = new CountDownLatch(1)

    val result = EventLogs.around(
      path,
      Some { e =>
        received.synchronized(received += e.sequence)
        sawFirst.countDown()
        if received.synchronized(received.size) >= 2 then sawSecond.countDown()
      }
    ) {
      os.write.append(path, event(1) + "\n")
      // liveness: this must arrive while the body is still running
      assert(sawFirst.await(10, TimeUnit.SECONDS), "the first event was not delivered before the body returned")

      os.write.append(path, event(2) + "\n")
      assert(sawSecond.await(10, TimeUnit.SECONDS), "the second event was not delivered before the body returned")

      // written last thing before returning - stop() must drain it rather than lose it
      os.write.append(path, summaryLine + "\n")
      Right("done")
    }

    assertEquals(result, Right("done"))
    assertEquals(received.synchronized(received.toList), List(1, 2, 99))
  }

  test("around keeps delivering after a handler throws") {
    val path = os.temp.dir() / "eventlog.txt"
    os.write(path, "")

    val received = mutable.ListBuffer.empty[Int]
    val done     = new CountDownLatch(1)

    val result = EventLogs.around(
      path,
      Some { e =>
        if e.sequence == 1 then throw new RuntimeException("boom")
        received.synchronized(received += e.sequence)
        if e.sequence == 99 then done.countDown()
      }
    ) {
      os.write.append(path, Seq(event(1), event(2), summaryLine).mkString("", "\n", "\n"))
      assert(done.await(10, TimeUnit.SECONDS), "delivery stopped after the handler threw")
      Right(())
    }

    assertEquals(result, Right(()))
    assertEquals(received.synchronized(received.toList), List(2, 99))
  }

  test("around drains a slow handler completely before returning") {
    val path = os.temp.dir() / "eventlog.txt"
    os.write(path, "")

    val received = mutable.ListBuffer.empty[Int]
    val total    = 12

    // the handler is slow enough that it is certainly still draining when the body returns and stop() is called,
    // so returning early would lose events - the wait after stop() is deliberately unbounded
    val result = EventLogs.around(
      path,
      Some { e =>
        Thread.sleep(150)
        received.synchronized(received += e.sequence)
      }
    ) {
      os.write.append(path, (1 to total).map(event(_)).mkString("", "\n", "\n"))
      Right(())
    }

    assertEquals(result, Right(()))
    assertEquals(received.synchronized(received.toList), (1 to total).toList)
  }

  test("around drops undecodable lines rather than failing the operation") {
    val path = os.temp.dir() / "eventlog.txt"
    os.write(path, "")

    val received = mutable.ListBuffer.empty[Int]
    val done     = new CountDownLatch(1)

    val result = EventLogs.around(
      path,
      Some { e =>
        received.synchronized(received += e.sequence)
        if e.sequence == 99 then done.countDown()
      }
    ) {
      os.write.append(path, Seq(event(1), "garbage", summaryLine).mkString("", "\n", "\n"))
      assert(done.await(10, TimeUnit.SECONDS), "delivery stopped at the undecodable line")
      Right(())
    }

    assertEquals(result, Right(()))
    assertEquals(received.synchronized(received.toList), List(1, 99))
  }

end EventLogsTest
