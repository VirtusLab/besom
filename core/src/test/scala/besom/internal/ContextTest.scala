package besom.internal

import RunResult.{given, *}
import com.google.protobuf.struct.*
import besom.types.{ Output => _, * }

sealed abstract class TestEnum(val name: String, val value: String) extends StringEnum

object TestEnum extends EnumCompanion[TestEnum]("TestEnum"):
  object Test1 extends TestEnum("Test1", "Test1 value")
  object AnotherTest extends TestEnum("AnotherTest", "AnotherTest value")
  object `weird-test` extends TestEnum("weird-test", "weird-test value")

  override val allInstances: Seq[TestEnum] = Seq(
    Test1,
    AnotherTest,
    `weird-test`
  )

case class PlainCaseClass(data: String, moreData: Int) derives Encoder
case class TestArgs(a: Output[String], b: Output[PlainCaseClass]) derives ArgsEncoder
case class TestProviderArgs(`type`: Output[String], pcc: Output[PlainCaseClass]) derives ProviderArgsEncoder

class ContextTest extends munit.FunSuite:
  case class TestResource(urn: Output[URN], id: Output[ResourceId], url: Output[String]) extends CustomResource
  case class AnotherTestResource(urn: Output[URN], id: Output[ResourceId], url: Output[String]) extends CustomResource

  test("resource identity - empty outputs, same class") {
    given Context = DummyContext().unsafeRunSync()

    val v1 = TestResource(Output.empty(), Output.empty(), Output.empty())
    val v2 = TestResource(Output.empty(), Output.empty(), Output.empty())

    assert(v1 != v2)
    assert(v1 == v1)
    assert(v2 == v2)
  }

  def encodeProviderArgs[A: ProviderArgsEncoder](a: A): (Map[String, Set[Resource]], Struct) =
    summon[ProviderArgsEncoder[A]].encode(a, filterOut = _ => false).unsafeRunSync()

  def encodeArgs[A: ArgsEncoder](a: A): (Map[String, Set[Resource]], Struct) =
    summon[ArgsEncoder[A]].encode(a, filterOut = _ => false).unsafeRunSync()

  def encode[A: Encoder](a: A): (Set[Resource], Value) =
    summon[Encoder[A]].encode(a).unsafeRunSync()

  test("quick dirty ProviderArgsEncoder test") {
    given Context = DummyContext().unsafeRunSync()

    val (res, value) = encodeProviderArgs(
      TestProviderArgs(
        Output("SOME-TEST-PROVIDER"),
        Output(PlainCaseClass(data = "werks?", moreData = 123))
      )
    )

    println(res)
    println(value)
  }

  test("ArgsEncoder works") {
    given Context = DummyContext().unsafeRunSync()

    val (res, value) = encodeArgs(
      TestArgs(
        Output("SOME-TEST-PROVIDER"),
        Output(PlainCaseClass(data = "werks?", moreData = 123))
      )
    )

    println(res)
    println(value)
  }

  test("quick dirty Encoder test - enums") {
    given Context = DummyContext().unsafeRunSync()

    val (res, value) = encode[TestEnum](
      TestEnum.`weird-test`
    )

    assert(res.isEmpty)
    assert(value.kind.isStringValue)
    assert(value.getStringValue == "weird-test")
  }
