package besom.model.pcl

import besom.model.{hcl2 => model}

/** Represents a semantically-analyzed Pulumi HCL2 program.
  *
  * @return
  *   create a new program with a list of nodes
  * @param nodes
  *   the nodes of the program
  */
case class Program(
  nodes: List[Node]
)

/** Node represents a single definition in a program or component. Nodes may be config, locals, resources, components, or outputs.
  */
sealed trait NodeLike

/** Resource represents a resource instantiation inside of a program or component.
  */
sealed trait ResourceLike extends NodeLike

/** Component represents a component reference in a program.
  */
sealed trait ComponentLike extends NodeLike

/** ConfigVariable represents a program- or component-scoped input variable. The value for a config variable may come from stack
  * configuration or component inputs, respectively, and may have a default value.
  */
sealed trait ConfigLike extends NodeLike

/** LocalVariable represents a program- or component-scoped local variable.
  */
sealed trait LocalLike extends NodeLike

/** OutputVariable represents a program- or component-scoped output variable.
  */
sealed trait OutputLike extends NodeLike

enum Node extends NodeLike:
  case Resource(name: String, `type`: model.Type) extends ResourceLike
  case Component(name: String, `type`: model.Type) extends ComponentLike
  case Config(name: String, `type`: model.Type) extends ConfigLike
  case Local(name: String, `type`: model.Type) extends LocalLike
  case Output(name: String, `type`: model.Type) extends OutputLike
