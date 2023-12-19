package besom.util

class NotProvidedTest extends munit.FunSuite with CompileAssertions:
  test("asOption works as expected") {
    val a: NotProvidedOr[String] = NotProvided
    val b: NotProvidedOr[String] = "hello"

    assertEquals(a.asOption, None)
    assertEquals(b.asOption, Some("hello"))
  }

  test("asOption does not infect every type") {
    failsToCompile("""
        import besom.util.*
        val x: String = ""
        x.asOption
        """)
  }
