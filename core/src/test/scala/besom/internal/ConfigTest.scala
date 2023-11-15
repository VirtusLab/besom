package besom.internal

import besom.*
import RunResult.{given, *}

class ConfigTest extends munit.FunSuite {

  def testConfig[A](testName: String)(configMap: Map[String, String], configSecretKeys: Set[NonEmptyString])(assertion: Context ?=> Unit): Unit =
    val nonEmptyKeyConfigMap = configMap.map{ (key, value) => NonEmptyString(key).get -> value }
    given Context = DummyContext(configMap = nonEmptyKeyConfigMap, configSecretKeys = configSecretKeys).unsafeRunSync()
    test(testName)(assertion)

  def testConfig[A](testName: String)(configMap: Map[String, String], configSecretKeys: Set[NonEmptyString], tested: Context ?=> Output[A], expected: OutputData[A]): Unit =
    testConfig[A](testName)(configMap, configSecretKeys) {
      val outputData = tested.getData.unsafeRunSync()
      assert(clue(outputData) == expected)
    }

  def testConfigError[A](testName: String)(configMap: Map[String, String], configSecretKeys: Set[NonEmptyString], tested: Context ?=> Output[A], checkErrorMessage: String => Boolean): Unit =
    testConfig[A](testName)(configMap, configSecretKeys) {
      val exception = intercept[Exception](tested.getData.unsafeRunSync())
      val message = exception.getMessage
      assert(checkErrorMessage(clue(message)))
    }

  testConfig("get missing key")(
    configMap = Map.empty,
    configSecretKeys = Set.empty,
    tested = config.getString("foo"),
    expected = OutputData(None, isSecret = false)
  )

  testConfigError("require missing key")(
    configMap = Map.empty,
    configSecretKeys = Set.empty,
    tested = config.requireString("foo"),
    checkErrorMessage = msg => msg.startsWith("Missing required configuration variable 'foo'")
  )

  testConfig("get string")(
    configMap = Map("foo" -> "abc", "bar" -> "def"),
    configSecretKeys = Set.empty,
    tested = config.getString("foo"),
    expected = OutputData(Some("abc"), isSecret = false)
  )

  testConfig("get boolean")(
    configMap = Map("foo" -> "true",  "bar" -> "false"),
    configSecretKeys = Set.empty,
    tested = config.getBoolean("foo"),
    expected = OutputData(Some(true), isSecret = false)
  )

  testConfig("get double")(
    configMap = Map("foo" -> "1.23",  "bar" -> "4.56"),
    configSecretKeys = Set.empty,
    tested = config.getDouble("foo"),
    expected = OutputData(Some(1.23d), isSecret = false)
  )

  testConfig("get int")(
    configMap = Map("foo" -> "123",  "bar" -> "456"),
    configSecretKeys = Set.empty,
    tested = config.getInt("foo"),
    expected = OutputData(Some(123), isSecret = false)
  )

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
    expected = OutputData(Some("def"), isSecret = false)
  )

  testConfig("prefer non-namespaced")(
    configMap = Map("foo" -> "abc", "qux:foo" -> "def"),
    configSecretKeys = Set.empty,
    tested = config.getString("foo"),
    expected = OutputData(Some("abc"), isSecret = false)
  )

  testConfig("get string-namespaced")(
    configMap = Map("foo" -> "abc", "qux:foo" -> "def"),
    configSecretKeys = Set.empty
  ) {
    val outputData = config.getString("qux:foo").getData.unsafeRunSync()
    clue(outputData)
    assertEquals(outputData, OutputData(Some("def"), isSecret = false))
  }

  testConfig("getOrElse")(
    configMap = Map.empty,
    configSecretKeys = Set.empty,
  ) {
    val outputData1 = config.getString("foo").getOrElse("bar").getData.unsafeRunSync()
    clue(outputData1)
    assertEquals(outputData1, OutputData("bar", isSecret = false))

    val outputData2 = config.getString("foo").getOrElse(Output("bar")).getData.unsafeRunSync()
    clue(outputData2)
    assertEquals(outputData2, OutputData("bar", isSecret = false))
  }

  testConfig("orElse")(
    configMap = Map.empty,
    configSecretKeys = Set.empty
  ) {
    val outputData1 = config.getString("foo").orElse(Some("bar")).getData.unsafeRunSync()
    clue(outputData1)
    assertEquals(outputData1, OutputData(Some("bar"), isSecret = false))

    val outputData2 = config.getString("foo").orElse(Output(Some("bar"))).getData.unsafeRunSync()
    clue(outputData2)
    assertEquals(outputData2, OutputData(Some("bar"), isSecret = false))
  }
}
