package besom.internal

import scala.concurrent.duration.Duration
import besom.util.*

/** Custom timeouts for create, update and delete operations.
  *
  * @param create
  *   The optional create timeout represented as a string e.g. `5.minutes`, `40.seconds`, `1.day`
  * @param update
  *   The optional update timeout represented as a string e.g. `5.minutes`, `40.seconds`, `1.day`
  * @param delete
  *   The optional delete timeout represented as a string e.g. `5.minutes`, `40.seconds`, `1.day`
  * @return
  *   A new instance of [[CustomTimeouts]]
  *
  * @see
  *   [[BesomSyntax.opts]]
  * @see
  *   [[besom.ComponentResourceOptions]]
  * @see
  *   [[besom.CustomResourceOptions]]
  * @see
  *   [[besom.StackReferenceResourceOptions]]
  */
case class CustomTimeouts(create: Option[Duration], update: Option[Duration], delete: Option[Duration])

object CustomTimeouts:
  // noinspection ScalaUnusedSymbol
  private[besom] def toGoDurationString(duration: Duration): String = s"${duration.toNanos}ns"

  def apply(
    create: NotProvidedOr[Duration] = NotProvided,
    update: NotProvidedOr[Duration] = NotProvided,
    delete: NotProvidedOr[Duration] = NotProvided
  ): CustomTimeouts = new CustomTimeouts(create.asOption, update.asOption, delete.asOption)

/** Companion object for [[CustomTimeouts]] */
trait CustomTimeoutsFactory:
  def apply(
    create: NotProvidedOr[Duration] = NotProvided,
    update: NotProvidedOr[Duration] = NotProvided,
    delete: NotProvidedOr[Duration] = NotProvided
  ): CustomTimeouts = new CustomTimeouts(create.asOption, update.asOption, delete.asOption)
