package yaga.codegen.aws.extractor

import tastyquery.Types.*

import yaga.codegen.core.extractor.ModelExtractor

class AwsModelExtractor extends ModelExtractor:
  override def isBuiltinClass(ref: TypeRef) =
    ref.showBasic.startsWith("yaga.extensions.aws.") || super.isBuiltinClass(ref)
