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

  test("fails to compile when creating via smart constructor with blank string") {
    failsToCompile("""
      import besom.util.NonEmptyString.*
      val x: NonEmptyString = NonEmptyString.from("  ")
      """)
  }

  test("fails to compile when calling smart constructor with a dynamic string") {
    failsToCompile("""
      import besom.util.NonEmptyString.*
      def getStr: String = "abc"
      val z = getStr
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

  test("compiles with non-empty string multiplicated if the multiplier is constant and > 0") {
    compiles("""
      import besom.util.NonEmptyString.*
      val z = "x" * 10
      val x: NonEmptyString = NonEmptyString.from(z)
      """)

    failsToCompile("""
      import besom.util.NonEmptyString.*
      val z = "x" * 0
      val x: NonEmptyString = NonEmptyString.from(z)
      """)

    failsToCompile("""
      import besom.util.NonEmptyString.*
      val z = "x" * -1
      val x: NonEmptyString = NonEmptyString.from(z)
      """)

    failsToCompile("""
      import besom.util.NonEmptyString.*
      val times = 10
      val z = "x" * times
      val x: NonEmptyString = NonEmptyString.from(z)
      """)

    compiles("""
      import besom.util.NonEmptyString.*
      def takesNES(nes: NonEmptyString): Unit = ()
      takesNES("x" * 10)
      """)

    failsToCompile("""
      import besom.util.NonEmptyString.*
      def takesNES(nes: NonEmptyString): Unit = ()
      val times = 10
      takesNES("x" * times)
      """)

    failsToCompile("""
      import besom.util.NonEmptyString.*
      def takesNES(nes: NonEmptyString): Unit = ()
      val times = 10
      val z = "x" * times
      takesNES(z)
      """)

    failsToCompile("""
      import besom.util.NonEmptyString.*
      def takesNES(nes: NonEmptyString): Unit = ()
      val times = 10
      val z = "x" * times
      takesNES(NonEmptyString.from(z))
      """)
  }

  test("NonEmptyString can be created from string interpolation with non-empty parts") {
    compiles("""
    import besom.util.NonEmptyString.*
    val x: NonEmptyString = s"abc${"def"}ghi"
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    def takesNES(nes: NonEmptyString): Unit = ()
    takesNES(s"abc${"def"}ghi")
    """)
  }

  test("NonEmptyString can't be created from string interpolation with empty parts") {
    failsToCompile("""
    import besom.util.NonEmptyString.*
    val x: NonEmptyString = s"${"whatever, this won't work"}"
    """)
  }

  test("NonEmptyString can infer non empty strings from constant string definitions") {
    compiles("""
    import besom.util.NonEmptyString.*
    val x = "abc"
    val y: NonEmptyString = x
    """)

    failsToCompile("""
    import besom.util.NonEmptyString.*
    val x = ""
    val y: NonEmptyString = x
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    def takesNES(nes: NonEmptyString): Unit = ()
    takesNES("abc")
    """)

    failsToCompile("""
    import besom.util.NonEmptyString.*
    def takesNES(nes: NonEmptyString): Unit = ()
    takesNES("")
    """)
  }

  test("NonEmptyString can be created from string concatenation") {
    compiles("""
    import besom.util.NonEmptyString.*
    val x: NonEmptyString = "abc" + "def" + ""
    """)

    failsToCompile("""
    import besom.util.NonEmptyString.*
    val x: NonEmptyString = "" + "" + ""
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    def takesNES(nes: NonEmptyString): Unit = ()
    takesNES("abc" + "def" + "")
    """)

    failsToCompile("""
    import besom.util.NonEmptyString.*
    def takesNES(nes: NonEmptyString): Unit = ()
    takesNES("" + "" + "")
    """)
  }

  test("NonEmptyString can be created from string concatenation at definition site") {
    compiles("""
    import besom.util.NonEmptyString.*
    val s: String = "abc" + "def"
    val x: NonEmptyString = s
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    val s: String = "abc" + ""
    val x: NonEmptyString = s
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    val s: String = "abc"
    val s2: String = s + "def"
    val x: NonEmptyString = s2
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    val s: String = "def"
    val s2: String = "abc" + s
    val x: NonEmptyString = s2
    """)

    failsToCompile("""
    import besom.util.NonEmptyString.*
    val s: String = "" + ""
    val x: NonEmptyString = s
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    def takesNES(nes: NonEmptyString): Unit = ()
    val s: String = "abc" + "def"
    takesNES(s)
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    def takesNES(nes: NonEmptyString): Unit = ()
    val s: String = "abc" + ""
    takesNES(s)
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    def takesNES(nes: NonEmptyString): Unit = ()
    val s: String = "abc"
    val s2: String = s + "def"
    takesNES(s2)
    """)
  }

  test("NonEmptyString can be inferred from remote declarations") {
    compiles("""
    import besom.util.NonEmptyString.*
    object example:
      val x: String = "abc"
    val y: NonEmptyString = example.x
    """)

    failsToCompile("""
    import besom.util.NonEmptyString.*
      object example:
        val x: String = ""
      val y: NonEmptyString = example.x
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    object example:
      val x: String = "abc"
    def takesNES(nes: NonEmptyString): Unit = ()
    takesNES(example.x)
    """)

    failsToCompile("""
    import besom.util.NonEmptyString.*
    object example:
      val x: String = ""
    def takesNES(nes: NonEmptyString): Unit = ()
    takesNES(example.x)
    """)
  }

  test("issue #138 - concatenation with dynamic string") {
    compiles("""
    import besom.util.NonEmptyString.*
    val api = "v1.2.3"
    val z = "enable-" + api.replace(".", "-")
    
    val x: NonEmptyString = z
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    val api = "v1.2.3"
    val x: NonEmptyString = "enable-" + api.replace(".", "-")
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    val api = "v1.2.3"
    val z = "enable-" + api.replace(".", "-")
    def takesNES(nes: NonEmptyString): Unit = ()
    takesNES(z)
    """)

    compiles("""
    import besom.util.NonEmptyString.*
    val api = "v1.2.3"
    def takesNES(nes: NonEmptyString): Unit = ()
    takesNES("enable-" + api.replace(".", "-"))
    """)
  }

  test("NonEmptyString can be created via apply") {
    NonEmptyString("sample") match
      case None              => fail("apply doesn't work")
      case Some(nonEmptyStr) => assert(nonEmptyStr == "sample")

    NonEmptyString("") match
      case None    => ()
      case Some(_) => fail("apply doesn't work")
  }
end NonEmptyStringTest
