package besom.internal

import besom.util.*
import RunResult.{given, *}

class OutputTest extends munit.FunSuite:

  def takesNEString(nestring: NonEmptyString | Output[NonEmptyString] | NotProvided)(using
    Context
  ): Output[NonEmptyString] = nestring.asOutput

  def takesAMap(map: Map[String, String] | Output[Map[String, String]] | Map[String, Output[String]] | NotProvided)(
    using Context
  ): Output[Map[String, String]] =
    map.asOutputMap

  test("multi-input type functions") {
    given Context = DummyContext().unsafeRunSync()

    assert(takesNEString("string").getData.unsafeRunSync() == OutputData("string"))
    assert(takesNEString(Output("string")).getData.unsafeRunSync() == OutputData("string"))
    assert(takesNEString(NotProvided).getData.unsafeRunSync() == OutputData.empty())

    summon[Context].waitForAllTasks.unsafeRunSync()
  }

  test("multi-input map type functions") {
    given Context = DummyContext().unsafeRunSync()

    assert(takesAMap(Map("key" -> "value")).getData.unsafeRunSync() == OutputData(Map("key" -> "value")))
    assert(takesAMap(Map("key" -> Output("value"))).getData.unsafeRunSync() == OutputData(Map("key" -> "value")))
    assert(takesAMap(Output(Map("key" -> "value"))).getData.unsafeRunSync() == OutputData(Map("key" -> "value")))
    assert(takesAMap(NotProvided).getData.unsafeRunSync() == OutputData.empty())

    summon[Context].waitForAllTasks.unsafeRunSync()
  }
