package besom.util

import munit.*
import besom.json.JsonParser

class JsonInterpolatorTest extends FunSuite with CompileAssertions:

  test("json interpolator should compile with correct json strings") {
    // baseline
    compiles(
      """import besom.util.JsonInterpolator.given
         import besom.internal.DummyContext
         import besom.internal.RunResult.*
         given besom.internal.Context = DummyContext().unsafeRunSync()
      """ +
        code.`val x = json"""{"a": 1, "b": 2}"""`
    )

    compiles(
      """import besom.util.JsonInterpolator.given
         import besom.internal.DummyContext
         import besom.internal.RunResult.*
         given besom.internal.Context = DummyContext().unsafeRunSync()
         val i = 2
      """ +
        code.`val x = json"""{"a": 1, "b": $i}"""`
    )
  }

  test("json interpolator should catch invalid jsons") {
    failsToCompile(
      """import besom.util.JsonInterpolator.given
         import besom.internal.DummyContext
         import besom.internal.RunResult.*
         given besom.internal.Context = DummyContext().unsafeRunSync()
      """ +
        code.`val x = json"""{"a": 1, "b": 2"""`
    )

    failsToCompile(
      """import besom.util.JsonInterpolator.given
         import besom.internal.DummyContext
         import besom.internal.RunResult.*
         given besom.internal.Context = DummyContext().unsafeRunSync()
      """ +
        code.`val x = json"""{"a": 1, "b": 2,}"""`
    )

    failsToCompile(
      """import besom.util.JsonInterpolator.given
         import besom.internal.DummyContext
         import besom.internal.RunResult.*
         given besom.internal.Context = DummyContext().unsafeRunSync()
      """ +
        code.`val x = json"""{"a"": 1, "b": 2}"""`
    )
  }

  test("json interpolator should interpolate primitives into json strings correctly") {
    import besom.util.JsonInterpolator.given
    import besom.internal.DummyContext
    import besom.internal.RunResult.given
    import besom.internal.RunOutput.{*, given}
    import besom.internal.Output

    given besom.internal.Context = DummyContext().unsafeRunSync()

    val str        = "test"
    val int        = 1
    val long       = 5L
    val float      = Output(2.3f)
    val double     = 3.4d
    val bool       = true
    val jsonOutput = json"""{"a": 1, "b": "$str", "c": $int, "d": $long, "e": $float, "f": $double, "g": $bool}"""
    jsonOutput.unsafeRunSync() match
      case None => fail("expected a json output")
      case Some(json) =>
        assertEquals(json, JsonParser("""{"a": 1, "b": "test", "c": 1, "d": 5, "e": 2.3, "f": 3.4, "g": true}"""))
  }

  test("json interpolator should interpolate JsValues into json strings correctly") {
    import besom.json.*
    import besom.util.JsonInterpolator.given
    import besom.internal.DummyContext
    import besom.internal.RunResult.given
    import besom.internal.RunOutput.{*, given}

    given besom.internal.Context = DummyContext().unsafeRunSync()

    val jsonValue  = JsObject("a" -> JsNumber(1), "b" -> JsString("test"))
    val jsonOutput = json"""{"a": 1, "b": $jsonValue}"""
    jsonOutput.unsafeRunSync() match
      case None => fail("expected a json output")
      case Some(json) =>
        assertEquals(json, JsonParser("""{"a": 1, "b": {"a": 1, "b": "test"}}"""))
  }

  test("json interpolator fails to compile when interpolating values that can't be interpolated") {
    failsToCompile(
      """import besom.util.JsonInterpolator.given
         import besom.internal.DummyContext
         import besom.internal.RunResult.*
         given besom.internal.Context = DummyContext().unsafeRunSync()
      """ +
        code.`val x = json"""{"a": 1, "b": $this}"""`
    )

    failsToCompile(
      """import besom.util.JsonInterpolator.given
         import besom.internal.DummyContext
         import besom.internal.RunResult.*
         given besom.internal.Context = DummyContext().unsafeRunSync()
      """ +
        code.`val x = json"""{"a": 1, "b": $this}"""`
    )
  }

  test("json interpolator sanitizes strings when interpolating") {
    import besom.json.*
    import besom.util.JsonInterpolator.given
    import besom.internal.DummyContext
    import besom.internal.RunResult.given
    import besom.internal.RunOutput.{*, given}

    given besom.internal.Context = DummyContext().unsafeRunSync()

    val badString = """well well well", "this is onixpected": "23"""
    json"""{"a": 1, "b": "$badString"}""".unsafeRunSync() match
      case None => fail("expected a json output")
      case Some(json) =>
        assertEquals(json, JsonParser("""{"a": 1, "b": "well well well\", \"this is onixpected\": \"23"}"""))
  }

end JsonInterpolatorTest
