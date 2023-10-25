package besom.codegen

//noinspection ScalaFileName,TypeAnnotation
class ClassCoordinatesTest extends munit.FunSuite {
  implicit val providerConfig: Config.ProviderConfig = Config.ProviderConfig()

  case class Data(
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    className: String,
    tags: munit.Tag*
  )(val expected: Expectations)
  case class Expectations(
    fullPackageName: String,
    fullyQualifiedTypeRef: String,
    filePath: String
  )

  val tests = List(
    Data(
      providerPackageParts = Seq("example"),
      modulePackageParts = Seq(),
      className = "Provider",
      tags = munit.Ignore
    )(expected =
      Expectations(
        fullPackageName = "besom.api.example",
        fullyQualifiedTypeRef = "besom.api.example.Provider",
        filePath = "src/index/Provider.scala"
      )
    ),
    Data(
      providerPackageParts = Seq("foo-bar"),
      modulePackageParts = Seq(),
      className = "DashNamedProvider",
      tags = munit.Ignore
    )(expected =
      Expectations(
        fullPackageName = "besom.api.foobar",
        fullyQualifiedTypeRef = "besom.api.foobar.DashNamedProvider",
        filePath = "src/index/DashNamedProvider.scala"
      )
    )
  )

  tests.foreach { data =>
    test(s"Type: ${data.className}".withTags(data.tags.toSet)) {
      val cc: ClassCoordinates = ClassCoordinates(
        providerPackageParts = data.providerPackageParts,
        modulePackageParts = data.modulePackageParts,
        className = data.className
      )

      assertEquals(cc.fullPackageName, data.expected.fullPackageName)
      assertEquals(cc.fullyQualifiedTypeRef.toString, data.expected.fullyQualifiedTypeRef)
      assertEquals(cc.filePath.osSubPath.toString(), data.expected.filePath)
    }
  }
}
