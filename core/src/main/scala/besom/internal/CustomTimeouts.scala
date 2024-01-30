package besom.internal

import scala.concurrent.duration.Duration
import besom.util.*

case class CustomTimeouts(create: Option[Duration], update: Option[Duration], delete: Option[Duration])

object CustomTimeouts:
  def apply(
    create: Duration | NotProvided = NotProvided,
    update: Duration | NotProvided = NotProvided,
    delete: Duration | NotProvided = NotProvided
  ): CustomTimeouts = CustomTimeouts(create.asOption, update.asOption, delete.asOption)

  private[besom] def toGoDurationString(duration: Duration): String = s"${duration.toNanos}ns"
