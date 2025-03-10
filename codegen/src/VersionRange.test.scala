package besom.codegen

import besom.model.SemanticVersion

class VersionRangeTests extends munit.FunSuite:
  test("SingleVersion matches exact version") {
    val range = SingleVersion(SemanticVersion(1, 0, 0))
    assert(range.matches(SemanticVersion(1, 0, 0)))
    assert(!range.matches(SemanticVersion(1, 0, 1)))
  }

  test("ExactRange matches versions within range") {
    val range = ExactRange(SemanticVersion(1, 0, 0), SemanticVersion(1, 0, 2))
    assert(range.matches(SemanticVersion(1, 0, 0)))
    assert(range.matches(SemanticVersion(1, 0, 1)))
    assert(range.matches(SemanticVersion(1, 0, 2)))
    assert(!range.matches(SemanticVersion(1, 0, 3)))
  }

  test("WildcardVersion matches versions with wildcards") {
    val range = WildcardVersion(1, None, None) // 1.x.x
    assert(range.matches(SemanticVersion(1, 0, 0)))
    assert(range.matches(SemanticVersion(1, 1, 0)))
    assert(range.matches(SemanticVersion(1, 2, 3)))
    assert(!range.matches(SemanticVersion(2, 0, 0)))

    val rangeWithMinor = WildcardVersion(1, Some(2), None) // 1.2.x
    assert(rangeWithMinor.matches(SemanticVersion(1, 2, 0)))
    assert(rangeWithMinor.matches(SemanticVersion(1, 2, 1)))
    assert(!rangeWithMinor.matches(SemanticVersion(1, 3, 0)))
  }

  test("VersionRange.parse parses single version") {
    val result = VersionRange.parse("1.0.0")
    assert(result.isRight)
    result.foreach {
      case SingleVersion(version) =>
        assertEquals(version, SemanticVersion(1, 0, 0))
      case _ => fail("Expected SingleVersion")
    }
  }

  test("VersionRange.parse parses version range") {
    val result = VersionRange.parse("1.0.0:1.0.2")
    assert(result.isRight)
    result.foreach {
      case ExactRange(from, to) =>
        assertEquals(from, SemanticVersion(1, 0, 0))
        assertEquals(to, SemanticVersion(1, 0, 2))
      case _ => fail("Expected ExactRange")
    }
    result.fold(
      e => fail(s"Failed to parse version range: $e"),
      range => {
        assert(range.matches(SemanticVersion(1, 0, 1)))
        assert(!range.matches(SemanticVersion(1, 0, 3)))
        assert(!range.matches(SemanticVersion(2, 0, 0)))
        assert(!range.matches(SemanticVersion(1, 1, 0)))
        assert(range.matches(SemanticVersion(1, 0, 0)))
        assert(range.matches(SemanticVersion(1, 0, 2)))
      }
    )
  }

  test("VersionRange.parse parses wildcard version") {
    val result = VersionRange.parse("1.x.x")
    assert(result.isRight)
    result.foreach {
      case WildcardVersion(major, minor, patch) =>
        assertEquals(major, 1)
        assertEquals(minor, None)
        assertEquals(patch, None)
      case _ => fail("Expected WildcardVersion")
    }

    result.fold(
      e => fail(s"Failed to parse version range: $e"),
      range => {
        assert(range.matches(SemanticVersion(1, 0, 0)))
        assert(range.matches(SemanticVersion(1, 1, 0)))
        assert(range.matches(SemanticVersion(1, 2, 3)))
        assert(!range.matches(SemanticVersion(2, 0, 0)))
        assert(!range.matches(SemanticVersion(0, 0, 1)))
      }
    )

    val result2 = VersionRange.parse("1.2.x")
    assert(result2.isRight)
    result2.foreach {
      case WildcardVersion(major, minor, patch) =>
        assertEquals(major, 1)
        assertEquals(minor, Some(2))
        assertEquals(patch, None)
      case _ => fail("Expected WildcardVersion")
    }
    result2.fold(
      e => fail(s"Failed to parse version range: $e"),
      range => {
        assert(range.matches(SemanticVersion(1, 2, 0)))
        assert(range.matches(SemanticVersion(1, 2, 1)))
        assert(!range.matches(SemanticVersion(1, 3, 0)))
        assert(!range.matches(SemanticVersion(2, 0, 0)))
        assert(!range.matches(SemanticVersion(0, 0, 1)))
      }
    )
  }
end VersionRangeTests
