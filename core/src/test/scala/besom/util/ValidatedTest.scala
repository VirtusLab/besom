package besom.util

class ValidatedTest extends munit.FunSuite:
  test("validated zip should aggregate errors") {
    val a = Validated.valid("a")
    val b = Validated.invalid("b")
    val c = Validated.invalid("c")

    val result = a.zip(b).zip(c)

    assertEquals(result, Validated.invalid("b", "c"))
  }

  test("validated map should apply function to valid value only") {
    val a = Validated.valid("a")

    val result = a.map(_.toUpperCase)

    assertEquals(result, Validated.valid("A"))

    val b: Validated[String, String] = Validated.invalid("error")

    val result2 = b.map(_.toUpperCase)

    assertEquals(result2, Validated.invalid("error"))
  }

  test("validated flatMap should apply function to valid value only") {
    val a = Validated.valid("a")

    val result = a.flatMap(v => Validated.valid(v.toUpperCase))

    assertEquals(result, Validated.valid("A"))

    val b: Validated[String, String] = Validated.invalid("error")

    val result2 = b.flatMap(v => Validated.valid(v.toUpperCase))

    assertEquals(result2, Validated.invalid("error"))
  }

  test("validated filterOrError should return valid value if predicate is true") {
    val a = Validated.valid("a")

    val result = a.filterOrError(_ == "a")("error")

    assertEquals(result, Validated.valid("a"))

    val b = Validated.invalid("error")

    val result2 = b.filterOrError(_ == "a")("error2")

    assertEquals(result2, Validated.invalid("error2"))
  }

  test("validated getOrElse should return valid value if valid") {
    val a = Validated.valid("a")

    val result = a.getOrElse("error")

    assertEquals(result, "a")

    val b = Validated.invalid("error")

    val result2 = b.getOrElse("error2")

    assertEquals(result2, "error2")
  }

  test("validated orElse should return valid value if valid") {
    val a = Validated.valid("a")

    val result = a.orElse(Validated.valid("b"))

    assertEquals(result, Validated.valid("a"))

    val b = Validated.invalid("error")

    val result2 = b.orElse(Validated.valid("b"))

    assertEquals(result2, Validated.valid("b"))
  }

  test("validated lmap should apply function to error value only") {
    val a: Validated[String, String] = Validated.valid("a")

    val result = a.lmap(_.toUpperCase)

    assertEquals(result, Validated.valid("a"))

    val b = Validated.invalid("error")

    val result2 = b.lmap(_.toUpperCase)

    assertEquals(result2, Validated.invalid("ERROR"))
  }

  import besom.internal.RunResult.{given, *}
  import Validated.*, ValidatedResult.*

  test("validated result zip should aggregate errors") {
    val a = ValidatedResult.valid("a")
    val b = ValidatedResult.invalid("b")
    val c = ValidatedResult.invalid("c")

    val result = a.zip(b).zip(c)

    assertEquals(result.unwrap.unsafeRunSync(), ValidatedResult.invalid("b", "c").unwrap.unsafeRunSync())
  }

  test("validated result map should apply function to valid value only") {
    val a = ValidatedResult.valid("a")

    val result = a.map(_.toUpperCase)

    assertEquals(result.unwrap.unsafeRunSync(), ValidatedResult.valid("A").unwrap.unsafeRunSync())

    val b: ValidatedResult[String, String] = ValidatedResult.invalid("error")

    val result2 = b.map(_.toUpperCase)

    assertEquals(result2.unwrap.unsafeRunSync(), ValidatedResult.invalid("error").unwrap.unsafeRunSync())
  }

  test("validated result flatMap should apply function to valid value only") {
    val a = ValidatedResult.valid("a")

    val result = a.flatMap(v => ValidatedResult.valid(v.toUpperCase))

    assertEquals(result.unwrap.unsafeRunSync(), ValidatedResult.valid("A").unwrap.unsafeRunSync())

    val b: ValidatedResult[String, String] = ValidatedResult.invalid("error")

    val result2 = b.flatMap(v => ValidatedResult.valid(v.toUpperCase))

    assertEquals(result2.unwrap.unsafeRunSync(), ValidatedResult.invalid("error").unwrap.unsafeRunSync())
  }

  test("validated result filterOrError should return valid value if predicate is true") {
    val a = ValidatedResult.valid("a")

    val result = a.filterOrError(_ == "a")("error")

    assertEquals(result.unwrap.unsafeRunSync(), ValidatedResult.valid("a").unwrap.unsafeRunSync())

    val b = ValidatedResult.invalid("error")

    val result2 = b.filterOrError(_ == "a")("error2")

    assertEquals(result2.unwrap.unsafeRunSync(), ValidatedResult.invalid("error2").unwrap.unsafeRunSync())
  }

  test("validated result getOrElse should return valid value if valid") {
    val a = ValidatedResult.valid("a")

    val result = a.getOrElse("error")

    assertEquals(result.unsafeRunSync(), "a")

    val b = ValidatedResult.invalid("error")

    val result2 = b.getOrElse("error2")

    assertEquals(result2.unsafeRunSync(), "error2")
  }

  test("validated result orElse should return valid value if valid") {
    val a = ValidatedResult.valid("a")

    val result = a.orElse(ValidatedResult.valid("b"))

    assertEquals(result.unwrap.unsafeRunSync(), ValidatedResult.valid("a").unwrap.unsafeRunSync())

    val b = ValidatedResult.invalid("error")

    val result2 = b.orElse(ValidatedResult.valid("b"))

    assertEquals(result2.unwrap.unsafeRunSync(), ValidatedResult.valid("b").unwrap.unsafeRunSync())
  }

  test("validated result lmap should apply function to error value only") {
    val a: ValidatedResult[String, String] = ValidatedResult.valid("a")

    val result = a.lmap(_.toUpperCase)

    assertEquals(result.unwrap.unsafeRunSync(), ValidatedResult.valid("a").unwrap.unsafeRunSync())

    val b = ValidatedResult.invalid("error")

    val result2 = b.lmap(_.toUpperCase)

    assertEquals(result2.unwrap.unsafeRunSync(), ValidatedResult.invalid("ERROR").unwrap.unsafeRunSync())
  }

end ValidatedTest
