package besom.util

import besom.types.Output

object IgnoredOutput:
  def unapply[T](out: Output[T]): Some[Output[T]] = Some(out)

final class ignoreOutput extends scala.annotation.StaticAnnotation
