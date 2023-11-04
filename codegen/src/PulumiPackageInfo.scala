package besom.codegen

import besom.codegen.PackageMetadata.SchemaName
import besom.codegen.PackageVersion.PackageVersion

case class PulumiPackageInfo(
  name: SchemaName,
  version: PackageVersion,
  enumTypeTokens: Set[String],
  objectTypeTokens: Set[String],
  providerTypeToken: String,
  resourceTypeTokens: Set[String],
  moduleToPackageParts: String => Seq[String],
  providerToPackageParts: String => Seq[String]
)
