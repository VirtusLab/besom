package besom.codegen

//noinspection ScalaFileName,TypeAnnotation
class PackageMetadataTest extends munit.FunSuite {

  test("fromJson") {
    val json = """{"name":"aws","version":"v6.7.0"}"""
    assertEquals(PackageMetadata.fromJson(json), PackageMetadata("aws", "6.7.0"))
  }

  test("fromJsonFile") {
    val json = """{"name":"aws","version":"6.7.0"}"""
    val path = os.temp(json)
    assertEquals(PackageMetadata.fromJsonFile(path), PackageMetadata("aws", "6.7.0"))
  }

  test("toJson") {
    val metadata = PackageMetadata("aws", "v6.7.0")
    assertEquals(
      metadata.version,
      "6.7.0"
    )

    assertEquals(
      metadata.toJson,
      """{"name":"aws","version":"6.7.0"}"""
    )

    assertEquals(
      PackageMetadata("aci", "0.0.6", Some("github://api.github.com/netascode/pulumi-aci")).toJson,
      """{"name":"aci","version":"0.0.6","server":"github://api.github.com/netascode/pulumi-aci"}"""
    )
  }

  test("withUrl") {
    val actual1   = PackageMetadata("aws", "6.7.0").withUrl("https://github.com/pulumi/pulumi-aws")
    val expected1 = PackageMetadata("aws", "6.7.0", None)
    assertEquals(actual1, expected1)

    val actual2   = PackageMetadata("aci", "0.0.6").withUrl("https://github.com/netascode/pulumi-aci")
    val expected2 = PackageMetadata("aci", "0.0.6", Some("github://api.github.com/netascode/pulumi-aci"))
    assertEquals(actual2, expected2)
  }
}

class PackageVersionTest extends munit.FunSuite {

  implicit val logger: Logger = new Logger

  test("apply") {
    assertEquals(PackageVersion("v6.7.0"), "6.7.0")
    assertEquals(PackageVersion("v6.7.0"), "6.7.0")
    assertEquals(PackageVersion("0.123.1"), "0.123.1")
    assertEquals(PackageVersion("1.0"), "1.0.0")
    assertEquals(PackageVersion("1"), "1.0.0")
    assertEquals(PackageVersion("v1"), "1.0.0")
    assertEquals(PackageVersion("v1.0.0-alpha.1"), "1.0.0-alpha.1")
    assertEquals(PackageVersion("v1.0.0-0.3.7"), "1.0.0-0.3.7")
    assertEquals(PackageVersion("v1.0.0-alpha+001"), "1.0.0-alpha+001")
    interceptMessage[Exception]("Invalid version format: ''") {
      PackageVersion("")
    }
    interceptMessage[Exception]("Invalid version format: 'v6.7.0.0'") {
      PackageVersion("v6.7.0.0")
    }
  }

  test("parse") {
    assertEquals(PackageVersion.parse("v6.7.0"), Some("6.7.0"))
    assertEquals(PackageVersion.parse("v6.7.0"), Some("6.7.0"))
    assertEquals(PackageVersion.parse("6.7.0"), Some("6.7.0"))
    assertEquals(PackageVersion.parse("1.0"), Some("1.0.0"))
    assertEquals(PackageVersion.parse("1"), Some("1.0.0"))
    assertEquals(PackageVersion.parse(""), None)
  }

  test("isDefault") {
    import PackageVersion._

    assertEquals(PackageVersion.default.isDefault, true)
    assertEquals(PackageVersion("6.7.0").isDefault, false)
    assertEquals(PackageVersion("v6.7.0").isDefault, false)
  }

  test("reconcile") {
    import PackageVersion._

    assertEquals(PackageVersion("v6.7.0").reconcile("6.7.0"), "6.7.0")
    assertEquals(PackageVersion("v6.7.0").reconcile("1.2.0"), "6.7.0")
    assertEquals(PackageVersion("v6.7.0").reconcile(PackageVersion.default), "6.7.0")
    assertEquals(PackageVersion.default.reconcile("v6.7.0"), "6.7.0")
    assertEquals(PackageVersion.default.reconcile(PackageVersion.default), "0.0.0")
  }
}
