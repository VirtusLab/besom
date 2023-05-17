package besom.internal

import besom.util.*
import scala.quoted.*
import scala.deriving.Mirror
import com.google.protobuf.struct.{Struct, Value}
import pulumirpc.resource.SupportsFeatureRequest
import besom.util.Types.ProviderType
import io.grpc.internal.DnsNameResolver.ResourceResolver

case class RawResourceResult(urn: String, id: Option[String], data: Struct, dependencies: Map[String, Set[Resource]])

case class EmptyArgs() derives ArgsEncoder

case class Stack(urn: Output[String]) extends ComponentResource derives ResourceDecoder
object Stack:
  val RootPulumiStackTypeName: NonEmptyString = "pulumi:pulumi:Stack"

trait ResourceResolver[A]:
  def resolve(errorOrResourceResult: Either[Throwable, RawResourceResult])(using Context): Result[Unit]

trait ResourceDecoder[A <: Resource]: // TODO rename to something more sensible
  def makeResolver(using Context): Result[(A, ResourceResolver[A])]

object ResourceDecoder:
  class CustomPropertyExtractor[A](propertyName: String, decoder: Decoder[A]):
    def extract(fields: Map[String, Value], dependencies: Map[String, Set[Resource]], resource: Resource)(using ctx: Context) =
      val fieldDependencies = dependencies.get(propertyName).getOrElse(Set.empty)

      val outputData = fields.get(propertyName).map { value =>
        decoder.decode(value).map(_.withDependency(resource)) match
          case Left(err) => throw err 
          case Right(value) => value
      }.getOrElse {
        if ctx.isDryRun then OutputData.unknown().withDependency(resource) 
        else OutputData.empty(Set(resource))
      }.withDependencies(fieldDependencies)

      outputData

  def makeResolver[A <: Resource](
    fromProduct: Product => A,
    customPropertyExtractors: Vector[CustomPropertyExtractor[?]]
  )(using Context): Result[(A, ResourceResolver[A])] =
    val customPropertiesCount = customPropertyExtractors.length
    val  customPropertiesResults = Result.sequence(
      Vector.fill(customPropertiesCount)(Promise[OutputData[Option[Any]]])
    )

    Promise[OutputData[String]].zip(Promise[OutputData[String]]).zip(customPropertiesResults).map { 
      case (urnPromise, idPromise, customPopertiesPromises) =>
        val allPromises = Vector(urnPromise, idPromise) ++ customPopertiesPromises.toList

        val propertiesOutputs = allPromises.map(promise => Output(promise.get)).toArray
        val resource = fromProduct(Tuple.fromArray(propertiesOutputs))

        def failAllPromises(err: Throwable): Result[Unit] =
          Result.sequence(
            allPromises.map(_.fail(err))
          ).void.flatMap(_ => Result.fail(err))

        val resolver = new ResourceResolver[A]:
          def resolve(errorOrResourceResult: Either[Throwable, RawResourceResult])(using ctx: Context): Result[Unit] =
            errorOrResourceResult match
              case Left(err) => failAllPromises(err)

              case Right(rawResourceResult) =>

                val urnOutputData = OutputData(rawResourceResult.urn).withDependency(resource)

                // TODO what if id is a blank string? does this happen? wtf?
                val idOutputData = rawResourceResult.id.map(OutputData(_).withDependency(resource)).getOrElse {
                  OutputData.unknown().withDependency(resource)
                }

                val fields = rawResourceResult.data.fields
                val dependencies = rawResourceResult.dependencies

                try
                  val propertiesFulfilmentResults = customPopertiesPromises.zip(customPropertyExtractors).map {
                    (promise, extractor) => promise.fulfillAny(extractor.extract(fields, dependencies, resource))
                  }

                  val fulfilmentResults = Vector(
                    urnPromise.fulfill(urnOutputData),
                    idPromise.fulfill(idOutputData),
                  ) ++ propertiesFulfilmentResults

                  Result.sequence(fulfilmentResults).void
                catch 
                  case err: DecodingError => failAllPromises(err)
        
        (resource, resolver)
    }

  inline def derived[A <: Resource]: ResourceDecoder[A] = ${ derivedImpl[A] }

  def derivedImpl[A <: Resource : Type](using q : Quotes): Expr[ResourceDecoder[A]] =
    Expr.summon[Mirror.Of[A]].get match
      case '{ $m: Mirror.ProductOf[A] { type MirroredElemLabels = elementLabels; type MirroredElemTypes = elementTypes } } =>
        def prepareExtractors(names: Type[?], types: Type[?]): List[Expr[CustomPropertyExtractor[?]]] =
          (names, types) match
            case ('[EmptyTuple], '[EmptyTuple]) => Nil
            case ('[namesHead *: namesTail], '[Output[typesHead] *: typesTail]) =>
              val propertyName = Expr(Type.valueOfConstant[namesHead].get.asInstanceOf[String])
              val propertyDecoder = Expr.summon[Decoder[typesHead]].getOrElse {
                quotes.reflect.report.errorAndAbort("Missing given instance of Decoder[" ++ Type.show[typesHead] ++ "]")
              } // TODO: Handle missing decoder
              val extractor = '{ CustomPropertyExtractor(${ propertyName }, ${propertyDecoder}) }
              extractor :: prepareExtractors(Type.of[namesTail], Type.of[typesTail])

        // Skip initial `urn` and `id` fields from the case class
        val customPropertiesOutputsType = Type.of[elementTypes] match
          case '[_ *: _ *: tpes] => Type.of[tpes]
        val customPropertiesNamesType = Type.of[elementLabels] match
          case '[_ *: _ *: tpes] => Type.of[tpes]
        

        val customPropertyExtractorsExpr = Expr.ofList(prepareExtractors(customPropertiesNamesType, customPropertiesOutputsType))

        '{
          new ResourceDecoder[A]:
            def makeResolver(using Context): Result[(A, ResourceResolver[A])] = 
              ResourceDecoder.makeResolver(
                fromProduct = ${ m }.fromProduct,
                customPropertyExtractors = ${ customPropertyExtractorsExpr }.toVector
              )
        }


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
  def version: String
  def pluginDownloadURL: String
  // def aliases: List[Output[F, String]]
  def name: String
  // def transformations: List[ResourceTransformation]
  def remoteComponent: Boolean

