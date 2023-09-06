package besom.codegen

import scala.meta.Type
import besom.codegen.metaschema.{TypeReference, PulumiPackage}

object Utils {
  // Placeholder module for classes that should be in the root package (according to pulumi's convention)
  val indexModuleName = "index"
  val jvmMaxParamsCount = 253 // https://github.com/scala/bug/issues/7324 // TODO: Find some workaround to enable passing the remaining arguments

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asScalaType(asArgsType: Boolean = false)(implicit typeMapper: TypeMapper, logger: Logger): Type = typeMapper.asScalaType(typeRef, asArgsType)
  }

  implicit class PulumiPackageOps(pulumiPackage: PulumiPackage) {
    def moduleToPackageParts = pulumiPackage.language.java.packages.view.mapValues { pkg =>
      pkg.split("\\.").filter(_.nonEmpty).toSeq
    }.toMap.withDefault(pkg => Seq(pkg))
  }
}
