package besom.codegen

import besom.codegen.Utils.TypeReferenceOps

import scala.meta.*
import scala.meta.dialects.Scala33

//noinspection TypeAnnotation,ScalaFileName
class TypeMapperTest extends munit.FunSuite {
  import besom.codegen.metaschema.*

  object TestPackageMetadata extends PackageMetadata("test-as-scala-type", Some(PackageVersion.default))

  case class Data(
    `type`: TypeReference,
    metadata: PackageMetadata = TestPackageMetadata,
    asArgsType: Boolean = false,
    tags: Set[munit.Tag] = Set()
  )(val expected: Expectations*)

  case class Expectations(scalaType: String)

  val tests = List(
    Data(UrnType)(Expectations("besom.types.URN")),
    Data(ResourceIdType)(Expectations("besom.types.ResourceId")),
    Data(MapType(IntegerType, plainProperties = false))(Expectations("scala.Predef.Map[String, Int]")),
    Data(UnionType(List(StringType, IntegerType), None))(Expectations("String | Int")),
    Data(
      UnionType(List(StringType, NamedType("aws:iam/documents:PolicyDocument")), Some(StringType)),
      metadata = PackageMetadata("aws", "6.7.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("String")
    ),
    Data(
      UnionType(List(StringType, NamedType("#/types/aws:iam/role:Role")), Some(StringType)),
      metadata = PackageMetadata("aws", "6.7.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("String | besom.api.aws.iam.Role")
    ),
    Data(
      UnionType(List(StringType, ArrayType(NamedType("#/types/abc:xyz/not:There", Some(StringType)), plainItems = false)), None),
      tags = Set(munit.Slow)
    )(
      Expectations("String | scala.collection.immutable.Iterable[String]")
    ),
    Data(
      NamedType("#/types/abc:xyz/not:There", Some(StringType))
    )(
      Expectations("String")
    ),
    Data(
      NamedType("#/types/aws-native:index%2Fregion:Region"),
      metadata = PackageMetadata("aws-native", "0.84.0"),
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
      metadata = PackageMetadata("kubernetes", "3.7.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("besom.api.kubernetes.rbac.v1beta1.outputs.RoleRef")
    ),
    Data(
      ArrayType(NamedType("#/types/kubernetes:rbac.authorization.k8s.io%2Fv1beta1:Subject"), plainItems = false),
      metadata = PackageMetadata("kubernetes", "3.7.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("scala.collection.immutable.Iterable[besom.api.kubernetes.rbac.v1beta1.outputs.Subject]")
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
    Data(ArrayType(StringType, plainItems = false))(Expectations("scala.collection.immutable.Iterable[String]")),
    Data(MapType(StringType, plainProperties = false))(Expectations("scala.Predef.Map[String, String]")),
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
    Data(UnionType(List(StringType, NumberType), None))(Expectations("String | Double")),
    Data(
      UnionType(List(StringType, NamedType("#types/aws:iam/role:Role")), Some(StringType)),
      metadata = PackageMetadata("aws", "6.7.0"),
      tags = Set(munit.Slow)
    )(
      Expectations("String | besom.api.aws.iam.Role")
    )
  )

  tests.foreach { data =>
    test(s"${data.`type`.getClass.getSimpleName}".withTags(data.tags)) {
      given Config                         = Config()
      given Logger                         = Logger()
      given schemaProvider: SchemaProvider = DownloadingSchemaProvider()
      val packageInfo = data.metadata match {
        case m @ TestPackageMetadata => schemaProvider.packageInfo(m, PulumiPackage(name = m.name))
        case _                       => schemaProvider.packageInfo(data.metadata)
      }
      given TypeMapper = TypeMapper(packageInfo)

      data.expected.foreach { e =>
        assertEquals(data.`type`.asScalaType(data.asArgsType).syntax, e.scalaType)
      }
    }
  }
}
