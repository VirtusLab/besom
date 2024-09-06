package besom.internal

import scribe.*, LoggerSupport.{apply => makeLogRecord}
import scribe.message.LoggableMessage
import scribe.mdc.{MDC => ScribeMDC}
import sourcecode.{FileName, Line, Name, Pkg}
import scala.util.{NotGiven => Not}

import scala.language.implicitConversions
import pulumirpc.engine.{LogRequest, LogSeverity}
import besom.types.Label
import besom.types.URN
import besom.util.printer

object logging:

  class Key[A](val value: String)
  object Key:
    val LabelKey: Key[Label] = Key[Label]("resource")

  class BesomMDC[A](private[logging] val inner: ScribeMDC) extends ScribeMDC:
    private[besom] def get[B](key: Key[B])(using ev: A <:< B): B =
      val opt = inner.get(key.value)

      opt
        .map(_.apply())
        .getOrElse { throw Exception(s"Expected key '${key.value}' was not found in MDC!") }
        .asInstanceOf[B]

    private[besom] def put[B](key: Key[A], value: A)(using Not[A <:< B]): BesomMDC[A & B] =
      inner.update(key.value, value)
      this.asInstanceOf[BesomMDC[A & B]]

    private[besom] def +[B](key: Key[A], value: A)(using Not[A <:< B]): BesomMDC[A & B] = put(key, value)

    def apply[R](block: BesomMDC[A] ?=> R): R = block(using this)

    export inner.*
  end BesomMDC

  object BesomMDC:
    private[besom] def apply[A](key: Key[A], value: A): BesomMDC[A] = ScribeMDC { scribeMdc =>
      scribeMdc.update(key.value, value)
      new BesomMDC(scribeMdc)
    }
    def empty: BesomMDC[_] = ScribeMDC { scribeMdc =>
      new BesomMDC(scribeMdc)
    }

  class MDC(private[logging] val inner: BesomMDC[_])

  object MDC:
    def empty: MDC = new MDC(BesomMDC.empty)
    def apply[R](values: Map[String, String])(block: MDC ?=> R)(using outer: MDC = MDC.empty): R =
      val outerValues = outer.inner.inner.map.map { case (k, v) => k -> v.apply() }
      val mdcWithOuterValues = outerValues.foldLeft(BesomMDC.empty) { case (mdc, (k, v)) =>
        mdc.update(k, v)
        mdc
      }
      val mdcWithValues = values.foldLeft(mdcWithOuterValues) { case (mdc, (k, v)) =>
        mdc.update(k, v)
        mdc
      }

      val mdc = new MDC(mdcWithValues)

      block(using mdc)

  def log(using Context): BesomLogger = Context().logger

  export scribe.message.LoggableMessage.*
  export scribe.Level

  // TODO this is probably way too wide
  // forcing a `derives` clause is quite limiting on the other hand
  given structuredLoggingSupport[A <: Product]: Conversion[A, LoggableMessage] = (a: A) => printer.render(a).toString

  extension (record: LogRecord)
    def toSeverity: LogSeverity =
      record.level match
        case Level.Trace => LogSeverity.DEBUG
        case Level.Debug => LogSeverity.DEBUG
        case Level.Info  => LogSeverity.INFO
        case Level.Warn  => LogSeverity.WARNING
        case Level.Error => LogSeverity.ERROR

    def toLogString: String =
      val logOutput = pulumiFormatter.format(record)
      val sb        = new StringBuilder
      scribe.output.format.ANSIOutputFormat(logOutput, str => sb.append(str))
      sb.toString.replace("<empty>.", "")

    def toPlainString: String = record.toLogString.replaceAll("\u001B\\[[;\\d]*m", "").trim // drops ANSI color codes

  def makeLogRequest(record: LogRecord, urn: URN, streamId: Int, ephemeral: Boolean): LogRequest =
    LogRequest(
      severity = record.toSeverity,
      message = record.toLogString,
      urn = urn.asString,
      streamId = streamId,
      ephemeral = ephemeral
    )

  trait BesomLogger:
    private[besom] def close(): Result[Unit]
    def log(record: LogRecord): Result[Unit]
    def log(record: LogRecord, urn: URN, streamId: Int, ephemeral: Boolean): Result[Unit]

    def log(level: Level, mdc: BesomMDC[_], urn: URN, streamId: Int, ephemeral: Boolean, messages: LoggableMessage*)(using
      pkg: Pkg,
      fileName: FileName,
      name: Name,
      line: Line
    ): Result[Unit] =
      log(makeLogRecord(level, messages.toList, pkg, fileName, name, line, mdc), urn, streamId, ephemeral)

    // TODO resource-less methods

    def trace(message: LoggableMessage)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_]
    ): Result[Unit] = log(Level.Trace, mdc, URN.empty, 0, false, message)

    def trace(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_] = BesomMDC.empty,
      ctx: Context
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse(URN.empty).flatMap(urn => log(Level.Trace, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Trace, mdc, URN.empty, streamId, ephemeral, message)

    def debug(message: LoggableMessage)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_]
    ): Result[Unit] = log(Level.Debug, mdc, URN.empty, 0, false, message)

    def debug(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_] = BesomMDC.empty,
      ctx: Context
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse(URN.empty).flatMap(urn => log(Level.Debug, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Debug, mdc, URN.empty, streamId, ephemeral, message)

    def info(message: LoggableMessage)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_]
    ): Result[Unit] = log(Level.Info, mdc, URN.empty, 0, false, message)

    def info(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_] = BesomMDC.empty,
      ctx: Context
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse(URN.empty).flatMap(urn => log(Level.Info, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Info, mdc, URN.empty, streamId, ephemeral, message)

    def warn(message: LoggableMessage)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_]
    ): Result[Unit] = log(Level.Warn, mdc, URN.empty, 0, false, message)

    def warn(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_] = BesomMDC.empty,
      ctx: Context
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse(URN.empty).flatMap(urn => log(Level.Warn, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Warn, mdc, URN.empty, streamId, ephemeral, message)

    def error(message: LoggableMessage)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_]
    ): Result[Unit] = log(Level.Error, mdc, URN.empty, 0, false, message)

    def error(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: BesomMDC[_] = BesomMDC.empty,
      ctx: Context
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse(URN.empty).flatMap(urn => log(Level.Error, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Error, mdc, URN.empty, streamId, ephemeral, message)
  end BesomLogger

  object LocalBesomLogger extends BesomLogger with LoggerSupport[Result[Unit]]:
    override def close(): Result[Unit]                = Result.unit
    override def log(record: LogRecord): Result[Unit] = Result(Logger(record.className).log(record))
    override def log(record: LogRecord, urn: URN, streamId: Int, ephemeral: Boolean): Result[Unit] =
      log(record)

  class UserLoggerFactory(using ctx: Context):
    def trace(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC = MDC.empty
    ): Output[Unit] = Output {
      res match
        case Some(r) =>
          r.urn
            .getValueOrElse(URN.empty)
            .flatMap(urn => ctx.logger.log(Level.Trace, mdc.inner, urn, streamId, ephemeral, message))
        case None => ctx.logger.log(Level.Trace, mdc.inner, URN.empty, streamId, ephemeral, message)
    }

    def debug(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC = MDC.empty
    ): Output[Unit] = Output {
      res match
        case Some(r) =>
          r.urn
            .getValueOrElse(URN.empty)
            .flatMap(urn => ctx.logger.log(Level.Debug, mdc.inner, urn, streamId, ephemeral, message))
        case None => ctx.logger.log(Level.Debug, mdc.inner, URN.empty, streamId, ephemeral, message)
    }

    def info(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC = MDC.empty
    ): Output[Unit] = Output {
      res match
        case Some(r) =>
          r.urn
            .getValueOrElse(URN.empty)
            .flatMap(urn => ctx.logger.log(Level.Info, mdc.inner, urn, streamId, ephemeral, message))
        case None => ctx.logger.log(Level.Info, mdc.inner, URN.empty, streamId, ephemeral, message)
    }

    def warn(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC = MDC.empty
    ): Output[Unit] = Output {
      res match
        case Some(r) =>
          r.urn
            .getValueOrElse(URN.empty)
            .flatMap(urn => ctx.logger.log(Level.Warn, mdc.inner, urn, streamId, ephemeral, message))
        case None => ctx.logger.log(Level.Warn, mdc.inner, URN.empty, streamId, ephemeral, message)
    }

    def error(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC = MDC.empty
    ): Output[Unit] = Output {
      res match
        case Some(r) =>
          r.urn
            .getValueOrElse(URN.empty)
            .flatMap(urn => ctx.logger.log(Level.Error, mdc.inner, urn, streamId, ephemeral, message))
        case None => ctx.logger.log(Level.Error, mdc.inner, URN.empty, streamId, ephemeral, message)
    }
  end UserLoggerFactory

  class DualBesomLogger private[logging] (
    private val queue: Queue[LogRequest | Queue.Stop],
    private val fib: Fiber[Unit]
  ) extends BesomLogger:
    def close(): Result[Unit] = queue.offer(Queue.Stop) *> fib.join

    override def log(record: LogRecord): Result[Unit] = Result(Logger(record.className).log(record))

    override def log(record: LogRecord, urn: URN, streamId: Int, ephemeral: Boolean): Result[Unit] =
      for
        _ <- log(record) // direct logging
        // logging via RPC (async via queue)
        _ <- queue.offer(makeLogRequest(record, urn, streamId, ephemeral)) // this unfortunately evaluates all lazy log messages
      yield ()

  object BesomLogger:
    def apply(engine: Engine, taskTracker: TaskTracker): Result[BesomLogger] =
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

    def setupLogger(): Result[Unit] =
      if Env.traceRunToFile then enableTraceLevelFileLogging()
      else setLogLevel(Env.logLevel)

  // scribe programmatic configuration follows

  import scribe.format.*
  import scribe.Level
  import scribe.LogRecord
  import scribe.output.{LogOutput, CompositeOutput, TextOutput, EmptyOutput}
  import scribe.file.*
  import scribe.writer.CacheWriter
  import java.nio.file.Paths
  import java.time.LocalDateTime, java.time.format.DateTimeFormatter

  private[logging] object mdcBlock extends FormatBlock:
    override def format(record: LogRecord): LogOutput =
      val map = ScribeMDC.map ++ record.data
      if map.nonEmpty then
        val prefix  = green(bold(string("["))).format(record)
        val postfix = green(bold(string("]"))).format(record)
        val entries = prefix :: map.toList.flatMap { case (key, value) =>
          List(
            new TextOutput(", "),
            brightWhite(string(s"$key: ")).format(record),
            new TextOutput(String.valueOf(value()))
          )
        }.tail ::: List(postfix)
        new CompositeOutput(entries)
      else EmptyOutput

  private[logging] val pulumiFormatter = Formatter.fromBlocks(
    green(position),
    space,
    mdcBlock,
    space,
    messages
  )

  private[logging] val logFileFormatter = Formatter.fromBlocks(
    groupBySecond(
      cyan(bold(dateFull)),
      space,
      italic(threadName),
      space,
      mdcBlock,
      space,
      levelColored,
      space,
      green(position),
      newLine
    ),
    multiLine(messages)
  )

  def setLogLevel(level: Level): Result[Unit] = Result.defer {
    scribe.Logger.root
      .clearHandlers()
      .clearModifiers()
      .withHandler(
        minimumLevel = Some(level),
        formatter = logFileFormatter
      )
      .replace()
  }

  /** For tests only: enable capturing of log output to a cache writer CacheWriter exposes two methods of interest:
    *
    * def records: List[LogRecord]
    *
    * def output: List[LogOutput]
    *
    * To get logs as string one should use
    *
    * `writer.output.map(_.plainText).mkString("\n")` or, alternatively,
    *
    * `writer.output.map(_.toString).mkString("\n")` for colored output.
    *
    * This method is side effecting and should be used with caution. Use in parallelized tests will cause issues as it changes global state
    * of the logger stack.
    *
    * @param level
    *   scribe.Level: the minimum level of log messages to capture
    * @param maxLines
    *   Int: the maximum number of log lines to capture
    *
    * @return
    *   a new CacheWriter: a variant of logger sink that captures log output and stores it up to `maxLines` lines
    */
  private[besom] def enableLogCapturing(level: Level, maxLines: Int = Int.MaxValue): CacheWriter =
    val writer = CacheWriter(max = maxLines)

    scribe.Logger.root
      .clearHandlers()
      .clearModifiers()
      .withHandler(
        minimumLevel = Some(level),
        writer = writer,
        formatter = logFileFormatter
      )
      .replace()

    writer

  def enableTraceLevelFileLogging(): Result[Unit] = Result.defer {
    val pathBuilder = PathBuilder.static(Paths.get("logs")) / traceFileName(LocalDateTime.now())
    scribe.Logger.root
      .clearHandlers()
      .clearModifiers()
      .withHandler(
        minimumLevel = Some(Level.Trace),
        writer = FileWriter(pathBuilder = pathBuilder),
        formatter = logFileFormatter
      )
      .replace()
  }

  def traceFileName(t: LocalDateTime): String = {
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")
    "besom-run-" + format.format(t) + ".log"
  }
end logging
