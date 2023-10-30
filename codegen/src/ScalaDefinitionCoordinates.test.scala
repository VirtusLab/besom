package besom.codegen

//noinspection ScalaFileName,TypeAnnotation
class ScalaDefinitionCoordinatesTest extends munit.FunSuite {
  implicit val providerConfig: Config.ProviderConfig = Config.ProviderConfig()

  case class Data(
    providerPackageParts: Seq[String],
    modulePackageParts: Seq[String],
    definitionName: String,
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
      definitionName = "Provider"
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
      definitionName = "DashNamedProvider"
    )(expected =
      Expectations(
        fullPackageName = "besom.api.foobar",
        fullyQualifiedTypeRef = "besom.api.foobar.DashNamedProvider",
        filePath = "src/index/DashNamedProvider.scala"
      )
    )
  )

  tests.foreach { data =>
    test(s"Type: ${data.definitionName}".withTags(data.tags.toSet)) {
      val coords: ScalaDefinitionCoordinates = ScalaDefinitionCoordinates(
        providerPackageParts = data.providerPackageParts,
        modulePackageParts = data.modulePackageParts,
        definitionName = data.definitionName
      )

      assertEquals(coords.fullPackageName, data.expected.fullPackageName)
      assertEquals(coords.fullyQualifiedTypeRef.toString, data.expected.fullyQualifiedTypeRef)
      assertEquals(coords.filePath.osSubPath.toString(), data.expected.filePath)
    }
  }
}
