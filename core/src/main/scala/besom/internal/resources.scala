package besom.internal

import besom.util.NonEmptyString
import besom.util.Types.*
import com.google.protobuf.struct.{Struct, Value}
import scala.quoted.*
import scala.deriving.Mirror

trait Resource:
  def urn: Output[String]

trait CustomResource extends Resource:
  def id: Output[String]

trait ComponentResource extends Resource

trait ProviderResource extends CustomResource

case class DependencyResource(urn: Output[String]) extends Resource derives ResourceDecoder

case class Stack(urn: Output[String]) extends ComponentResource derives ResourceDecoder
object Stack:
  val RootPulumiStackTypeName: ResourceType = "pulumi:pulumi:Stack"

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
  def provider: ProviderResource
  def providers: Map[String, ProviderResource]
  def version: String
  def pluginDownloadUrl: String
  // def aliases: List[Output[F, String]]
  def name: String
  // def transformations: List[ResourceTransformation]
  def remoteComponent: Boolean

case class CommonResourceState(
  // urn: Output[String], // TODO BALEET, URN is in custom resource anyway
  // rawOutputs: Output[_], // TODO BALEET this is for StackReference only and is a hack used by pulumi-go, we'll use the non-hacky way from pulumi-java
  children: Set[Resource],
  provider: ProviderResource,
  providers: Map[String, ProviderResource],
  version: String,
  pluginDownloadUrl: String,
  // aliases: List[Output[F, String]],
  name: String,
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
