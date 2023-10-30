package besom.codegen

import besom.codegen.SchemaProvider.{SchemaName, SchemaVersion}

case class PulumiPackageInfo(
  schemaName: SchemaName,
  schemaVersion: SchemaVersion,
  enumTypeTokens: Set[String],
  objectTypeTokens: Set[String],
  resourceTypeTokens: Set[String],
  moduleToPackageParts: String => Seq[String],
  providerToPackageParts: String => Seq[String]
)
