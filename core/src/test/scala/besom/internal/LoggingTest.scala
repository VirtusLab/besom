package besom.internal

import RunResult.{given, *}
import scala.collection.mutable
import besom.util.Types.*

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
    val urn = URN("urn:pulumi:stack::project::custom:resources:Resource$besom:testing/test:Resource::my-test-resource")
    val logger   = BesomLogger.local().unsafeRunSync()
    val res      = TestResource(Output(urn), Output(ResourceId("bar")), Output("url"))
    val messages = mutable.ArrayBuffer.empty[LogRecord]

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

    val loggingResult =
      for
        _ <- logger.info("hello")
        _ <- logger.warn("world")
        _ <- logger.debug(StructuralLog(23, "foo", true))
        _ <- logger.error("oh no")
        _ <- logger.trace("traced")
        _ <- logger.close()
      yield ()

    loggingResult.unsafeRunSync()

    assert(messages.size == 5)
    assert(messages(0).level == Level.Info)
    assert(messages(0).toPlainString == "hello")
    assert(messages(1).level == Level.Warn)
    assert(messages(1).toPlainString == "world")
    assert(messages(2).level == Level.Debug)
    assert(messages(2).toPlainString == "StructuralLog(a = 23, b = \"foo\", c = true)")
    assert(messages(3).level == Level.Error)
    assert(messages(3).toPlainString == "oh no")
    assert(messages(4).level == Level.Trace)
    assert(messages(4).toPlainString == "traced")
  }

  // TODO test("logging via RPC works") {}
