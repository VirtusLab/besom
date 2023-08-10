package besom.internal

trait ProductLike:
  def productElems: List[Any]

trait ProductLikeMirror[A]:
  type MirroredElemLabels <: Tuple
  type MirroredElemTypes <: Tuple
  //def fromProductLike(p: ProductLike): A