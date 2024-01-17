package besom.codegen

import scala.meta.*
import scala.meta.dialects.Scala33

//noinspection ScalaFileName
class ScalaMetaTest extends munit.FunSuite {
  test("Union") {
    assertEquals(
      scalameta.types.Union(List(scalameta.types.Boolean, scalameta.types.Int, scalameta.types.String)).syntax,
      "Boolean | Int | String"
    )
  }
}
