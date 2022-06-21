//> using lib "org.scalameta::munit::1.0.0-M1"

package besom.test

import besom.api.Output

class SanityTest extends munit.FunSuite:
  test("sanity") {
    assert(2 + 2 == 4)
  }
