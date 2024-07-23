package besom.codegen.crd

extension [A](ie: Iterable[Either[Throwable, A]])
  def flattenWithFirstError: Either[Throwable, Iterable[A]] =
    val (error, pass) = ie.partitionMap(identity)
    if (error.nonEmpty) Left(error.head) else Right(pass)

extension (s: String)
  def toWorkingDirectoryPath: os.Path =
    besom.codegen.FilePath(s.split("/").toSeq).osSubPath.resolveFrom(os.pwd)
