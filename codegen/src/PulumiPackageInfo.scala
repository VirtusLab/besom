package besom.codegen

import besom.codegen.Utils.{FunctionName, PulumiPackageOps, isMethod}
import besom.codegen.metaschema.{FunctionDefinition, *}
import besom.codegen.{PackageVersion, SchemaName}

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

type EnumInstanceName = String

case class PulumiPackageInfo(
  name: SchemaName,
  version: PackageVersion,
  providerTypeToken: String,
  moduleToPackageParts: String => Seq[String],
  providerToPackageParts: String => Seq[String],
  enumTypeTokens: Set[String],
  objectTypeTokens: Set[String],
  resourceTypeTokens: Set[String],
  enumValueToInstances: Map[PulumiToken, Map[ConstValue, EnumInstanceName]],
  parsedTypes: Map[PulumiDefinitionCoordinates, (TypeDefinition, Boolean)],
  parsedResources: Map[PulumiDefinitionCoordinates, (ResourceDefinition, Boolean)],
  parsedFunctions: Map[PulumiDefinitionCoordinates, (FunctionDefinition, Boolean)]
)(
  private[codegen] val pulumiPackage: PulumiPackage,
  private[codegen] val providerConfig: Config.Provider
) {
  import PulumiPackageInfo.*

  def asPackageMetadata: PackageMetadata = PackageMetadata(name, Some(version))

  def parseMethods(
    resourceDefinition: ResourceDefinition
  )(using logger: Logger): Map[FunctionName, (PulumiDefinitionCoordinates, (FunctionDefinition, Boolean))] = {
    val (methods, notMethods) = resourceDefinition.methods.toSeq
      .sortBy { case (name, _) => name }
      .map { case (name, token) =>
        (
          name,
          PulumiDefinitionCoordinates.fromRawToken(
            typeToken = token,
            moduleToPackageParts = moduleToPackageParts,
            providerToPackageParts = providerToPackageParts
          )
        )
      }
      .map { case (name, definition) =>
        (
          name,
          (
            definition,
            parsedFunctions.getOrElse(
              definition,
              throw TypeMapperError(
                s"Function '${definition.token.asString}' defined as a resource method not found in package '${this.name}'"
              )
            )
          )
        )
      }
      .partition { case (_, (_, (d, _))) => isMethod(d) }

    notMethods.foreach { case (token, _) =>
      logger.info(s"Method '${token}' was not generated because it was not marked as method")
    }
    methods.toMap
  }
}

object PulumiPackageInfo {
  def from(
    pulumiPackage: PulumiPackage,
    packageMetadata: PackageMetadata
  )(using Logger, Config): PulumiPackageInfo = PreProcessed.from(pulumiPackage, packageMetadata).process

  private[PulumiPackageInfo] case class PreProcessed(
    name: SchemaName,
    version: PackageVersion,
    moduleToPackageParts: String => Seq[String],
    providerToPackageParts: String => Seq[String]
  )(
    private[codegen] val pulumiPackage: PulumiPackage,
    private[codegen] val providerConfig: Config.Provider
  ):
    def parseResources: Map[PulumiDefinitionCoordinates, (ResourceDefinition, Boolean)] =
      pulumiPackage.resources.map { case (token, resource) =>
        val coordinates = PulumiDefinitionCoordinates.fromRawToken(
          typeToken = token,
          moduleToPackageParts = moduleToPackageParts,
          providerToPackageParts = providerToPackageParts
        )
        (coordinates, (resource, resource.isOverlay))
      }

    def parseTypes(using Logger): Map[PulumiDefinitionCoordinates, (TypeDefinition, Boolean)] =
      pulumiPackage.types.map { case (token, typeRef) =>
        val coordinates = PulumiDefinitionCoordinates.fromRawToken(
          typeToken = token,
          moduleToPackageParts = moduleToPackageParts,
          providerToPackageParts = providerToPackageParts
        )
        (coordinates, (typeRef, typeRef.isOverlay))
      }

    def parseFunctions(using Logger): Map[PulumiDefinitionCoordinates, (FunctionDefinition, Boolean)] =
      pulumiPackage.functions.map { case (token, function) =>
        val coordinates = PulumiDefinitionCoordinates.fromRawToken(
          typeToken = token,
          moduleToPackageParts = moduleToPackageParts,
          providerToPackageParts = providerToPackageParts
        )
        (coordinates, (function, function.isOverlay))
      }

    def process(using logger: Logger): PulumiPackageInfo =
      // pre-process the package to gather information about types, that are used later during various parts of codegen
      // most notable place is TypeMapper.scalaTypeFromTypeUri
      val enumTypeTokensBuffer     = ListBuffer.empty[String]
      val objectTypeTokensBuffer   = ListBuffer.empty[String]
      val resourceTypeTokensBuffer = ListBuffer.empty[String]

      val enumValueToInstancesBuffer = ListBuffer.empty[(PulumiToken, Map[ConstValue, EnumInstanceName])]

      def valueToInstances(enumDefinition: EnumTypeDefinition): Map[ConstValue, EnumInstanceName] =
        enumDefinition.`enum`.map { (valueDefinition: EnumValueDefinition) =>
          val caseRawName: EnumInstanceName = valueDefinition.name.getOrElse {
            valueDefinition.value match {
              case StringConstValue(value) => value
              case const                   => throw GeneralCodegenException(s"The name of enum cannot be derived from value ${const}")
            }
          }
          valueDefinition.value -> caseRawName
        }.toMap

