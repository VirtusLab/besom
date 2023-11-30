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

  test("multi-input type functions") {
    given Context = DummyContext().unsafeRunSync()

    assert(takesNEString("string").getData.unsafeRunSync() == OutputData(Some("string")))
    assert(takesNEString(Output("string")).getData.unsafeRunSync() == OutputData(Some("string")))
    assert(takesNEString(None).getData.unsafeRunSync() == OutputData(None))

    summon[Context].waitForAllTasks.unsafeRunSync()
  }

  test("multi-input list type functions") {
    given Context = DummyContext().unsafeRunSync()

    assert(takesAList(List("value")).getData.unsafeRunSync() == OutputData(List("value")))
    assert(takesAList(List(Output("value"))).getData.unsafeRunSync() == OutputData(List("value")))
    assert(takesAList(Output(List("value"))).getData.unsafeRunSync() == OutputData(List("value")))
    assert(takesAList(Output(List(Output("value")))).getData.unsafeRunSync() == OutputData(List("value")))

    assert(takesAnOptionalList(List("value")).getData.unsafeRunSync() == OutputData(Option(List("value"))))
    assert(takesAnOptionalList(List(Output("value"))).getData.unsafeRunSync() == OutputData(Option(List("value"))))
    assert(takesAnOptionalList(Output(List("value"))).getData.unsafeRunSync() == OutputData(Option(List("value"))))
    assert(takesAnOptionalList(Output(List(Output("value")))).getData.unsafeRunSync() == OutputData(Option(List("value"))))
    assert(takesAnOptionalList(Option(List("value"))).getData.unsafeRunSync() == OutputData(Option(List("value"))))
    assert(takesAnOptionalList(Option(List(Output("value")))).getData.unsafeRunSync() == OutputData(Option(List("value"))))
    assert(takesAnOptionalList(Output(Option(List("value")))).getData.unsafeRunSync() == OutputData(Option(List("value"))))
    assert(takesAnOptionalList(Output(Option(List(Output("value"))))).getData.unsafeRunSync() == OutputData(Option(List("value"))))

    summon[Context].waitForAllTasks.unsafeRunSync()
  }

  test("multi-input map type functions") {
    given Context = DummyContext().unsafeRunSync()

    assert(takesAMap(Map("key" -> "value")).getData.unsafeRunSync() == OutputData(Map("key" -> "value")))
    assert(takesAMap(Map("key" -> Output("value"))).getData.unsafeRunSync() == OutputData(Map("key" -> "value")))
    assert(takesAMap(Output(Map("key" -> "value"))).getData.unsafeRunSync() == OutputData(Map("key" -> "value")))
    assert(takesAMap(Output(Map("key" -> Output("value")))).getData.unsafeRunSync() == OutputData(Map("key" -> "value")))

    assert(takesAnOptionalMap(Map("key" -> "value")).getData.unsafeRunSync() == OutputData(Option(Map("key" -> "value"))))
    assert(takesAnOptionalMap(Map("key" -> Output("value"))).getData.unsafeRunSync() == OutputData(Option(Map("key" -> "value"))))
    assert(takesAnOptionalMap(Output(Map("key" -> "value"))).getData.unsafeRunSync() == OutputData(Option(Map("key" -> "value"))))
    assert(takesAnOptionalMap(Output(Map("key" -> Output("value")))).getData.unsafeRunSync() == OutputData(Option(Map("key" -> "value"))))
    assert(takesAnOptionalMap(Option(Map("key" -> "value"))).getData.unsafeRunSync() == OutputData(Option(Map("key" -> "value"))))
    assert(takesAnOptionalMap(Option(Map("key" -> Output("value")))).getData.unsafeRunSync() == OutputData(Option(Map("key" -> "value"))))
    assert(takesAnOptionalMap(Output(Option(Map("key" -> "value")))).getData.unsafeRunSync() == OutputData(Option(Map("key" -> "value"))))
    assert(
      takesAnOptionalMap(Output(Option(Map("key" -> Output("value"))))).getData.unsafeRunSync() == OutputData(Option(Map("key" -> "value")))
    )

    summon[Context].waitForAllTasks.unsafeRunSync()
  }

  test("multiple evaluations of sequence") {
    given Context = DummyContext().unsafeRunSync()

    val seq = Output.sequence(List(Output("value"), Output("value2")))

    val firstEval = seq.getData.unsafeRunSync()
    assertEquals(firstEval, OutputData(List("value", "value2")))

    val secondEval = seq.getData.unsafeRunSync()
    assertEquals(secondEval, OutputData(List("value", "value2")))

    summon[Context].waitForAllTasks.unsafeRunSync()
  }

end OutputTest
