package besom.internal

import besom.util.NonEmptyString
import besom.types.*
import com.google.protobuf.struct.*
import scala.deriving.Mirror
import scala.annotation.implicitNotFound
import besom.util.*

/** An abstract representation of a Pulumi resource. It is used to register resources with the Pulumi engine.
  */
sealed trait Resource:
  /** @return
    *   the URN of the resource
    */
  def urn: Output[URN]

  /** @return
    *   the logical name of this [[besom.internal.Resource]]
    */
  def pulumiResourceName: Output[String] = urn.map(_.resourceName)

  private[internal] def isCustom: Boolean = this match
    case _: CustomResource => true
    case _                 => false

trait CustomResource extends Resource:
  /** @return
    *   the [[ResourceId]] of the resource
    */
  def id: Output[ResourceId]

trait ComponentResource(using
  @implicitNotFound(
    "A component resource class should have a `(using ComponentBase)` parameter clause at the end of its constructor"
  )
  base: ComponentBase
) extends Resource:
  /** @return
    *   the URN of the resource
    */
  override def urn: Output[URN] = base.urn

trait ProviderResource extends CustomResource:
  private[internal] def registrationId: Result[String] =
    for
      urn <- urn.getValueOrElse(URN.empty)
      id  <- id.getValueOrElse(Constants.UnknownValue)
    yield s"${urn}::${id}"

case class DependencyResource(urn: Output[URN]) extends Resource derives ResourceDecoder

case class StackResource()(using ComponentBase) extends ComponentResource
object StackResource:
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

  def initializeStack(runInfo: RunInfo)(using ctx: Context): Result[StackResource] =
    for given ComponentBase <- ctx.registerComponentResource(stackName(runInfo), RootPulumiStackTypeName)
    yield StackResource()

case class ComponentBase(urn: Output[URN]) extends Resource derives ResourceDecoder
