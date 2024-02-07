package besom.internal

import besom.*
import besom.internal.RunResult.{*, given}
import besom.json.*
import besom.json.DefaultJsonProtocol.*

class ConfigTest extends munit.FunSuite {

  def testConfig[A](testName: String)(
    configMap: Map[String, String],
    configSecretKeys: Set[NonEmptyString]
  )(
    assertion: Context ?=> Unit
  ): Unit =
    val nonEmptyKeyConfigMap = configMap.map { (key, value) => NonEmptyString(key).get -> value }
    given Context            = DummyContext(configMap = nonEmptyKeyConfigMap, configSecretKeys = configSecretKeys).unsafeRunSync()
    test(testName)(assertion)

  def testConfig[A](testName: String)(
    configMap: Map[String, String],
    configSecretKeys: Set[NonEmptyString],
    tested: Context ?=> Output[A]
  )(
    assertion: Context ?=> OutputData[A] => Unit
  ): Unit =
    testConfig[A](testName)(configMap, configSecretKeys)(assertion(tested.getData.unsafeRunSync()))

  def testConfig[A](testName: String)(
    configMap: Map[String, String],
    configSecretKeys: Set[NonEmptyString],
    tested: Context ?=> Output[A],
    expected: OutputData[A]
  ): Unit =
    testConfig[A](testName)(configMap, configSecretKeys) {
      val outputData = tested.getData.unsafeRunSync()
      assert(clue(outputData) == expected)
    }

  def testConfigError[A](testName: String)(
    configMap: Map[String, String],
    configSecretKeys: Set[NonEmptyString],
    tested: Context ?=> Output[A],
    checkErrorMessage: String => Boolean
  ): Unit =
    testConfig[A](testName)(configMap, configSecretKeys) {
      val exception = intercept[Exception](tested.getData.unsafeRunSync())
      val message   = exception.getMessage
      assert(checkErrorMessage(clue(message)))
    }

  testConfig("get missing key")(
    configMap = Map.empty,
    configSecretKeys = Set.empty,
    tested = config.getString("foo"),
    expected = OutputData(None)
  )

  testConfigError("require missing key")(
    configMap = Map.empty,
    configSecretKeys = Set.empty,
    tested = config.requireString("foo"),
    checkErrorMessage = _.startsWith("Missing required configuration variable 'foo'")
  )

  testConfig("get string")(
    configMap = Map("foo" -> "abc", "bar" -> "def"),
    configSecretKeys = Set.empty,
    tested = config.getString("foo"),
    expected = OutputData(Some("abc"))
  )

  testConfig("get boolean")(
    configMap = Map("foo" -> "true", "bar" -> "false"),
    configSecretKeys = Set.empty,
    tested = config.getBoolean("foo"),
    expected = OutputData(Some(true))
  )

  testConfig("get double")(
    configMap = Map("foo" -> "1.23", "bar" -> "4.56"),
    configSecretKeys = Set.empty,
    tested = config.getDouble("foo"),
    expected = OutputData(Some(1.23d))
  )

  testConfig("get int")(
    configMap = Map("foo" -> "123", "bar" -> "456"),
    configSecretKeys = Set.empty,
    tested = config.getInt("foo"),
    expected = OutputData(Some(123))
  )

  testConfig("get JSON")(
    configMap = Map("names" -> """["a","b","c","super secret name"]"""),
    configSecretKeys = Set("names"),
    tested = config.getJson("names")
  ) { (actual: OutputData[Option[JsValue]]) =>
    val expectedJson = List("a", "b", "c", "super secret name").toJson
    val expected     = OutputData(Some(expectedJson), isSecret = true)
    assertEquals(actual, expected)
  }

  testConfig("require JSON")(
    configMap = Map("names" -> """["a","b","c","super secret name"]"""),
    configSecretKeys = Set("names"),
    tested = config.requireJson("names")
  ) { (actual: OutputData[JsValue]) =>
    val expectedJson = List("a", "b", "c", "super secret name").toJson
    val expected     = OutputData(expectedJson, isSecret = true)
    assertEquals(actual, expected)
  }

  testConfigError("require JSON with invalid JSON")(
    configMap = Map("names" -> """[a,"""),
    configSecretKeys = Set("names"),
    tested = config.requireJson("names"),
    checkErrorMessage = _.startsWith("Config value 'names' is not a valid JSON:")
  )

