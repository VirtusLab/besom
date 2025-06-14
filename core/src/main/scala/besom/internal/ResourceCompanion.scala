package besom.internal

import besom.types.ResourceType

trait ResourceCompanion[R <: Resource, A] extends TypedResourceMatcher:
  override type Args = A

  private[besom] def typeToken: ResourceType

  override def matchesResource(resourceInfo: TransformedResourceInfo): Boolean =
    resourceInfo.typ == this.typeToken

trait CustomResourceCompanion
