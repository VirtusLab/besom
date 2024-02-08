package besom.model

//noinspection ScalaFileName
class SemanticVersionTest extends munit.FunSuite:

  Map(
    "1.0.0" -> SemanticVersion(1, 0, 0),
    "v1.0.0" -> SemanticVersion(1, 0, 0),
    "1.0.0-alpha" -> SemanticVersion(1, 0, 0, "alpha"),
    "1.0.0-rc.1" -> SemanticVersion(1, 0, 0, "rc.1"),
    "0.0.1-SNAPSHOT" -> SemanticVersion(0, 0, 1, "SNAPSHOT"),
    "v0.0.1-SNAPSHOT" -> SemanticVersion(0, 0, 1, "SNAPSHOT"),
    "1.0.0-alpha+001" -> SemanticVersion(1, 0, 0, "alpha", "001"),
    "1.0.0+20130313144700" -> SemanticVersion(1, 0, 0, None, Some("20130313144700")),
    "1.0.0-beta+exp.sha.5114f85" -> SemanticVersion(1, 0, 0, "beta", "exp.sha.5114f85"),
    "1.0.0+21AF26D3----117B344092BD" -> SemanticVersion(1, 0, 0, None, Some("21AF26D3----117B344092BD"))
  ).foreachEntry((input, expected) =>
    test(s"parse $input") {
      assertEquals(SemanticVersion.parseTolerant(input), Right(expected))
    }
  )

  Map(
    SemanticVersion(0, 0, 1, "SNAPSHOT") -> "0.0.1-SNAPSHOT"
  ).foreachEntry((input, expected) =>
    test(s"format $input") {
      assertEquals(input.toString, expected)
    }
  )
  Map(
    SemanticVersion(0, 1, 1, "SNAPSHOT") -> "0.1.1-SNAPSHOT",
    SemanticVersion(1, 1, 0, "SNAPSHOT") -> "1.1-SNAPSHOT",
    SemanticVersion(1, 0, 0, "SNAPSHOT", "deadbeef") -> "1-SNAPSHOT+deadbeef"
  ).foreachEntry((input, expected) =>
    test(s"short format $input") {
      assertEquals(input.toShortString, expected)
    }
  )

  Map(
    "0.0.0" -> "0.0.1",
    "0.0.1" -> "0.1.0",
    "0.1.0" -> "0.1.1",
    "1.0.0" -> "2.0.0",
    "1.0.0-alpha" -> "1.0.0",
    "1.0.0-alpha" -> "1.0.0-alpha.1",
    "1.0.0-alpha.1" -> "1.0.0-alpha.beta",
    "1.0.0-alpha.beta" -> "1.0.0-beta",
    "1.0.0-beta" -> "1.0.0-beta.2",
    "1.0.0-beta.2" -> "1.0.0-beta.11",
    "1.0.0-beta.11" -> "1.0.0-rc.1",
    "1.0.0-rc.1" -> "1.0.0",
    "1.0.0-alpha+001" -> "1.0.0-alpha.1",
    "1.0.0-alpha.1" -> "1.0.0-alpha.beta"
  ).foreachEntry((left, right) =>
    test(s"$left < $right") {
      assertEquals(SemanticVersion.parse(left).toTry.get < SemanticVersion.parse(right).toTry.get, true)
    }
  )

  test("1.0.0 == 1.0.0") {
    assertEquals(SemanticVersion.parse("1.0.0").toTry.get == SemanticVersion.parse("1.0.0").toTry.get, true)
  }
end SemanticVersionTest
