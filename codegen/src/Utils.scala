package besom.codegen

import scala.meta.Type
import besom.codegen.metaschema.{PulumiPackage, TypeReference}

import scala.util.matching.Regex

object Utils {
  // "index" is a placeholder module for classes that should be in
  // the root package (according to pulumi's convention)
  // Needs to be used in Pulumi types, but should NOT be translated to Scala code
  // Placeholder module for classes that should be in the root package (according to pulumi's convention)
  val indexModuleName = "index"
  val jvmMaxParamsCount =
    253 // https://github.com/scala/bug/issues/7324 // TODO: Find some workaround to enable passing the remaining arguments

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asScalaType(asArgsType: Boolean = false)(implicit typeMapper: TypeMapper, logger: Logger): Type =
      typeMapper.asScalaType(typeRef, asArgsType)
  }

  implicit class PulumiPackageOps(pulumiPackage: PulumiPackage) {
    private def slashModuleToPackageParts: String => Seq[String] =
      pkg => pkg.split("/").toSeq.filter(_.nonEmpty)

    private def languageModuleToPackageParts: String => Seq[String] = {
      pulumiPackage.language.java.packages.view
        .mapValues { pkg =>
          pkg.split("\\.").filter(_.nonEmpty).toSeq
        }
        .toMap
        .withDefault(slashModuleToPackageParts)
    }

    private def packageFormatModuleToPackageParts: String => Seq[String] = { module: String =>
      val moduleFormat: Regex = pulumiPackage.meta.moduleFormat.r
      module match {
        case moduleFormat(name)     => languageModuleToPackageParts(name)
        case moduleFormat(all @ _*) => all
        case _ =>
          throw TypeMapperError(
            s"Cannot parse module portion '$module'" +
              s"moduleFormat: $moduleFormat"
          )
      }
    }

    def moduleToPackageParts: String => Seq[String] = packageFormatModuleToPackageParts
  }
}
