package besom.json

import org.specs2.mutable.*

class DerivedFormatsSpec extends Specification {

  "The derives keyword" should {
    "behave as expected" in {
      import besom.json.default.*

      case class Color(name: String, red: Int, green: Int, blue: Int) derives JsonFormat
      val color = Color("CadetBlue", 95, 158, 160)

      color.toJson.convertTo[Color] mustEqual color
    }
  }
}
