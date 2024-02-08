package besom.internal

import besom.*
import besom.internal.RunResult.{*, given}

class CodegenTest extends munit.FunSuite:

  test("config - Map[String, PulumiAny]") {
    val configMap            = Map("provider:key" -> """{"a": "b"}""")
    val nonEmptyKeyConfigMap = configMap.map { (key, value) => NonEmptyString(key).get -> value }

    given Context = DummyContext(configMap = nonEmptyKeyConfigMap, configSecretKeys = Set()).unsafeRunSync()

    import CodegenProtocol.*
    Codegen.config[Map[String, PulumiAny]]("provider")("key", false, List(), None)
  }
