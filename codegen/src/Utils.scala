package besom.codegen

import scala.meta.Type
import besom.codegen.metaschema.{PulumiPackage, TypeReference}

import scala.collection.immutable.Seq

object Utils {
  val jvmMaxParamsCount = 253 // https://github.com/scala/bug/issues/7324 // TODO: Find some workaround to enable passing the remaining arguments

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asScalaType(asArgsType: Boolean = false)(implicit typeMapper: TypeMapper, logger: Logger): Type = typeMapper.asScalaType(typeRef, asArgsType)
  }

  implicit class PulumiPackageOps(pulumiPackage: PulumiPackage)(implicit providerConfig: Config.ProviderConfig) {
    private def defaultModuleToPackageParts: String => Seq[String] = pkg => Seq(pkg)
    def moduleToPackageParts: String => Seq[String] = providerConfig.moduleToPackage match {
      case Config.JavaModuleToPackage => pulumiPackage.language.java.packages.view.mapValues { pkg =>
        pkg.split("\\.").filter(_.nonEmpty).toSeq
      }.toMap.withDefault(defaultModuleToPackageParts)
      case Config.NodeJsModuleToPackage => pulumiPackage.language.nodejs.moduleToPackage.view.mapValues { pkg =>
        pkg.split("/").filter(_.nonEmpty).toSeq
      }.toMap.withDefault(defaultModuleToPackageParts)
      case _ => defaultModuleToPackageParts
    }
  }
}
