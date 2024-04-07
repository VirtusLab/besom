package besom.util

import munit.*
import besom.json.JsonParser
import scala.annotation.unused

class Outer[A](@unused a: A)

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

    val str        = "\"test"
    val int        = 1
    val long       = 5L
    val float      = Output(2.3f)
    val double     = 3.4d
    val bool       = true
    val jsonOutput = json"""{"a": 1, "b": $str, "c": $int, "d": $long, "e": $float, "f": $double, "g": $bool}"""
    jsonOutput.unsafeRunSync() match
      case None => fail("expected a json output")
      case Some(json) =>
        assertEquals(json, JsonParser("""{"a": 1, "b": "\"test", "c": 1, "d": 5, "e": 2.3, "f": 3.4, "g": true}"""))
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

  test("json interpolator can interpolate wrapped values correctly") {
    import besom.json.*
    import besom.util.JsonInterpolator.given
    import besom.internal.DummyContext
    import besom.internal.RunResult.given
    import besom.internal.RunOutput.{*, given}
    import besom.internal.Output

    given besom.internal.Context = DummyContext().unsafeRunSync()

    val str         = Option(Some("test"))
    val int         = Output(Option(1))
    val long        = Some(Output(5L))
    val float       = Output(Some(2.3f))
    val double      = Option(Output(3.4d))
    val bool        = Output(Output(true))
    val nullValue   = None
    val anotherNull = Some(None)
    val literalNull = null: String

    val jsonOutput =
      json"""{"a": 1, "b": $str, "c": $int, "d": $long, "e": $float, "f": $double, "g": $bool, "h": $nullValue, "i": $anotherNull, "j": $literalNull}"""

    jsonOutput.unsafeRunSync() match
      case None => fail("expected a json output")
      case Some(json) =>
        assertEquals(
          json,
          JsonParser("""{"a": 1, "b": "test", "c": 1, "d": 5, "e": 2.3, "f": 3.4, "g": true, "h": null, "i": null, "j": null}""")
        )
  }

  test("json interpolator fails to compile when interpolating values that can't be interpolated with nice error messages") {
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

    val errsWrappedOutput = scala.compiletime.testing.typeCheckErrors(
      """import besom.util.JsonInterpolator.given
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()
         
         def x = Output(java.time.Instant.now())
      """ +
        code.`val json = json"""{"a": 1, "b": $x}"""`
    )

    val expectedErrorWrappedOutput =
      """Value of type `besom.internal.Output[java.time.Instant]` is not a valid JSON interpolation type because of type `java.time.Instant`.
        |
        |Types available for interpolation are: String, Int, Short, Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.
        |If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.
        |""".stripMargin

    assert(errsWrappedOutput.size == 1, s"expected 1 errors, got ${errsWrappedOutput.size}")
    assertEquals(errsWrappedOutput.head.message, expectedErrorWrappedOutput)

    val errsWrappedOption = scala.compiletime.testing.typeCheckErrors(
      """import besom.util.JsonInterpolator.given
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()
         
         val x = Option(java.time.Instant.now())
      """ +
        code.`val json = json"""{"a": 1, "b": $x}"""`
    )

    val expectedErrorWrappedOption =
      """Value of type `scala.Option[java.time.Instant]` is not a valid JSON interpolation type because of type `java.time.Instant`.
        |
        |Types available for interpolation are: String, Int, Short, Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.
        |If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.
        |""".stripMargin

    assert(errsWrappedOption.size == 1, s"expected 1 errors, got ${errsWrappedOption.size}")
    assertEquals(errsWrappedOption.head.message, expectedErrorWrappedOption)

    val errsWrappedOutputOption = scala.compiletime.testing.typeCheckErrors(
      """import besom.util.JsonInterpolator.given
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()
         
         val x = Output(Option(java.time.Instant.now()))
      """ +
        code.`val json = json"""{"a": 1, "b": $x}"""`
    )

    val expectedErrorWrappedOutputOption =
      """Value of type `besom.internal.Output[scala.Option[java.time.Instant]]` is not a valid JSON interpolation type because of type `java.time.Instant`.
        |
        |Types available for interpolation are: String, Int, Short, Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.
        |If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.
        |""".stripMargin

    assert(errsWrappedOutputOption.size == 1, s"expected 1 errors, got ${errsWrappedOutputOption.size}")
    assertEquals(errsWrappedOutputOption.head.message, expectedErrorWrappedOutputOption)

    val errsWrappedOptionOutput = scala.compiletime.testing.typeCheckErrors(
      """import besom.util.JsonInterpolator.given
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()
         
         val x = Option(Output(java.time.Instant.now()))
      """ +
        code.`val json = json"""{"a": 1, "b": $x}"""`
    )

    val expectedErrorWrappedOptionOutput =
      """Value of type `scala.Option[besom.internal.Output[java.time.Instant]]` is not a valid JSON interpolation type because of type `java.time.Instant`.
        |
        |Types available for interpolation are: String, Int, Short, Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.
        |If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.
        |""".stripMargin

    assert(errsWrappedOptionOutput.size == 1, s"expected 1 errors, got ${errsWrappedOptionOutput.size}")
    assertEquals(errsWrappedOptionOutput.head.message, expectedErrorWrappedOptionOutput)

    val errsWrappedOutputOutput = scala.compiletime.testing.typeCheckErrors(
      """import besom.util.JsonInterpolator.given
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()
         
         val x = Output(Output(java.time.Instant.now()))
      """ +
        code.`val json = json"""{"a": 1, "b": $x}"""`
    )

    val expectedErrorWrappedOutputOutput =
      """Value of type `besom.internal.Output[besom.internal.Output[java.time.Instant]]` is not a valid JSON interpolation type because of type `java.time.Instant`.
        |
        |Types available for interpolation are: String, Int, Short, Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.
        |If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.
        |""".stripMargin

    assert(errsWrappedOutputOutput.size == 1, s"expected 1 errors, got ${errsWrappedOutputOutput.size}")
    assertEquals(errsWrappedOutputOutput.head.message, expectedErrorWrappedOutputOutput)

    val errsWrappedOptionOption = scala.compiletime.testing.typeCheckErrors(
      """import besom.util.JsonInterpolator.given
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()
         
         val x = Option(Option(java.time.Instant.now()))
      """ +
        code.`val json = json"""{"a": 1, "b": $x}"""`
    )

    val expectedErrorWrappedOptionOption =
      """Value of type `scala.Option[scala.Option[java.time.Instant]]` is not a valid JSON interpolation type because of type `java.time.Instant`.
        |
        |Types available for interpolation are: String, Int, Short, Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.
        |If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.
        |""".stripMargin

    assert(errsWrappedOptionOption.size == 1, s"expected 1 errors, got ${errsWrappedOptionOption.size}")
    assertEquals(errsWrappedOptionOption.head.message, expectedErrorWrappedOptionOption)

    val errsWrappedOuter = scala.compiletime.testing.typeCheckErrors(
      """import besom.util.JsonInterpolator.given
         import besom.internal.{Output, DummyContext}
         import besom.internal.RunOutput.{*, given}
         given besom.internal.Context = DummyContext().unsafeRunSync()
         
         val x = new Outer(1)
      """ +
        code.`val json = json"""{"a": 1, "b": $x}"""`
    )

    val expectedErrorWrappedOuter =
      """Value of type `x: besom.util.Outer` is not a valid JSON interpolation type.
        |
        |Types available for interpolation are: String, Int, Short, Long, Float, Double, Boolean, JsValue and Options and Outputs containing these types.
        |If you want to interpolate a custom data type - derive or implement a JsonFormat for it and convert it to JsValue.
        |""".stripMargin

    assert(errsWrappedOuter.size == 1, s"expected 1 errors, got ${errsWrappedOuter.size}")
    assertEquals(errsWrappedOuter.head.message, expectedErrorWrappedOuter)
  }

  test("json interpolator sanitizes strings when interpolating") {
    import besom.json.*
    import besom.util.JsonInterpolator.given
    import besom.internal.DummyContext
    import besom.internal.RunResult.given
    import besom.internal.RunOutput.{*, given}

    given besom.internal.Context = DummyContext().unsafeRunSync()

    val badString = """well well well", "this is onixpected": "23"""
    json"""{"a": 1, "b": $badString}""".unsafeRunSync() match
      case None => fail("expected a json output")
      case Some(json) =>
        assertEquals(json, JsonParser("""{"a": 1, "b": "well well well\", \"this is onixpected\": \"23"}"""))
  }

end JsonInterpolatorTest
