package besom.internal

import besom.*
import besom.json.*

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {

  case class Structured(a: Output[String], b: Double) derives JsonReader, Encoder
  case class SourceStack(sshKeyUrn: String, value1: Int, value2: String, structured: Structured) derives JsonReader

  val sourceStackName = config.requireString("sourceStack").map(NonEmptyString(_).get)
  val sourceStack = StackReference(
    "stackRef",
    StackReferenceArgs(sourceStackName),
    StackReferenceResourceOptions()
  )

  val sshKeyUrn = sourceStack.flatMap(
    _.getOutput("sshKeyUrn")
      .map {
        case Some(JsString(s)) => s
        case other             => throw RuntimeException(s"Expected string, got $other")
      }
      .flatMap(URN.parse)
  )

  val value1 = sourceStack.flatMap(_.getOutput("value1").map {
    case Some(JsNumber(s)) =>
      val i = s.toInt
      assert(i == 23, "value1 should be 23")
      i
    case other => throw RuntimeException(s"Expected string, got $other")
  })

  val value2 = sourceStack.flatMap(_.getOutput("value2").map {
    case Some(JsString(s)) =>
      assert(s == "Hello world!", "value2 should be Hello world!")
      s
    case other => throw RuntimeException(s"Expected string, got $other")
  })

  val structured = sourceStack.flatMap(_.getOutput("structured")).map {
    case Some(js @ JsObject(map)) =>
      assert(map.size == 2, "structured should have 2 fields")
      assert(map.get("a").flatMap(_.asJsObject.fields.get("value")).contains(JsString("ABCDEF")), "structured.a should be ABCDEF")
      assert(map.get("b").map(_.toString.toDouble).contains(42.0), "structured.b should be 42.0")
      js.asInstanceOf[JsValue]
    case Some(_) => throw RuntimeException("structured should be a JsObject")
    case None    => throw RuntimeException("structured should be a JsObject")
  }

  val sanityCheck = Output {
    for
      sku <- sshKeyUrn.getData.map(_.secret)
      v1  <- value1.getData.map(_.secret)
      v2  <- value2.getData.map(_.secret)
      s   <- structured.getData.map(_.secret)
    yield
      assert(!sku, "sshKeyUrn should not be a secret")
      assert(!v1, "value1 should not be a secret")
      assert(!v2, "value2 should not be a secret")
      assert(s, "structured should be a secret")
  }

  val typedSourceStack = StackReference[SourceStack](
    "stackRef",
    StackReferenceArgs(sourceStackName),
    StackReferenceResourceOptions()
  )

  val typedSanityCheck = typedSourceStack.flatMap { sourceStack =>
    val outputs = sourceStack.outputs
    outputs.structured.a.flatMap { a =>
      assert(a == "ABCDEF", "structured.a should be ABCDEF")

      Output {

        assert(
          outputs.sshKeyUrn.startsWith("urn:pulumi:tests-stack-outputs-and-references-should-work") &&
            outputs.sshKeyUrn.endsWith("::source-stack-test::tls:index/privateKey:PrivateKey::sshKey")
        )
        assert(outputs.value1 == 23, "value1 should be 23")
        assert(outputs.value2 == "Hello world!", "value2 should be Hello world!")
        assert(outputs.structured.b == 42.0, "structured.b should be 42.0")
        outputs
      }
    }
  }

  Stack(typedSanityCheck, sanityCheck).exports(
    sshKeyUrn = sshKeyUrn,
    value1 = value1,
    value2 = value2,
    structured = typedSourceStack.map(_.outputs.structured)
  )
}
