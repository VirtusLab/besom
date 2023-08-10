package besom.internal

// case class EmptyArgs() derives ArgsEncoder

class EmptyArgs() extends ProductLike:
  override def productElems = List.empty

object EmptyArgs:
  given ProductLikeMirror[EmptyArgs] with
    type MirroredElemLabels = EmptyTuple
    type MirroredElemTypes = EmptyTuple
    //def fromProductLike(p: ProductLike): EmptyArgs = EmptyArgs()

  given ArgsEncoder[EmptyArgs] = ArgsEncoder.derived[EmptyArgs]
