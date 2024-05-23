package besom.cfg

import besom.cfg.k8s.*
import besom.cfg.*
import besom.internal.DummyContext
import besom.internal.RunOutput.*

import besom.types.{Context, Output}
import besom.api.kubernetes.core.v1.inputs.EnvVarArgs

class StructSerdeTest extends munit.FunSuite:
  // ConfiguredContainerArgs(
  //   name = "my-app",
  //   image = "my-app:0.1",
  // Struct(
  //   shouldBeIntAndItsOK = 1,
  //   shouldBeStringButItsNot = 23.0,
  //   shouldBeBoolButItsStruct = Struct(
  //     x = "y"
  //   ),
  //   thisOneIsUnnecessary = "not needed",
  //   shouldBeStructButItsNot = "it's a string",
  //   shouldBeListOfInts = List(1.23, 2, 3), // but it's a list of doubles
  //   oneFieldInsideIsWrongAndOneIsUnnecessary = Struct(
  //     x = 10,
  //     y = 10,
  //     z = 10
  //   ),
  //   shouldBeListOfStructs = "but it isn't",
  //   shouldBeListOfStructsButItsAStruct = Struct(
  //     a = 1,
  //     b = 2
  //   ),
  //   shouldBeAStructButItsAListOfStructs = List(
  //     Struct(
  //       one = "no"
  //     )
  //   ),
  //   shouldBeAStringButItsAListOfStructs = List(
  //     Struct(
  //       one = 1
  //     )
  //   ),
  //   shouldBeAListOfStructsButItsAString = "oops",
  //   unnecessaryListOfStructs = List(
  //     Struct(
  //       a = 1,
  //       b = 2
  //     )
  //   ),
  //   unnecessaryStruct = Struct(
  //     one = "no",
  //     two = "not",
  //     three = "needed"
  //   )
  // )
  // )

  given Context = DummyContext().unsafeRunSync()

  test("serialization of Structs to kubernetes EnvVarArgs") {

    // values can be: simple types, Output, Struct, List and their combinations without Lists of Lists
    val s = Struct(
      name = "test", // simple type
      int = Output(23), // Output[simple]
      s = Struct( // Struct
        d = 23,
        e = "test"
      ),
      l = List(1.2, 2.3, 3.4), // List[simple]
      ls = List( // List[Struct]
        Struct(
          f1 = Output(List(Struct(deep = "q"))), // List[Struct->Output[List[Struct]]]
          f2 = "w"
        )
      ),
      lo = List(Output("a"), Output("b"), Output("c")), // List[Output[simple]]
      ol = Output(List("x", "y", "z")), // Output[List[simple]]
      os = Output( // Output[Struct]
        Struct(
          oh = "yeah",
          it = "works!"
        )
      )
    )

    val expected = List(
      "name" -> "test",
      "int" -> "23",
      "s.d" -> "23",
      "s.e" -> "test",
      "l.0" -> "1.2",
      "l.1" -> "2.3",
      "l.2" -> "3.4",
      "ls.0.f1.0.deep" -> "q",
      "ls.0.f2" -> "w",
      "lo.0" -> "a",
      "lo.1" -> "b",
      "lo.2" -> "c",
      "ol.0" -> "x",
      "ol.1" -> "y",
      "ol.2" -> "z",
      "os.oh" -> "yeah",
      "os.it" -> "works!"
    )

    val asEnv = s.foldToEnv

    asEnv.unsafeRunSync().get.zip(expected).foreach((a, b) => assertEquals(a, b))

    import besom.cfg.k8s.syntax.*
    val asEnvVarArgs = s.foldedToEnvVarArgs

    asEnvVarArgs
      .unsafeRunSync()
      .get
      .map { case EnvVarArgs(name, value, _) =>
        val k = name.unsafeRunSync().get
        val v = value.unsafeRunSync().get.get

        k -> v
      }
      .zip(expected)
      .foreach((a, b) => assertEquals(a, b))
  }
end StructSerdeTest
