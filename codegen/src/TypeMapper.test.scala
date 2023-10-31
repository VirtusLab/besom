package besom.codegen

import besom.codegen.Config.ProviderConfig

//noinspection TypeAnnotation
class TypeMapperTest extends munit.FunSuite {
  import besom.codegen.Utils.TypeReferenceOps
  import besom.codegen.metaschema._

  implicit val logger: Logger = new Logger

  val schemaDir = os.pwd / ".out" / "schemas"

  case class Data(
    `type`: TypeReference,
    providerName: SchemaProvider.ProviderName = "example",
    schemaVersion: SchemaProvider.SchemaVersion = "0.0.0",
    tags: Set[munit.Tag] = Set()
  )(val expected: Expectations*)

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
    Data(NamedType("pulumi.json#/Archive", None))(Expectations("besom.types.Archive"))
  )

  tests.foreach { data =>
    test(s"${data.`type`.getClass.getSimpleName}".withTags(data.tags)) {
      implicit val providerConfig: ProviderConfig = Config.providersConfigs(data.providerName)

      val schemaProvider = new DownloadingSchemaProvider(schemaCacheDirPath = schemaDir)

      implicit val tm: TypeMapper = new TypeMapper(
        defaultProviderName = data.providerName,
        defaultSchemaVersion = data.schemaVersion,
        schemaProvider = schemaProvider
      )

      data.expected.foreach { e =>
        assertEquals(data.`type`.asScalaType().toString(), e.scalaType)
      }
    }
  }
}
