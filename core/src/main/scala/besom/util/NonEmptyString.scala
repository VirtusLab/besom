package besom.util

import scala.compiletime.{error, requireConst}
import scala.language.implicitConversions

extension (nes: NonEmptyString)
  inline def +++(other: String): NonEmptyString = nes + other
  inline def asString: String                   = nes

opaque type NonEmptyString <: String = String

object NonEmptyString:
  def apply(s: String): Option[NonEmptyString] =
    if s.isEmpty then None else Some(s)

  inline def from(inline s: String): NonEmptyString =
    requireConst(s)
    inline if s == "" then error("got an empty string") else s

  implicit inline def str2NonEmptyString(inline s: String): NonEmptyString = NonEmptyString.from(s)
