package besom.util

trait CompileAssertions:
  self: munit.FunSuite =>

  private val NL = System.lineSeparator()

  inline def failsToCompile(inline code: String): Unit =
    assert(
      !scala.compiletime.testing.typeChecks(code),
      s"Code compiled correctly when expecting type errors:$NL$code"
    )

  inline def compiles(inline code: String): Unit =
    if !scala.compiletime.testing.typeChecks(code) then
      val errors        = scala.compiletime.testing.typeCheckErrors(code)
      val errorMessages = errors.map(_.message).mkString(NL)
      fail(s"Code failed to compile:$NL$code$NL$errorMessages")
