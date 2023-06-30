package besom.internal

import besom.util.*
import RunResult.{given, *}

class RegistersOutputsDerivationTest extends munit.FunSuite {
  case class TestRegistersOutputs(a: Output[Int]) extends ComponentResource {
    def urn: besom.internal.Output[String] = ???
  }

  test("derive an instance for TestRegistersOutputs") {
    given Context = DummyContext().unsafeRunSync()

    val testRegistersOutputs = TestRegistersOutputs(Output(1))
    val instance = summon[RegistersOutputs[TestRegistersOutputs]]
    assertEquals(instance.toMapOfOutputs(testRegistersOutputs), Map("a" -> testRegistersOutputs.a))
  }

  case class TestRegistersOutputs3(aField: Output[Int], alsoAField: Output[String], anotherFields: Output[Float]) extends ComponentResource {
    def urn: besom.internal.Output[String] = ???
  }

  test("derive an instance for TestRegistersOutputs3") {
    given Context = DummyContext().unsafeRunSync()

    val testRegistersOutputs = TestRegistersOutputs3(Output(1), Output("XD"), Output(1.0f))
    val instance = summon[RegistersOutputs[TestRegistersOutputs3]]
    val expected = Map(
      "aField" -> testRegistersOutputs.aField,
      "alsoAField" -> testRegistersOutputs.alsoAField,
      "anotherFields" -> testRegistersOutputs.anotherFields
    )
    assertEquals(instance.toMapOfOutputs(testRegistersOutputs), expected)
  }

  test("not derive an instance for a class with non-Output case fields") {
    val errors = compileErrors(
      """given Context = DummyContext().unsafeRunSync()
         case class TestRegistersOutputs2(a: Output[Int], b: String) extends ComponentResource {
           def urn: besom.internal.Output[String] = ???
         }
         summon[RegistersOutputs[TestRegistersOutputs2]]
         """
    )
    assert(errors.nonEmpty)
  }

}
