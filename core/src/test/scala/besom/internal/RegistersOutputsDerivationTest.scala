package besom.internal

import besom.util.*
import besom.types.*
import RunResult.{given, *}
import com.google.protobuf.struct.Struct

class RegistersOutputsDerivationTest extends munit.FunSuite {
  case class TestRegistersOutputs(a: Output[Int])(using ComponentBase) extends ComponentResource

  test("derive an instance for TestRegistersOutputs") {
    given Context                        = DummyContext().unsafeRunSync()
    given ComponentBase                  = ComponentBase(Output(URN.empty))
    val intEncoder: Encoder[Output[Int]] = summon

    val testRegistersOutputs = TestRegistersOutputs(Output(1))
    val instance             = summon[RegistersOutputs[TestRegistersOutputs]]
    val expectedStruct = Struct(
      Map("a" -> intEncoder.encode(testRegistersOutputs.a).map(_._2).unsafeRunSync())
    )

    assertEquals(
      instance.serializeOutputs(testRegistersOutputs).unsafeRunSync(),
      expectedStruct
    )
  }

  case class TestRegistersOutputs3(aField: Output[Int], alsoAField: Output[String], anotherFields: Output[Boolean])(
    using ComponentBase
  ) extends ComponentResource

  test("derive an instance for TestRegistersOutputs3") {
    given Context                                = DummyContext().unsafeRunSync()
    given ComponentBase                          = ComponentBase(Output(URN.empty))
    val intEncoder: Encoder[Output[Int]]         = summon
    val stringEncoder: Encoder[Output[String]]   = summon
    val booleanEncoder: Encoder[Output[Boolean]] = summon

    val testRegistersOutputs = TestRegistersOutputs3(Output(1), Output("XD"), Output(false))
    val instance             = summon[RegistersOutputs[TestRegistersOutputs3]]
    val expectedStruct = Struct(
      Map(
        "aField" -> intEncoder.encode(testRegistersOutputs.aField).map(_._2).unsafeRunSync(),
        "alsoAField" -> stringEncoder.encode(testRegistersOutputs.alsoAField).map(_._2).unsafeRunSync(),
        "anotherFields" -> booleanEncoder.encode(testRegistersOutputs.anotherFields).map(_._2).unsafeRunSync()
      )
    )
    assertEquals(
      instance.serializeOutputs(testRegistersOutputs).unsafeRunSync(),
      expectedStruct
    )
  }

  test("not derive an instance for a class with non-Output case fields".ignore /* TODO(kπ) */ ) {
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
