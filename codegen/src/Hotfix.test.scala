package besom.codegen

import besom.codegen.metaschema.*
import besom.model.SemanticVersion

class HotfixTests extends munit.FunSuite:
  given Config = Config()
  given Logger = Logger()

  test("Hotfix applies field renames to resources") {
    // Create a test hotfix file
    val packageName  = "test-provider"
    val version      = "1.0.0"
    val resourcePath = "compute/instance"
    val resourceName = "VirtualMachine"

    val hotfixDir = Config.DefaultOverlaysDir / "hotfixes" / packageName / version / "resources" / os.RelPath(resourcePath)
    os.makeDir.all(hotfixDir)

    val hotfixContent =
      """{"fieldRemovals": [{"name": "urn", "fix": null}, {"name": "name", "fix": null}],"typeRename":{"renameScalaTypeTo":"VM"}}"""
    os.write.over(hotfixDir / s"$resourceName.json", hotfixContent)

    // Create a test package
    val testResource = ResourceDefinition(
      properties = Map(
        "urn" -> PropertyDefinition(typeReference = StringType),
        "name" -> PropertyDefinition(typeReference = StringType)
      ),
      inputProperties = Map(
        "urn" -> PropertyDefinition(typeReference = StringType),
        "name" -> PropertyDefinition(typeReference = StringType)
      ),
      required = List("urn"),
      requiredInputs = List("name")
    )

    val testPackage = PulumiPackage(
      name = packageName,
      resources = Map(
        s"$packageName:$resourcePath:$resourceName" -> testResource
      )
    )

    // Apply hotfix
    val modifiedPackage = Hotfix.applyToPackage(
      testPackage,
      packageName,
      SemanticVersion(1, 0, 0)
    )

    // Verify the fields were renamed
    val modifiedResource = modifiedPackage.resources(s"$packageName:$resourcePath:$resourceName")

    // Check properties
    assert(!modifiedResource.properties.contains("urn"))
    assert(!modifiedResource.properties.contains("name"))

    // Check input properties
    assert(!modifiedResource.inputProperties.contains("urn"))
    assert(!modifiedResource.inputProperties.contains("name"))

    // Check required fields
    assert(!modifiedResource.required.contains("urn"))

    // Check required inputs
    assert(!modifiedResource.requiredInputs.contains("name"))

    // Check type renames
    assert(modifiedPackage.typeRenames.contains(s"$packageName:$resourcePath:$resourceName"))
    assert(modifiedPackage.typeRenames(s"$packageName:$resourcePath:$resourceName") == "VM")
    assert(modifiedPackage.typeRenames.size == 1)

    // Cleanup
    os.remove.all(Config.DefaultOverlaysDir / "hotfixes" / packageName)
  }

  test("Hotfix handles wildcard version ranges") {
    val packageName  = "test-provider"
    val version      = "1.x.x"
    val resourcePath = "compute/instance"
    val resourceName = "VirtualMachine"

    val hotfixDir = Config.DefaultOverlaysDir / "hotfixes" / packageName / version / "resources" / os.RelPath(resourcePath)
    os.makeDir.all(hotfixDir)

    val hotfixContent = """{"fieldRemovals": [{"name": "urn", "fix": null}]}"""
    os.write.over(hotfixDir / s"$resourceName.json", hotfixContent)

    val testResource = ResourceDefinition(
      properties = Map(
        "urn" -> PropertyDefinition(typeReference = StringType)
      )
    )

    val testPackage = PulumiPackage(
      name = packageName,
      resources = Map(
        s"$packageName:$resourcePath:$resourceName" -> testResource
      )
    )

    // Test with different versions in 1.x.x range
    val versions = List(
      SemanticVersion(1, 0, 0),
      SemanticVersion(1, 1, 0),
      SemanticVersion(1, 2, 3)
    )

    versions.foreach { version =>
      val modifiedPackage  = Hotfix.applyToPackage(testPackage, packageName, version)
      val modifiedResource = modifiedPackage.resources(s"$packageName:$resourcePath:$resourceName")
      assert(!modifiedResource.properties.contains("urn"))
    }

    // Test version outside range
    val outsideVersion     = SemanticVersion(2, 0, 0)
    val unmodifiedPackage  = Hotfix.applyToPackage(testPackage, packageName, outsideVersion)
    val unmodifiedResource = unmodifiedPackage.resources(s"$packageName:$resourcePath:$resourceName")
    assert(unmodifiedResource.properties.contains("urn"))

    // Cleanup
    os.remove.all(Config.DefaultOverlaysDir / "hotfixes" / packageName)
  }

  test("Hotfix handles version range with colon") {
    val packageName  = "test-provider"
    val version      = "1.0.0:1.0.2"
    val resourcePath = "compute/instance"
    val resourceName = "VirtualMachine"

    val hotfixDir = Config.DefaultOverlaysDir / "hotfixes" / packageName / version / "resources" / os.RelPath(resourcePath)
    os.makeDir.all(hotfixDir)

    val hotfixContent = """{"fieldRemovals": [{"name": "urn", "fix": "just to test logging"}]}"""
    os.write.over(hotfixDir / s"$resourceName.json", hotfixContent)

    val testResource = ResourceDefinition(
      properties = Map(
        "urn" -> PropertyDefinition(typeReference = StringType)
      )
    )

    val testPackage = PulumiPackage(
      name = packageName,
      resources = Map(
        s"$packageName:$resourcePath:$resourceName" -> testResource
      )
    )

    // Test versions within range
    val versions = List(
      SemanticVersion(1, 0, 0),
      SemanticVersion(1, 0, 1),
      SemanticVersion(1, 0, 2)
    )

    versions.foreach { version =>
      val modifiedPackage  = Hotfix.applyToPackage(testPackage, packageName, version)
      val modifiedResource = modifiedPackage.resources(s"$packageName:$resourcePath:$resourceName")
      assert(!modifiedResource.properties.contains("urn"))
    }

    // Test versions outside range
    val outsideVersions = List(
      SemanticVersion(1, 0, 3),
      SemanticVersion(1, 1, 0),
      SemanticVersion(2, 0, 0)
    )

    outsideVersions.foreach { version =>
      val unmodifiedPackage  = Hotfix.applyToPackage(testPackage, packageName, version)
      val unmodifiedResource = unmodifiedPackage.resources(s"$packageName:$resourcePath:$resourceName")
      assert(unmodifiedResource.properties.contains("urn"))
    }

    // Cleanup
    os.remove.all(Config.DefaultOverlaysDir / "hotfixes" / packageName)
  }

  test("Hotfix applies provider method removal hotfixes") {
    val packageName = "test-provider"
    val version     = "1.0.0"

    val hotfixDir = Config.DefaultOverlaysDir / "hotfixes" / packageName / version / "provider"
    os.makeDir.all(hotfixDir)

    val hotfixContent = """{"methodRemovals": [{"name": "create"}]}"""
    os.write.over(hotfixDir / "provider.json", hotfixContent)

    val testProvider = ResourceDefinition(
      methods = Map("create" -> "create", "delete" -> "delete")
    )

    val testPackage = PulumiPackage(
      name = packageName,
      provider = testProvider
    )

    val modifiedPackage = Hotfix.applyToPackage(testPackage, packageName, SemanticVersion(1, 0, 0))

    assert(!modifiedPackage.provider.methods.contains("create"))
    assert(modifiedPackage.provider.methods.contains("delete"))

    // Cleanup
    os.remove.all(Config.DefaultOverlaysDir / "hotfixes" / packageName)
  }
end HotfixTests
