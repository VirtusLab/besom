package besom.cfg

import besom.cfg.Configured.FromEnv

case class Test1(los: List[String]) derives Configured.FromEnv

case class Test2(name: String, int: Int, struct: First, list: List[Double]) derives Configured.FromEnv
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
) derives Configured.FromEnv
case class Second(f1: List[Fourth], f2: String)
case class Third(oh: String, it: String)
case class Fourth(deep: String)

class ConfiguredTest extends munit.FunSuite:

  extension (m: Map[String, String])
    def withBesomCfgPrefix: Map[String, String] =
      m.map { case (k, v) => s"${from.env.ReadFromEnvVars.Prefix}_$k" -> v }

  test("very simple case class") {
    val env = Map("los.0" -> "test", "los.1" -> "test2").withBesomCfgPrefix

    summon[Configured.FromEnv[Test1]].newInstance(env) match
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
    ).withBesomCfgPrefix

    summon[Configured.FromEnv[Test2]].newInstance(env) match
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
    ).withBesomCfgPrefix

    summon[Configured.FromEnv[Test3]].newInstance(env) match
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

  test("proper error messages") {
    val confErr = intercept[ConfigurationError](summon[Configured.FromEnv[Test2]].newInstance(Map.empty))
    assertEquals(confErr.errors.size, 4)
    assertNoDiff(
      confErr.getMessage(),
      """Start of the application was impossible due to the following configuration errors:
  * Missing value for key: name (env var: `BESOM_CFG_name`)
  * Missing value for key: int (env var: `BESOM_CFG_int`)
  * Missing value for key: struct.d (env var: `BESOM_CFG_struct.d`)
  * Missing value for key: struct.e (env var: `BESOM_CFG_struct.e`)"""
    )
  }

  val env = Map("los.0" -> "test", "los.1" -> "test2").withBesomCfgPrefix

  test("resolve configuration - use default") {
    given Default[FromEnv.EnvData] = new Default[FromEnv.EnvData]:
      def default: FromEnv.EnvData = env

    try
      resolveConfiguration[Test1] match
        case Test1(los) =>
          assertEquals(los, List("test", "test2"))
    catch
      case e: ConfigurationError =>
        fail(e.getMessage())
  }

  test("resolve configuration - use direct argument") {
    try
      resolveConfiguration[Test1](env) match
        case Test1(los) =>
          assertEquals(los, List("test", "test2"))
    catch
      case e: ConfigurationError =>
        fail(e.getMessage())
  }

  test("resolve configuration - use default and either variant") {
    given Default[FromEnv.EnvData] = new Default[FromEnv.EnvData]:
      def default: FromEnv.EnvData = env

      resolveConfigurationEither[Test1] match
        case Right(Test1(los)) =>
          assertEquals(los, List("test", "test2"))
        case Left(cfgError) =>
          fail(cfgError.getMessage())
  }

  test("resolve configuration - use direct argument and either variant") {
    resolveConfigurationEither[Test1](env) match
      case Right(Test1(los)) =>
        assertEquals(los, List("test", "test2"))
      case Left(cfgError) =>
        fail(cfgError.getMessage())
  }
end ConfiguredTest
