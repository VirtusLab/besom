package besom.codegen

import besom.codegen.UpickleApi.*
import besom.codegen.metaschema.PulumiPackage
import besom.model.SemanticVersion

case class FieldRemoval(name: String, fix: Option[String])
case class TypeRename(renameScalaTypeTo: String)
case class FunctionRemoval(name: String)
case class MethodRemoval(name: String)

case class ResourceHotfixDefinition(
  fieldRemovals: List[FieldRemoval] = List.empty,
  typeRename: Option[TypeRename] = None
)

case class ProviderHotfixDefinition(
  fieldRemovals: List[FieldRemoval] = List.empty,
  methodRemovals: List[MethodRemoval] = List.empty
)

case class FunctionHotfixDefinition(
  functionRemovals: List[FunctionRemoval] = List.empty
)

/** A single hotfix file: the Pulumi token it targets, the file it was read from, and what it declares.
  *
  * The source path is carried so that a diagnostic can name the file to delete or re-range instead of leaving the reader to grep for it.
  */
case class ResourceHotfix(
  token: String,
  source: os.Path,
  definition: ResourceHotfixDefinition
)

case class PackageHotfixes(
  ranges: Seq[String], // the version range directories these were loaded from, e.g. Seq("2.89.0:3.8.0", "^2.89.0")
  resourceHotfixes: Seq[ResourceHotfix],
  providerHotfixes: Option[ProviderHotfixDefinition],
  functionsHotfixes: Option[FunctionHotfixDefinition]
)