  testConfig("get JSON with invalid JSON")(
    configMap = Map("names" -> """[a,"""),
    configSecretKeys = Set("names")
  ) {
    interceptMessage[ConfigError]("Config value 'names' is not a valid JSON: [a,") {
      config.getJson("names").getData.unsafeRunSync()
    }
  }

  testConfig("get object List[String]")(
    configMap = Map("names" -> """["a","b","c","super secret name"]"""),
    configSecretKeys = Set("names"),
    tested = config.getObject[List[String]]("names")
  ) { (actual: OutputData[Option[List[String]]]) =>
    val expected = OutputData(Some(List("a", "b", "c", "super secret name")), isSecret = true)
    assertEquals(actual, expected)
  }

  testConfig("get object Map[String, Int]")(
    configMap = Map("foo" -> """{"a":1,"b":2}"""),
    configSecretKeys = Set.empty,
    tested = config.getObject[Map[String, Int]]("foo")
  ) { (actual: OutputData[Option[Map[String, Int]]]) =>
    val expected = OutputData(Some(Map("a" -> 1, "b" -> 2)))
    assertEquals(actual, expected)
  }

  testConfig("require object Map[String, Int]")(
    configMap = Map("foo" -> """{"a":1,"b":2}"""),
    configSecretKeys = Set.empty,
    tested = config.requireObject[Map[String, Int]]("foo")
  ) { (actual: OutputData[Map[String, Int]]) =>
    val expected = OutputData(Map("a" -> 1, "b" -> 2))
    assertEquals(actual, expected)
  }

  sealed abstract class Region(val name: String, val value: String) extends besom.types.StringEnum
  object Region extends EnumCompanion[String, Region]("Region"):
    object AFSouth1 extends Region("AFSouth1", "af-south-1")
    object APEast1 extends Region("APEast1", "ap-east-1")
    object APNortheast1 extends Region("APNortheast1", "ap-northeast-1")
    object APNortheast2 extends Region("APNortheast2", "ap-northeast-2")
    override val allInstances: Seq[Region] = Seq(
      AFSouth1,
      APEast1,
      APNortheast1,
      APNortheast2
    )
    given EnumCompanion[String, Region] = this

  testConfig("get StringEnum")(
    configMap = Map("prov:region" -> "ap-northeast-1"),
    configSecretKeys = Set.empty,
    tested = Codegen.config[Region]("prov")(
      key = "region",
      isSecret = false,
      environment = Nil,
      default = None
    )
  ) { actual =>
    val expected = OutputData(Some(Region.APNortheast1))
    assertEquals(actual, expected)
  }


  sealed abstract class Switch(val name: String, val value: Boolean) extends besom.types.BooleanEnum
  object Switch extends EnumCompanion[Boolean, Switch]("Switch"):
    object On extends Switch("On", true)
    object Off extends Switch("Off", false)
    override val allInstances: Seq[Switch] = Seq(On, Off)
    given EnumCompanion[Boolean, Switch] = this

  testConfig("get BooleanEnum")(
    configMap = Map("prov:switch" -> "false"),
    configSecretKeys = Set.empty,
    tested = Codegen.config[Switch]("prov")(
      key = "switch",
      isSecret = false,
      environment = Nil,
      default = None
    )
  ) { actual =>
    val expected = OutputData(Some(Switch.Off))
    assertEquals(actual, expected)
  }

  case class Foo(a: Int, b: Int)
  object Foo:
    given JsonFormat[Foo] = jsonFormatN
  testConfig("get case class Foo")(
    configMap = Map("foo" -> """{"a":1,"b":2}"""),
    configSecretKeys = Set("foo"),
    tested = {
      config.getObject[Foo]("foo")
    }
  ) { (actual: OutputData[Option[Foo]]) =>
    val expected = OutputData(Some(Foo(1, 2)), isSecret = true)
    assertEquals(actual, expected)
  }

