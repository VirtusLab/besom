package besom.codegen

//noinspection ScalaFileName,TypeAnnotation
class TypeMapperTest extends munit.FunSuite {
  import besom.codegen.Utils.PulumiPackageOps
  import besom.codegen.metaschema.{Java, Language, Meta, PulumiPackage}

  implicit val logger: Logger = new Logger

  val schemaDir = os.pwd / ".out" / "schemas"

  case class Data(
    schemaName: SchemaProvider.SchemaName,
    typeToken: String,
    schemaVersion: SchemaProvider.SchemaVersion = "0.0.0",
    meta: Meta = Meta(),
    language: Language = Language(),
    tags: Set[munit.Tag] = Set()
  )(val expected: Expectations*) {
    val pulumiPackage = PulumiPackage(name = schemaName, meta = meta, language = language)
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
      schemaName = "example",
      typeToken = "example::SomeType"
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.example",
        fullyQualifiedTypeRef = "besom.api.example.SomeType",
        filePath = "src/index/SomeType.scala"
      )
    ),
    Data(
      schemaName = "digitalocean",
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
      schemaName = "digitalocean",
      typeToken = "digitalocean::Domain",
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
      schemaName = "foo-bar",
      typeToken = "foo-bar:index:TopLevel"
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.foobar",
        fullyQualifiedTypeRef = "besom.api.foobar.TopLevel",
        filePath = "src/index/TopLevel.scala"
      )
    ),
    Data(
      schemaName = "kubernetes",
      typeToken = "kubernetes:meta/v1:APIVersions"
    )(
      ResourceClassExpectations(
        fullPackageName = "besom.api.kubernetes.meta.v1",
        fullyQualifiedTypeRef = "besom.api.kubernetes.meta.v1.APIVersions",
        filePath = "src/meta/v1/APIVersions.scala"
      )
    ),
    Data(
      schemaName = "kubernetes",
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
      schemaName = "kubernetes",
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
      implicit val providerConfig: Config.ProviderConfig = Config.providersConfigs(data.schemaName)

      val schemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = schemaDir)
      val packageInfo    = schemaProvider.packageInfo(data.pulumiPackage)
      val tm: TypeMapper = new TypeMapper(packageInfo, schemaProvider)

      val ptc = tm.parseTypeToken(
        typeToken = data.typeToken,
        moduleToPackageParts = data.pulumiPackage.moduleToPackageParts,
        providerToPackageParts = data.pulumiPackage.providerToPackageParts
      )

      data.expected.foreach {
        case ResourceClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath, asArgsType) =>
          val rc = ptc.asResourceClass(asArgsType = asArgsType)
          assertEquals(rc.fullPackageName, fullPackageName)
          assertEquals(rc.fullyQualifiedTypeRef.toString, fullyQualifiedTypeRef)
          assertEquals(rc.filePath.osSubPath.toString(), filePath)
        case ObjectClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath, asArgsType) =>
          val oc = ptc.asObjectClass(asArgsType = asArgsType)
          assertEquals(oc.fullPackageName, fullPackageName)
          assertEquals(oc.fullyQualifiedTypeRef.toString, fullyQualifiedTypeRef)
          assertEquals(oc.filePath.osSubPath.toString(), filePath)
        case EnumClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath) =>
          val ec = ptc.asEnumClass
          assertEquals(ec.fullPackageName, fullPackageName)
          assertEquals(ec.fullyQualifiedTypeRef.toString, fullyQualifiedTypeRef)
          assertEquals(ec.filePath.osSubPath.toString(), filePath)
        case FunctionClassExpectations(fullPackageName, fullyQualifiedTypeRef, filePath) => ??? // TODO
      }
    }
  }
}

//noinspection TypeAnnotation
class AsScalaTypeTest extends munit.FunSuite {
  import besom.codegen.Utils.TypeReferenceOps
  import besom.codegen.metaschema._

  implicit val logger: Logger = new Logger

  val schemaDir = os.pwd / ".out" / "schemas"

  case class Data(
    `type`: TypeReference,
    schemaName: SchemaProvider.SchemaName = "test-as-scala-type",
    schemaVersion: SchemaProvider.SchemaVersion = SchemaProvider.DefaultSchemaVersion,
    tags: Set[munit.Tag] = Set()
  )(val expected: Expectations*) {
    val pulumiPackage = PulumiPackage(name = schemaName)
  }

  case class Expectations(scalaType: String)

  val tests = List(
    Data(BooleanType)(Expectations("Boolean")),
    Data(StringType)(Expectations("String")),
    Data(IntegerType)(Expectations("Int")),
    Data(NumberType)(Expectations("Double")),
    Data(ArrayType(StringType))(Expectations("scala.collection.immutable.List[String]")),
    Data(MapType(IntegerType))(Expectations("scala.Predef.Map[String, Int]")),
    Data(UrnType)(Expectations("besom.types.URN")),
    Data(ResourceIdType)(Expectations("besom.types.ResourceId")),
    Data(UnionType(List(StringType, IntegerType), None))(Expectations("String | Int")),
    Data(NamedType("pulumi.json#/Archive"))(Expectations("besom.types.Archive")),
    Data(UnionType(List(StringType, NamedType("aws:iam/documents:PolicyDocument")), Some(StringType)))(
      Expectations("String | besom.api.aws.iam.documents.PolicyDocument")
    ),
    Data(NamedType("#/types/kubernetes:rbac.authorization.k8s.io/v1beta1:RoleRef"))(
      Expectations("rbac.authorization.k8s.io/v1beta1:RoleRef")
    ),
    Data(ArrayType(NamedType("#/types/kubernetes:rbac.authorization.k8s.io%2Fv1beta1:Subject")))(
      Expectations("besom.api.rbac.authorization.v1beta1.Subject")
    ),
//    Data(NamedType("/kubernetes/v3.7.0/schema.json#/provider", None))(Expectations("besom.api.kubernetes.Provider")),
    Data(NamedType("/aws/v4.36.0/schema.json#/resources/aws:ec2%2FsecurityGroup:SecurityGroup"))(
      Expectations("besom.api.aws.ec2.SecurityGroup")
    ),
    Data(
      NamedType(
        "/google-native/v0.18.2/schema.json#/types/google-native:accesscontextmanager/v1:DevicePolicyAllowedDeviceManagementLevelsItem"
      )
    )(
      Expectations("besom.api.googlenative.accesscontextmanager.v1.enums.DevicePolicyAllowedDeviceManagementLevelsItem")
    ),
    Data(
      NamedType("https://example.com/random/v2.3.1/schema.json#/resources/random:index%2FrandomPet:RandomPet")
    )(
      Expectations("besom.api.example.randomPet.RandomPet")
    )
  )

  tests.foreach { data =>
    test(s"${data.`type`.getClass.getSimpleName}".withTags(data.tags)) {
      val schemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = schemaDir)
      val packageInfo    = schemaProvider.packageInfo(PulumiPackage(name = data.schemaName))

      implicit val tm: TypeMapper = new TypeMapper(packageInfo, schemaProvider)

      data.expected.foreach { e =>
        assertEquals(data.`type`.asScalaType().toString(), e.scalaType)
      }
    }
  }
}
