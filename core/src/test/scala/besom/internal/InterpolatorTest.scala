package besom.internal

import besom.*
import RunResult.{given, *}

class InterpolatorTest extends munit.FunSuite {
  test("interpolator works for a simple dummy case") {
    val output       = Output("Holmes")
    val interpolated = pulumi"Sherlock $output"

    given Context = DummyContext().unsafeRunSync()

    assertEquals(interpolated.getData.unsafeRunSync(), OutputData("Sherlock Holmes"))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("interpolator works for a longer dummy case") {
    val output       = Output("Holmes")
    val output1      = Output("is")
    val output2      = Output("!")
    val interpolated = pulumi"Sherlock $output $output1 a detective${output2}"

    given Context = DummyContext().unsafeRunSync()

    assertEquals(interpolated.getData.unsafeRunSync(), OutputData("Sherlock Holmes is a detective!"))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("interpolator works for a simple case with non-output arguments") {
    val output       = "Holmes"
    val interpolated = pulumi"Sherlock $output and Watson"

    given Context = DummyContext().unsafeRunSync()

    assertEquals(interpolated.getData.unsafeRunSync(), OutputData("Sherlock Holmes and Watson"))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("interpolator works for a longer case with non-output arguments") {
    val output       = "Holmes"
    val output1      = "is"
    val output2      = "!"
    val interpolated = pulumi"Sherlock $output $output1 a detective${output2}"

    given Context = DummyContext().unsafeRunSync()

    assertEquals(interpolated.getData.unsafeRunSync(), OutputData("Sherlock Holmes is a detective!"))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("interpolator works for a simple case with non-output arguments and output arguments") {
    val output       = Output("Holmes")
    val output1      = "Watson"
    val interpolated = pulumi"Sherlock $output and $output1"

    given Context = DummyContext().unsafeRunSync()

    assertEquals(interpolated.getData.unsafeRunSync(), OutputData("Sherlock Holmes and Watson"))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("interpolator works for a simple case with mixed non-output arguments and output arguments") {
    val output       = Output("1")
    val output1      = "2"
    val output2      = 3
    val interpolated = pulumi"$output + $output1 = $output2"

    given Context = DummyContext().unsafeRunSync()

    assertEquals(interpolated.getData.unsafeRunSync(), OutputData("1 + 2 = 3"))

    Context().waitForAllTasks.unsafeRunSync()
  }

  test("interpolator works for a simple case with mixed non-output arguments and output arguments and nested interpolators") {
    val output       = Output("1")
    val output1      = "2"
    val output2      = 3
    val interpolated = pulumi"$output + $output1 = ${pulumi"$output2"}"

    given Context = DummyContext().unsafeRunSync()

    assertEquals(interpolated.getData.unsafeRunSync(), OutputData("1 + 2 = 3"))

    Context().waitForAllTasks.unsafeRunSync()
  }
}
