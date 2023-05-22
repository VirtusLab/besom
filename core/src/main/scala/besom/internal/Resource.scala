package besom.internal

import besom.util.NonEmptyString
import besom.util.Types.*
import com.google.protobuf.struct.{Struct, Value}
import scala.quoted.*
import scala.deriving.Mirror

sealed trait Resource:
  def urn: Output[String]
  private[internal] def isCustom: Boolean = this match
    case _: CustomResource => true
    case _                 => false

trait CustomResource extends Resource:
  def id: Output[String]

trait ComponentResource extends Resource

trait ProviderResource extends CustomResource:
  private[internal] def registrationId: Result[String] =
    for
      urn <- urn.getValueOrElse("")
      id  <- id.getValueOrElse(Constants.UnknownValue)
    yield s"${urn}::${id}"

case class DependencyResource(urn: Output[String]) extends Resource derives ResourceDecoder

case class Stack(urn: Output[String]) extends ComponentResource derives ResourceDecoder
object Stack:
  val RootPulumiStackTypeName: ResourceType = "pulumi:pulumi:Stack"