case class CommonResourceState(
  // urn: Output[String], // TODO BALEET, URN is in custom resource anyway
  // rawOutputs: Output[_], // TODO BALEET this is for StackReference only and is a hack used by pulumi-go, we'll use the non-hacky way from pulumi-java
  children: Set[Resource],
  provider: ProviderResource,
  version: String,
  pluginDownloadURL: String,
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
  common: CommonResourceState,
  providers: Map[String, ProviderResource]
) extends ResourceState:
  export common.*

// needed for parent/child relationship tracking
class ResourceManager(private val resources: Ref[Map[Resource, ResourceState]])

trait Resource:
  def urn: Output[String]

trait CustomResource extends Resource:
  def id: Output[String]

trait ComponentResource extends Resource

trait ProviderResource extends CustomResource

case class DependencyResource(urn: Output[String]) extends Resource

trait Context {

  def projectName: NonEmptyString
  def stackName: NonEmptyString
  def config: Config

  // just testing out possible shapes
  def component[Args: Encoder, Out](tpe: NonEmptyString, name: NonEmptyString, args: Args)(
    block: => Output[Out]
  ): Output[Out] = ???

  private[besom] val runInfo: RunInfo
  private[besom] val keepResources: Boolean
  private[besom] val keepOutputValues: Boolean
  private[besom] val monitor: Monitor
  private[besom] val engine: Engine
  private[besom] val workgroup: WorkGroup

  private[besom] def initializeStack: Result[Unit]

  private[besom] def getStack: Result[Stack]

  private[besom] def isDryRun: Boolean = runInfo.dryRun

  private[besom] def registerTask[A](fa: => Result[A]): Result[A]

  private[besom] def waitForAllTasks: Result[Unit]

  private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
    typ: ProviderType, // TODO: ProviderType
    name: NonEmptyString,
    args: A,
    options: CustomResourceOptions
  ): Output[R]

  private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder](
    typ: NonEmptyString,
    name: NonEmptyString
  ): Output[R]

  private[besom] def registerResource[R <: Resource: ResourceDecoder](
    typ: NonEmptyString,
    name: NonEmptyString
  ): Output[R]

  private[besom] def readResource[R <: Resource: ResourceDecoder](
    typ: NonEmptyString,
    name: NonEmptyString
  ): Output[R]

  private[besom] def createResourceState(
    typ: NonEmptyString,
    name: NonEmptyString,
    resourceOptions: ResourceOptions
  ): Result[ResourceState]

  private[besom] def close: Result[Unit]
}

