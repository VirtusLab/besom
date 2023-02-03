package besom.internal

import RunResult.{given, *}
import com.google.protobuf.struct.*

enum TestEnum derives Encoder:
  case Test1
  case AnotherTest
  case `weird-test`

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
    import besom.api.k8s.*
    given Context = DummyContext().unsafeRunSync()

    val (res, value) = encodeProviderArgs(
      ProviderArgs(
        Output("test"),
        Output(ObjectMetaArgs(name = "to", selfLink = "działa", finalizers = List("Panie", "Lama")))
      )
    )

    println(res)
    println(value)
  }

  test("quick dirty Encoder test - enums") {
    given Context = DummyContext().unsafeRunSync()

    val (res, value) = encode(
      TestEnum.`weird-test`
    )

    println(res)
    println(value)
  }
