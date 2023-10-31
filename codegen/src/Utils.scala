package besom.codegen

import scala.meta.Type
import besom.codegen.metaschema.{PulumiPackage, TypeReference}

import scala.util.matching.Regex

object Utils {
  // "index" is a placeholder module for classes that should be in
  // the root package (according to pulumi's convention)
  // Needs to be used in Pulumi types, but should NOT be translated to Scala code
  // Placeholder module for classes that should be in the root package (according to pulumi's convention)
  val IndexModuleName = "index"

  val ProviderTypeName = "Provider"

  // TODO: Find some workaround to enable passing the remaining arguments
  val jvmMaxParamsCount = 253 // https://github.com/scala/bug/issues/7324

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asScalaType(asArgsType: Boolean = false)(implicit typeMapper: TypeMapper, logger: Logger): Type =
      typeMapper.asScalaType(typeRef, asArgsType)
  }

  implicit class PulumiPackageOps(pulumiPackage: PulumiPackage) {
    private def slashModuleToPackageParts: String => Seq[String] =
      pkg => pkg.split("/").filter(_.nonEmpty).toSeq

    private def languageModuleToPackageParts: String => Seq[String] = {
      if (pulumiPackage.language.java.packages.view.nonEmpty) {
        pulumiPackage.language.java.packages.view.mapValues { pkg =>
          pkg.split("\\.").filter(_.nonEmpty).toSeq
        }.toMap
      } else {
        // use nodejs mapping as a fallback
        pulumiPackage.language.nodejs.moduleToPackage.view
          .mapValues { pkg =>
            pkg.split("/").filter(_.nonEmpty).toSeq
          }
          .toMap
      }.withDefault(slashModuleToPackageParts) // fallback to slash mapping
    }

    private def packageFormatModuleToPackageParts: String => Seq[String] = { module: String =>
      val moduleFormat: Regex = pulumiPackage.meta.moduleFormat.r
      module match {
        case _ if module == IndexModuleName || module.isEmpty => Seq(module)
        case moduleFormat(name) => languageModuleToPackageParts(name)
        case _ =>
          throw TypeMapperError(
            s"Cannot parse module portion '$module' with " +
              s"moduleFormat: $moduleFormat"
          )
      }
    }

    // to get all of the package parts, first use the regexp provided by the schema
    // then use a language specific mapping, and if everything fails, fallback to slash mapping
    def moduleToPackageParts: String => Seq[String]   = packageFormatModuleToPackageParts
    def providerToPackageParts: String => Seq[String] = module => Seq(module)

    def providerTypeToken: String = s"pulumi:providers:${pulumiPackage.name}"
  }
}