object Context:

  private[besom] class ContextImpl(
    private[besom] val runInfo: RunInfo,
    private[besom] val keepResources: Boolean,
    private[besom] val keepOutputValues: Boolean,
    private[besom] val monitor: Monitor,
    private[besom] val engine: Engine,
    private[besom] val workgroup: WorkGroup,
    private[besom] val stackPromise: Promise[Stack]
  ) extends Context:

    val projectName: NonEmptyString = runInfo.project
    val stackName: NonEmptyString   = runInfo.stack
    val config: Config              = runInfo.config

    private[besom] def getStack: Result[Stack] = stackPromise.get

    private[besom] def initializeStack: Result[Unit] =
      val rootPulumiStackName = projectName +++ "-" +++ stackName
      for
        stack <- registerResourceInternal[Stack, EmptyArgs](
          Stack.RootPulumiStackTypeName,
          rootPulumiStackName,
          EmptyArgs(),
          ComponentResourceOptions(using this)() // TODO pass initial ResourceTransformations here
        )
        _ <- registerResourceOutputsInternal()
        _ <- stackPromise.fulfill(stack)
      yield ()

    private[besom] def registerProvider[R <: Resource: ResourceDecoder, A: ProviderArgsEncoder](
      typ: ProviderType,
      name: NonEmptyString,
      args: A,
      options: CustomResourceOptions
    ): Output[R] = ???

    override private[besom] def registerTask[A](fa: => Result[A]): Result[A] = workgroup.runInWorkGroup(fa)

    override private[besom] def waitForAllTasks: Result[Unit] = workgroup.waitForAll

    override private[besom] def close: Result[Unit] =
      for
        _ <- monitor.close()
        _ <- engine.close()
      yield ()

    private[besom] def registerResourceOutputsInternal(): Result[Unit] = ???

    override private[besom] def readOrRegisterResource[R <: Resource: ResourceDecoder](
      typ: NonEmptyString,
      name: NonEmptyString
    ): Output[R] = ???

    private[besom] def registerResourceInternal[R <: Resource: ResourceDecoder, A: ArgsEncoder](
      typ: NonEmptyString,
      name: NonEmptyString,
      args: A,
      options: ResourceOptions
    ): Result[R] = ???

    override private[besom] def registerResource[R <: Resource: ResourceDecoder](
      typ: NonEmptyString,
      name: NonEmptyString
    ): Output[R] = ???
    override private[besom] def readResource[R <: Resource: ResourceDecoder](
      typ: NonEmptyString,
      name: NonEmptyString
    ): Output[R] = ???
    // summon[ResourceDecoder[R]].makeFulfillable(using this) match
    //  case (r, fulfillable) =>

    private def resolveParent(typ: NonEmptyString, resourceOptions: ResourceOptions): Result[Option[Resource]] =
      if typ == Stack.RootPulumiStackTypeName then Result.pure(None)
      else
        resourceOptions.parent match
          case Some(parent) => Result.pure(Some(parent))
          case None         => getStack.map(Some(_))

    private def applyTransformations[A](
      args: A,
      resourceOptions: ResourceOptions,
      parent: Option[Resource]
    ): Result[(A, ResourceOptions)] =
      Result.pure((args, resourceOptions))

    private def mergeProviders(opts: ResourceOptions, parent: Option[Resource]): Result[Map[String, ProviderResource]] =
      ???

    override private[besom] def createResourceState(
      typ: NonEmptyString,
      name: NonEmptyString,
      resourceOptions: ResourceOptions
    ): Result[ResourceState] =
      val parent: Option[Resource] =
        if typ == Stack.RootPulumiStackTypeName then None
        else if resourceOptions.parent.isDefined then resourceOptions.parent
        else None // todo

      ???

  def apply(
    runInfo: RunInfo,
    keepResources: Boolean,
    keepOutputValues: Boolean,
    monitor: Monitor,
    engine: Engine,
    workgroup: WorkGroup,
    stackPromise: Promise[Stack]
  ): Context = new ContextImpl(
    runInfo,
    keepResources,
    keepOutputValues,
    monitor,
    engine,
    workgroup,
    stackPromise
  )

  def apply(
    runInfo: RunInfo,
    monitor: Monitor,
    engine: Engine,
    keepResources: Boolean,
    keepOutputValues: Boolean
  ): Result[Context] =
    for
      wg    <- WorkGroup()
      stack <- Promise[Stack]
    yield apply(runInfo, keepResources, keepOutputValues, monitor, engine, wg, stack)

  def apply(runInfo: RunInfo): Result[Context] =
    for
      monitor          <- Monitor(runInfo.monitorAddress)
      engine           <- Engine(runInfo.engineAddress)
      keepResources    <- monitor.supportsFeature(SupportsFeatureRequest("resourceReferences")).map(_.hasSupport)
      keepOutputValues <- monitor.supportsFeature(SupportsFeatureRequest("outputValues")).map(_.hasSupport)
      ctx              <- apply(runInfo, monitor, engine, keepResources, keepOutputValues)
      _                <- ctx.initializeStack
    yield ctx

object Providers:
  val ProviderResourceTypePrefix: NonEmptyString = "pulumi:providers:"
  def providerResourceType(`package`: NonEmptyString): NonEmptyString =
    ProviderResourceTypePrefix +++ `package`
