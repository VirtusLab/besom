package besom.codegen

import besom.codegen.PackageMetadata.SchemaVersion
import besom.codegen.Utils.{PulumiPackageOps, TypeReferenceOps}

//noinspection TypeAnnotation,ScalaFileName
class TypeMapperTest extends munit.FunSuite {
  import besom.codegen.PackageMetadata.SchemaName
  import besom.codegen.metaschema._

  implicit val logger: Logger = new Logger

  private val defaultTestSchemaName = "test-as-scala-type"

  case class Data(
    `type`: TypeReference,
    schemaName: Option[SchemaName] = None,
    schemaVersion: Option[SchemaVersion] = None,
    pulumiPackage: Option[PulumiPackage] = None,
    asArgsType: Boolean = false,
    tags: Set[munit.Tag] = Set()
  )(val expected: Expectations*)

  case class Expectations(scalaType: String)

  val tests = List(
    Data(UrnType)(Expectations("besom.types.URN")),
    Data(ResourceIdType)(Expectations("besom.types.ResourceId")),
    Data(MapType(IntegerType))(Expectations("scala.Predef.Map[String, Int]")),
    Data(UnionType(List(StringType, IntegerType), None))(Expectations("String | Int")),
    Data(
      UnionType(List(StringType, NamedType("aws:iam/documents:PolicyDocument")), Some(StringType)),
      schemaName = Some("aws"),
      schemaVersion = Some("6.7.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("String")
    ),
    Data(
      UnionType(List(StringType, NamedType("#/types/aws:iam/role:Role")), Some(StringType)),
      schemaName = Some("aws"),
      schemaVersion = Some("6.7.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("String | besom.api.aws.iam.Role")
    ),
    Data(
      NamedType("#/types/aws-native:index%2Fregion:Region"),
      schemaName = Some("aws-native"),
      schemaVersion = Some("0.84.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("besom.api.awsnative.region.enums.Region")
    ),
    Data(
      NamedType("/kubernetes/v3.7.0/schema.json#/provider"),
      tags = Set(munit.Slow)
    )(
      Expectations("besom.api.kubernetes.Provider")
    ),
    Data(
      NamedType("/aws/v4.36.0/schema.json#/resources/aws:ec2%2FsecurityGroup:SecurityGroup"),
      tags = Set(munit.Slow)
    )(
      Expectations("besom.api.aws.ec2.SecurityGroup")
    ),
    Data(
      NamedType("/aws/v6.5.0/schema.json#/resources/pulumi:providers:aws"),
      tags = Set(munit.Slow)
    )(
      Expectations("besom.api.aws.Provider")
    ),
    Data(
      NamedType("#/types/kubernetes:rbac.authorization.k8s.io/v1beta1:RoleRef"),
      schemaName = Some("kubernetes"),
      schemaVersion = Some("3.7.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("besom.api.kubernetes.rbac.v1beta1.outputs.RoleRef")
    ),
    Data(
      ArrayType(NamedType("#/types/kubernetes:rbac.authorization.k8s.io%2Fv1beta1:Subject")),
      schemaName = Some("kubernetes"),
      schemaVersion = Some("3.7.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("scala.collection.immutable.List[besom.api.kubernetes.rbac.v1beta1.outputs.Subject]")
    ),
    Data(
      NamedType(
        "/google-native/v0.18.2/schema.json#/types/google-native:accesscontextmanager/v1:DevicePolicyAllowedDeviceManagementLevelsItem"
      ),
      tags = Set(munit.Slow)
    )(
      Expectations("besom.api.googlenative.accesscontextmanager.v1.enums.DevicePolicyAllowedDeviceManagementLevelsItem")
    ),
    // Test data from https://github.com/pulumi/pulumi/blob/42784f6204a021575f0fdb9b50c4f93d4d062b72/pkg/codegen/testing/test/testdata/types.json
    Data(ArrayType(StringType))(Expectations("scala.collection.immutable.List[String]")),
    Data(MapType(StringType))(Expectations("scala.Predef.Map[String, String]")),
    Data(NamedType("pulumi.json#/Any"))(Expectations("besom.types.PulumiAny")),
    Data(NamedType("pulumi.json#/Archive"))(Expectations("besom.types.Archive")),
    Data(NamedType("pulumi.json#/Asset"))(
      Expectations("besom.types.AssetOrArchive")
    ),
    Data(BooleanType)(Expectations("Boolean")),
    Data(IntegerType)(Expectations("Int")),
    Data(NamedType("pulumi.json#/Json"))(Expectations("besom.types.PulumiJson")),
    Data(NumberType)(Expectations("Double")),
    Data(StringType)(Expectations("String")),
    Data(UnionType(List(StringType, NumberType), None))(Expectations("String | Double"))
    // TODO: input tests
  )

  tests.foreach { data =>
    test(s"${data.`type`.getClass.getSimpleName}".withTags(data.tags)) {
      val schemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = Config.DefaultSchemasDir)
      val (_, packageInfo) = data.pulumiPackage
        .map { p =>
          schemaProvider.packageInfo(p, p.toPackageMetadata())
        }
        .getOrElse {
          (data.schemaName, data.schemaVersion) match {
            case (Some(schemaName), Some(schemaVersion)) =>
              schemaProvider.packageInfo(PackageMetadata(schemaName, schemaVersion))
            case _ =>
              schemaProvider.packageInfo(
                PulumiPackage(defaultTestSchemaName),
                PackageMetadata(defaultTestSchemaName, "0.0.0")
              )
          }
        }
      implicit val tm: TypeMapper = new TypeMapper(packageInfo, schemaProvider)

      data.expected.foreach { e =>
        assertEquals(data.`type`.asScalaType(data.asArgsType).toString(), e.scalaType)
      }
    }
  }
}
