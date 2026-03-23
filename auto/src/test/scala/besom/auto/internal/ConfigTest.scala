package besom.auto.internal

import besom.json.*

class ConfigTest extends munit.FunSuite:

  test("ConfigValue plain string JSON round-trip") {
    val cv       = ConfigValue("hello")
    val json     = cv.toJson
    val expected = JsObject("value" -> JsString("hello"), "secret" -> JsFalse)
    assertEquals(json, expected)

    val parsed = json.convertTo[ConfigValue]
    assertEquals(parsed.value, "hello")
    assertEquals(parsed.secret, false)
    assertEquals(parsed.objectValue, None)
  }

  test("ConfigValue secret string JSON round-trip") {
    val cv       = ConfigValue("secret-val", secret = true)
    val json     = cv.toJson
    val expected = JsObject("value" -> JsString("secret-val"), "secret" -> JsTrue)
    assertEquals(json, expected)

    val parsed = json.convertTo[ConfigValue]
    assertEquals(parsed.value, "secret-val")
    assertEquals(parsed.secret, true)
    assertEquals(parsed.objectValue, None)
  }

  test("ConfigValue.structured serializes with JSON value, not string-escaped") {
    val jsVal = JsArray(JsObject("name" -> JsString("node-1"), "type" -> JsString("cx22")))
    val cv    = ConfigValue.structured(jsVal)
    val json  = cv.toJson

    // The "value" field should be the actual JSON array, not a string containing JSON
    val fields = json.asJsObject.fields
    assert(fields("value").isInstanceOf[JsArray], s"Expected JsArray for value, got: ${fields("value")}")
    assertEquals(fields("secret"), JsFalse)
  }

  test("ConfigValue.structured secret serializes correctly") {
    val jsVal = JsObject("key" -> JsString("val"))
    val cv    = ConfigValue.structured(jsVal, secret = true)
    val json  = cv.toJson

    val fields = json.asJsObject.fields
    assert(fields("value").isInstanceOf[JsObject], s"Expected JsObject for value, got: ${fields("value")}")
    assertEquals(fields("secret"), JsTrue)
  }

  test("ConfigValue deserialization handles non-string value as structured") {
    val json = JsObject(
      "value" -> JsArray(JsObject("name" -> JsString("a"))),
      "secret" -> JsFalse
    )
    val cv = json.convertTo[ConfigValue]
    assertEquals(cv.secret, false)
    assert(cv.objectValue.isDefined, "Expected objectValue to be Some")
    assert(cv.objectValue.get.isInstanceOf[JsArray])
    // value field should hold the compact-printed JSON string
    assertEquals(cv.value, cv.objectValue.get.compactPrint)
  }

  test("ConfigValue deserialization handles missing value field") {
    val json = JsObject("secret" -> JsTrue)
    val cv   = json.convertTo[ConfigValue]
    assertEquals(cv.value, "")
    assertEquals(cv.secret, true)
    assertEquals(cv.objectValue, None)
  }

  test("ConfigMap JSON serialization with mixed plain and structured values") {
    val structured = ConfigValue.structured(JsArray(JsObject("name" -> JsString("node-1"))))
    val plain      = ConfigValue("hello")
    val secret     = ConfigValue("s3cret", secret = true)

    val configMap: ConfigMap = Map(
      "proj:servers" -> structured,
      "proj:greeting" -> plain,
      "proj:password" -> secret
    )

    val json   = configMap.toJson
    val fields = json.asJsObject.fields

    // structured value should have array as value
    val serversFields = fields("proj:servers").asJsObject.fields
    assert(serversFields("value").isInstanceOf[JsArray])
    assertEquals(serversFields("secret"), JsFalse)

    // plain value should have string as value
    val greetingFields = fields("proj:greeting").asJsObject.fields
    assertEquals(greetingFields("value"), JsString("hello"))
    assertEquals(greetingFields("secret"), JsFalse)

    // secret value should have secret=true
    val passwordFields = fields("proj:password").asJsObject.fields
    assertEquals(passwordFields("value"), JsString("s3cret"))
    assertEquals(passwordFields("secret"), JsTrue)
  }

  test("ConfigMap JSON round-trip preserves structured values") {
    val structured = ConfigValue.structured(JsArray(JsObject("name" -> JsString("node-1"))))
    val plain      = ConfigValue("hello")

    val configMap: ConfigMap = Map(
      "proj:servers" -> structured,
      "proj:greeting" -> plain
    )

    val json        = configMap.toJson.compactPrint
    val parsed      = json.parseJson[ConfigMap]
    val parsedRight = parsed.fold(e => fail(s"Failed to parse: ${e.getMessage}"), identity)

    // Plain value round-trips
    assertEquals(parsedRight("proj:greeting").value, "hello")
    assertEquals(parsedRight("proj:greeting").secret, false)
    assertEquals(parsedRight("proj:greeting").objectValue, None)

    // Structured value round-trips
    assertEquals(parsedRight("proj:servers").secret, false)
    assert(parsedRight("proj:servers").objectValue.isDefined)
    assert(parsedRight("proj:servers").objectValue.get.isInstanceOf[JsArray])
  }

  test("ConfigOption.Json and ConfigOption.Path are distinct enum values") {
    assert(ConfigOption.Json != ConfigOption.Path)
    assert(Seq(ConfigOption.Json, ConfigOption.Path).contains(ConfigOption.Json))
    assert(Seq(ConfigOption.Json, ConfigOption.Path).contains(ConfigOption.Path))
  }

end ConfigTest
