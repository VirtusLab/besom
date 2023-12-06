package besom.codegen

import scala.meta.*

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
      providerPackageParts = "example" :: Nil,
      modulePackageParts = Nil,
      definitionName = "Provider"
    )(
      Expectations(
        fullPackageName = "besom.api.example",
        fullyQualifiedTypeRef = "besom.api.example.Provider",
        filePath = "src/index/Provider.scala"
      )
    ),
    Data(
      providerPackageParts = "foo-bar" :: Nil,
      modulePackageParts = Nil,
      definitionName = "DashNamedProvider"
    )(
      Expectations(
        fullPackageName = "besom.api.foobar",
        fullyQualifiedTypeRef = "besom.api.foobar.DashNamedProvider",
        filePath = "src/index/DashNamedProvider.scala"
      )
    ),
    Data(
      providerPackageParts = "aws-native" :: Nil,
      modulePackageParts = "index" :: "Region" :: Nil, // unexpected upper case
      definitionName = "Region"
    )(
      Expectations(
        fullPackageName = "besom.api.awsnative.region",
        fullyQualifiedTypeRef = "besom.api.awsnative.region.Region",
        filePath = "src/index/region/Region.scala"
      )
    )
  )

  tests.foreach { data =>
    test(s"Type: ${data.definitionName}".withTags(data.tags.toSet)) {
      val coords: ScalaDefinitionCoordinates = ScalaDefinitionCoordinates(
        providerPackageParts = data.providerPackageParts,
        modulePackageParts = data.modulePackageParts,
        definitionName = Some(data.definitionName)
      )

      assertEquals(coords.packageRef.syntax, data.expected.fullPackageName)
      assertEquals(coords.typeRef.syntax, data.expected.fullyQualifiedTypeRef)
      assertEquals(coords.filePath.osSubPath.toString(), data.expected.filePath)
    }
  }
}
