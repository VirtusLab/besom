package besom.internal

import besom.util.*
import RunResult.{given, *}

class OutputTest extends munit.FunSuite:

  def takesNEString(nestring: Input.Optional[NonEmptyString])(using
    Context
  ): Output[Option[NonEmptyString]] = nestring.asOptionOutput()

  def takesAList(list: Input[List[Input[String]]])(using
    Context
  ): Output[List[String]] =
    list.asOutput()

  def takesAnOptionalList(list: Input.Optional[List[Input[String]]])(using
    Context
  ): Output[Option[List[String]]] =
    list.asOptionOutput()

  def takesAMap(map: Input[Map[String, Input[String]]])(using
    Context
  ): Output[Map[String, String]] =
    map.asOutput()

  def takesAnOptionalMap(map: Input.Optional[Map[String, Input[String]]])(using
    Context
  ): Output[Option[Map[String, String]]] =
    map.asOptionOutput()

  def takesManyStrings(strings: Input.OneOrList[String])(using
    Context
  ): Output[List[String]] =
    strings.asManyOutput()

  test("multi-input type functions") {
    given Context = DummyContext().unsafeRunSync()

    assertEquals(takesNEString("string").getData.unsafeRunSync(), OutputData(Option("string")))
    assertEquals(takesNEString(Output("string")).getData.unsafeRunSync(), OutputData(Option("string")))
    assertEquals(takesNEString(None).getData.unsafeRunSync(), OutputData(None))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multi-input many type functions") {
    given Context = DummyContext().unsafeRunSync()

    assertEquals(takesManyStrings("value").getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(List("value")).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(List("value")).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(List(Output("value"))).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(Output(List("value"))).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesManyStrings(Output(List(Output("value")))).getData.unsafeRunSync(), OutputData(List("value")))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multi-input list type functions") {
    given Context = DummyContext().unsafeRunSync()

    assertEquals(takesAList(List("value")).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesAList(List(Output("value"))).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesAList(Output(List("value"))).getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(takesAList(Output(List(Output("value")))).getData.unsafeRunSync(), OutputData(List("value")))

    assertEquals(takesAnOptionalList(List("value")).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(List(Output("value"))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Output(List("value"))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Output(List(Output("value")))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Option(List("value"))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Option(List(Output("value")))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Output(Option(List("value")))).getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(takesAnOptionalList(Output(Option(List(Output("value"))))).getData.unsafeRunSync(), OutputData(Option(List("value"))))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multi-input map type functions") {
    given Context = DummyContext().unsafeRunSync()

    assertEquals(takesAMap(Map("key" -> "value")).getData.unsafeRunSync(), OutputData(Map("key" -> "value")))
    assertEquals(takesAMap(Map("key" -> Output("value"))).getData.unsafeRunSync(), OutputData(Map("key" -> "value")))
    assertEquals(takesAMap(Output(Map("key" -> "value"))).getData.unsafeRunSync(), OutputData(Map("key" -> "value")))
    assertEquals(takesAMap(Output(Map("key" -> Output("value")))).getData.unsafeRunSync(), OutputData(Map("key" -> "value")))

    assertEquals(takesAnOptionalMap(Map("key" -> "value")).getData.unsafeRunSync(), OutputData(Option(Map("key" -> "value"))))
    assertEquals(takesAnOptionalMap(Map("key" -> Output("value"))).getData.unsafeRunSync(), OutputData(Option(Map("key" -> "value"))))
    assertEquals(takesAnOptionalMap(Output(Map("key" -> "value"))).getData.unsafeRunSync(), OutputData(Option(Map("key" -> "value"))))
    assertEquals(
      takesAnOptionalMap(Output(Map("key" -> Output("value")))).getData.unsafeRunSync(),
      OutputData(Option(Map("key" -> "value")))
    )
    assertEquals(takesAnOptionalMap(Option(Map("key" -> "value"))).getData.unsafeRunSync(), OutputData(Option(Map("key" -> "value"))))
    assertEquals(
      takesAnOptionalMap(Option(Map("key" -> Output("value")))).getData.unsafeRunSync(),
      OutputData(Option(Map("key" -> "value")))
    )
    assertEquals(
      takesAnOptionalMap(Output(Option(Map("key" -> "value")))).getData.unsafeRunSync(),
      OutputData(Option(Map("key" -> "value")))
    )
    assertEquals(
      takesAnOptionalMap(Output(Option(Map("key" -> Output("value"))))).getData.unsafeRunSync(),
      OutputData(Option(Map("key" -> "value")))
    )

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("multiple evaluations of sequence") {
    given Context = DummyContext().unsafeRunSync()

    val seq = Output.sequence(List(Output("value"), Output("value2")))

    val firstEval = seq.getData.unsafeRunSync()
    assertEquals(firstEval, OutputData(List("value", "value2")))

    val secondEval = seq.getData.unsafeRunSync()
    assertEquals(secondEval, OutputData(List("value", "value2")))

    Context().waitForAllTasks.unsafeRunSync()
  }

end OutputTest
