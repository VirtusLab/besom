package besom.cfg

case class Test1(los: List[String]) derives Configured

case class Test2(name: String, int: Int, struct: First, list: List[Double]) derives Configured
case class First(d: Int, e: String)

case class Test3(
  name: String,
  int: Int,
  s: First,
  l: List[Double],
  ls: List[Second],
  lo: List[String],
  ol: List[String],
  os: Third
) derives Configured
case class Second(f1: List[Fourth], f2: String)
case class Third(oh: String, it: String)
case class Fourth(deep: String)

class ConfiguredTest extends munit.FunSuite:

  test("very simple case class") {
    val env = Map("los.0" -> "test", "los.1" -> "test2")

    summon[Configured[Test1]].newInstanceFromEnv(env) match
      case Test1(los) =>
        assertEquals(los, List("test", "test2"))
  }

  test("can read a simple case class from environment variables") {
    val env = Map(
      "name" -> "test",
      "int" -> "23",
      "struct.d" -> "23",
      "struct.e" -> "test",
      "list.0" -> "1.2",
      "list.1" -> "2.3",
      "list.2" -> "3.4"
    )

    summon[Configured[Test2]].newInstanceFromEnv(env) match
      case Test2(name, int, s, l) =>
        assertEquals(name, "test")
        assertEquals(int, 23)
        assertEquals(s, First(23, "test"))
        assertEquals(l, List(1.2, 2.3, 3.4))
  }

  test("can read a complex case class from environment variables") {
    val env = Map(
      "name" -> "test",
      "int" -> "23",
      "s.d" -> "23",
      "s.e" -> "test",
      "l.0" -> "1.2",
      "l.1" -> "2.3",
      "l.2" -> "3.4",
      "ls.0.f1.0.deep" -> "q",
      "ls.0.f2" -> "w",
      "lo.0" -> "a",
      "lo.1" -> "b",
      "lo.2" -> "c",
      "ol.0" -> "x",
      "ol.1" -> "y",
      "ol.2" -> "z",
      "os.oh" -> "yeah",
      "os.it" -> "works!"
    )

    summon[Configured[Test3]].newInstanceFromEnv(env) match
      case Test3(name, int, s, l, ls, lo, ol, os) =>
        assertEquals(name, "test")
        assertEquals(int, 23)
        assertEquals(s, First(23, "test"))
        assertEquals(l, List(1.2, 2.3, 3.4))
        assertEquals(ls, List(Second(List(Fourth("q")), "w")))
        assertEquals(lo, List("a", "b", "c"))
        assertEquals(ol, List("x", "y", "z"))
        assertEquals(os, Third("yeah", "works!"))
  }
end ConfiguredTest
