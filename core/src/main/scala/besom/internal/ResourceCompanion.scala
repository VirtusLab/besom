package besom.internal

import besom.types.ResourceType

trait ResourceCompanion[A <: Resource]:
  private[besom] def typeToken: ResourceType
