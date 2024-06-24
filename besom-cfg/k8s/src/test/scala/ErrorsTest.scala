package besom.cfg

import besom.json.*
import besom.cfg.internal.*
import besom.types.{Context, Output}
import besom.internal.DummyContext
import besom.internal.RunOutput.*

class ErrorsTest extends munit.FunSuite:

  given Context = DummyContext().unsafeRunSync()

  test("should fail on wrong type") {
    val struct = Struct(
      shouldBeIntAndItsOK = 1,
      shouldBeStringButItsNot = 23.0,
      shouldBeBoolButItsStruct = Struct(
        x = "y"
      ),
      thisOneIsUnnecessary = "not needed",
      shouldBeStructButItsNot = "it's a string",
      shouldBeListOfInts = List(1.23, 2, 3), // but it's a list of doubles
      oneFieldInsideIsWrongAndOneIsUnnecessary = Struct(
        x = 10,
        y = 10,
        z = 10
      ),
      shouldBeListOfStructs = "but it isn't",
      shouldBeListOfStructsButItsAStruct = Struct(
        a = 1,
        b = 2
      ),
      shouldBeAStructButItsAListOfStructs = List(
        Struct(
          one = "no"
        )
      ),
      shouldBeAStringButItsAListOfStructs = List(
        Struct(
          one = 1
        )
      ),
      shouldBeAListOfStructsButItsAString = "oops",
      unnecessaryListOfStructs = List(
        Struct(
          a = 1,
          b = 2
        )
      ),
      unnecessaryStruct = Struct(
        one = "no",
        two = "not",
        three = "needed"
      )
    )

    ErrorsSupport(
      """{
  "schema": [
    {
      "details": {
        "type": "int"
      },
      "name": "shouldBeIntAndItsOK"
    },
    {
      "details": {
        "type": "string"
      },
      "name": "shouldBeStringButItsNot"
    },
    {
      "details": {
        "type": "boolean"
      },
      "name": "shouldBeBoolButItsStruct"
    },
    {
      "details": {
        "fields": {
          "a": {
            "type": "int"
          },
          "b": {
            "type": "string"
          }
        },
        "type": "struct"
      },
      "name": "shouldBeStructButItsNot"
    },
    {
      "details": {
        "fields": {
          "x": {
            "type": "int"
          },
          "y": {
            "type": "double"
          }
        },
        "type": "struct"
      },
      "name": "oneFieldInsideIsWrongAndOneIsUnnecessary"
    },
    {
      "details": {
        "fields": {
          "q": {
            "type": "int"
          },
          "w": {
            "type": "string"
          }
        },
        "type": "struct"
      },
      "name": "wholeStructMissing"
    },
    {
      "details": {
        "inner": {
          "type": "int"
        },
        "type": "array"
      },
      "name": "shouldBeListOfInts"
    },
    {
      "details": {
        "inner": {
          "fields": {
            "a": {
              "type": "int"
            },
            "b": {
              "type": "string"
            }
          },
          "type": "struct"
        },
        "type": "array"
      },
      "name": "shouldBeListOfStructs"
    },
    {
      "details": {
        "inner": {
          "fields": {
            "x": {
              "type": "int"
            },
            "y": {
              "type": "double"
            }
          },
          "type": "struct"
        },
        "type": "array"
      },
      "name": "shouldBeListOfStructsButItsAStruct"
    },
    {
      "details": {
        "fields": {
          "x": {
            "type": "int"
          },
          "y": {
            "type": "double"
          }
        },
        "type": "struct"
      },
      "name": "shouldBeAStructButItsAListOfStructs"
    },
    {
      "details": {
        "type": "string"
      },
      "name": "shouldBeAStringButItsAListOfStructs"
    },
    {
      "details": {
        "inner": {
          "fields": {
            "a": {
              "type": "int"
            },
            "b": {
              "type": "string"
            }
          },
          "type": "struct"
        },
        "type": "array"
      },
      "name": "shouldBeAListOfStructsButItsAString"
    }
  ],
  "version": "0.1.0",
  "medium": "env"
}""",
      struct
    ) match
      case Left(diff) =>
        val renderedDiff = diff.toString

        assertEquals(renderedDiff, expected)
      case Right(()) => fail("should fail")
    end match

  }

  private val expected: String =
    val src = scala.io.Source.fromResource("expected-diff.txt")
    try src.mkString
    finally src.close()

end ErrorsTest
