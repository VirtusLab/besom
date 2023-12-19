package besom.util

private[besom] def isTruthy(s: String): Boolean    = s == "1" || s.equalsIgnoreCase("true")
private[besom] def isNotTruthy(s: String): Boolean = !isTruthy(s)

given eitherOps: {} with

  extension [E <: Throwable, A](a: Either[E, A]) def get: A = a.fold(e => throw e, identity)

  extension [L, R](a: Either[L, R])
    def bimap[L1, R1](left: L => L1, right: R => R1): Either[L1, R1] = a.fold(l => Left(left(l)), r => Right(right(r)))
