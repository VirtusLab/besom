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

  // A typeRename may target a *type* rather than a resource - azure-native renames an enum this way to
  // keep two definitions whose names differ only by case from colliding on a case-insensitive filesystem.
  test("Hotfix applies a type rename that targets a type, not a resource") {
    val packageName    = "test-provider"
    val version        = "1.0.0"
    val modulePath     = "dbforpostgresql"
    val definitionName = "StorageAutogrow"
    val token          = s"$packageName:$modulePath:$definitionName"

    val hotfixDir = Config.DefaultOverlaysDir / "hotfixes" / packageName / version / "resources" / os.RelPath(modulePath)
    os.makeDir.all(hotfixDir)
    os.write.over(hotfixDir / s"$definitionName.json", """{"typeRename":{"renameScalaTypeTo":"StorageAutoGrowOld"}}""")

    val testPackage = PulumiPackage(
      name = packageName,
      types = Map(
        token -> EnumTypeDefinition(`enum` = List(EnumValueDefinition(value = StringConstValue("Enabled"))), `type` = StringType)
      )
    )

    val modifiedPackage = Hotfix.applyToPackage(testPackage, packageName, SemanticVersion(1, 0, 0))

    assert(modifiedPackage.typeRenames.get(token).contains("StorageAutoGrowOld"))

    // Cleanup
    os.remove.all(Config.DefaultOverlaysDir / "hotfixes" / packageName)
  }

  test("Hotfix drops a type rename that targets neither a resource nor a type") {
    val packageName    = "test-provider"
    val version        = "1.0.0"
    val modulePath     = "chaos/v20240101"
    val definitionName = "Capability"

    val hotfixDir = Config.DefaultOverlaysDir / "hotfixes" / packageName / version / "resources" / os.RelPath(modulePath)
    os.makeDir.all(hotfixDir)
    os.write.over(hotfixDir / s"$definitionName.json", """{"typeRename":{"renameScalaTypeTo":"CapabilityOld"}}""")

    val testPackage = PulumiPackage(name = packageName)

    val modifiedPackage = Hotfix.applyToPackage(testPackage, packageName, SemanticVersion(1, 0, 0))

    // an obsolete rename must not end up in typeRenames, where it would silently rename nothing
    assert(modifiedPackage.typeRenames.isEmpty)

    // Cleanup
    os.remove.all(Config.DefaultOverlaysDir / "hotfixes" / packageName)
  }

  test("Hotfix fails on an obsolete hotfix under strictHotfixes") {
    val packageName    = "test-provider"
    val version        = "1.0.0"
    val modulePath     = "compute"
    val definitionName = "GoneAway"

    val hotfixDir = Config.DefaultOverlaysDir / "hotfixes" / packageName / version / "resources" / os.RelPath(modulePath)
    os.makeDir.all(hotfixDir)
    os.write.over(hotfixDir / s"$definitionName.json", """{"fieldRemovals": [{"name": "urn", "fix": null}]}""")

    val testPackage = PulumiPackage(name = packageName)

    val thrown = intercept[GeneralCodegenException] {
      Hotfix.applyToPackage(testPackage, packageName, SemanticVersion(1, 0, 0))(using Config(strictHotfixes = true), summon[Logger])
    }
    assert(thrown.getMessage.contains(s"$packageName:$modulePath:$definitionName"))
    assert(thrown.getMessage.contains("Delete"))

    // the same hotfix only warns by default
    val tolerated = Hotfix.applyToPackage(testPackage, packageName, SemanticVersion(1, 0, 0))
    assert(tolerated.resources.isEmpty)

    // Cleanup
    os.remove.all(Config.DefaultOverlaysDir / "hotfixes" / packageName)
  }

  // Ranges overlap on purpose: a narrow one records a workaround that a specific set of upstream versions
  // needed, a broad one covers a problem that is still with us. Both must apply, not just whichever the
  // filesystem happened to list first.
  test("Hotfix merges hotfixes from all matching version ranges") {
    val packageName = "test-provider"
    val boundedDir  = Config.DefaultOverlaysDir / "hotfixes" / packageName / "1.0.0:1.0.2" / "resources" / "compute"
    val openDir     = Config.DefaultOverlaysDir / "hotfixes" / packageName / "^1.0.0" / "resources" / "storage"
    os.makeDir.all(boundedDir)
    os.makeDir.all(openDir)
    os.write.over(boundedDir / "Legacy.json", """{"fieldRemovals": [{"name": "urn", "fix": null}]}""")
    os.write.over(openDir / "Bucket.json", """{"typeRename":{"renameScalaTypeTo":"BucketOld"}}""")

    val testPackage = PulumiPackage(
      name = packageName,
      resources = Map(
        s"$packageName:compute:Legacy" -> ResourceDefinition(
          properties = Map("urn" -> PropertyDefinition(typeReference = StringType))
        ),
        s"$packageName:storage:Bucket" -> ResourceDefinition()
      )
    )

    val modifiedPackage = Hotfix.applyToPackage(testPackage, packageName, SemanticVersion(1, 0, 1))

    assert(!modifiedPackage.resources(s"$packageName:compute:Legacy").properties.contains("urn"))
    assert(modifiedPackage.typeRenames.get(s"$packageName:storage:Bucket").contains("BucketOld"))

    // outside the bounded range only the open-ended one applies
    val laterPackage = Hotfix.applyToPackage(testPackage, packageName, SemanticVersion(1, 5, 0))
    assert(laterPackage.resources(s"$packageName:compute:Legacy").properties.contains("urn"))
    assert(laterPackage.typeRenames.get(s"$packageName:storage:Bucket").contains("BucketOld"))

    // Cleanup
    os.remove.all(Config.DefaultOverlaysDir / "hotfixes" / packageName)
  }

  test("Hotfix fails when two matching ranges hotfix the same token") {
    val packageName = "test-provider"
    val boundedDir  = Config.DefaultOverlaysDir / "hotfixes" / packageName / "1.0.0:1.0.2" / "resources" / "compute"
    val openDir     = Config.DefaultOverlaysDir / "hotfixes" / packageName / "^1.0.0" / "resources" / "compute"
    os.makeDir.all(boundedDir)
    os.makeDir.all(openDir)
    os.write.over(boundedDir / "Legacy.json", """{"fieldRemovals": [{"name": "urn", "fix": null}]}""")
    os.write.over(openDir / "Legacy.json", """{"typeRename":{"renameScalaTypeTo":"LegacyOld"}}""")

    val testPackage = PulumiPackage(name = packageName, resources = Map(s"$packageName:compute:Legacy" -> ResourceDefinition()))

    val thrown = intercept[GeneralCodegenException] {
      Hotfix.applyToPackage(testPackage, packageName, SemanticVersion(1, 0, 1))
    }
    assert(thrown.getMessage.contains(s"Conflicting hotfixes for '$packageName:compute:Legacy'"))
    assert(thrown.getMessage.contains("1.0.0:1.0.2"))
    assert(thrown.getMessage.contains("^1.0.0"))

    // Cleanup
    os.remove.all(Config.DefaultOverlaysDir / "hotfixes" / packageName)
  }
end HotfixTests
