package besom.internal

import RunResult.{given, *}
import com.google.protobuf.struct.*

enum TestEnum derives Encoder:
  case Test1
  case AnotherTest
  case `weird-test`

case class PlainCaseClass(data: String, moreData: Int) derives Encoder
case class TestProviderArgs(`type`: Output[String], pcc: Output[PlainCaseClass]) derives ProviderArgsEncoder

class ContextTest extends munit.FunSuite:
  case class TestResource(urn: Output[String], id: Output[String], url: Output[String]) extends CustomResource
  case class AnotherTestResource(urn: Output[String], id: Output[String], url: Output[String]) extends CustomResource

  test("resource identity - empty outputs, same class") {
    given Context = DummyContext().unsafeRunSync()

    val v1 = TestResource(Output.empty(), Output.empty(), Output.empty())
    val v2 = TestResource(Output.empty(), Output.empty(), Output.empty())

    assert(v1 != v2)
    assert(v1 == v1)
    assert(v2 == v2)
  }

  def encodeProviderArgs[A: ProviderArgsEncoder](a: A): (Map[String, Set[Resource]], Value) =
    summon[ProviderArgsEncoder[A]].encode(a, filterOut = _ => false).unsafeRunSync()

  def encodeArgs[A: ArgsEncoder](a: A)(): (Map[String, Set[Resource]], Value) =
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

    // println(res)
    // println(value)
  }

  test("quick dirty Encoder test - enums") {
    given Context = DummyContext().unsafeRunSync()

    val (res, value) = encode(
      TestEnum.`weird-test`
    )

    assert(res.isEmpty)
    assert(value.kind.isStringValue)
    assert(value.getStringValue == "weird-test")
  }
