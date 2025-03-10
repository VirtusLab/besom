package besom.codegen

import besom.codegen.UpickleApi.*
import besom.codegen.metaschema.PulumiPackage
import besom.model.SemanticVersion

case class FieldRemoval(name: String, fix: Option[String])

case class HotfixDefinition(
  fieldRemovals: List[FieldRemoval]
)

object HotfixDefinition:
  implicit val readerRemoval: Reader[FieldRemoval] = macroR
  implicit val reader: Reader[HotfixDefinition]    = macroR

object Hotfix:
  private val hotfixesDir = "hotfixes"

  def findMatchingHotfix(
    packageName: String,
    version: SemanticVersion,
    resourcePath: String,
    resourceName: String
  )(using config: Config, logger: Logger): Option[HotfixDefinition] =
    val hotfixesPath = config.overlaysDir / hotfixesDir / packageName
    if !os.exists(hotfixesPath) then None
    else
      // List all version range directories and find matching one
      val matchingVersionDir = os.list(hotfixesPath).filter(os.isDir(_)).find { dir =>
        val versionRange = dir.last
        VersionRange.parse(versionRange) match
          case Right(range) => range.matches(version)
          case Left(error) =>
            logger.warn(s"Invalid version range format in directory: $versionRange")
            false
      }

      matchingVersionDir.flatMap { dir =>
        val hotfixPath = dir / os.RelPath(resourcePath) / s"$resourceName.json"
        if !os.exists(hotfixPath) then None
        else
          try Some(read[HotfixDefinition](ujson.read(os.read(hotfixPath))))
          catch
            case e: Exception =>
              logger.warn(s"Failed to parse hotfix file: $hotfixPath: ${e.getMessage}")
              None
      }

  def applyToPackage(
    pulumiPackage: PulumiPackage,
    packageName: String,
    version: SemanticVersion
  )(using config: Config, logger: Logger): PulumiPackage =
    val modifiedResources = pulumiPackage.resources.map { case (tokenStr, resourceDef) =>
      try
        val token = PulumiToken(tokenStr)
        findMatchingHotfix(packageName, version, token.module, token.name) match
          case Some(hotfix) =>
            hotfix.fieldRemovals.foreach { fieldRemoval =>
              val fix = fieldRemoval.fix.getOrElse("no")
              logger.warn(
                s"Removing field ${fieldRemoval.name} from resource ${token.name} ($tokenStr) in package $packageName:$version, fix in progress: $fix"
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

            (
              tokenStr,
              resourceDef.copy(
                properties = withoutRemovedProperties,
                inputProperties = withoutRemovedInputProperties,
                required = withoutRemovedRequired,
                requiredInputs = withoutRemovedRequiredInputs
              )
            )
          case None => (tokenStr, resourceDef)
      catch
        case e: Exception =>
          logger.warn(s"Failed to parse token: $tokenStr: ${e.getMessage}")
          (tokenStr, resourceDef)
    }

    pulumiPackage.copy(resources = modifiedResources)
  end applyToPackage
end Hotfix
