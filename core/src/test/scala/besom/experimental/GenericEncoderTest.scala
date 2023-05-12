package besom.experimental.genericEncoder

class GenericEncoderTest extends munit.FunSuite:
  test("unions & lists & maps") {
    case class WrappedInt(x: Int)

    object WrappedInt:
      given Encoder[WrappedInt] = wrappedInt => s"WrappedInt(${wrappedInt.x})"

    def encode[A](a: A)(using enc: Encoder[A]) = enc.encode(a)

    assert(encode(123) == """int(123)""")
    assert(encode("abc") == """string("abc")""")
    assert(encode(WrappedInt(0)) == """WrappedInt(0)""")
    assert(encode(123: Int | String | WrappedInt) == """int(123)""")
    assert(encode("abc": Int | String | WrappedInt) == """string("abc")""")
    assert(encode(WrappedInt(0): Int | String | WrappedInt) == """WrappedInt(0)""")
    assert(encode(List(1, 2, 3))  == """list(int(1), int(2), int(3))""")
    assert(encode(List(1, 2, "abc"): List[Int | String])  == """list(int(1), int(2), string("abc"))""")
    assert(encode(1: Int | List[Int])  == """int(1)""")
    assert(encode(List(1): Int | List[Int])  == """list(int(1))""")
    assert(encode(List(1, List(2, 3)): List[Int | List[Int]])  == """list(int(1), list(int(2), int(3)))""")
    assert(encode(List(1, 2, 3): List[Int] | List[String])  == """list(int(1), int(2), int(3))""")
    assert(encode(List(1, 2, 3): List[Int | String])  == """list(int(1), int(2), int(3))""")
    assert(encode(List("a", "b", "c"): List[Int | String])  == """list(string("a"), string("b"), string("c"))""")
    assert(encode(List("a", "b", "c"): List[Int] | List[String])  == """list(string("a"), string("b"), string("c"))""")
    assert(encode(List(true, false): List[Int] | List[String] | List[Boolean])  == """list(bool(true), bool(false))""")
    assert(encode(Map[String, String | Int]("foo" -> 123, "bar" -> "abcd")) == """map("foo" -> int(123), "bar" -> string("abcd"))""")
  }