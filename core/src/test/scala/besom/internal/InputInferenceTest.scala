package besom.internal

import RunResult.{given, *}
import Input.given
import scala.collection.immutable.Iterable

/** Regression tests for the inference of the type parameter of the `Input` opaque types.
  *
  * `Input[A]`, `Input.Optional[A]` and `Input.OneOrIterable[A]` are unions, and the alternative naming the bare `A` used to come first.
  * When the argument is an application - which every resource constructor is - the solver matched that leftmost alternative directly and
  * instantiated `A := Output[R]` instead of `A := R`. The result typechecked, but `wrappedAsOptionOutput` then took its `case output:
  * Output[_]` arm (an erased class test that cannot tell a bare `A` that happens to be an `Output` from an `Output` wrapper), stripped a
  * level of wrapping that was never there, and produced a value whose runtime shape contradicted its static type. Any later use of the
  * element as an `Output` then threw a ClassCastException.
  *
  * The unions are now ordered wrapper-first, which is the same type - union subtyping is commutative - but stops the solver taking that
  * shortcut.
  *
  * Two things matter about how these tests are written, and both are why the pre-existing `Output.when` cases in OutputTest never caught
  * this:
  *
  *   - every argument is passed as an APPLICATION, never bound to a `val` first. A stable identifier infers correctly even on the old
  *     ordering.
  *   - each result is bound WITHOUT an expected type and asserted on the following line. Ascribing at the definition would drive inference
  *     and hide what is under test.
  */
class InputInferenceTest extends munit.FunSuite:

  def mkStr(using Context): Output[String]                     = Output.pure("value")
  def mkOpt(using Context): Output[Option[String]]             = Output.pure(Option("value"))
  def mkList(using Context): Output[List[Input[String]]]       = Output.pure[List[Input[String]]](List("value"))
  def mkMap(using Context): Output[Map[String, Input[String]]] = Output.pure[Map[String, Input[String]]](Map("k" -> "value"))

  test("Input[A].asOutput infers A from an applied argument") {
    given Context = DummyContext().unsafeRunSync()

    val v                 = mkStr.asOutput()
    val _: Output[String] = v

    assertEquals(v.getData.unsafeRunSync(), OutputData("value"))
    Context().waitForAllTasks.unsafeRunSync()
  }

  test("Input.Optional[A].asOptionOutput infers A from an applied argument") {
    given Context = DummyContext().unsafeRunSync()

    val fromValue                 = mkStr.asOptionOutput()
    val _: Output[Option[String]] = fromValue

    val fromOption                = mkOpt.asOptionOutput()
    val _: Output[Option[String]] = fromOption

    assertEquals(fromValue.getData.unsafeRunSync(), OutputData(Option("value")))
    assertEquals(fromOption.getData.unsafeRunSync(), OutputData(Option("value")))
    Context().waitForAllTasks.unsafeRunSync()
  }

  test("Input.OneOrIterable[A].asManyOutput infers A from an applied argument") {
    given Context = DummyContext().unsafeRunSync()

    val many                        = mkList.asManyOutput()
    val _: Output[Iterable[String]] = many

    assertEquals(many.getData.unsafeRunSync(), OutputData(List("value")))
    Context().waitForAllTasks.unsafeRunSync()
  }

  test("the iterable and map variants infer A from an applied argument") {
    given Context = DummyContext().unsafeRunSync()

    val list                        = mkList.asOutput()
    val _: Output[Iterable[String]] = list

    val optList                             = mkList.asOptionOutput()
    val _: Output[Option[Iterable[String]]] = optList

    val map                            = mkMap.asOutput()
    val _: Output[Map[String, String]] = map

    val optMap                                 = mkMap.asOptionOutput()
    val _: Output[Option[Map[String, String]]] = optMap

    assertEquals(list.getData.unsafeRunSync(), OutputData(List("value")))
    assertEquals(optList.getData.unsafeRunSync(), OutputData(Option(List("value"))))
    assertEquals(map.getData.unsafeRunSync(), OutputData(Map("k" -> "value")))
    assertEquals(optMap.getData.unsafeRunSync(), OutputData(Option(Map("k" -> "value"))))
    Context().waitForAllTasks.unsafeRunSync()
  }

  test("Output.when infers A from an applied argument, not the Output wrapping it") {
    given Context = DummyContext().unsafeRunSync()

    val applied                   = Output.when(true)(mkStr)
    val _: Output[Option[String]] = applied

    val appliedInBlock            = Output.when(true) { mkStr }
    val _: Output[Option[String]] = appliedInBlock

    val explicitTypeArg           = Output.when[String](true)(mkStr)
    val _: Output[Option[String]] = explicitTypeArg

    val fromOption                = Output.when(true)(mkOpt)
    val _: Output[Option[String]] = fromOption

    val bare                      = Output.when(true)("value")
    val _: Output[Option[String]] = bare

    val skipped                   = Output.when(false)(mkStr)
    val _: Output[Option[String]] = skipped

    assertEquals(applied.getData.unsafeRunSync(), OutputData(Option("value")))
    assertEquals(appliedInBlock.getData.unsafeRunSync(), OutputData(Option("value")))
    assertEquals(explicitTypeArg.getData.unsafeRunSync(), OutputData(Option("value")))
    assertEquals(fromOption.getData.unsafeRunSync(), OutputData(Option("value")))
    assertEquals(bare.getData.unsafeRunSync(), OutputData(Option("value")))
    assertEquals(skipped.getData.unsafeRunSync(), OutputData(None))
    Context().waitForAllTasks.unsafeRunSync()
  }

  test("the element is the value itself, not an Output wrapping it") {
    given Context = DummyContext().unsafeRunSync()

    // The original failure mode: if A were inferred as Output[String] this would not compile; if
    // inference were right but the runtime shape wrong, it would throw ClassCastException here.
    val lengths = Output.when(true)(mkStr).map(_.map(_.length))
    assertEquals(lengths.getData.unsafeRunSync(), OutputData(Option(5)))

    Context().waitForAllTasks.unsafeRunSync()
  }
end InputInferenceTest
