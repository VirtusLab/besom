package besom.util

import scala.compiletime.*
import scala.compiletime.ops.string.*
import scala.language.implicitConversions

object Types:

  opaque type ProviderType <: String = String

  object ProviderType:
    inline def from(inline s: String): ProviderType =
      requireConst(s)
      inline if !constValue[Matches[s.type, "pulumi:providers:.+"]] then 
        error("this string doesn't have a prefix of `pulumi:providers:` or the provider identifier is missing")
      else s

    implicit inline def str2NonEmptyString(inline s: String): ProviderType = ProviderType.from(s)

