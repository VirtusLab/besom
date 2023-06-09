package besom.internal

import scribe.*, LoggerSupport.{apply => makeLogRecord}
import scribe.message.LoggableMessage
import scribe.output.LogOutput
import scribe.data.{MDC => ScribeMDC}
import sourcecode.{FileName, Line, Name, Pkg}
import scala.util.{NotGiven => Not}

import scala.language.implicitConversions
import pulumirpc.engine.{LogRequest, LogSeverity}
import besom.util.Types.Label

object logging:

  enum Key[A](val value: String):
    case LabelKey extends Key[Label]("resource")

  class MDC[A](private val inner: ScribeMDC) extends ScribeMDC:
    def get[B](key: Key[B])(using ev: A <:< B): B = key match
      case Key.LabelKey =>
        val opt = inner.get(key.value)

        println(opt)
        val v = opt.map(_.apply())
        println(v)
        v
          .getOrElse { throw Exception(s"Expected key '${key.value}' was not found in MDC!") }
          .asInstanceOf[B]

    def put[B](key: Key[A], value: A)(using Not[A <:< B]): MDC[A & B] =
      inner.update(key.value, value)
      this.asInstanceOf[MDC[A & B]]

    def +[B](key: Key[A], value: A)(using Not[A <:< B]): MDC[A & B] = put(key, value)

    def apply[R](block: MDC[A] ?=> R): R = block(using this)

    export inner.*

  object MDC:
    def apply[A](key: Key[A], value: A): MDC[A] = ScribeMDC { scribeMdc =>
      scribeMdc.update(key.value, value)
      new MDC(scribeMdc)
    }

  def log(using Context): BesomLogger = summon[Context].logger

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

    def log(level: Level, mdc: MDC[_], urn: String, streamId: Int, ephemeral: Boolean, messages: LoggableMessage*)(using
      pkg: Pkg,
      fileName: FileName,
      name: Name,
      line: Line
    ): Result[Unit] =
      log(makeLogRecord(level, messages.toList, pkg, fileName, name, line, mdc), urn, streamId, ephemeral)

    def trace(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC[_]
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Trace, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Trace, mdc, "", streamId, ephemeral, message)

    def debug(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC[_]
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Debug, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Debug, mdc, "", streamId, ephemeral, message)

    def info(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC[_]
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Info, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Info, mdc, "", streamId, ephemeral, message)

    def warn(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC[_]
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Warn, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Warn, mdc, "", streamId, ephemeral, message)

    def error(message: LoggableMessage, res: Option[Resource] = None, streamId: Int = 0, ephemeral: Boolean = false)(
      using
      pkg: sourcecode.Pkg,
      fileName: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line,
      mdc: MDC[_]
    ): Result[Unit] =
      res match
        case Some(r) =>
          r.urn.getValueOrElse("").flatMap(urn => log(Level.Error, mdc, urn, streamId, ephemeral, message))
        case None => log(Level.Error, mdc, "", streamId, ephemeral, message)

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

  private[logging] val formatter = Formatter.fromBlocks(
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
        formatter = formatter
      )
      .replace()
  }

  def enableTraceLevelFileLogging(): Result[Unit] = Result.defer {
    val format      = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val now         = LocalDateTime.now()
    val fileName    = "besom-run-" + format.format(now) + ".log"
    val pathBuilder = PathBuilder.static(Paths.get("logs")) / fileName
    scribe.Logger.root
      .clearHandlers()
      .clearModifiers()
      .withHandler(
        minimumLevel = Some(Level.Trace),
        writer = FileWriter(pathBuilder = pathBuilder),
        formatter = formatter
      )
      .replace()
  }