      // Post-process the tokens to unify them to lower case to circumvent inconsistencies in low quality schemas (e.g. aws)
      // This allows us to use case-insensitive matching when looking up tokens
      val parsedTypes = parseTypes
      parsedTypes.foreach {
        case (coordinates, (definition: EnumTypeDefinition, _)) =>
          enumValueToInstancesBuffer += coordinates.token -> valueToInstances(definition)

          if (enumTypeTokensBuffer.contains(coordinates.token.asLookupKey))
            logger.warn(s"Duplicate enum type token '${coordinates.token.asLookupKey}' in package '${name}'")
          enumTypeTokensBuffer += coordinates.token.asLookupKey
        case (coordinates, (_: ObjectTypeDefinition, _)) =>
          if (objectTypeTokensBuffer.contains(coordinates.token.asLookupKey))
            logger.warn(s"Duplicate object type token '${coordinates.token.asLookupKey}' in package '${name}'")
          objectTypeTokensBuffer += coordinates.token.asLookupKey
      }

      val parsedResources = parseResources
      parsedResources.foreach { case (coordinates, (_: ResourceDefinition, _)) =>
        if (resourceTypeTokensBuffer.contains(coordinates.token.asLookupKey))
          logger.warn(s"Duplicate resource type token '${coordinates.token.asLookupKey}' in package '${name}'")
        resourceTypeTokensBuffer += coordinates.token.asLookupKey
      }

      PulumiPackageInfo(
        name = name,
        version = version,
        providerTypeToken = s"pulumi:providers:${name}",
        moduleToPackageParts = moduleToPackageParts,
        providerToPackageParts = providerToPackageParts,
        enumTypeTokens = enumTypeTokensBuffer.toSet,
        objectTypeTokens = objectTypeTokensBuffer.toSet,
        resourceTypeTokens = resourceTypeTokensBuffer.toSet,
        enumValueToInstances = enumValueToInstancesBuffer.toMap,
        parsedTypes = parsedTypes,
        parsedResources = parsedResources,
        parsedFunctions = parseFunctions
      )(pulumiPackage, providerConfig)
    end process
  end PreProcessed
  private[PulumiPackageInfo] object PreProcessed:
    private[PulumiPackageInfo] def from(
      pulumiPackage: PulumiPackage,
      packageMetadata: PackageMetadata
    )(using logger: Logger, config: Config): PreProcessed = {
      val originalName = pulumiPackage.name
      val overrideName = packageMetadata.name
      if (originalName != overrideName) {
        logger.warn(
          s"Package name mismatch for '$overrideName' != '$originalName', " +
            s"will be reconciled - this is fine in tests"
        )
      }
      val originalVersion = PackageVersion(pulumiPackage.version).orDefault
      val overrideVersion = packageMetadata.version.orDefault
      if (originalVersion != overrideVersion) {
        logger.warn(
          s"Package version mismatch for '$overrideName:$overrideVersion' != '${originalVersion}', " +
            s"will be reconciled - this is fine in tests"
        )
      }

      val reconciledMetadata = pulumiPackage.toPackageMetadata(packageMetadata)
      val providerConfig     = config.providers(reconciledMetadata.name)
      val overrideModuleToPackages = providerConfig.moduleToPackages.view.mapValues { pkg =>
        pkg.split("\\.").filter(_.nonEmpty).toSeq
      }.toMap

      // needed for overlays
      val additionalKubernetesModule =
        Option.when(pulumiPackage.name == "kubernetes")("apiextensions.k8s.io", Seq("apiextensions"))

      PreProcessed(
        name = reconciledMetadata.name,
        version = reconciledMetadata.version.orDefault,
        moduleToPackageParts = moduleToPackageParts(pulumiPackage, overrideModuleToPackages ++ additionalKubernetesModule),
        providerToPackageParts = providerToPackageParts
      )(pulumiPackage, providerConfig)
    }

    // to get all of the package parts, first use the regexp provided by the schema
    // then use a language specific mapping, and if everything fails, fallback to slash mapping
    private def moduleToPackageParts(
      pulumiPackage: PulumiPackage,
      overrideModuleToPackages: Map[String, Seq[String]]
    ): String => Seq[String] = { (module: String) =>
      val moduleFormat: Regex = pulumiPackage.meta.moduleFormat.r
      module match {
        case _ if module.isEmpty =>
          throw TypeMapperError("Module cannot be empty")
        case _ if module == Utils.indexModuleName  => Seq(Utils.indexModuleName)
        case _ if module == Utils.configModuleName => Seq(Utils.configModuleName)
        case moduleFormat(name) => overrideModuleToPackages.withDefault(languageModuleToPackageParts(pulumiPackage))(name)
        case _ =>
          throw TypeMapperError(
            s"Cannot parse module portion '$module' with moduleFormat: $moduleFormat"
          )
      }
    }

    def providerToPackageParts: String => Seq[String] = module => Seq(module)

    private def languageModuleToPackageParts(pulumiPackage: PulumiPackage): String => Seq[String] = {
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

    private def slashModuleToPackageParts: String => Seq[String] =
      pkg => pkg.split("/").filter(_.nonEmpty).toSeq
  end PreProcessed
}
