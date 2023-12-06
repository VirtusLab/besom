package besom.codegen

import besom.codegen.PackageMetadata.SchemaName
import besom.codegen.PackageVersion
import besom.codegen.metaschema.ConstValue

type EnumInstanceName = String

case class PulumiPackageInfo(
  name: SchemaName,
  version: PackageVersion,
  enumTypeTokens: Set[String],
  objectTypeTokens: Set[String],
  providerTypeToken: String,
  resourceTypeTokens: Set[String],
  moduleToPackageParts: String => Seq[String],
  providerToPackageParts: String => Seq[String],
  enumValueToInstances: Map[PulumiToken, Map[ConstValue, EnumInstanceName]]
) {
  def asPackageMetadata: PackageMetadata = PackageMetadata(name, Some(version))
}
