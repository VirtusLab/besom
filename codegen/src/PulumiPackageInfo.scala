package besom.codegen

import scala.util.matching.Regex

case class PulumiPackageInfo(
  enumTypeTokens: Set[String],
  objectTypeTokens: Set[String],
  resourceTypeTokens: Set[String],
  moduleToPackageParts: String => Seq[String],
  moduleFormat: Regex
)