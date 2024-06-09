package besom.scripts

//noinspection ScalaFileName
class VersionTest extends munit.FunSuite:

  Vector(
    ("//> using dep org.virtuslab::besom-codegen:0.3.0", ("//> using dep org.virtuslab::besom-", "codegen:0.3.0", "")),
    ("//> using dep \"org.virtuslab::besom-codegen:0.3.0\"", ("//> using dep \"org.virtuslab::besom-", "codegen:0.3.0", "\"")),
    (
      "//> using dep org.virtuslab::besom-kubernetes:4.11.0-core.0.3-SNAPSHOT",
      ("//> using dep org.virtuslab::besom-", "kubernetes:4.11.0-core.0.3-SNAPSHOT", "")
    )
  ).foreach { (v, exp) =>
    test("besom version regexp") {
      v match
        case Version.besomDependencyPattern(prefix, version, suffix) => assertEquals((prefix, version, suffix), exp)
        case _                                                       => fail("unexpected error")
    }
  }
