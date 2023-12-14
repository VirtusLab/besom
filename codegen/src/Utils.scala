package besom.codegen

import besom.codegen.metaschema.*

import scala.meta.{Lit, Type}
import scala.util.matching.Regex

object Utils {
  // "index" is a placeholder module for classes that should be in
  // the root package (according to pulumi's convention)
  // Needs to be used in Pulumi types, but should NOT be translated to Scala code
  // Placeholder module for classes that should be in the root package (according to pulumi's convention)
  val indexModuleName  = "index"
  val configModuleName = "config"
  val providerTypeName = "Provider"
  val configTypeName   = "Config"

  // Name of the self parameter of resource methods
  val selfParameterName = "__self__"

  // TODO: Find some workaround to enable passing the remaining arguments
  val jvmMaxParamsCount = 253 // https://github.com/scala/bug/issues/7324

  type FunctionName = String

  implicit class ConstValueOps(constValue: ConstValue) {
    def asScala: Lit = constValue match {
      case StringConstValue(value)  => Lit.String(value)
      case BooleanConstValue(value) => Lit.Boolean(value)
      case IntConstValue(value)     => Lit.Int(value)
      case DoubleConstValue(value)  => Lit.Double(value)
    }
  }

  implicit class TypeReferenceOps(typeRef: TypeReference) {
    def asTokenAndDependency(implicit typeMapper: TypeMapper): Vector[(Option[PulumiToken], Option[PackageMetadata])] =
      typeMapper.findTokenAndDependencies(typeRef)

    def asScalaType(asArgsType: Boolean = false)(implicit typeMapper: TypeMapper): Type =
      try {
        typeMapper.asScalaType(typeRef, asArgsType)
      } catch {
        case t: Throwable =>
          throw TypeMapperError(s"Failed to map type: '${typeRef}', asArgsType: $asArgsType", t)
      }
  }

  implicit class PulumiPackageOps(pulumiPackage: PulumiPackage) {
    private def slashModuleToPackageParts: String => Seq[String] =
      pkg => pkg.split("/").filter(_.nonEmpty).toSeq

    private def languageModuleToPackageParts: String => Seq[String] = {
      if (pulumiPackage.language.java.packages.view.nonEmpty) {
        pulumiPackage.language.java.packages.view
          .mapValues { pkg =>
            pkg.split("\\.").filter(_.nonEmpty).toSeq
          }
          .toMap
          .withDefault(slashModuleToPackageParts) // fallback to slash mapping
      } else {
        // use nodejs mapping as a fallback
        pulumiPackage.language.nodejs.moduleToPackage.view
          .mapValues { pkg =>
            pkg.split("/").filter(_.nonEmpty).toSeq
          }
          .toMap
          .withDefault(slashModuleToPackageParts) // fallback to slash mapping
      }
    }

    private def packageFormatModuleToPackageParts: String => Seq[String] = { (module: String) =>
      val moduleFormat: Regex = pulumiPackage.meta.moduleFormat.r
      module match {
        case _ if module.isEmpty =>
          throw TypeMapperError("Module cannot be empty")
        case _ if module == indexModuleName  => Seq(indexModuleName)
        case _ if module == configModuleName => Seq(configModuleName)
        case moduleFormat(name)              => languageModuleToPackageParts(name)
        case _ =>
          throw TypeMapperError(
            s"Cannot parse module portion '$module' with moduleFormat: $moduleFormat"
          )
      }
    }

    // to get all of the package parts, first use the regexp provided by the schema
    // then use a language specific mapping, and if everything fails, fallback to slash mapping
    def moduleToPackageParts: String => Seq[String]   = packageFormatModuleToPackageParts
    def providerToPackageParts: String => Seq[String] = module => Seq(module)

    def providerTypeToken: String = s"pulumi:providers:${pulumiPackage.name}"

    def toPackageMetadata(overrideMetadata: PackageMetadata): PackageMetadata =
      toPackageMetadata(Some(overrideMetadata))
    def toPackageMetadata(overrideMetadata: Option[PackageMetadata] = None): PackageMetadata = {
      import PackageVersion.*
      overrideMetadata match {
        case Some(d) => PackageMetadata(d.name, PackageVersion(pulumiPackage.version).reconcile(d.version))
        case None    => PackageMetadata(pulumiPackage.name, PackageVersion(pulumiPackage.version))
      }
    }

    type FunctionToken = String

    private[Utils] def nonOverlayFunctions(implicit logger: Logger): Map[FunctionToken, FunctionDefinition] = {
      val (overlays, functions) = pulumiPackage.functions.partition { case (_, d) => d.isOverlay }
      overlays.foreach { case (token, _) =>
        logger.info(s"Function '${token}' was not generated because it was marked as overlay")
      }
      functions
    }

    def parsedFunctions(implicit logger: Logger): Map[PulumiDefinitionCoordinates, FunctionDefinition] = {
      nonOverlayFunctions.map { case (token, function) =>
        val coordinates = PulumiDefinitionCoordinates.fromRawToken(
          typeToken = token,
          moduleToPackageParts = moduleToPackageParts,
          providerToPackageParts = providerToPackageParts
        )
        (coordinates, function)
      }
    }

    def parsedTypes(implicit logger: Logger): Map[PulumiDefinitionCoordinates, TypeDefinition] = {
      val (overlays, types) = pulumiPackage.types.partition { case (_, d) => d.isOverlay }
      overlays.foreach { case (token, _) =>
        logger.info(s"Type '${token}' was not generated because it was marked as overlay")
      }
      types.collect { case (token, typeRef) =>
        val coordinates = PulumiDefinitionCoordinates.fromRawToken(
          typeToken = token,
          moduleToPackageParts = moduleToPackageParts,
          providerToPackageParts = providerToPackageParts
        )
        (coordinates, typeRef)
      }
    }

    def parsedResources(implicit logger: Logger): Map[PulumiDefinitionCoordinates, ResourceDefinition] = {
      val (overlays, resources) = pulumiPackage.resources.partition { case (_, d) => d.isOverlay }
      overlays.foreach { case (token, _) =>
        logger.info(s"Resource '${token}' was not generated because it was marked as overlay")
      }
      resources.collect { case (token, resource) =>
        val coordinates = PulumiDefinitionCoordinates.fromRawToken(
          typeToken = token,
          moduleToPackageParts = moduleToPackageParts,
          providerToPackageParts = providerToPackageParts
        )
        (coordinates, resource)
      }
    }

    def parsedMethods(
      resourceDefinition: ResourceDefinition
    )(implicit logger: Logger): Map[FunctionName, (PulumiDefinitionCoordinates, FunctionDefinition)] = {
      val (methods, notMethods) = resourceDefinition.methods.toSeq
        .sortBy { case (name, _) => name }
        .map { case (name, token) =>
          (
            name,
            (
              PulumiDefinitionCoordinates.fromRawToken(
                typeToken = token,
                moduleToPackageParts = pulumiPackage.moduleToPackageParts,
                providerToPackageParts = pulumiPackage.providerToPackageParts
              ),
              pulumiPackage.nonOverlayFunctions.getOrElse(
                token,
                throw TypeMapperError(s"Function '${token}' not found in package '${pulumiPackage.name}'")
              )
            )
          )
        }
        .partition { case (_, (_, d)) => isMethod(d) }

      notMethods.foreach { case (token, _) =>
        logger.info(s"Method '${token}' was not generated because it was not marked as method")
      }
      methods.toMap
    }
  }

  def isMethod(functionDefinition: FunctionDefinition): Boolean =
    functionDefinition.inputs.properties.isDefinedAt(Utils.selfParameterName)
}
