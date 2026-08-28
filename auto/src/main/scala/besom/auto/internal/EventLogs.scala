package besom.auto.internal

import scala.util.Try
import scala.util.control.NonFatal

/** A line of the engine event log that could not be decoded.
  *
  * Parse errors are collected instead of failing the operation they belong to - Pulumi is free to add new event types and new values of
  * existing enums at any time and a successful deployment must not be reported as a failure because of it.
  *
  * @param lineNumber
  *   the 1-based number of the offending line in the event log
  * @param line
  *   the raw content of the offending line
  * @param error
  *   the decoding error
  */
case class EventLogParseError(lineNumber: Int, line: String, error: Exception)

/** The result of decoding an engine event log.
  *
  * @param events
  *   all events that could be decoded, in the order the engine emitted them
  * @param parseErrors
  *   all lines that could not be decoded
  */
case class ParsedEventLog(events: List[EngineEvent], parseErrors: List[EventLogParseError])
object ParsedEventLog:
  val empty: ParsedEventLog = ParsedEventLog(Nil, Nil)

/** Everything related to reading Pulumi's `--event-log` file, both post-hoc and live. */
private[auto] object EventLogs:

  /** Reads and decodes an engine event log.
    *
    * Undecodable lines are collected in [[ParsedEventLog.parseErrors]] and never fail the read; only a failure to read the file itself
    * produces a `Left`.
    *
    * @param path
    *   the path of the event log
    * @return
    *   the decoded log or an error if the file could not be read
    */
  def parse(path: os.Path): Either[Exception, ParsedEventLog] =
    Try(os.read.lines(path)).toEither.left
      .map(e => AutoError(s"Failed to read event log: $path", e))
      .map { lines =>
        val (events, errors) = lines.iterator.zipWithIndex
          .filter { case (line, _) => line.nonEmpty }
          .foldLeft((List.empty[EngineEvent], List.empty[EventLogParseError])) { case ((events, errors), (line, idx)) =>
            EngineEvent.fromJson(line) match
              case Right(event) => (event :: events, errors)
              case Left(error)  => (events, EventLogParseError(idx + 1, line, error) :: errors)
          }
        ParsedEventLog(events.reverse, errors.reverse)
      }
  end parse

  /** Finds the [[SummaryEvent]] emitted at the end of an operation.
    *
    * @param events
    *   the decoded events
    * @param parseErrors
    *   the parse errors from the same log, used to explain a missing summary caused by a wire format drift
    * @return
    *   the summary event or an error if there was none
    */
  def summary(events: List[EngineEvent], parseErrors: List[EventLogParseError] = Nil): Either[Exception, SummaryEvent] =
    events
      .collectFirst { case e if e.summaryEvent.isDefined => e.summaryEvent.get }
      .toRight {
        val suffix =
          if parseErrors.isEmpty then ""
          else s" (${parseErrors.size} event log line(s) failed to parse, the summary event may be among them)"
        AutoError(s"No summary event found in event log$suffix")
      }
  end summary

  def resourcePreEvents(events: List[EngineEvent]): List[ResourcePreEvent] = events.flatMap(_.resourcePreEvent)
  def resourceOutputs(events: List[EngineEvent]): List[ResOutputsEvent]    = events.flatMap(_.resOutputsEvent)
  def failures(events: List[EngineEvent]): List[ResOpFailedEvent]          = events.flatMap(_.resOpFailedEvent)
  def diagnostics(events: List[EngineEvent]): List[DiagnosticEvent]        = events.flatMap(_.diagnosticEvent)

  /** Runs `body` while tailing `path`, handing every decoded engine event to `handler` as it is written.
    *
    * The follower is opened before `body` runs, so no event written by the subprocess can be missed. When `body` returns the follower is
    * stopped, which drains whatever is already on disk before signalling EOF, so events written just before the process exited are still
    * delivered.
    *
    * Undecodable lines are dropped here - [[parse]] accounts for them post-hoc in `parseErrors`. Exceptions thrown by `handler` are
    * swallowed so that a single misbehaving consumer cannot end the stream.
    *
    * Every event is delivered before this returns. `stop()` takes effect at EOF and the reader re-checks it after each `rereadSleep`
    * (100ms), so the wait is short - but it is unbounded, which means a `handler` that never returns blocks the operation from returning.
    * That is the reason handlers must not block.
    *
    * Caveat: `tailf`'s follower treats a file that got shorter than the current read position as rotated and restarts from offset 0.
    * Nothing truncates a Pulumi event log mid-run, but it is the only path that could replay events, so consumers should stay idempotent
    * per URN.
    *
    * @param path
    *   the event log to tail, which must already exist
    * @param handler
    *   the consumer of the events, or `None` to run `body` without tailing at all
    * @param body
    *   the operation producing the events
    * @return
    *   the result of `body`, or a `Left` if the follower could not be opened
    */
  def around[A](path: os.Path, handler: Option[EngineEvent => Unit])(body: => Either[Exception, A]): Either[Exception, A] =
    handler match
      case None                        => body
      case Some(_) if !os.exists(path) =>
        // tailf would happily wait for the file to appear; for us its absence is a setup error, not something to wait out
        Left(AutoError(s"Cannot stream engine events, event log does not exist: $path"))
      case Some(onEvent) =>
        shell.tail(path).flatMap { follower =>
          val reader = new Thread(
            new Runnable:
              def run(): Unit =
                try
                  val lines = scala.io.Source.fromInputStream(follower)(using scala.io.Codec.UTF8).getLines()
                  while lines.hasNext do
                    val line = lines.next()
                    if line.nonEmpty then
                      EngineEvent.fromJson(line) match
                        case Right(event) =>
                          try onEvent(event)
                          catch case NonFatal(_) => () // a broken consumer must not end the stream
                        case Left(_) => () // accounted for post-hoc by parse
                catch case NonFatal(_) => () // the follower was closed from under us, nothing left to read
            ,
            s"besom-auto-event-tail-${path.last}"
          )
          reader.setDaemon(true)
          reader.start()

          try body
          finally
            follower.stop() // takes effect at EOF, so the reader drains what is already written and then ends
            try reader.join() // no deadline: cutting the reader short here would silently drop events it still holds
            finally
              try follower.close()
              catch case NonFatal(_) => () // releasing the fd must not mask the operation's own outcome
        }
  end around

end EventLogs
