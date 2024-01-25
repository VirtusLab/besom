package besom.internal

/** The Stack is the final result of a Pulumi program. It contains the exports and dependencies of the program. * Exports are the values
  * that are exposed to the Pulumi runtime and available to other stacks via StackReference * Dependencies are the values that have to be
  * evaluated (and thus created) for the Stack to be created
  *
  * There are three ways to create a Stack in user's code:
  *
  * * Stack(a, b) - creates a stack with dependencies a and b
  *
  * * Stack.exports(a = x, b = y) - creates a stack with exports a and b
  *
  * * Stack(a, b).exports(c = x, d = y) - creates a stack with dependencies a and b and exports c and d
  */
trait StackFactory:
  val exports: EmptyExport.type = EmptyExport

  def apply(dependsOn: Output[?]*)(using Context): Stack =
    Stack.empty.copy(dependsOn = dependsOn.toVector)
