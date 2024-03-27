package besom.util

import besom.types.ResourceType

class ResourceTypeTest extends munit.FunSuite with CompileAssertions:

  test("fails on assignment of empty string") {
    failsToCompile("""
      import besom.types.ResourceType.*
      val x: ResourceType = ""
      """)
  }

  test("compiles on assignment of valid string") {
    compiles("""
        import besom.types.ResourceType
        import besom.types.ResourceType.*
        val x: ResourceType = "pkg:mod:valid"
        """)
  }

  test("fails on assignment of invalid string") {
    failsToCompile("""
        import besom.types.ResourceType.*
        val x: ResourceType = "invalid"
        """)
  }

  test("fails to compile when creating via smart constructor with empty string") {
    failsToCompile("""
      import besom.types.ResourceType.*
      val x: ResourceType = ResourceType.from("")
      """)
  }

  test("fails to compile when creating via smart constructor with invalid string") {
    failsToCompile("""
      import besom.types.ResourceType.*
      val x: ResourceType = ResourceType.from("invalid")
      """)
  }

  test("fails to compile when creating via smart constructor with blank string") {
    failsToCompile("""
      import besom.types.ResourceType.*
      val x: ResourceType = ResourceType.from("  ")
      """)
  }

  test("fails to compile when calling smart constructor with a dynamic string") {
    failsToCompile("""
      import besom.types.ResourceType.*
      def getStr: String = "abc"
      val z = getStr
      val x: ResourceType = ResourceType.from(z)
      """)
  }

  test("can be used in place of normal string") {
    compiles("""
      import besom.types.ResourceType
      import besom.types.ResourceType.*
      def test(param: String) = s"Got: $param"
      test(ResourceType.from("pkg:mod:valid"))
      val str: String = ResourceType.from("pkg:mod:valid")
      """)
  }

  test("can be created via apply") {
    ResourceType("pkg:mod:valid") match
      case None        => fail("apply doesn't work")
      case Some(valid) => assert(valid == "pkg:mod:valid")

    ResourceType("invalid") match
      case None    => ()
      case Some(_) => fail("apply doesn't work")
  }

  test("can be created via from") {
    import besom.internal.DummyContext
    import besom.internal.RunResult.{*, given}
    import besom.internal.Context
    import besom.*

    given Context = DummyContext().unsafeRunSync()

    val pkg                     = "pkg"
    val mod                     = "mod"
    val name                    = "valid"
    val o: Output[ResourceType] = p"$pkg:$mod:$name".flatMap(ResourceType.parseOutput(_))
    o
  }
end ResourceTypeTest
