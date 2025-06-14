package besom.internal

import besom.types.ResourceType
import besom.util.NonEmptyString

trait TransformedResourceInfo:
  def typ: ResourceType
  def name: NonEmptyString
  // def isCustom: Boolean

private[internal] class TransformedResourceInfoImpl(
  val typ: ResourceType,
  val name: NonEmptyString
) extends TransformedResourceInfo

sealed trait ResourceTransformation

trait ResourceArgsTransformation extends ResourceTransformation:
  def transformArgs(args: Any)(using info: TransformedResourceInfo): Any

trait ResourceOptsTransformation extends ResourceTransformation
