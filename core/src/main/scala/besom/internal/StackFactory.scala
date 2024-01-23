package besom.internal

trait StackFactory:
  val exports: EmptyExport.type = EmptyExport

  def apply(dependsOn: Output[?]*)(using Context): Stack =
    Stack.empty.copy(dependsOn = dependsOn.toVector)
