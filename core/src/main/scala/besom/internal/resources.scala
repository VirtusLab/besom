package besom.internal

class Resources(private val resources: Ref[Map[Resource, ResourceState]]):
  def add(resource: ProviderResource, state: ProviderResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: CustomResource, state: CustomResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: ComponentResource, state: ComponentResourceState): Result[Unit] =
    resources.update(_ + (resource -> state))

  def add(resource: Resource, state: ResourceState): Result[Unit] = (resource, state) match
    case (pr: ProviderResource, prs: ProviderResourceState) =>
      add(pr, prs)
    case (cr: CustomResource, crs: CustomResourceState) =>
      add(cr, crs)
    case (compr: ComponentResource, comprs: ComponentResourceState) =>
      add(compr, comprs)
    case _ => Result.fail(new Exception(s"resource ${resource} and state ${state} don't match"))

  def getStateFor(resource: ProviderResource): Result[ProviderResourceState] =
    resources.get.flatMap(_.get(resource) match
      case Some(state) =>
        state match
          case crs: CustomResourceState =>
            Result.fail(new Exception(s"state for ProviderResource ${resource} is a CustomResourceState!"))
          case prs: ProviderResourceState => Result.pure(prs)
          case comprs: ComponentResourceState =>
            Result.fail(new Exception(s"state for ProviderResource ${resource} is a ComponentResourceState!"))

      case None => Result.fail(new Exception(s"state for resource ${resource} not found"))
    )

  def getStateFor(resource: CustomResource): Result[CustomResourceState] =
    resources.get.flatMap(_.get(resource) match
      case Some(state) =>
        state match
          case crs: CustomResourceState => Result.pure(crs)
          case prs: ProviderResourceState =>
            Result.fail(new Exception(s"state for CustomResource ${resource} is a ProviderResourceState!"))
          case comprs: ComponentResourceState =>
            Result.fail(new Exception(s"state for CustomResource ${resource} is a ComponentResourceState!"))

      case None => Result.fail(new Exception(s"state for resource ${resource} not found"))
    )

  def getStateFor(resource: ComponentResource): Result[ComponentResourceState] =
    resources.get.flatMap(_.get(resource) match
      case Some(state) =>
        state match
          case crs: CustomResourceState =>
            Result.fail(new Exception(s"state for ComponentResource ${resource} is a CustomResourceState!"))
          case prs: ProviderResourceState =>
            Result.fail(new Exception(s"state for ComponentResource ${resource} is a ProviderResourceState!"))
          case comprs: ComponentResourceState => Result.pure(comprs)

      case None => Result.fail(new Exception(s"state for resource ${resource} not found"))
    )

  def getStateFor(resource: Resource): Result[ResourceState] =
    resources.get.flatMap(_.get(resource) match
      case Some(state) => Result.pure(state)
      case None        => Result.fail(new Exception(s"state for resource ${resource} not found"))
    )

  def updateStateFor(resource: Resource)(f: ResourceState => ResourceState): Result[Unit] =
    resources.update(_.updatedWith(resource)(_.map(f)))

object Resources:
  def apply(): Result[Resources] = Ref(Map.empty).map(new Resources(_))
