package besom.codegen

import besom.codegen.Config.ProviderConfig

//noinspection ScalaFileName,TypeAnnotation
class PulumiDefinitionCoordinatesTest extends munit.FunSuite {
  import besom.codegen.Utils.PulumiPackageOps
  import besom.codegen.metaschema.{Java, Language, Meta, PulumiPackage}

  implicit val logger: Logger = new Logger

  val schemaDir = os.pwd / ".out" / "schemas"

  case class Data(
    providerName: SchemaProvider.ProviderName,
    typeToken: String,
    schemaVersion: SchemaProvider.SchemaVersion = "0.0.0",
    meta: Meta = Meta(),
    language: Language = Language(),
    tags: Set[munit.Tag] = Set()
  )(val expected: Expectations*) {
    val pulumiPackage = PulumiPackage(name = providerName, meta = meta, language = language)
  }

  sealed trait Expectations {
    def fullPackageName: String
    def fullyQualifiedTypeRef: String
    def filePath: String
  }

  case class ResourceClassExpectations(
    fullPackageName: String,
    fullyQualifiedTypeRef: String,
    filePath: String,
    asArgsType: Boolean = false
  ) extends Expectations

  case class ObjectClassExpectations(
    fullPackageName: String,
    fullyQualifiedTypeRef: String,
    filePath: String,
    asArgsType: Boolean = false
  ) extends Expectations

  case class EnumClassExpectations(
    fullPackageName: String,
    fullyQualifiedTypeRef: String,
    filePath: String
  ) extends Expectations

  case class FunctionClassExpectations(
    fullPackageName: String,
    fullyQualifiedTypeRef: String,
    filePath: String
  ) extends Expectations

  val tests = List(
    Data(
      providerName = "digitalocean",
      typeToken = "digitalocean:index:Domain",
      meta = Meta(
        moduleFormat = "(.*)(?:/[^/]*)"
      )
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.digitalocean",
        fullyQualifiedTypeRef = "besom.api.digitalocean.Domain",
        filePath = "src/index/Domain.scala"
      )
    ),
    Data(
      providerName = "digitalocean",
      typeToken = "digitalocean:index/getProjectsProject:getProjectsProject",
      meta = Meta(
        moduleFormat = "(.*)(?:/[^/]*)"
      )
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.digitalocean",
        fullyQualifiedTypeRef = "besom.api.digitalocean.GetProjectsProject",
        filePath = "src/index/GetProjectsProject.scala"
      )
    ),
    Data(
      providerName = "foo-bar",
      typeToken = "foo-bar:index:TopLevel"
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.foobar",
        fullyQualifiedTypeRef = "besom.api.foobar.TopLevel",
        filePath = "src/index/TopLevel.scala"
      )
    ),
    Data(
      providerName = "kubernetes",
      typeToken = "kubernetes:meta/v1:APIVersions"
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.kubernetes.meta.v1",
        fullyQualifiedTypeRef = "besom.api.kubernetes.meta.v1.APIVersions",
        filePath = "src/meta/v1/APIVersions.scala"
      )
    ),
    Data(
      providerName = "kubernetes",
      typeToken = "kubernetes:authentication.k8s.io/v1:TokenRequest",
      language = Language(
        java = Java(
          packages = Map(
            "authentication.k8s.io/v1" -> "authentication.v1"
          )
        )
      )
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.kubernetes.authentication.v1",
        fullyQualifiedTypeRef = "besom.api.kubernetes.authentication.v1.TokenRequest",
        filePath = "src/authentication/v1/TokenRequest.scala"
      )
    ),
    Data(
      providerName = "kubernetes",
      typeToken = "kubernetes:rbac.authorization.k8s.io/v1:ClusterRoleBinding",
      language = Language(
        java = Java(
          packages = Map(
            "rbac.authorization.k8s.io/v1" -> "rbac.authorization.v1"
          )
        )
      )
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.kubernetes.rbac.authorization.v1",
        fullyQualifiedTypeRef = "besom.api.kubernetes.rbac.authorization.v1.ClusterRoleBinding",
        filePath = "src/rbac/authorization/v1/ClusterRoleBinding.scala"
      )
    )
  )

  tests.foreach { data =>
    test(s"Type: ${data.typeToken}".withTags(data.tags)) {
      implicit val providerConfig: ProviderConfig = Config.providersConfigs(data.providerName)

      val coords = PulumiDefinitionCoordinates.fromRawToken(
        typeToken = data.typeToken,
        moduleToPackageParts = data.pulumiPackage.moduleToPackageParts,
        providerToPackageParts = data.pulumiPackage.providerToPackageParts
      )

      data.expected.foreach {
        case ResourceClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath, asArgsType) =>
          val rc = coords.asResourceClass(asArgsType = asArgsType)
          assertEquals(rc.fullPackageName, fullPackageName)
          assertEquals(rc.fullyQualifiedTypeRef.toString, fullyQualifiedTypeRef)
          assertEquals(rc.filePath.osSubPath.toString(), filePath)
        case ObjectClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath, asArgsType) =>
          val oc = coords.asObjectClass(asArgsType = asArgsType)
          assertEquals(oc.fullPackageName, fullPackageName)
          assertEquals(oc.fullyQualifiedTypeRef.toString, fullyQualifiedTypeRef)
          assertEquals(oc.filePath.osSubPath.toString(), filePath)
        case EnumClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath) =>
          val ec = coords.asEnumClass
          assertEquals(ec.fullPackageName, fullPackageName)
          assertEquals(ec.fullyQualifiedTypeRef.toString, fullyQualifiedTypeRef)
          assertEquals(ec.filePath.osSubPath.toString(), filePath)
        case FunctionClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath) => ??? // TODO
      }
    }
  }
}

//noinspection ScalaFileName,TypeAnnotation
class TypeTokenTest extends munit.FunSuite {
  test("must provide string representation") {
    val t1 = PulumiToken("provider", "index", "SomeType")
    assertEquals(t1.asString, "provider:index:SomeType")
    val t2 = PulumiToken("provider:index:SomeType")
    assertEquals(t2.asString, "provider:index:SomeType")
  }

  test("must provide the missing module") {
    val t1 = PulumiToken("provider", "", "SomeType")
    assertEquals(t1.asString, "provider:index:SomeType")
    val t2 = PulumiToken("provider::SomeType")
    assertEquals(t2.asString, "provider:index:SomeType")
  }
}
