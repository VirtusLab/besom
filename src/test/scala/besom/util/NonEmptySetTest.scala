package besom.util

class NonEmptySetTest extends munit.FunSuite with CompileAssertions:

  test("fails on assignment of empty set") {
    failsToCompile("""
      import besom.util.NonEmptySet.*
      val x: NonEmptySet[Nothing] = Set()
      """)
  }

  test("fails on assignment of non-empty set") {
    failsToCompile("""
      import besom.util.NonEmptySet.*
      val x: NonEmptySet[Int] = Set(1, 2, 3)
      """)
  }

  test("smart constructor allows creation of NonEmptySet from values") {
    compiles("""
      import besom.util.NonEmptySet.*
      val x: NonEmptySet[Int] = NonEmptySet(1, 2, 3)
      """)
  }

  test("NonEmptySet can be used in place of normal set") {
    compiles("""
    import besom.util.NonEmptySet.*
    def test(param: Set[Int]) = s"Got: $param"
    NonEmptySet(Set(1, 2, 3)).foreach(test)
    val maybeSet: Option[Set[Int]] = NonEmptySet(Set(1, 2, 3))
    """)
  }

  test("NonEmptySet can be created via apply") {
    assert(NonEmptySet(1, 2, 3) == Set(1, 2, 3))
  }
