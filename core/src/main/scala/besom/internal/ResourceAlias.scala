package besom.internal

/** Allows to specify details of parent of resource for alias.
  */
sealed trait ResourceAliasParent

/** Allows to specify urn of parent.
  * @param urn
  *   of parent
  */
final case class ResourceAliasParentUrn(urn: String) extends ResourceAliasParent

/** Allows to indicate that resource previously had no parent. This property is ignored when set to false.
  * @param noParent
  *   set to true when no parent
  */
final case class ResourceAliasNoParent(noParent: Boolean) extends ResourceAliasParent

/** The alias for a resource or component resource. It can be any combination of the old name, type, parent, stack, and/or project values.
  * Alternatively, you can just specify the URN directly.
  */
sealed trait ResourceAlias

/** Allows to specify urn of resource.
  * @param urn
  *   of resource
  */
final case class UrnResourceAlias(urn: String) extends ResourceAlias

/** Allows to specify any combination of old name, type, parent, stack, and/or project values.
  * @param name
  *   of the old resource
  * @param `type`
  *   of the old resource
  * @param stack
  *   of the old resource
  * @param project
  *   of the old resource
  * @param parent
  *   of the old resource
  */
final case class SpecResourceAlias(
  name: Option[String] = None,
  `type`: Option[String] = None,
  stack: Option[String] = None,
  project: Option[String] = None,
  parent: Option[ResourceAliasParent] = None
) extends ResourceAlias
