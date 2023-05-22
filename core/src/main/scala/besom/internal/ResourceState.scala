package besom.internal

import besom.util.Types.ResourceType

// type ResourceState struct {
// 	m sync.RWMutex

// 	urn URNOutput `pulumi:"urn"`

// 	rawOutputs        Output
// 	children          resourceSet
// 	providers         map[string]ProviderResource
// 	provider          ProviderResource
// 	version           string
// 	pluginDownloadURL string
// 	aliases           []URNOutput
// 	name              string
// 	transformations   []ResourceTransformation

// 	remoteComponent bool
// }

sealed trait ResourceState:
  // def urn: Output[String] // TODO BALEET, URN is in resource anyway
  // def rawOutputs: Output[_] // TODO BALEET this is for StackReference only and is a hack used by pulumi-go, we'll use the non-hacky way from pulumi-java
  def children: Set[Resource]
  def provider: Option[ProviderResource]
  def providers: Map[String, ProviderResource]
  def version: String
  def pluginDownloadUrl: String
  // def aliases: List[Output[F, String]]
  def name: String
  def typ: ResourceType
  // def transformations: List[ResourceTransformation]
  def remoteComponent: Boolean

  def addChild(child: Resource): ResourceState = this match
    case crs: CustomResourceState =>
      crs.copy(common = crs.common.copy(children = crs.common.children + child))
    case prs: ProviderResourceState =>
      prs.copy(custom = prs.custom.copy(common = prs.custom.common.copy(children = prs.custom.common.children + child)))
    case comprs: ComponentResourceState =>
      comprs.copy(common = comprs.common.copy(children = comprs.common.children + child))

case class CommonResourceState(
  // urn: Output[String], // TODO BALEET, URN is in custom resource anyway
  // rawOutputs: Output[_], // TODO BALEET this is for StackReference only and is a hack used by pulumi-go, we'll use the non-hacky way from pulumi-java
  children: Set[Resource],
  provider: Option[ProviderResource],
  providers: Map[String, ProviderResource],
  version: String,
  pluginDownloadUrl: String,
  // aliases: List[Output[F, String]],
  name: String,
  typ: ResourceType,
  // transformations: List[ResourceTransformation],
  remoteComponent: Boolean
)

case class CustomResourceState(
  common: CommonResourceState,
  id: Output[String]
) extends ResourceState:
  export common.*

case class ProviderResourceState(
  custom: CustomResourceState,
  pkg: String
) extends ResourceState:
  export custom.*

case class ComponentResourceState(
  common: CommonResourceState
) extends ResourceState:
  export common.*
