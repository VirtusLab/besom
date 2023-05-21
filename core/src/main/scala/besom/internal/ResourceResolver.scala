package besom.internal

trait ResourceResolver[A]:
  def resolve(errorOrResourceResult: Either[Throwable, RawResourceResult])(using Context): Result[Unit]
