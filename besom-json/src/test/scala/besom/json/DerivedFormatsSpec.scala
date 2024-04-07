package besom.json.test

import org.specs2.mutable.*

class DerivedFormatsSpec extends Specification {

  "The derives keyword" should {
    "behave as expected" in {
      import besom.json.*

      case class Color(name: String, red: Int, green: Int, blue: Int) derives JsonFormat
      val color = Color("CadetBlue", 95, 158, 160)

      color.toJson.convertTo[Color] mustEqual color
    }

    "be able to support default argument values" in {
      import besom.json.*

      case class Color(name: String, red: Int, green: Int, blue: Option[Int] = Some(255)) derives JsonFormat
      val color = Color("CadetBlue", 95, 158)

      val json = """{"name":"CadetBlue","red":95,"green":158}"""

      color.toJson.convertTo[Color] mustEqual color
      json.parseJson.convertTo[Color] mustEqual color
    }

    "be able to support missing fields when there are default argument values" in {
      import besom.json.*

      case class Color(name: String, red: Int, green: Int, blue: Option[Int] = None) derives JsonFormat
      val color = Color("CadetBlue", 95, 158)

      val json = """{"green":158,"red":95,"name":"CadetBlue"}"""

      color.toJson.compactPrint mustEqual json
      color.toJson.convertTo[Color] mustEqual color
      json.parseJson.convertTo[Color] mustEqual color
    }

    "be able to write and read nulls for optional fields" in {
      import besom.json.custom.*

      locally {
        given jp: JsonProtocol = new DefaultJsonProtocol {
          override def writeNulls             = true
          override def requireNullsForOptions = true
        }
        import jp.*

        case class Color(name: String, red: Int, green: Int, blue: Option[Int]) derives JsonFormat
        val color = Color("CadetBlue", 95, 158, None)

        val json = """{"blue":null,"green":158,"red":95,"name":"CadetBlue"}"""

        color.toJson.compactPrint mustEqual json

        color.toJson.convertTo[Color] mustEqual color
        json.parseJson.convertTo[Color] mustEqual color

        val noExplicitNullJson = """{"green":158,"red":95,"name":"CadetBlue"}"""
        noExplicitNullJson.parseJson.convertTo[Color] must throwA[DeserializationException]
      }

      locally {
        given jp2: JsonProtocol = new DefaultJsonProtocol {
          override def writeNulls             = false
          override def requireNullsForOptions = false
        }
        import jp2.*

        case class Color(name: String, red: Int, green: Int, blue: Option[Int]) derives JsonFormat
        val color = Color("CadetBlue", 95, 158, None)

        val json = """{"green":158,"red":95,"name":"CadetBlue"}"""

        color.toJson.compactPrint mustEqual json

        color.toJson.convertTo[Color] mustEqual color
        json.parseJson.convertTo[Color] mustEqual color
      }
    }
  }
}
