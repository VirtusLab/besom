package besom.cfg

import fastparse._, MultiLineWhitespace._

class TypeParserTest extends munit.FunSuite:

  import Tpe.*

  test("should parse simple type") {
    parse("a: Int", field(_)) match
      case Parsed.Success(value, _) => assertEquals(value, ("a", Tpe.Simple("Int")))
      case f: Parsed.Failure        => fail(f.trace().longMsg)
  }

  test("should parse union of simple types") {
    parse("Int | String", anyType(_)) match
      case Parsed.Success(value, _) => assertEquals(value, Tpe.Union(List(Tpe.Simple("Int"), Tpe.Simple("String"))))
      case f: Parsed.Failure        => fail(f.trace().longMsg)
  }

  test("should parse union of simple type and Output type") {
    parse("Int | Output[Int]", anyType(_)) match
      case Parsed.Success(value, _) => assertEquals(value, Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int")))))
      case f: Parsed.Failure        => fail(f.trace().longMsg)
  }

  test("should parse list of simple type") {
    parse("List[Int]", anyType(_)) match
      case Parsed.Success(value, _) => assertEquals(value, Tpe.Lst(Tpe.Simple("Int")))
      case f: Parsed.Failure        => fail(f.trace().longMsg)
  }

  test("should parse an empty struct") {
    parse("{}", structType(_)) match
      case Parsed.Success(value, _) => assertEquals(value, Tpe.Struct(List.empty))
      case f: Parsed.Failure        => fail(f.trace().longMsg)
  }

  test("should parse a simple struct type") {
    parse(
      """{
        |  a: Int
        |}""".stripMargin,
      structType(_)
    ) match
      case Parsed.Success(value, _) =>
        assertEquals(value, Tpe.Struct(List(("a", Tpe.Simple("Int")))))
      case f: Parsed.Failure => fail(f.trace().longMsg)
  }

  test("should parse a struct type with union") {
    parse(
      """{
        |  a: Int | String
        |}""".stripMargin,
      structType(_)
    ) match
      case Parsed.Success(value, _) =>
        assertEquals(value, Tpe.Struct(List(("a", Tpe.Union(List(Tpe.Simple("Int"), Tpe.Simple("String")))))))
      case f: Parsed.Failure => fail(f.trace().longMsg)
  }

  test("should parse an output type of struct") {
    parse(
      """Output[{
        |  a: Int
        |}]""".stripMargin,
      outputType(_)
    ) match
      case Parsed.Success(value, _) =>
        assertEquals(value, Tpe.Output(Tpe.Struct(List(("a", Tpe.Simple("Int"))))))
      case f: Parsed.Failure => fail(f.trace().longMsg)
  }

  test("should parse an union of simple type and output of simple type") {
    parse(
      """List[Int | Output[Int]] | List[Output[Int | Output[Int]]]""",
      anyType(_)
    ) match
      case Parsed.Success(value, _) =>
        assertEquals(
          value,
          Tpe.Union(
            List(
              Tpe.Lst(Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int"))))),
              Tpe.Lst(Tpe.Output(Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int"))))))
            )
          )
        )
      case f: Parsed.Failure => fail(f.trace().longMsg)
  }

  test("should parse an union of struct and output of struct") {
    parse(
      """shouldBeStructButItsNot: {
            |  a: Int | Output[Int]
            |  b: String | Output[String]
            |} | Output[{
            |  a: Int | Output[Int]
            |  b: String | Output[String]
            |}]""".stripMargin,
      field(_)
    ) match
      case Parsed.Success(value, _) =>
        assertEquals(
          value,
          (
            "shouldBeStructButItsNot",
            Tpe.Union(
              List(
                Tpe.Struct(
                  List(
                    ("a", Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int"))))),
                    ("b", Tpe.Union(List(Tpe.Simple("String"), Tpe.Output(Tpe.Simple("String")))))
                  )
                ),
                Tpe.Output(
                  Tpe.Struct(
                    List(
                      ("a", Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int"))))),
                      ("b", Tpe.Union(List(Tpe.Simple("String"), Tpe.Output(Tpe.Simple("String")))))
                    )
                  )
                )
              )
            )
          )
        )
      case f: Parsed.Failure => fail(f.trace().longAggregateMsg)
  }

  test("should parse simple field") {
    parse("shouldBeIntAndItsOK: Int | Output[Int]", field(_)) match
      case Parsed.Success(value, _) =>
        assertEquals(value, ("shouldBeIntAndItsOK", Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int"))))))
      case f: Parsed.Failure => fail(f.trace().longMsg)
  }

  test("should parse union field") {
    parse("shouldBeStringButItsNot: String | Output[String]", field(_)) match
      case Parsed.Success(value, _) =>
        assertEquals(value, ("shouldBeStringButItsNot", Tpe.Union(List(Tpe.Simple("String"), Tpe.Output(Tpe.Simple("String"))))))
      case f: Parsed.Failure => fail(f.trace().longMsg)
  }

  test("should parse very complex unions") {
    val union = """shouldBeListOfStructs: List[{
    a: Int | Output[Int]
    b: String | Output[String]
  } | Output[{
    a: Int | Output[Int]
    b: String | Output[String]
  }]] | List[Output[{
    a: Int | Output[Int]
    b: String | Output[String]
  } | Output[{
    a: Int | Output[Int]
    b: String | Output[String]
  }]]]"""
    parse(union, field(_)) match
      case Parsed.Success(value, _) =>
        assertEquals(
          value,
          (
            "shouldBeListOfStructs",
            Tpe.Union(
              List(
                Tpe.Lst(
                  Tpe.Union(
                    List(
                      Tpe.Struct(
                        List(
                          ("a", Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int"))))),
                          ("b", Tpe.Union(List(Tpe.Simple("String"), Tpe.Output(Tpe.Simple("String")))))
                        )
                      ),
                      Tpe.Output(
                        Tpe.Struct(
                          List(
                            ("a", Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int"))))),
                            ("b", Tpe.Union(List(Tpe.Simple("String"), Tpe.Output(Tpe.Simple("String")))))
                          )
                        )
                      )
                    )
                  )
                ),
                Tpe.Lst(
                  Tpe.Output(
                    Tpe.Union(
                      List(
                        Tpe.Struct(
                          List(
                            ("a", Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int"))))),
                            ("b", Tpe.Union(List(Tpe.Simple("String"), Tpe.Output(Tpe.Simple("String")))))
                          )
                        ),
                        Tpe.Output(
                          Tpe.Struct(
                            List(
                              ("a", Tpe.Union(List(Tpe.Simple("Int"), Tpe.Output(Tpe.Simple("Int"))))),
                              ("b", Tpe.Union(List(Tpe.Simple("String"), Tpe.Output(Tpe.Simple("String")))))
                            )
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      case f: Parsed.Failure => fail(f.trace().longMsg)
    end match
  }

  test("should strip outputs from types correctly") {
    val union = """shouldBeListOfStructs: List[{
        a: Int | Output[Int]
        b: String | Output[String]
      } | Output[{
        a: Int | Output[Int]
        b: String | Output[String]
      }]] | List[Output[{
        a: Int | Output[Int]
        b: String | Output[String]
      } | Output[{
        a: Int | Output[Int]
        b: String | Output[String]
      }]]]"""

    parse(union, field(_)) match
      case Parsed.Success((_, tpe), _) =>
        assertEquals(tpe.stripOutputs, Tpe.Lst(Tpe.Struct(List("a" -> Tpe.Simple("Int"), "b" -> Tpe.Simple("String")))))

      case f: Parsed.Failure => fail(f.trace().longMsg)
  }
end TypeParserTest
