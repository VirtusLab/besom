package besom.internal

import besom.*
import besom.internal.RunOutput.{*, given}
import besom.json.JsString

class CodegenTest extends munit.FunSuite:

  test("config - Map[String, PulumiAny]") {
    val configMap            = Map("provider:key" -> """{"a": "b"}""")
    val nonEmptyKeyConfigMap = configMap.map { (key, value) => NonEmptyString(key).get -> value }

    given Context = DummyContext(configMap = nonEmptyKeyConfigMap, configSecretKeys = Set()).unsafeRunSync()

    import CodegenProtocol.*

    Codegen.config[Map[String, PulumiAny]]("provider")("key", false, List(), None).unsafeRunSync() match
      case None                                    => fail("Expected config to be defined")
      case Some(None)                              => fail("Expected config to be defined")
      case Some(Some(map: Map[String, PulumiAny])) => assertEquals(map, Map("a" -> JsString("b")))
  }
