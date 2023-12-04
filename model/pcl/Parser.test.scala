package besom.model.pcl

import scala.meta._
import scala.meta.dialects.Scala33

//noinspection ScalaFileName
class ParserTest extends munit.FunSuite {

  case class Data(
    name: String,
    pcl: String,
    expected: String
  )

  Vector(
    Data(
      name = "Random pet",
      pcl =
        s"""|resource random-pet "random:index/randomPet:RandomPet" {
            |  prefix = "doggo"
            |}
            |""".stripMargin,
      expected = {
        s"""|val randomPet = new besom.api.random.RandomPet(
            |  name = "random-pet",
            |  args = besom.api.random.RandomPetArgs(
            |    prefix = "doggo"
            |  )
            |)
            |""".stripMargin
      }
    )
  ).foreach(data => {
    test(data.name) {
      assertEquals(Parser.crossCompile(data.pcl).syntax, data.expected)
    }
  })
}
