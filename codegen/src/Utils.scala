package besom.codegen

import scala.meta.Type
import besom.codegen.metaschema.{TypeReference, PulumiPackage}

object Utils {
  // Placeholder module for classes that should be in the root package (according to pulumi's convention)
  val indexModuleName = "index"

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asScalaType(asArgsType: Boolean = false)(implicit typeMapper: TypeMapper, logger: Logger): Type = typeMapper.asScalaType(typeRef, asArgsType)
  }

  implicit class PulumiPackageOps(pulumiPackage: PulumiPackage) {
    def moduleToPackageParts = pulumiPackage.language.java.packages.view.mapValues { pkg =>
      pkg.split("\\.").filter(_.nonEmpty).toSeq
    }.toMap.withDefault(pkg => Seq(pkg))
  }
}