  case class Bar(a: Int, b: Int)
  testConfig("get case class Bar compile error")(
    configMap = Map("bar" -> """{"a":1,"b":2}"""),
    configSecretKeys = Set.empty
  ) {
    assertNoDiff(
      compileErrors("""config.getObject[Bar]("bar")"""),
      """|error:
         |No given instance of type besom.internal.ConfigValueReader[ConfigTest.this.Bar] was found for an implicit parameter of method getObject in class Config.
         |I found:
         |
         |    besom.internal.ConfigValueReader.objectReader[ConfigTest.this.Bar](
         |      /* missing */summon[besom.json.JsonReader[ConfigTest.this.Bar]])
         |
         |But no implicit values were found that match type besom.json.JsonReader[ConfigTest.this.Bar].
         |config.getObject[Bar]("bar")
         |                           ^
         |""".stripMargin
    )
  }

  testConfig("require case Bar class compile error")(
    configMap = Map("bar" -> """{"a":1,"b":2}"""),
    configSecretKeys = Set.empty
  ) {
    assertNoDiff(
      compileErrors("""config.requireObject[Bar]("bar")"""),
      """|error:
         |No given instance of type besom.internal.ConfigValueReader[ConfigTest.this.Bar] was found for an implicit parameter of method requireObject in class Config.
         |I found:
         |
         |    besom.internal.ConfigValueReader.objectReader[ConfigTest.this.Bar](
         |      /* missing */summon[besom.json.JsonReader[ConfigTest.this.Bar]])
         |
         |But no implicit values were found that match type besom.json.JsonReader[ConfigTest.this.Bar].
         |config.requireObject[Bar]("bar")
         |                               ^
         |""".stripMargin
    )
  }

  testConfig("get secret")(
    configMap = Map("foo" -> "abc", "bar" -> "def"),
    configSecretKeys = Set("foo"),
    tested = config.getString("foo"),
    expected = OutputData(Some("abc"), isSecret = true)
  )

  testConfig("get namespaced")(
    configMap = Map("foo" -> "abc", "qux:foo" -> "def"),
    configSecretKeys = Set.empty,
    tested = Config("qux").getString("foo"),
    expected = OutputData(Some("def"))
  )

  testConfig("prefer non-namespaced")(
    configMap = Map("foo" -> "abc", "qux:foo" -> "def"),
    configSecretKeys = Set.empty,
    tested = config.getString("foo"),
    expected = OutputData(Some("abc"))
  )

  testConfig("get string-namespaced")(
    configMap = Map("foo" -> "abc", "qux:foo" -> "def"),
    configSecretKeys = Set.empty,
    tested = config.getString("qux:foo"),
    expected = OutputData(Some("def"))
  )

  testConfig("getOrElse")(
    configMap = Map.empty,
    configSecretKeys = Set.empty
  ) {
    val outputData1 = config.getString("foo").getOrElse("bar").getData.unsafeRunSync()
    clue(outputData1)
    assertEquals(outputData1, OutputData("bar"))

    val outputData2 = config.getString("foo").getOrElse(Output("bar")).getData.unsafeRunSync()
    clue(outputData2)
    assertEquals(outputData2, OutputData("bar"))
  }

  testConfig("orElse")(
    configMap = Map.empty,
    configSecretKeys = Set.empty
  ) {
    val outputData1 = config.getString("foo").orElse(Some("bar")).getData.unsafeRunSync()
    clue(outputData1)
    assertEquals(outputData1, OutputData(Some("bar")))

    val outputData2 = config.getString("foo").orElse(Output(Some("bar"))).getData.unsafeRunSync()
    clue(outputData2)
    assertEquals(outputData2, OutputData(Some("bar")))
  }

  testConfig("Codegen.config - List")(
    configMap = Map("prov:names" -> """["a","b","c","super secret name"]"""),
    configSecretKeys = Set.empty,
    tested = Codegen.config[List[String]]("prov")(
      key = "names",
      isSecret = true,
      environment = Nil,
      default = None
    )
  ) { actual =>
    val expected = OutputData(Some(List("a", "b", "c", "super secret name")), isSecret = true)
    assertEquals(actual, expected)
  }
  
  testConfig("Codegen.config - Map")(
    configMap = Map("prov:names" -> """{"a":1,"b":2}"""),
    configSecretKeys = Set.empty,
    tested = Codegen.config[Map[String, JsValue]]("prov")(
      key = "names",
      isSecret = false,
      environment = Nil,
      default = None
    )
  ) { actual =>
    val expected = OutputData(Some(Map("a" -> JsNumber(1), "b" -> JsNumber(2))))
    assertEquals(actual, expected)
  }
}
