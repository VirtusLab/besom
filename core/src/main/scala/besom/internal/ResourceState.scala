package besom.internal

import besom.util.*
import besom.types.*

sealed trait ResourceState:
  def children: Set[Resource]
  def provider: Option[ProviderResource]
  def providers: Map[String, ProviderResource]
  def version: String
  def pluginDownloadUrl: String
  // def aliases: List[Output[String]]
  def name: NonEmptyString
  def typ: ResourceType
  // def transformations: List[ResourceTransformation]
  def keepDependency: Boolean

  def addChild(child: Resource): ResourceState = this match
    case crs: CustomResourceState =>
      crs.copy(common = crs.common.copy(children = crs.common.children + child))
    case prs: ProviderResourceState =>
      prs.copy(custom = prs.custom.copy(common = prs.custom.common.copy(children = prs.custom.common.children + child)))
    case comprs: ComponentResourceState =>
      comprs.copy(common = comprs.common.copy(children = comprs.common.children + child))

extension (rs: ResourceState) def asLabel: Label = Label.fromNameAndType(rs.name, rs.typ)

case class CommonResourceState(
  children: Set[Resource],
  provider: Option[ProviderResource],
  providers: Map[String, ProviderResource],
  version: String,
  pluginDownloadUrl: String,
  // aliases: List[Output[String]],
  name: NonEmptyString,
  typ: ResourceType,
  // transformations: List[ResourceTransformation],
  keepDependency: Boolean
):
  override def toString(): String = "CommonResourceState"

case class CustomResourceState(
  common: CommonResourceState,
  id: Output[ResourceId]
) extends ResourceState:
  export common.*
  override def toString(): String = "CustomResourceState"

case class ProviderResourceState(
  custom: CustomResourceState,
  pkg: String
) extends ResourceState:
  export custom.*
  override def toString(): String = "ProviderResourceState"

case class ComponentResourceState(
  common: CommonResourceState
) extends ResourceState:
  export common.*
  override def toString(): String = "ComponentResourceState"
