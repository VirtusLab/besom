package besom.internal

class ResourceStateMismatchException(msg: String) extends Exception(msg)
object ResourceStateMismatchException:
  def fail(r: Resource, state: ResourceState, expected: String)(using dbg: Debug): Result[Nothing] =
    (for
      rstr <- r.asString
      msg = s"state for resource $r / ${rstr.getOrElse("???")} is $state, expected $expected, caller: $dbg"
    yield new ResourceStateMismatchException(msg)).flatMap(e => Result.fail(e))

class ResourceStateMissingException(msg: String) extends Exception(msg)
object ResourceStateMissingException:
  inline private def nl = System.lineSeparator
  def fail(r: Resource, rs: Map[Resource, ResourceState])(using dbg: Debug): Result[Nothing] =
    (for
      rstr <- r.asString
      msg = s"state for resource $r / ${rstr.getOrElse("???")} not found$nl - caller: $dbg$nl - state available for resources:$nl${rs.keys
          .mkString("   * ", nl + "   * ", "")}"
    yield new ResourceStateMissingException(msg)).flatMap(e => Result.fail(e))

class Resources private (private val resources: Ref[Map[Resource, ResourceState]]):
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

  def getStateFor(resource: ProviderResource)(using Debug): Result[ProviderResourceState] =
    resources.get.flatMap { rs =>
      rs.get(resource) match
        case Some(state) =>
          state match
            case prs: ProviderResourceState => Result.pure(prs)
            case _                          => ResourceStateMismatchException.fail(resource, state, "ProviderResourceState")

        case None =>
          ResourceStateMissingException.fail(resource, rs)
    }

  def getStateFor(resource: CustomResource)(using Debug): Result[CustomResourceState] =
    resources.get.flatMap { rs =>
      rs.get(resource) match
        case Some(state) =>
          state match
            case crs: CustomResourceState => Result.pure(crs)
            case _                        => ResourceStateMismatchException.fail(resource, state, "CustomResourceState")

        case None =>
          ResourceStateMissingException.fail(resource, rs)
    }

  def getStateFor(resource: ComponentResource)(using Debug): Result[ComponentResourceState] =
    resources.get.flatMap { rs =>
      rs.get(resource.componentBase) match
        case Some(state) =>
          state match
            case comprs: ComponentResourceState => Result.pure(comprs)
            case _                              => ResourceStateMismatchException.fail(resource, state, "ComponentResourceState")

        case None =>
          ResourceStateMissingException.fail(resource, rs)
    }

  def getStateFor(resource: Resource)(using dbg: Debug): Result[ResourceState] =
    resources.get.flatMap { rs =>
      resource match
        case compr: ComponentResource =>
          rs.get(compr.componentBase) match
            case Some(state) => Result.pure(state)
            case None        => ResourceStateMissingException.fail(resource, rs)
        case _ =>
          rs.get(resource) match
            case Some(state) => Result.pure(state)
            case None        => ResourceStateMissingException.fail(resource, rs)
    }

  def updateStateFor(resource: Resource)(f: ResourceState => ResourceState): Result[Unit] =
    resources.update(_.updatedWith(resource)(_.map(f)))

end Resources

object Resources:
  def apply(): Result[Resources] =
    for resources <- Ref(Map.empty[Resource, ResourceState])
    yield new Resources(resources)