object Hotfix:
  implicit val readerFieldRemoval: Reader[FieldRemoval]                         = macroR
  implicit val readerTypeRename: Reader[TypeRename]                             = macroR
  implicit val readerResourceHotfixDefinition: Reader[ResourceHotfixDefinition] = macroR
  implicit val readerProviderHotfixDefinition: Reader[ProviderHotfixDefinition] = macroR
  implicit val readerFunctionHotfixDefinition: Reader[FunctionHotfixDefinition] = macroR
  implicit val readerFunctionRemoval: Reader[FunctionRemoval]                   = macroR
  implicit val readerMethodRemoval: Reader[MethodRemoval]                       = macroR

  private val hotfixesDir = "hotfixes"

  /** The fate of one feature (field removals, type rename) of one hotfix file. */
  private enum Outcome:
    case Applied(hotfix: ResourceHotfix, what: String)
    case Obsolete(hotfix: ResourceHotfix, why: String)

  /** Read every hotfix declared under a single version range directory. */
  private def loadRangeDir(packageName: String, dir: os.Path): (Seq[ResourceHotfix], Option[ProviderHotfixDefinition]) =
    val resourcesDirForPackage = dir / "resources"
    val providerDirForPackage  = dir / "provider"

    // Recursively find all JSON files in the resources directory
    val resourceHotfixFiles =
      if os.exists(resourcesDirForPackage) then os.walk(resourcesDirForPackage).filter(_.ext == "json") else IndexedSeq.empty

    // One hotfix per file; the path under 'resources' is the module path, the file name the definition name
    val resourceHotfixes = resourceHotfixFiles.map { file =>
      try
        val relativePath   = file.relativeTo(resourcesDirForPackage)
        val modulePath     = relativePath / os.up
        val definitionName = file.baseName
        val definition     = read[ResourceHotfixDefinition](ujson.read(os.read(file)))

        ResourceHotfix(
          token = s"$packageName:${modulePath.toString}:$definitionName",
          source = file,
          definition = definition
        )
      catch
        case e: Exception =>
          throw GeneralCodegenException(s"Failed to parse hotfix file: $file: ${e.getMessage}")
    }

    val providerHotfixes =
      if os.exists(providerDirForPackage / "provider.json") then
        try Some(read[ProviderHotfixDefinition](ujson.read(os.read(providerDirForPackage / "provider.json"))))
        catch
          case e: Exception =>
            throw GeneralCodegenException(s"Failed to parse hotfix file: ${providerDirForPackage / "provider.json"}: ${e.getMessage}")
      else None

    (resourceHotfixes.toSeq, providerHotfixes)
  end loadRangeDir

  private def loadPackageHotfixes(
    packageName: String,
    version: SemanticVersion,
    hotfixesPath: os.Path
  ): Option[PackageHotfixes] =
    if !os.exists(hotfixesPath) then None
    else
      // Every matching range directory contributes. Ranges deliberately overlap - a narrow range is kept as
      // the record of a workaround that a specific set of upstream versions needed, while a broader one covers
      // a problem that is still with us - so taking only the first match would silently drop one of them.
      val matchingVersionDirs = os
        .list(hotfixesPath)
        .filter(os.isDir(_))
        .filter { dir =>
          val versionRange = dir.last
          VersionRange.parse(versionRange) match
            case Right(range) => range.matches(version)
            case Left(error) =>
              throw GeneralCodegenException(s"Invalid version range format in directory $dir: '$versionRange'", error)
        }
        .sortBy(_.last) // stable order regardless of filesystem listing order

      Option.when(matchingVersionDirs.nonEmpty) {
        val loaded = matchingVersionDirs.map(dir => loadRangeDir(packageName, dir))

        val resourceHotfixes = loaded.flatMap { case (hotfixes, _) => hotfixes }.sortBy(_.token)

        // The same token hotfixed from two matching ranges is ambiguous: one of them would have to win, and
        // silently picking one is exactly the failure mode this merge removes.
        val conflicts = resourceHotfixes.groupBy(_.token).filter(_._2.size > 1).toSeq.sortBy(_._1)
        if conflicts.nonEmpty then
          throw GeneralCodegenException(
            conflicts
              .map { case (token, hotfixes) =>
                s"Conflicting hotfixes for '$token' in $packageName:$version from ${hotfixes.size} matching version ranges: " +
                  hotfixes.map(_.source).mkString(", ")
              }
              .mkString("; ") +
              ". Narrow the ranges so that only one applies to this version."
          )

        // provider hotfixes are removals, so overlapping ranges compose
        val providerHotfixes = loaded.flatMap { case (_, provider) => provider } match
          case Seq()         => None
          case Seq(provider) => Some(provider)
          case many =>
            Some(
              ProviderHotfixDefinition(
                fieldRemovals = many.flatMap(_.fieldRemovals).distinct.toList,
                methodRemovals = many.flatMap(_.methodRemovals).distinct.toList
              )
            )

        PackageHotfixes(matchingVersionDirs.map(_.last).toSeq, resourceHotfixes, providerHotfixes, None)
      }

  def applyToPackage(
    pulumiPackage: PulumiPackage,
    packageName: String,
    version: SemanticVersion
  )(using config: Config, logger: Logger): PulumiPackage =
    val hotfixesPath = config.overlaysDir / hotfixesDir / packageName
    loadPackageHotfixes(packageName, version, hotfixesPath) match
      case None =>
        logger.debug(s"No hotfixes found for $packageName:$version")
        pulumiPackage

      case Some(packageHotfixes) =>
        // A hotfix file declares two independent features with different targets:
        //   - fieldRemovals: only meaningful for resources
        //   - typeRename:    meaningful for resources *and* types, because PulumiPackageInfo applies
        //                    renames while parsing both
        // They are validated separately: a rename that lands on a type is applied, not "skipped",
        // and a rename that lands on nothing is reported instead of silently doing nothing.
        //
        // Tokens are matched exactly, on purpose. This mechanism exists to disambiguate definitions
        // whose names differ only by case (azure-native has both StorageAutoGrow and StorageAutogrow
        // in one module), so a case-insensitive lookup would defeat its purpose.
        def existsAsResource(token: String): Boolean = pulumiPackage.resources.contains(token)
        def existsAsType(token: String): Boolean     = pulumiPackage.types.contains(token)

        val (updatedResources, removalOutcomes) =
          packageHotfixes.resourceHotfixes.foldLeft((pulumiPackage.resources, Vector.empty[Outcome])) {
            case ((resources, outcomes), hotfix) =>
              val removals = hotfix.definition.fieldRemovals
              if removals.isEmpty then (resources, outcomes)
              else
                resources.get(hotfix.token) match
                  case Some(resourceDefinition) =>
                    removals.foreach { removal =>
                      logger.warn(
                        s"Removing field '${removal.name}' from '${hotfix.token}' in $packageName:$version, " +
                          s"fix in progress: ${removal.fix.getOrElse("no")}"
                      )
                    }
                    val removedNames = removals.map(_.name).toSet
                    val patched = resourceDefinition.copy(
                      properties = resourceDefinition.properties.filterNot { case (name, _) => removedNames.contains(name) },
                      inputProperties = resourceDefinition.inputProperties.filterNot { case (name, _) => removedNames.contains(name) },
                      required = resourceDefinition.required.filterNot(removedNames.contains),
                      requiredInputs = resourceDefinition.requiredInputs.filterNot(removedNames.contains)
                    )
                    (
                      resources + (hotfix.token -> patched),
                      outcomes :+ Outcome.Applied(hotfix, s"${removals.size} field removal(s)")
                    )
                  case None =>
                    (
                      resources,
                      outcomes :+ Outcome.Obsolete(
                        hotfix,
                        s"${removals.size} field removal(s) target a resource that does not exist in this schema"
                      )
                    )
          }

        val (typeRenames, renameOutcomes) =
          packageHotfixes.resourceHotfixes.foldLeft((Map.empty[String, String], Vector.empty[Outcome])) {
            case ((renames, outcomes), hotfix) =>
              hotfix.definition.typeRename match
                case None => (renames, outcomes)
                case Some(TypeRename(renameScalaTypeTo)) =>
                  if existsAsResource(hotfix.token) || existsAsType(hotfix.token) then
                    (
                      renames + (hotfix.token -> renameScalaTypeTo),
                      outcomes :+ Outcome.Applied(hotfix, s"rename to '$renameScalaTypeTo'")
                    )
                  else
                    (
                      renames,
                      outcomes :+ Outcome.Obsolete(
                        hotfix,
                        s"rename to '$renameScalaTypeTo' targets a token that is neither a resource nor a type in this schema"
                      )
                    )
          }

        val emptyOutcomes = packageHotfixes.resourceHotfixes.collect {
          case hotfix if hotfix.definition.fieldRemovals.isEmpty && hotfix.definition.typeRename.isEmpty =>
            Outcome.Obsolete(hotfix, "declares no changes")
        }

        val updatedProvider = packageHotfixes.providerHotfixes.foldLeft(pulumiPackage.provider) { (provider, hotfix) =>
          val withoutRemovedMethods = hotfix.methodRemovals.foldLeft(provider) { case (provider, methodRemoval) =>
            logger.warn(s"Removing method ${methodRemoval.name} from provider $packageName:$version, fix in progress: $methodRemoval")
            provider.copy(methods = provider.methods.filterNot { case (name, _) => name == methodRemoval.name })
          }

          val withoutRemovedFields = hotfix.fieldRemovals.foldLeft(withoutRemovedMethods) { case (provider, fieldRemoval) =>
            provider.copy(properties = provider.properties.filterNot { case (name, _) => name == fieldRemoval.name })
          }

          withoutRemovedFields
        }

        report(packageName, version, packageHotfixes.ranges, removalOutcomes ++ renameOutcomes ++ emptyOutcomes)

        pulumiPackage.copy(
          resources = updatedResources,
          typeRenames = typeRenames,
          provider = updatedProvider
        )

    end match
  end applyToPackage

  /** Summarise what the hotfixes did, and name the files that no longer do anything.
    *
    * Obsolete entries are warnings by default so that generation still succeeds, and hard failures under [[Config.strictHotfixes]] so that
    * CI cannot drift into shipping overlays that silently stopped applying.
    */
  private def report(
    packageName: String,
    version: SemanticVersion,
    ranges: Seq[String],
    outcomes: Seq[Outcome]
  )(using config: Config, logger: Logger): Unit =
    val applied  = outcomes.collect { case a: Outcome.Applied => a }
    val obsolete = outcomes.collect { case o: Outcome.Obsolete => o }

    if applied.nonEmpty || obsolete.nonEmpty then
      logger.info(
        s"Hotfixes for $packageName:$version from ${ranges.map(r => s"'$r'").mkString(", ")}: " +
          s"${applied.size} applied, ${obsolete.size} obsolete"
      )

    applied.foreach { a =>
      logger.debug(s"Applied hotfix '${a.hotfix.token}': ${a.what} (${a.hotfix.source})")
    }

    val problems = obsolete.map { o =>
      s"Obsolete hotfix '${o.hotfix.token}' for $packageName:$version: ${o.why}. " +
        s"Delete '${o.hotfix.source}' or narrow its version range."
    }

    if problems.nonEmpty then
      if config.strictHotfixes
      then throw GeneralCodegenException(problems.mkString("\n"))
      else problems.foreach(logger.warn)
  end report
end Hotfix
