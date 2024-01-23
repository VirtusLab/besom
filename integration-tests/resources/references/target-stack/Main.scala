package besom.internal

import besom.*
import besom.json.*

//noinspection UnitMethodIsParameterless,TypeAnnotation
@main def main = Pulumi.run {
  val sourceStackName = config.requireString("sourceStack").map(NonEmptyString(_).get)
  val sourceStack = besom.StackReference(
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
    case Some(JsNumber(s)) => s.toInt
    case other             => throw RuntimeException(s"Expected string, got $other")
  })
  val value2 = sourceStack.flatMap(_.getOutput("value2").map {
    case Some(JsString(s)) => s
    case other             => throw RuntimeException(s"Expected string, got $other")
  })
  val structured = sourceStack.flatMap(_.getOutput("structured"))

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

  Stack(Output(sanityCheck)).exports(
    sshKeyUrn = sshKeyUrn,
    value1 = value1,
    value2 = value2,
    structured = structured
  )
}
