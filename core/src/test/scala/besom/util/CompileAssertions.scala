package besom.util

trait CompileAssertions:
  self: munit.FunSuite =>
  inline def failsToCompile(inline code: String): Unit =
    assert(
      !scala.compiletime.testing.typeChecks(code),
      s"Code compiled correctly when expecting type errors:${System.lineSeparator()}$code"
    )

  inline def compiles(inline code: String): Unit =
    assert(scala.compiletime.testing.typeChecks(code), s"Code failed to compile:${System.lineSeparator()}$code")
