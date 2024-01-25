package besom.internal

import com.google.protobuf.struct.Struct

/** The Stack is the final result of a Pulumi program. It contains the exports and dependencies of the program. * Exports are the values
  * that are exposed to the Pulumi runtime and available to other stacks via StackReference * Dependencies are the values that have to be
  * evaluated (and thus created) for the Stack to be created.
  *
  * The Stack is created in user's code using [[StackFactory]] and not directly to offer a nicer API.
  *
  * @param _exports
  * @param dependsOn
  */
case class Stack private[besom] (private val _exports: Exports, private val dependsOn: Vector[Output[?]]):
  private[besom] def evaluateDependencies(using Context): Result[Unit] =
    Output.sequence(dependsOn).getData.void

  private[besom] def getExports: Exports = _exports

  private[besom] def getDependsOn: Vector[Output[?]] = dependsOn

  def exports: Export = Export(this)

object Stack:
  def empty: Stack = Stack(Exports(Result.pure(Struct(Map.empty))), Vector.empty)
