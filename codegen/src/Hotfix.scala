package besom.codegen

import besom.codegen.UpickleApi.*
import besom.codegen.metaschema.PulumiPackage
import besom.model.SemanticVersion

case class FieldRemoval(name: String, fix: Option[String])
case class TypeRename(renameScalaTypeTo: String)

case class HotfixDefinition(
  fieldRemovals: List[FieldRemoval] = List.empty,
  typeRename: Option[TypeRename] = None
)

// Map of module path -> resource name -> hotfix definition
case class PackageHotfixes(hotfixes: Map[String, Map[String, HotfixDefinition]])

object HotfixDefinition:
  implicit val readerFieldRemoval: Reader[FieldRemoval] = macroR
  implicit val readerTypeRename: Reader[TypeRename]     = macroR
  implicit val reader: Reader[HotfixDefinition]         = macroR

object Hotfix:
  private val hotfixesDir = "hotfixes"

  private def loadPackageHotfixes(
    version: SemanticVersion,
    hotfixesPath: os.Path
  ): Option[PackageHotfixes] =
    if !os.exists(hotfixesPath) then None
    else
      // List all version range directories and find matching one
      val matchingVersionDir = os.list(hotfixesPath).filter(os.isDir(_)).find { dir =>
        val versionRange = dir.last
        VersionRange.parse(versionRange) match
          case Right(range) => range.matches(version)
          case Left(error) =>
            throw GeneralCodegenException(s"Invalid version range format in directory $dir: '$versionRange'", error)
      }

      matchingVersionDir.map { dir =>
        // Recursively find all JSON files in the directory
        val hotfixFiles = os.walk(dir).filter(_.ext == "json")

        // Group hotfixes by module path and resource name
        val hotfixMap = hotfixFiles.foldLeft(Map.empty[String, Map[String, HotfixDefinition]]) { (acc, file) =>
          try
            val relativePath = file.relativeTo(dir)
            val modulePath   = relativePath / os.up
            val resourceName = file.baseName
            val hotfix       = read[HotfixDefinition](ujson.read(os.read(file)))

            val moduleMap = acc.getOrElse(modulePath.toString, Map.empty)
            acc + (modulePath.toString -> (moduleMap + (resourceName -> hotfix)))
          catch
            case e: Exception =>
              throw GeneralCodegenException(s"Failed to parse hotfix file: $file: ${e.getMessage}")
        }

        PackageHotfixes(hotfixMap)
      }

  def applyToPackage(
    pulumiPackage: PulumiPackage,
    packageName: String,
    version: SemanticVersion
  )(using config: Config, logger: Logger): PulumiPackage =
    val hotfixesPath = config.overlaysDir / hotfixesDir / packageName
    loadPackageHotfixes(version, hotfixesPath) match
      case Some(packageHotfixes) =>
        // Apply hotfixes only to resources that have them
        val updatedResources = packageHotfixes.hotfixes.foldLeft(pulumiPackage.resources) { (resources, moduleEntry) =>
          val (modulePath, moduleHotfixes) = moduleEntry
          moduleHotfixes.foldLeft(resources) { (resources, resourceEntry) =>
            val (resourceName, hotfix) = resourceEntry
            val tokenStr               = s"$packageName:$modulePath:$resourceName"

            resources.get(tokenStr) match
              case Some(resourceDef) =>
                hotfix.fieldRemovals.foreach { fieldRemoval =>
                  val fix = fieldRemoval.fix.getOrElse("no")
                  logger.warn(
                    s"Removing field ${fieldRemoval.name} from resource ${resourceName} ($tokenStr) in package $packageName:$version, fix in progress: $fix"
                  )
                }

                val withoutRemovedProperties = resourceDef.properties.filterNot { case (name, _) =>
                  hotfix.fieldRemovals.exists(_.name == name)
                }
                val withoutRemovedInputProperties = resourceDef.inputProperties.filterNot { case (name, _) =>
                  hotfix.fieldRemovals.exists(_.name == name)
                }
                val withoutRemovedRequired       = resourceDef.required.filterNot(name => hotfix.fieldRemovals.exists(_.name == name))
                val withoutRemovedRequiredInputs = resourceDef.requiredInputs.filterNot(name => hotfix.fieldRemovals.exists(_.name == name))

                resources + (tokenStr -> resourceDef.copy(
                  properties = withoutRemovedProperties,
                  inputProperties = withoutRemovedInputProperties,
                  required = withoutRemovedRequired,
                  requiredInputs = withoutRemovedRequiredInputs
                ))

              case None =>
                logger.warn(s"Resource $tokenStr not found in package, skipping hotfix")
                resources
          }
        }

        val typeRenames = packageHotfixes.hotfixes.flatMap { case (modulePath, moduleHotfixes) =>
          moduleHotfixes.flatMap { case (resourceName, hotfix) =>
            hotfix.typeRename.map { case TypeRename(renameScalaTypeTo) =>
              (s"$packageName:$modulePath:$resourceName", renameScalaTypeTo)
            }
          }
        }

        pulumiPackage.copy(resources = updatedResources, typeRenames = typeRenames)

      case None =>
        logger.debug(s"No hotfixes found for $packageName:$version")
        pulumiPackage

    end match
  end applyToPackage
end Hotfix
