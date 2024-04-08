/*
 * Copyright (C) 2011 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package besom.json

import org.specs2.mutable.*

class ProductFormatsSpec extends Specification {

  case class Test0()
  case class Test2(a: Int, b: Option[Double])
  case class Test3[A, B](as: List[A], bs: Option[List[B]] = Some(List.empty))
  case class Test4(t2: Test2)
  case class Test5(optA: Option[String] = Some("default"))
  case class TestTransient(a: Int, b: Option[Double]) {
    @transient var c = false
  }
  @SerialVersionUID(1L) // SerialVersionUID adds a static field to the case class
  case class TestStatic(a: Int, b: Option[Double])
  case class TestMangled(`foo-bar!`: Int, `User ID`: String, `ü$bavf$u56ú$`: Boolean, `-x-`: Int, `=><+-*/!@#%^&~?|`: Float)

  trait TestProtocol {
    this: DefaultJsonProtocol =>
    implicit val test0Format: JsonFormat[Test0]                                         = jsonFormatN[Test0]
    implicit val test2Format: JsonFormat[Test2]                                         = jsonFormatN[Test2]
    implicit def test3Format[A: JsonFormat, B: JsonFormat]: RootJsonFormat[Test3[A, B]] = jsonFormatN[Test3[A, B]]
    implicit def test4Format: JsonFormat[Test4]                                         = jsonFormatN[Test4]
    implicit def test5Format: JsonFormat[Test5]                                         = jsonFormatN[Test5]
    implicit def testTransientFormat: JsonFormat[TestTransient]                         = jsonFormatN[TestTransient]
    implicit def testStaticFormat: JsonFormat[TestStatic]                               = jsonFormatN[TestStatic]
    implicit def testMangledFormat: JsonFormat[TestMangled]                             = jsonFormatN[TestMangled]
  }
  object TestProtocol1 extends DefaultJsonProtocol with TestProtocol
  object TestProtocol2 extends DefaultJsonProtocol with TestProtocol with NullOptions

  case class Foo(a: Int, b: Int)
  object Foo:
    import DefaultJsonProtocol.*
    given JsonFormat[Foo] = jsonFormatN

  "A JsonFormat derived for an inner class" should {
    "compile" in {

      val compileErrors = scala.compiletime.testing.typeCheckErrors(
        """
          class Test:
            case class Foo(a: Int, b: Int)
            object Foo:
              import DefaultJsonProtocol.*
              given JsonFormat[Foo] = jsonFormatN"""
      )

      compileErrors must beEmpty
    }
  }

  "A JsonFormat created with `jsonFormat`, for a case class with 2 elements," should {
    import TestProtocol1.*
    val obj  = Test2(42, Some(4.2))
    val json = JsObject("a" -> JsNumber(42), "b" -> JsNumber(4.2))
    "convert to a respective JsObject" in {
      obj.toJson mustEqual json
    }
    "convert a JsObject to the respective case class instance" in {
      json.convertTo[Test2] mustEqual obj
    }
    "throw a DeserializationException if the JsObject does not all required members" in (
      JsObject("b" -> JsNumber(4.2)).convertTo[Test2] must
        throwA(new DeserializationException("Object is missing required member 'a'"))
    )
    "not require the presence of optional fields for deserialization" in {
      JsObject("a" -> JsNumber(42)).convertTo[Test2] mustEqual Test2(42, None)
    }
    "not render `None` members during serialization" in {
      Test2(42, None).toJson mustEqual JsObject("a" -> JsNumber(42))
    }
    "ignore additional members during deserialization" in {
      JsObject("a" -> JsNumber(42), "b" -> JsNumber(4.2), "c" -> JsString(Symbol("no"))).convertTo[Test2] mustEqual obj
    }
    "not depend on any specific member order for deserialization" in {
      JsObject("b" -> JsNumber(4.2), "a" -> JsNumber(42)).convertTo[Test2] mustEqual obj
    }
    "throw a DeserializationException if the JsValue is not a JsObject" in (
      JsNull.convertTo[Test2] must throwA(new DeserializationException("Object expected"))
    )
    "expose the fieldName in the DeserializationException when able" in {
      JsNull.convertTo[Test2] must throwA[DeserializationException].like { case DeserializationException(_, _, fieldNames) =>
        fieldNames mustEqual "a" :: "b" :: Nil
      }
    }
    "expose all gathered fieldNames in the DeserializationException" in {
      JsObject("t2" -> JsObject("a" -> JsString("foo"))).convertTo[Test4] must throwA[DeserializationException].like {
        case DeserializationException(msg, _, fieldNames) =>
          println(msg)
          fieldNames mustEqual "t2" :: "a" :: Nil
      }
    }
  }

  "A JsonProtocol mixing in NullOptions" should {
    "render `None` members to `null`" in {
      import TestProtocol2.*
      Test2(42, None).toJson mustEqual JsObject("a" -> JsNumber(42), "b" -> JsNull)
    }
  }

  "A JsonFormat for a generic case class and created with `jsonFormat`" should {
    import TestProtocol1.*
    val obj = Test3(42 :: 43 :: Nil, Some("x" :: "y" :: "z" :: Nil))
    val json = JsObject(
      "as" -> JsArray(JsNumber(42), JsNumber(43)),
      "bs" -> JsArray(JsString("x"), JsString("y"), JsString("z"))
    )
    "convert to a respective JsObject" in {
      obj.toJson mustEqual json
    }
    "convert a JsObject to the respective case class instance" in {
      json.convertTo[Test3[Int, String]] mustEqual obj
    }
  }
  "A JsonFormat for a case class with 18 parameters and created with `jsonFormat`" should {
    object Test18Protocol extends DefaultJsonProtocol {
      implicit val test18Format: JsonFormat[Test18] = jsonFormatN[Test18]
    }
    case class Test18(
      a1: String,
      a2: String,
      a3: String,
      a4: String,
      a5: Int,
      a6: String,
      a7: String,
      a8: String,
      a9: String,
      a10: String,
      a11: String,
      a12: Double,
      a13: String,
      a14: String,
      a15: String,
      a16: String,
      a17: String,
      a18: String
    )

    import Test18Protocol.*
    val obj = Test18("a1", "a2", "a3", "a4", 5, "a6", "a7", "a8", "a9", "a10", "a11", 12d, "a13", "a14", "a15", "a16", "a17", "a18")

    val json = JsonParser(
      """{"a1":"a1","a2":"a2","a3":"a3","a4":"a4","a5":5,"a6":"a6","a7":"a7","a8":"a8","a9":"a9","a10":"a10","a11":"a11","a12":12.0,"a13":"a13","a14":"a14","a15":"a15","a16":"a16","a17":"a17","a18":"a18"}"""
    )
    "convert to a respective JsObject" in {
      obj.toJson mustEqual json
    }
    "convert a JsObject to the respective case class instance" in {
      json.convertTo[Test18] mustEqual obj
    }
  }

  "A JsonFormat for a generic case class with an explicitly provided type parameter" should {
    "support the jsonFormatN syntax" in {
      case class Box[A](a: A)
      object BoxProtocol extends DefaultJsonProtocol {
        implicit val boxFormat: JsonFormat[Box[Int]] = jsonFormatN[Box[Int]]
      }
      import BoxProtocol.*
      Box(42).toJson === JsObject(Map("a" -> JsNumber(42)))
    }
  }

  "A JsonFormat for a case class with transient fields and created with `jsonFormat`" should {
    import TestProtocol1.*
    val obj  = TestTransient(42, Some(4.2))
    val json = JsObject("a" -> JsNumber(42), "b" -> JsNumber(4.2))
    "convert to a respective JsObject" in {
      obj.toJson mustEqual json
    }
    "convert a JsObject to the respective case class instance" in {
      json.convertTo[TestTransient] mustEqual obj
    }
  }

  "A JsonFormat for a case class with default parameters and created with `jsonFormat`" should {
    "read case classes with optional members from JSON with missing fields" in {
      import TestProtocol1.*
      JsObject().convertTo[Test5] mustEqual Test5(Some("default"))
    }

    "read a generic case class with optional members from JSON with missing fields" in {
      import TestProtocol1.*
      val json = JsObject("as" -> JsArray(JsNumber(23), JsNumber(5)))

      json.convertTo[Test3[Int, String]] mustEqual Test3(List(23, 5), Some(List.empty))
    }
  }

  "A JsonFormat for a case class with static fields and created with `jsonFormat`" should {
    import TestProtocol1.*
    val obj  = TestStatic(42, Some(4.2))
    val json = JsObject("a" -> JsNumber(42), "b" -> JsNumber(4.2))
    "convert to a respective JsObject" in {
      obj.toJson mustEqual json
    }
    "convert a JsObject to the respective case class instance" in {
      json.convertTo[TestStatic] mustEqual obj
    }
  }

  "A JsonFormat created with `jsonFormat`, for a case class with 0 elements," should {
    import TestProtocol1.*
    val obj  = Test0()
    val json = JsObject()
    "convert to a respective JsObject" in {
      obj.toJson mustEqual json
    }
    "convert a JsObject to the respective case class instance" in {
      json.convertTo[Test0] mustEqual obj
    }
    "ignore additional members during deserialization" in {
      JsObject("a" -> JsNumber(42)).convertTo[Test0] mustEqual obj
    }
    "throw a DeserializationException if the JsValue is not a JsObject" in (
      JsNull.convertTo[Test0] must throwA(new DeserializationException("Object expected"))
    )
  }

  "A JsonFormat created with `jsonFormat`, for a case class with mangled-name members," should {
    import TestProtocol1.*
    val json = """{"ü$bavf$u56ú$":true,"=><+-*/!@#%^&~?|":1.0,"foo-bar!":42,"-x-":26,"User ID":"Karl"}""".parseJson
    "produce the correct JSON" in {
      TestMangled(42, "Karl", true, 26, 1.0f).toJson === json
    }
    "convert a JsObject to the respective case class instance" in {
      json.convertTo[TestMangled] === TestMangled(42, "Karl", true, 26, 1.0f)
    }
  }
}
