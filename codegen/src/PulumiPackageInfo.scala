package besom.codegen

import besom.codegen.metaschema.ConstValue
import besom.codegen.{PackageVersion, SchemaName}

type EnumInstanceName = String

case class PulumiPackageInfo(
  name: SchemaName,
  version: PackageVersion,
  allowedPackageNames: Set[SchemaName],
  providerTypeToken: String,
  moduleToPackageParts: String => Seq[String],
  providerToPackageParts: String => Seq[String],
  enumTypeTokens: Set[String],
  objectTypeTokens: Set[String],
  resourceTypeTokens: Set[String],
  enumValueToInstances: Map[PulumiToken, Map[ConstValue, EnumInstanceName]]
) {
  def asPackageMetadata: PackageMetadata = PackageMetadata(name, Some(version))
}
