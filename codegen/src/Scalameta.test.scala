package besom.codegen

import scala.meta._

//noinspection ScalaFileName
class ScalametaTest extends munit.FunSuite {
  test("package_") {
    val pkg = "besom.api.aws.config.endpoints.outputs"
    assertEquals(scalameta.package_(scalameta.ref(pkg.split('.').toList))().syntax, s"package $pkg")
  }

  test("import_") {
    val pkg = "besom.internal.CodegenProtocol"
    assertEquals(scalameta.importAll(scalameta.ref(pkg.split('.').toList)).syntax, s"import ${pkg}._")
  }

  test("apply_") {
    assertEquals(scalameta.apply_(scalameta.List).syntax, "scala.List.apply")
  }

  test("method") {
    assertEquals(scalameta.method(scalameta.List, "apply").syntax, "scala.List.apply")
  }

  test("Unit") {
    assertEquals(scalameta.Unit.syntax, "scala.Unit")
  }

  test("None") {
    assertEquals(scalameta.None.syntax, "scala.None")
  }

  test("Some") {
    assertEquals(scalameta.Some(Lit.String("test")).syntax, "scala.Some(\"test\")")
  }

  test("List size 1") {
    assertEquals(scalameta.List(Lit.String("test")).syntax, "scala.List(\"test\")")
  }

  test("List empty") {
    assertEquals(scalameta.List().syntax, "scala.List()")
  }

  test("besom.internal.CodegenProtocol.jsonFormatN") {
    assertEquals(
      scalameta.besom.internal.CodegenProtocol.jsonFormatN(1)(scalameta.apply_(scalameta.List)).syntax,
      "besom.internal.CodegenProtocol.jsonFormat1(scala.List.apply)"
    )
  }

  test("types.String") {
    assertEquals(scalameta.types.String.syntax, "String")
  }

  test("types.Map") {
    assertEquals(scalameta.types.Map.syntax, "scala.Predef.Map")
  }

  test("types.Option[String]") {
    assertEquals(scalameta.types.Option(Type.Name("String")).syntax, "scala.Option[String]")
  }

  test("types.List[String]") {
    assertEquals(scalameta.types.List(Type.Name("String")).syntax, "scala.List[String]")
  }

  test("types.Map[String, String]") {
    assertEquals(scalameta.types.Map(Type.Name("String"), Type.Name("String")).syntax, "scala.Predef.Map[String, String]")
    assertEquals(scalameta.types.Map(Type.Name("String")).syntax, "scala.Predef.Map[String, String]")
  }

  test("besom.types.Context") {
    assertEquals(scalameta.types.besom.types.Context.syntax, "besom.types.Context")
  }

  test("besom.types.Output[String]") {
    assertEquals(scalameta.types.besom.types.Output(Type.Name("String")).syntax, "besom.types.Output[String]")
  }

  test("besom.types.Input[String]") {
    assertEquals(scalameta.types.besom.types.Input(Type.Name("String")).syntax, "besom.types.Input[String]")
  }

  test("spray.json.JsonFormat[String]") {
    assertEquals(scalameta.types.spray.json.JsonFormat(Type.Name("String")).syntax, "spray.json.JsonFormat[String]")
  }

  test("besom.types.Output[besom.types.Input[String]]") {
    assertEquals(
      scalameta.types.besom.types.Output(scalameta.types.besom.types.Input(Type.Name("String"))).syntax,
      "besom.types.Output[besom.types.Input[String]]"
    )
  }
}
