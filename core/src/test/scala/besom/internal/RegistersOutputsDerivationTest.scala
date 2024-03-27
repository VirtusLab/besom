package besom.internal

import besom.types.*
import RunResult.{given, *}
import com.google.protobuf.struct.Struct

class RegistersOutputsDerivationTest extends munit.FunSuite {
  case class TestRegistersOutputs(a: Output[Int])(using ComponentBase) extends ComponentResource

  runWithBothOutputCodecs {
    test(s"derive an instance for TestRegistersOutputs (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      given ComponentBase = ComponentBase(Output(URN.empty))

      val intEncoder = summon[Encoder[Output[Int]]]
      val instance   = summon[RegistersOutputs[TestRegistersOutputs]]

      val testRegistersOutputs = TestRegistersOutputs(Output(1))
      val serializedStruct     = instance.serializeOutputs(Output(testRegistersOutputs)).unsafeRunSync()
      val expectedStruct = Struct(
        Map("a" -> intEncoder.encode(testRegistersOutputs.a).map(_._2).unsafeRunSync())
      )

      assertEquals(serializedStruct, expectedStruct, serializedStruct.toProtoString)
    }
  }

  case class TestRegistersOutputs3(aField: Output[Int], alsoAField: Output[String], anotherFields: Output[Boolean])(using
    ComponentBase
  ) extends ComponentResource

  runWithBothOutputCodecs {
    test(s"derive an instance for TestRegistersOutputs3 (keepOutputValues: ${Context().featureSupport.keepOutputValues})") {
      given ComponentBase = ComponentBase(Output(URN.empty))

      val intEncoder     = summon[Encoder[Output[Int]]]
      val stringEncoder  = summon[Encoder[Output[String]]]
      val booleanEncoder = summon[Encoder[Output[Boolean]]]
      val instance       = summon[RegistersOutputs[TestRegistersOutputs3]]

      val testRegistersOutputs = TestRegistersOutputs3(Output(1), Output("XD"), Output(false))
      val serializedStruct     = instance.serializeOutputs(Output(testRegistersOutputs)).unsafeRunSync()
      val expectedStruct = Struct(
        Map(
          "aField" -> intEncoder.encode(testRegistersOutputs.aField).map(_._2).unsafeRunSync(),
          "alsoAField" -> stringEncoder.encode(testRegistersOutputs.alsoAField).map(_._2).unsafeRunSync(),
          "anotherFields" -> booleanEncoder.encode(testRegistersOutputs.anotherFields).map(_._2).unsafeRunSync()
        )
      )

      assertEquals(serializedStruct, expectedStruct, serializedStruct.toProtoString)
    }
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
