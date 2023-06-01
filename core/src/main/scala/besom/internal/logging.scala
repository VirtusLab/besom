package besom.internal

import scribe.*, LoggerSupport.{apply => makeLogRecord}
import scribe.message.LoggableMessage
import scribe.output.LogOutput
import scribe.data.MDC
import sourcecode.{FileName, Line, Name, Pkg}

import scala.language.implicitConversions
import pulumirpc.engine.{LogRequest, LogSeverity}

object logging:

  export scribe.message.LoggableMessage.*
  export scribe.Level

  // TODO this is probably way too wide
  // forcing a `derives` clause is quite limiting on the other hand
  given structuredLoggingSupport[A <: Product]: Conversion[A, LoggableMessage] = (a: A) => pprint(a).toString

  extension (record: LogRecord)
    def toSeverity: LogSeverity =
      record.level match
        case Level.Trace => LogSeverity.DEBUG
        case Level.Debug => LogSeverity.DEBUG
        case Level.Info  => LogSeverity.INFO
        case Level.Warn  => LogSeverity.WARNING
        case Level.Error => LogSeverity.ERROR

    def toLogString: String   = record.logOutput.plainText
    def toPlainString: String = record.toLogString.replaceAll("\u001B\\[[;\\d]*m", "") // drops ANSI color codes

  def makeLogRequest(record: LogRecord, urn: String, streamId: Int, ephemeral: Boolean): LogRequest =
    LogRequest(
      severity = record.toSeverity,
      message = record.toLogString, // I really hope pulumi can deal with ANSI colored strings
      urn = urn,
      streamId = streamId,
      ephemeral = ephemeral
    )

  trait BesomLogger extends LoggerSupport[Result[Unit]]:
    def close(): Result[Unit]

  object LocalBesomLogger extends BesomLogger:
    def log(record: LogRecord): Result[Unit] = Result(Logger(record.className).log(record))
    def close(): Result[Unit]                = Result.unit

  class DualBesomLogger private[logging] (
    private val queue: Queue[LogRequest | Queue.Stop],
    private val fib: Fiber[Unit]
  ) extends BesomLogger:

    def close(): Result[Unit] = queue.offer(Queue.Stop) *> fib.join

    def log(record: LogRecord): Result[Unit] = Result(Logger(record.className).log(record))

    def log(record: LogRecord, urn: String, streamId: Int, ephemeral: Boolean): Result[Unit] =
      for
        _ <- log(record) // direct logging
        _ <- queue.offer(makeLogRequest(record, urn, streamId, ephemeral)) // logging via RPC (async via queue)
      yield ()

    def log(level: Level, mdc: MDC, urn: String, streamId: Int, ephemeral: Boolean, messages: LoggableMessage*)(implicit
      pkg: Pkg,
      fileName: FileName,
      name: Name,
      line: Line
    ): Result[Unit] =
      log(makeLogRecord(level, messages.toList, pkg, fileName, name, line, mdc), urn, streamId, ephemeral)

    def trace(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      implicit
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Trace, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Trace, mdc, "", streamId, ephemeral, message)

    def debug(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      implicit
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Debug, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Debug, mdc, "", streamId, ephemeral, message)

    def info(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      implicit
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Info, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Info, mdc, "", streamId, ephemeral, message)

    def warn(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      implicit
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Warn, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Warn, mdc, "", streamId, ephemeral, message)

    def error(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      implicit
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Error, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Error, mdc, "", streamId, ephemeral, message)

  object BesomLogger:
    def apply(engine: Engine)(using taskTracker: TaskTracker): Result[BesomLogger] =
      def publishLoop(queue: Queue[LogRequest | Queue.Stop]): Result[Unit] =
        queue.take.flatMap {
          case Queue.Stop      => Result.unit
          case req: LogRequest => taskTracker.registerTask(engine.log(req)).flatMap(_ => publishLoop(queue))
        }

      for
        queue <- Queue[LogRequest | Queue.Stop](100)
        fib   <- publishLoop(queue).fork
      yield new DualBesomLogger(queue, fib)

    def local(): Result[BesomLogger] = Result(LocalBesomLogger)

    def setLogLevel(level: Level): Result[Unit] = Result {
      scribe.Logger.root
        .clearHandlers()
        .clearModifiers()
        .withHandler(
          minimumLevel = Some(level)
        )
        .replace()
    }

    import scribe.file.*

    def unsafeEnableTraceLevelFileLogging(): Unit =
      scribe.Logger.root
        .clearHandlers()
        .clearModifiers()
        .withHandler(
          minimumLevel = Some(Level.Trace),
          writer = FileWriter(pathBuilder = "besom-run-" % year % "-" % month % "-" % day % "T" % hour % ".log")
        )
        .replace()
