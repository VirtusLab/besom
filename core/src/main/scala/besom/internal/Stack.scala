package besom.internal

import com.google.protobuf.struct.Struct

case class Stack private[besom] (private val _exports: Exports, private val dependsOn: Vector[Output[?]]):
  private[besom] def evaluateDependencies(using Context): Result[Unit] =
    Output.sequence(dependsOn).getData.void

  private[besom] def getExports: Exports = _exports

  private[besom] def getDependsOn: Vector[Output[?]] = dependsOn

  def exports: Export = Export(this)

object Stack:
  def empty: Stack = Stack(Exports(Result.pure(Struct(Map.empty))), Vector.empty)
