package besom.internal

import scala.annotation.unused

//noinspection ScalaFileName
class Resources private (
  private val resources: Ref[Map[Resource, ResourceState]],
  private val cache: Ref[Map[(String, String), Promise[Resource]]]
):
  def add(resource: ProviderResource, state: ProviderResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: CustomResource, state: CustomResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: ComponentBase, state: ComponentResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: RemoteComponentResource, state: ComponentResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: Resource, state: ResourceState): Result[Unit] = (resource, state) match
    case (pr: ProviderResource, prs: ProviderResourceState) =>
      add(pr, prs)
    case (cr: CustomResource, crs: CustomResourceState) =>
      add(cr, crs)
    case (rc: RemoteComponentResource, rcs: ComponentResourceState) =>
      add(rc, rcs)
    case (compb: ComponentBase, comprs: ComponentResourceState) =>
      add(compb, comprs)
    case _ =>
      resource.asString.flatMap(s => Result.fail(Exception(s"resource ${s} and state ${state} don't match")))

  def getStateFor(resource: ProviderResource): Result[ProviderResourceState] =
    resources.get.flatMap {
      _.get(resource) match
        case Some(state) =>
          state match
            case _: CustomResourceState =>
              resource.asString.flatMap(s => Result.fail(Exception(s"state for ProviderResource ${s} is a CustomResourceState!")))
            case prs: ProviderResourceState => Result.pure(prs)
            case _: ComponentResourceState =>
              resource.asString.flatMap(s => Result.fail(Exception(s"state for ProviderResource ${s} is a ComponentResourceState!")))

        case None =>
          resource.asString.flatMap(s => Result.fail(Exception(s"state for resource ${s} not found")))
    }

  def getStateFor(resource: CustomResource): Result[CustomResourceState] =
    resources.get.flatMap {
      _.get(resource) match
        case Some(state) =>
          state match
            case crs: CustomResourceState => Result.pure(crs)
            case _: ProviderResourceState =>
              resource.asString.flatMap(s => Result.fail(Exception(s"state for CustomResource ${s} is a ProviderResourceState!")))
            case _: ComponentResourceState =>
              resource.asString.flatMap(s => Result.fail(Exception(s"state for CustomResource ${s} is a ComponentResourceState!")))

        case None =>
          resource.asString.flatMap(s => Result.fail(Exception(s"state for resource ${s} not found")))
    }

  def getStateFor(resource: ComponentResource): Result[ComponentResourceState] =
    resources.get.flatMap {
      _.get(resource) match
        case Some(state) =>
          state match
            case _: CustomResourceState =>
              resource.asString.flatMap(s => Result.fail(Exception(s"state for ComponentResource ${s} is a CustomResourceState!")))
            case _: ProviderResourceState =>
              resource.asString.flatMap(s => Result.fail(Exception(s"state for ComponentResource ${s} is a ProviderResourceState!")))
            case comprs: ComponentResourceState => Result.pure(comprs)

        case None =>
          resource.asString.flatMap(s => Result.fail(Exception(s"state for resource ${s} not found")))
    }

  def getStateFor(resource: Resource): Result[ResourceState] =
    resources.get.flatMap {
      _.get(resource) match
        case Some(state) => Result.pure(state)
        case None        => resource.asString.flatMap(s => Result.fail(Exception(s"state for resource ${s} not found")))
    }

  def updateStateFor(resource: Resource)(f: ResourceState => ResourceState): Result[Unit] =
    resources.update(_.updatedWith(resource)(_.map(f)))

  def cacheResource(typ: String, name: String, @unused args: Any, @unused opts: ResourceOptions, resource: Resource): Result[Boolean] =
    cache.get.flatMap(_.get((typ, name)) match
      case Some(_) => Result.pure(false)
      case None =>
        Promise[Resource]().flatMap { promise =>
          cache
            .update(_.updated((typ, name), promise))
            .flatMap(_ => promise.fulfill(resource) *> Result.pure(true))
        }
    )

  def getCachedResource(typ: String, name: String, @unused args: Any, @unused opts: ResourceOptions): Result[Resource] =
    cache.get.flatMap(_((typ, name)).get)

end Resources

//noinspection ScalaFileName
object Resources:
  def apply(): Result[Resources] =
    for
      resources <- Ref(Map.empty[Resource, ResourceState])
      cache     <- Ref(Map.empty[(String, String), Promise[Resource]])
    yield new Resources(resources, cache)
