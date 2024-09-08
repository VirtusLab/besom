package besom.internal

import RunResult.{*, given}

import scala.collection.mutable
import besom.types.{ResourceId, URN}

import java.time.LocalDateTime

case class TestResource(urn: Output[URN], id: Output[ResourceId], url: Output[String]) extends CustomResource

class LoggingTest extends munit.FunSuite:
  case class StructuralLog(a: Int, b: String, c: Boolean)
  import logging.*, logging.given
  import scribe.*
  import scribe.writer.Writer
  import scribe.output.*
  import scribe.output.format.OutputFormat

  given Context = DummyContext().unsafeRunSync()

  test("plain logging works") {
    val urn      = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource")
    val logger   = BesomLogger.local().unsafeRunSync()
    val res      = TestResource(Output.pure(urn), Output.pure("bar"), Output.pure("url"))
    val messages = mutable.ArrayBuffer.empty[LogRecord]

    given logging.BesomMDC[_] = logging.BesomMDC.empty

    scribe.Logger.root
      .clearHandlers()
      .clearModifiers()
      .withHandler(
        minimumLevel = Some(Level.Trace),
        writer = new Writer:
          def write(record: LogRecord, output: LogOutput, outputFormat: OutputFormat): Unit =
            messages += record
      )
      .replace()

    import scala.language.implicitConversions // for structural logging

    val loggingResult =
      for
        _ <- logger.info("hello")
        _ <- logger.warn("world")
        _ <- logger.debug(StructuralLog(23, "foo", true))
        _ <- logger.error("oh no")
        _ <- logger.trace("traced", Some(res))
        _ <- logger.close()
      yield ()

    loggingResult.unsafeRunSync()

    assert(messages.size == 5)
    assert(messages(0).level == Level.Info)
    assert(messages(0).toPlainString.endsWith("hello"))
    assert(messages(1).level == Level.Warn)
    assert(messages(1).toPlainString.endsWith("world"))
    assert(messages(2).level == Level.Debug)
    assert(messages(2).toPlainString.endsWith("StructuralLog(a = 23, b = \"foo\", c = true)"))
    assert(messages(3).level == Level.Error)
    assert(messages(3).toPlainString.endsWith("oh no"))
    assert(messages(4).level == Level.Trace)
    assert(messages(4).toPlainString.endsWith("traced"))
  }

  // TODO test("logging via RPC works") {}

  test("trace file name is correct") {
    val name = traceFileName(LocalDateTime.parse("2007-12-03T10:15:30"))
    assertEquals(name, "besom-run-2007-12-03T10-15-30.log")
  }
end LoggingTest
