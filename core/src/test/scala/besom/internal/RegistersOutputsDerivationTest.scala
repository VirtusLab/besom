package besom.internal

import besom.util.*
import RunResult.{given, *}

class RegistersOutputsDerivationTest extends munit.FunSuite {
  case class TestRegistersOutputs(a: Output[Int]) extends ComponentResource {
    def urn: besom.internal.Output[String] = ???
  }

  test("derive an instance for TestRegistersOutputs") {
    given Context = DummyContext().unsafeRunSync()
    val intEncoder: Encoder[Output[Int]] = summon

    val testRegistersOutputs = TestRegistersOutputs(Output(1))
    val instance = summon[RegistersOutputs[TestRegistersOutputs]]
    assertEquals(
      instance.toMapOfOutputs(testRegistersOutputs).view.mapValues(_.unsafeRunSync()).toMap,
      Map("a" -> intEncoder.encode(testRegistersOutputs.a).unsafeRunSync())
    )
  }

  case class TestRegistersOutputs3(aField: Output[Int], alsoAField: Output[String], anotherFields: Output[Boolean]) extends ComponentResource {
    def urn: besom.internal.Output[String] = ???
  }

  test("derive an instance for TestRegistersOutputs3") {
    given Context = DummyContext().unsafeRunSync()
    val intEncoder: Encoder[Output[Int]] = summon
    val stringEncoder: Encoder[Output[String]] = summon
    val booleanEncoder: Encoder[Output[Boolean]] = summon

    val testRegistersOutputs = TestRegistersOutputs3(Output(1), Output("XD"), Output(false))
    val instance = summon[RegistersOutputs[TestRegistersOutputs3]]
    val expected = Map(
      "aField" -> intEncoder.encode(testRegistersOutputs.aField).unsafeRunSync(),
      "alsoAField" -> stringEncoder.encode(testRegistersOutputs.alsoAField).unsafeRunSync(),
      "anotherFields" -> booleanEncoder.encode(testRegistersOutputs.anotherFields).unsafeRunSync()
    )
    assertEquals(
      instance.toMapOfOutputs(testRegistersOutputs).view.mapValues(_.unsafeRunSync()).toMap,
      expected
    )
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
