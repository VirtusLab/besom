package besom.codegen

//noinspection ScalaFileName,TypeAnnotation
class PackageMetadataTest extends munit.FunSuite {
  import besom.codegen.PackageVersion._

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
      metadata.version.orDefault,
      "6.7.0"
    )

    assertEquals(
      metadata.toJson,
      """{"name":"aws","version":"6.7.0"}"""
    )

    assertEquals(
      PackageMetadata("aci", "0.0.6", "github://api.github.com/netascode/pulumi-aci").toJson,
      """{"name":"aci","version":"0.0.6","server":"github://api.github.com/netascode/pulumi-aci"}"""
    )
  }

  test("withUrl") {
    val actual1   = PackageMetadata("aws", "6.7.0").withUrl("https://github.com/pulumi/pulumi-aws")
    val expected1 = PackageMetadata("aws", "6.7.0")
    assertEquals(actual1, expected1)

    val actual2   = PackageMetadata("aci", "0.0.6").withUrl("https://github.com/netascode/pulumi-aci")
    val expected2 = PackageMetadata("aci", "0.0.6", "github://api.github.com/netascode/pulumi-aci")
    assertEquals(actual2, expected2)
  }
}

class PackageVersionTest extends munit.FunSuite {

  implicit val logger: Logger = new Logger

  test("parse") {
    assertEquals(PackageVersion.parse("v6.7.0"), Some("6.7.0"))
    assertEquals(PackageVersion.parse("v6.7.0"), Some("6.7.0"))
    assertEquals(PackageVersion.parse("0.123.1"), Some("0.123.1"))
    assertEquals(PackageVersion.parse("1.0"), Some("1.0.0"))
    assertEquals(PackageVersion.parse("1"), Some("1.0.0"))
    assertEquals(PackageVersion.parse("v1"), Some("1.0.0"))
    assertEquals(PackageVersion.parse("v1.0.0-alpha.1"), Some("1.0.0-alpha.1"))
    assertEquals(PackageVersion.parse("v1.0.0-0.3.7"), Some("1.0.0-0.3.7"))
    assertEquals(PackageVersion.parse("v1.0.0-alpha+001"), Some("1.0.0-alpha+001"))
    assertEquals(PackageVersion.parse(""), None)
    assertEquals(PackageVersion.parse("v6.7.0.0"), None)
  }

  test("parse") {
    assertEquals(PackageVersion.parse("v6.7.0"), Some("6.7.0"))
    assertEquals(PackageVersion.parse("v6.7.0"), Some("6.7.0"))
    assertEquals(PackageVersion.parse("6.7.0"), Some("6.7.0"))
    assertEquals(PackageVersion.parse("1.0"), Some("1.0.0"))
    assertEquals(PackageVersion.parse("1"), Some("1.0.0"))
    assertEquals(PackageVersion.parse(""), None)
  }

  test("reconcile") {
    import PackageVersion._

    assertEquals(PackageVersion.parse("v6.7.0").reconcile(Some("6.7.0")), Some("6.7.0"))
    assertEquals(PackageVersion.parse("v6.7.0").reconcile(Some("1.2.0")), Some("6.7.0"))
    assertEquals(PackageVersion.parse("v6.7.0").reconcile(Some(PackageVersion.default)), Some("6.7.0"))
    assertEquals(Some(PackageVersion.default).reconcile(Some("v6.7.0")), Some("6.7.0"))
    assertEquals(Some(PackageVersion.default).reconcile(Some(PackageVersion.default)), None)
  }
}
