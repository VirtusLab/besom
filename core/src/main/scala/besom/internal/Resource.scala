package besom.internal

import besom.util.NonEmptyString
import besom.util.Types.*
import com.google.protobuf.struct.*
import scala.quoted.*
import scala.deriving.Mirror
import scala.annotation.implicitNotFound
import besom.util.*

sealed trait Resource:
  def urn: Output[URN]
  private[internal] def isCustom: Boolean = this match
    case _: CustomResource => true
    case _                 => false

trait CustomResource extends Resource:
  def id: Output[ResourceId]

trait ComponentResource(using
  @implicitNotFound(
    "A component resource class should have a `(using ComponentBase)` parameter clause at the end of its constructor"
  )
  base: ComponentBase
) extends Resource:
  override def urn: Output[URN] = base.urn

trait ProviderResource extends CustomResource:
  private[internal] def registrationId: Result[String] =
    for
      urn <- urn.getValueOrElse(URN.empty)
      id  <- id.getValueOrElse(Constants.UnknownValue)
    yield s"${urn}::${id}"

case class DependencyResource(urn: Output[URN]) extends Resource derives ResourceDecoder

case class Stack()(using ComponentBase) extends ComponentResource
object Stack:
  val RootPulumiStackTypeName: ResourceType = "pulumi:pulumi:Stack"

  def stackName(runInfo: RunInfo): NonEmptyString =
    runInfo.project +++ "-" +++ runInfo.stack

  def registerStackOutputs(runInfo: RunInfo, userOutputs: Result[Struct])(using
    ctx: Context
  ): Result[Unit] =
    ctx.registerResourceOutputs(
      stackName(runInfo),
      RootPulumiStackTypeName,
      ctx.getParentURN,
      userOutputs
    )

  def initializeStack(runInfo: RunInfo)(using ctx: Context): Result[Stack] =
    for given ComponentBase <- ctx.registerComponentResource(stackName(runInfo), RootPulumiStackTypeName)
    yield Stack()

case class ComponentBase(urn: Output[URN]) extends Resource derives ResourceDecoder
