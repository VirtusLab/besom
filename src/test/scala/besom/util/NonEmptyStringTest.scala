package besom.util

class NonEmptyStringTest extends munit.FunSuite with CompileAssertions:

  test("fails on assignment of empty string") {
    failsToCompile("""
      import besom.util.NonEmptyString.*
      val x: NonEmptyString = ""
      """)
  }

  test("compiles on assignment of non-empty string") {
    compiles("""
      import besom.util.NonEmptyString.*
      val x: NonEmptyString = "abc"
      """)
  }

  test("compiles when creating via smart constructor") {
    compiles("""
      import besom.util.NonEmptyString.*
      val x: NonEmptyString = NonEmptyString.from("abc")
      """)
  }

  test("fails to compile when creating via smart constructor with empty string") {
    failsToCompile("""
      import besom.util.NonEmptyString.*
      val x: NonEmptyString = NonEmptyString.from("")
      """)
  }

  test("fails to compile when calling smart constructor with a dynamic string") {
    failsToCompile("""
      import besom.util.NonEmptyString.*
      val z = "x" * 10
      val x: NonEmptyString = NonEmptyString.from(z)
      """)
  }

  test("NonEmptyString can be used in place of normal string") {
    compiles("""
    import besom.util.NonEmptyString.*
    def test(param: String) = s"Got: $param"
    test(NonEmptyString.from("a random string"))
    val str: String = NonEmptyString.from("a random string")
    """)
  }

  test("NonEmptyString can be created via apply") {
    NonEmptyString("sample") match
      case None              => fail("apply doesn't work")
      case Some(nonEmptyStr) => assert(nonEmptyStr == "sample")
  }
