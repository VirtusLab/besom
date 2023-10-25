package besom.codegen

case class PulumiPackageInfo(
  enumTypeTokens: Set[String],
  objectTypeTokens: Set[String],
  resourceTypeTokens: Set[String],
  moduleToPackageParts: String => Seq[String],
  providerToPackageParts: String => Seq[String]
)
