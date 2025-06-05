package besom.internal

import besom.internal.RunResult.{given, *}

class UpdateTest extends munit.FunSuite:
  final case class FooArgs private (bar: Output[BarArgs], baz: Output[Int])
  object FooArgs:
    def apply(bar: Output[BarArgs], baz: Output[Int]): FooArgs = new FooArgs(bar, baz)

    extension (fooArgs: FooArgs)
      def withArgs(bar: Output[BarArgs], baz: Output[Int]): FooArgs =
        new FooArgs(bar, baz)

  final case class BarArgs private (baz: Output[Int])
  object BarArgs:
    def apply(baz: Output[Int]): BarArgs = new BarArgs(baz)

    extension (barArgs: BarArgs)
      def withArgs(baz: Output[Int]): BarArgs =
        new BarArgs(baz)

  test("update top-level product") {
    given Context = DummyContext().unsafeRunSync()
    val foo       = FooArgs(Output.pure(BarArgs(Output.pure(1))), Output.pure(10))
    val updater   = summon[Update[FooArgs]]
    val updated = updater.update(
      foo,
      { case foo: FooArgs =>
        foo.withArgs(bar = Output.pure(BarArgs(Output.pure(2))), baz = Output.pure(20))
      }
    )
    assertEquals(updated.bar.flatMap(_.baz).getData.unsafeRunSync(), OutputData(2))
    assertEquals(updated.baz.getData.unsafeRunSync(), OutputData(20))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("update nested in product") {

    given Context = DummyContext().unsafeRunSync()

    val foo = FooArgs(Output.pure(BarArgs(Output.pure(1))), Output.pure(10))

    val updater = summon[Update[FooArgs]]
    val updated = updater.update(
      foo,
      { case bar: BarArgs =>
        bar.withArgs(baz = bar.baz.map(_ + 1))
      }
    )
    assertEquals(updated.bar.flatMap(_.baz).getData.unsafeRunSync(), OutputData(2))
    assertEquals(updated.baz.getData.unsafeRunSync(), OutputData(10))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("update nested in collection") {

    given Context = DummyContext().unsafeRunSync()

    val list = List(BarArgs(Output.pure(1)), BarArgs(Output.pure(10)))

    val updater = summon[Update[List[BarArgs]]]
    val updated = updater.update(
      list,
      { case bar: BarArgs =>
        bar.withArgs(baz = Output.pure(2))
      }
    )
    assertEquals(updated.length, 2)
    assertEquals(updated(0).baz.getData.unsafeRunSync(), OutputData(2))
    assertEquals(updated(1).baz.getData.unsafeRunSync(), OutputData(2))

    Context().waitForAllTasks.unsafeRunSync()
  }
end UpdateTest
