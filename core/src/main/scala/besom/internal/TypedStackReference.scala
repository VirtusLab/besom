package besom.internal

import besom.types.*

case class TypedStackReference[T](
  urn: Output[URN],
  id: Output[ResourceId],
  name: Output[String],
  outputs: T,
  secretOutputNames: Output[Set[String]]
) extends CustomResource
